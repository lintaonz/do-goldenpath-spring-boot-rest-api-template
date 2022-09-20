package nz.co.twg.common.encryption;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import nz.co.twg.schema.exception.DecryptionException;
import nz.co.twg.schema.exception.EncryptionException;
import nz.co.twg.schema.spi.EncryptionProcessingResult;
import nz.co.twg.schema.spi.EncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* An implementation of {@link EncryptionService} that interfaces with the encryption microservice.
* It will first retrieve the access token from the configured IdP, then use the token as the Bearer
* token to communicate with the encryption microservice.
*/
public class EncryptionServiceVaultProxy implements EncryptionService {

    private Logger logger = LoggerFactory.getLogger(EncryptionServiceVaultProxy.class);

    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String HEADER_AUTHORIZATION = "Authorization";

    private static final String DECRYPTION_UNEXPECTED_RESPONSE_CODE_TEMPLATE =
            "failed to decrypt; unexpected response code: [%d]; body: [%s]";
    private static final String ENCRYPTION_UNEXPECTED_RESPONSE_CODE_TEMPLATE =
            "failed to encrypt; unexpected response code: [%d]; body: [%s]";

    private static final String RETRIEVE_TOKEN_UNEXPECTED_RESPONSE_CODE_TEMPLATE =
            "failed to retrieve access token; unexpected response code: [%d]; body: [%s]";

    private final Duration cachedAccessTokenPreemptiveExpiryDuration;

    private final EncryptionServiceApiConfig apiConfig;

    private final HttpClient client;

    private final ObjectMapper objectMapper;

    private volatile String cachedAccessToken;

    private Clock clock = Clock.systemUTC();

    private final Lock tokenLock = new ReentrantLock();

    /** Constructor. */
    public EncryptionServiceVaultProxy(EncryptionServiceApiConfig apiConfig) {
        if (apiConfig == null) {
            throw new IllegalArgumentException("apiConfig cannot be null");
        }
        this.apiConfig = apiConfig;

        this.cachedAccessTokenPreemptiveExpiryDuration =
                Duration.of(apiConfig.getCachedAccessTokenPreemptiveExpirySeconds(), ChronoUnit.SECONDS);
        this.client = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    // exposed for testing purpose
    EncryptionServiceVaultProxy withClock(Clock clock) {
        this.clock = clock;
        return this;
    }

    // exposed for testing purpose
    EncryptionServiceVaultProxy withToken(String token) {
        this.cachedAccessToken = token;
        return this;
    }

    // exposed for testing purpose
    EncryptionServiceVaultProxy withLogger(Logger logger) {
        this.logger = logger;
        return this;
    }

    @Override
    public Map<String, EncryptionProcessingResult> decrypt(
            String encryptionKeyId, Map<String, String> values) {
        return performAction(
                encryptionKeyId,
                values,
                () -> {
                    try {
                        HttpRequest httpRequest =
                                prepareEncryptDecryptPostRequest("decrypt", encryptionKeyId, values);
                        HttpResponse<String> response =
                                this.client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                        return handleDecryptionResponse(response);
                    } catch (DecryptionException e) {
                        throw e;
                    } catch (JsonProcessingException e) {
                        throw new DecryptionException(
                                "failed to convert the key value pairs to decrypt into JSON payload", e);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new DecryptionException(
                                "failed to contact the vault proxy service; thread interrupted", e);
                    } catch (IOException e) {
                        throw new DecryptionException("failed to contact the vault proxy service", e);
                    } catch (AuthException e) {
                        throw new DecryptionException(
                                "failed to retrieve token from the IdP to communicate with the vault proxy service",
                                e);
                    } catch (Exception e) {
                        throw new DecryptionException("unexpected exception occurred during decryption", e);
                    }
                });
    }

    private Map<String, EncryptionProcessingResult> handleDecryptionResponse(
            HttpResponse<String> response) throws JsonProcessingException {
        if (!is2XX(response.statusCode())) { // any non 200 response
            if (isUnauthorized(response.statusCode()) || isForbidden(response.statusCode())) {
                invalidateToken();
            }
            if (logger.isTraceEnabled()) {
                logger.trace(
                        String.format(
                                DECRYPTION_UNEXPECTED_RESPONSE_CODE_TEMPLATE,
                                response.statusCode(),
                                response.body()));
            }
            throw new DecryptionException(
                    String.format(
                            DECRYPTION_UNEXPECTED_RESPONSE_CODE_TEMPLATE, response.statusCode(), "<omitted>"));
        }
        if (response.body() != null && !response.body().isBlank()) {
            Map<String, Object> body =
                    this.objectMapper.readValue(response.body(), new TypeReference<>() {});
            Map<String, EncryptionProcessingResult> output =
                    convertVaultProxyBodyToCanonicalModel(body, "failedToDecrypt");
            logger.debug("decrypted [{}] values", output.size());
            return output;
        }
        throw new DecryptionException("failed to decrypt. unexpected response body: [<null>]");
    }

    @Override
    public Map<String, EncryptionProcessingResult> encrypt(
            String encryptionKeyId, Map<String, String> values) {
        return performAction(
                encryptionKeyId,
                values,
                () -> {
                    try {
                        HttpRequest httpRequest =
                                prepareEncryptDecryptPostRequest("encrypt", encryptionKeyId, values);
                        HttpResponse<String> response =
                                this.client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                        return handleEncryptionResponse(response);
                    } catch (EncryptionException e) {
                        throw e;
                    } catch (JsonProcessingException e) {
                        throw new EncryptionException(
                                "failed to convert the key value pairs to encrypt into JSON payload", e);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new EncryptionException(
                                "failed to contact the vault proxy service; thread interrupted", e);
                    } catch (IOException e) {
                        throw new EncryptionException("failed to contact the vault proxy service", e);
                    } catch (AuthException e) {
                        throw new EncryptionException(
                                "failed to retrieve token from the IdP to communicate with the vault proxy service",
                                e);
                    } catch (Exception e) {
                        throw new EncryptionException("unexpected exception occurred during encryption", e);
                    }
                });
    }

    private Map<String, EncryptionProcessingResult> handleEncryptionResponse(
            HttpResponse<String> response) throws JsonProcessingException {
        if (!is2XX(response.statusCode())) {
            if (isUnauthorized(response.statusCode()) || isForbidden(response.statusCode())) {
                invalidateToken();
            }
            if (logger.isTraceEnabled()) {
                logger.trace(
                        String.format(
                                ENCRYPTION_UNEXPECTED_RESPONSE_CODE_TEMPLATE,
                                response.statusCode(),
                                response.body()));
            }
            throw new EncryptionException(
                    String.format(
                            ENCRYPTION_UNEXPECTED_RESPONSE_CODE_TEMPLATE, response.statusCode(), "<omitted>"));
        }
        if (response.body() != null && !response.body().isBlank()) {
            Map<String, Object> body =
                    this.objectMapper.readValue(response.body(), new TypeReference<>() {});
            Map<String, EncryptionProcessingResult> output =
                    convertVaultProxyBodyToCanonicalModel(body, "failedToEncrypt");
            logger.debug("encrypted [{}] values", output.size());
            return output;
        }
        throw new EncryptionException("failed to decrypt. unexpected response body: [<null>]");
    }

    /** Handles the precondition checks for both encrypt and decrypt actions. */
    private Map<String, EncryptionProcessingResult> performAction(
            String encryptionKeyId,
            Map<String, String> values,
            Supplier<Map<String, EncryptionProcessingResult>> supplier) {

        if (encryptionKeyId == null || encryptionKeyId.isBlank()) {
            throw new IllegalArgumentException("encryptionKeyId cannot be null or blank");
        }
        if (values == null) {
            throw new IllegalArgumentException("values cannot be null");
        }
        if (values.isEmpty()) {
            return Collections.emptyMap();
        }
        return supplier.get();
    }

    private String getAccessToken() throws AuthException {
        tokenLock.lock();
        try {
            if (!verifyToken(cachedAccessToken)) {
                logger.debug("token is invalid. acquiring new token from idp.");
                cachedAccessToken = getTokenFromIdp();
            }
            return cachedAccessToken;
        } finally {
            tokenLock.unlock();
        }
    }

    /**
    * Inspect and verify if the token is valid. <br>
    * Currently only checks the token locally by inspecting the <code>exp</code> attribute and
    * comparing it against the current time (with a 5 min buffered time. If the token is too close to
    * expiry, flag as invalid).
    *
    * @param token the token to verify.
    * @return true if token is still valid, false otherwise.
    */
    // exposed for testing
    boolean verifyToken(String token) {
        if (token != null) {
            try {
                // assume token to be JWT
                String[] tokenSections = token.split("\\.");
                if (tokenSections.length == 3) {
                    String payloadJson =
                            new String(Base64.getDecoder().decode(tokenSections[1]), StandardCharsets.UTF_8);
                    Map<String, Object> payload =
                            objectMapper.readValue(payloadJson, new TypeReference<>() {});
                    long expiry = ((Number) payload.getOrDefault("exp", 0L)).longValue();
                    // deem the token invalid if it is close to expiring
                    Instant expiryInstant =
                            Instant.ofEpochSecond(expiry).minus(cachedAccessTokenPreemptiveExpiryDuration);
                    return clock.instant().isBefore(expiryInstant);
                } else {
                    logger.warn(
                            "detected JWT token with incorrect number of segments; expected [3]; actual [{}]",
                            tokenSections.length);
                }
            } catch (Exception e) {
                // any exception or error will result in the token being invalidated
                logger.warn("unexpected exception occurred while verifying the cached token", e);
            }
        }
        return false;
    }

    // exposed for testing
    void invalidateToken() {
        tokenLock.lock();
        try {
            this.cachedAccessToken = null;
            logger.debug("the cached token has been invalidated.");
        } finally {
            tokenLock.unlock();
        }
    }

    /**
    * Retrieve the token from the configured IdP (Identity Provider).
    *
    * @return the newly issued token
    * @throws AuthException if any error or unexpected state is found while retrieving the token
    */
    // exposed for testing
    String getTokenFromIdp() throws AuthException {
        AuthConfig authConfig = apiConfig.getAuth();

        Map<String, String> paramMap = new LinkedHashMap<>();
        paramMap.put("grant_type", "client_credentials");
        paramMap.put("client_id", authConfig.getClientId());
        paramMap.put("client_secret", authConfig.getClientSecret());
        paramMap.put("scope", authConfig.getScope());

        String param =
                paramMap.entrySet().stream()
                        .map(
                                entry ->
                                        String.join(
                                                "=",
                                                URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8),
                                                URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8)))
                        .collect(Collectors.joining("&"));

        String url = authConfig.getTokenUrl();

        try {
            logger.debug(
                    "will retrieve token from IdP. token endpoint: [{}]; client id: [{}]; scope: [{}]",
                    url,
                    authConfig.getClientId(),
                    authConfig.getScope());
            Map<String, String> headers =
                    Map.of(HEADER_CONTENT_TYPE, "application/x-www-form-urlencoded;charset=UTF-8");

            HttpRequest.BodyPublisher requestBody = HttpRequest.BodyPublishers.ofString(param);
            HttpRequest.Builder requestBuilder =
                    HttpRequest.newBuilder().uri(URI.create(url)).POST(requestBody);
            headers.forEach(requestBuilder::header);
            HttpRequest request = requestBuilder.build();
            HttpResponse<String> response =
                    this.client.send(request, HttpResponse.BodyHandlers.ofString());
            if (!is2XX(response.statusCode())) {
                throw new AuthException(
                        String.format(
                                RETRIEVE_TOKEN_UNEXPECTED_RESPONSE_CODE_TEMPLATE,
                                response.statusCode(),
                                response.body()));
            }
            if (response.body() != null) {
                Map<String, String> body =
                        this.objectMapper.readValue(response.body(), new TypeReference<>() {});
                String accessToken = body.get("access_token");
                if (accessToken == null || accessToken.isBlank()) {
                    throw new AuthException(
                            "failed to retrieve access token. " + "response body does not have 'access_token'");
                }
                logger.debug("successfully retrieved token from IdP.");
                return accessToken;
            }
            throw new AuthException(
                    "failed to retrieve access token. unexpected response body: [<null>]");
        } catch (AuthException e) {
            throw e;
        } catch (JsonProcessingException e) {
            throw new AuthException(
                    "failed to convert the key value pairs to encrypt into JSON payload", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AuthException("failed to contact the IdP; thread interrupted", e);
        } catch (IOException e) {
            throw new AuthException("failed to contact the IdP", e);
        } catch (Exception e) {
            throw new AuthException("unexpected exception occurred during access token acquisition", e);
        }
    }

    private Map<String, EncryptionProcessingResult> convertVaultProxyBodyToCanonicalModel(
            Map<String, Object> body, String failureKeyName) {
        Map<String, EncryptionProcessingResult> output = new HashMap<>();
        body.forEach(
                (key, value) -> {
                    if (failureKeyName.equals(key)) {
                        Map<String, String> failures = (Map<String, String>) value;
                        failures.forEach(
                                (failureKey, failureReason) ->
                                        output.put(failureKey, EncryptionProcessingResult.failure(failureReason)));
                    } else {
                        output.put(key, EncryptionProcessingResult.success((String) value));
                    }
                });
        return output;
    }

    /**
    * Check if the response code is in the successful range (2XX).
    *
    * @param statusCode the status code to check.
    * @return whether the response code is in the successful range.
    */
    private boolean is2XX(int statusCode) {
        return (statusCode / 100) == 2;
    }

    private boolean isUnauthorized(int statusCode) {
        return statusCode == 401;
    }

    private boolean isForbidden(int statusCode) {
        return statusCode == 403;
    }

    private HttpRequest prepareEncryptDecryptPostRequest(
            String action, String encryptionKeyId, Object body)
            throws AuthException, JsonProcessingException {

        String url =
                String.format(
                        "%s/%s/%s",
                        this.apiConfig.getBaseUrl(),
                        action,
                        URLEncoder.encode(encryptionKeyId, StandardCharsets.UTF_8));

        String accessToken = getAccessToken();
        Map<String, String> headers =
                Map.of(
                        HEADER_CONTENT_TYPE,
                        "application/json;charset=UTF-8",
                        HEADER_AUTHORIZATION,
                        "Bearer " + accessToken);
        HttpRequest.BodyPublisher requestBody =
                HttpRequest.BodyPublishers.ofString(this.objectMapper.writeValueAsString(body));

        HttpRequest.Builder requestBuilder =
                HttpRequest.newBuilder().uri(URI.create(url)).POST(requestBody);

        headers.forEach(requestBuilder::header);
        return requestBuilder.build();
    }
}
