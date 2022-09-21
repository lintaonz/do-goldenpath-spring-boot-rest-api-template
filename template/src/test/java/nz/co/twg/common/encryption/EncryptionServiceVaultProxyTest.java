package nz.co.twg.common.encryption;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import nz.co.twg.schema.exception.DecryptionException;
import nz.co.twg.schema.exception.EncryptionException;
import nz.co.twg.schema.spi.EncryptionProcessingResult;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;

class EncryptionServiceVaultProxyTest {

    private MockWebServer server;

    @BeforeEach
    void setUp() {
        server = new MockWebServer();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.close();
    }

    @Test
    void invalidConfig() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new EncryptionServiceVaultProxy(null),
                "expected " + IllegalArgumentException.class + " to be thrown");
    }

    @Test
    void invalidParameter() throws AuthException {
        // given
        AuthConfig authConfig =
                new AuthConfig(
                        "http://localhost/auth", "test-client-id", "test-client-secret&=", "api://test-scope");
        EncryptionServiceApiConfig encryptionServiceApiConfig =
                createEncryptionServiceApiConfig("http://localhost/vault-proxy", authConfig);
        EncryptionServiceVaultProxy encryptionService =
                spy(new EncryptionServiceVaultProxy(encryptionServiceApiConfig));
        Map<String, String> encryptionPayload = Map.of("a", "decryptedA");
        Map<String, String> decryptionPayload = Map.of("a", "encryptedA");

        // when + then
        assertThrows(
                IllegalArgumentException.class, () -> encryptionService.encrypt(null, encryptionPayload));
        assertThrows(
                IllegalArgumentException.class,
                () -> encryptionService.encrypt("test-encryption-key-id", null));
        assertThrows(
                IllegalArgumentException.class, () -> encryptionService.decrypt(null, decryptionPayload));
        assertThrows(
                IllegalArgumentException.class,
                () -> encryptionService.decrypt("test-encryption-key-id", null));

        verify(encryptionService, never()).getTokenFromIdp();
    }

    @Test
    void testEncryption_emptyMapToEncrypt() throws AuthException {
        // given
        AuthConfig authConfig =
                new AuthConfig(
                        "http://localhost/auth", "test-client-id", "test-client-secret&=", "api://test-scope");
        EncryptionServiceApiConfig encryptionServiceApiConfig =
                createEncryptionServiceApiConfig("http://localhost/vault-proxy", authConfig);
        EncryptionServiceVaultProxy encryptionService =
                spy(new EncryptionServiceVaultProxy(encryptionServiceApiConfig));

        // when
        Map<String, EncryptionProcessingResult> result =
                encryptionService.encrypt("test-encryption-key-id", Map.of());

        // then
        assertTrue(result.isEmpty());
        verify(encryptionService, never()).getTokenFromIdp();
    }

    @Test
    void testEncryption_happyPath() throws AuthException, InterruptedException {
        // given

        // now = curr time = 1672484400 seconds since epoch = 2023-01-01T00:00:00
        Clock clock = Clock.fixed(Instant.ofEpochSecond(1672484400L), ZoneOffset.UTC);
        // exp = expire at = 4102398000 seconds since epoch = 2100-01-01T00:00:00
        String accessToken = createJwtToken(4102398000L);

        final Dispatcher dispatcher =
                new Dispatcher() {

                    @Override
                    public @NotNull MockResponse dispatch(RecordedRequest request) {

                        if (request.getPath() != null) {
                            switch (request.getPath()) {
                                case "/auth/oauth2/token":
                                    return new MockResponse()
                                            .setResponseCode(200)
                                            .setBody("{\"access_token\": \"" + accessToken + "\"}");
                                case "/vault-proxy/encrypt/test-key-id":
                                    return new MockResponse().setResponseCode(200).setBody("{\"a\": \"encryptedA\"}");
                                default:
                            }
                        }
                        return new MockResponse().setResponseCode(404);
                    }
                };
        server.setDispatcher(dispatcher);
        String tokenUrl = server.url("/auth/oauth2/token").toString();
        String encryptionServiceApiBaseUrl = server.url("/vault-proxy").toString();

        AuthConfig authConfig =
                new AuthConfig(tokenUrl, "test-client-id", "test-client-secret&=", "api://test-scope");
        EncryptionServiceApiConfig encryptionServiceApiConfig =
                createEncryptionServiceApiConfig(encryptionServiceApiBaseUrl, authConfig);

        EncryptionServiceVaultProxy encryptionService =
                spy(new EncryptionServiceVaultProxy(encryptionServiceApiConfig).withClock(clock));

        // when
        Map<String, EncryptionProcessingResult> output =
                encryptionService.encrypt("test-key-id", Map.of("a", "decryptedA"));

        // then
        assertEquals(1, output.size());
        assertTrue(output.containsKey("a"));
        assertNotNull(output.get("a"));

        EncryptionProcessingResult result = output.get("a");
        assertTrue(result.isSuccessful());
        assertEquals("encryptedA", result.getValue());

        verify(encryptionService).verifyToken(null);
        verify(encryptionService).getTokenFromIdp();

        RecordedRequest request1 = server.takeRequest();
        assertEquals("/auth/oauth2/token", request1.getPath());

        // the body contains leading and trailing double quotes
        String body = request1.getBody().readUtf8().replaceAll("^\"|\"$", "");
        Map<String, String> requestBody = splitQuery(body);
        assertEquals("client_credentials", requestBody.get("grant_type"));
        assertEquals("test-client-id", requestBody.get("client_id"));
        assertEquals("test-client-secret%26%3D", requestBody.get("client_secret"));
        assertEquals("api%3A%2F%2Ftest-scope", requestBody.get("scope"));

        RecordedRequest request2 = server.takeRequest();
        assertEquals("/vault-proxy/encrypt/test-key-id", request2.getPath());
        assertEquals("Bearer " + accessToken, request2.getHeader("Authorization"));
    }

    @Test
    void testEncryption_partialFailure() throws AuthException, InterruptedException {
        // given

        // now = curr time = 1672484400 seconds since epoch = 2023-01-01T00:00:00
        Clock clock = Clock.fixed(Instant.ofEpochSecond(1672484400L), ZoneOffset.UTC);
        // exp = expire at = 4102398000 seconds since epoch = 2100-01-01T00:00:00
        String accessToken = createJwtToken(4102398000L);

        final Dispatcher dispatcher =
                new Dispatcher() {

                    @Override
                    public @NotNull MockResponse dispatch(RecordedRequest request) {
                        if (request.getPath() != null) {
                            switch (request.getPath()) {
                                case "/auth/oauth2/token":
                                    return new MockResponse()
                                            .setResponseCode(200)
                                            .setBody("{\"access_token\": \"" + accessToken + "\"}");
                                case "/vault-proxy/encrypt/test-key-id":
                                    return new MockResponse()
                                            .setResponseCode(200)
                                            .setBody(
                                                    "{"
                                                            + "\"a\": \"encryptedA\", "
                                                            + "\"failedToEncrypt\": {\"b\": \"something went wrong!\"}"
                                                            + "}");
                                default:
                            }
                        }
                        return new MockResponse().setResponseCode(404);
                    }
                };
        server.setDispatcher(dispatcher);
        String tokenUrl = server.url("/auth/oauth2/token").toString();
        String encryptionServiceApiBaseUrl = server.url("/vault-proxy").toString();

        AuthConfig authConfig =
                new AuthConfig(tokenUrl, "test-client-id", "test-client-secret", "test-scope");
        EncryptionServiceApiConfig encryptionServiceApiConfig =
                createEncryptionServiceApiConfig(encryptionServiceApiBaseUrl, authConfig);

        EncryptionServiceVaultProxy encryptionService =
                spy(new EncryptionServiceVaultProxy(encryptionServiceApiConfig).withClock(clock));

        // when
        Map<String, EncryptionProcessingResult> output =
                encryptionService.encrypt("test-key-id", Map.of("a", "decryptedA", "b", "decryptedB"));

        // then
        assertEquals(2, output.size());
        assertTrue(output.containsKey("a"));
        assertTrue(output.containsKey("b"));

        EncryptionProcessingResult resultA = output.get("a");
        assertTrue(resultA.isSuccessful());
        assertEquals("encryptedA", resultA.getValue());

        EncryptionProcessingResult resultB = output.get("b");
        assertFalse(resultB.isSuccessful());
        assertEquals("something went wrong!", resultB.getReason());

        verify(encryptionService).verifyToken(null);
        verify(encryptionService).getTokenFromIdp();

        RecordedRequest request1 = server.takeRequest();
        assertEquals("/auth/oauth2/token", request1.getPath());

        RecordedRequest request2 = server.takeRequest();
        assertEquals("/vault-proxy/encrypt/test-key-id", request2.getPath());
        assertEquals("Bearer " + accessToken, request2.getHeader("Authorization"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "false", "\"hello\""})
    void testEncryption_invalidResponseBody(String body) throws AuthException, InterruptedException {
        // given

        // now = curr time = 1672484400 seconds since epoch = 2023-01-01T00:00:00
        Clock clock = Clock.fixed(Instant.ofEpochSecond(1672484400L), ZoneOffset.UTC);
        // exp = expire at = 4102398000 seconds since epoch = 2100-01-01T00:00:00
        String accessToken = createJwtToken(4102398000L);

        final Dispatcher dispatcher =
                new Dispatcher() {

                    @Override
                    public @NotNull MockResponse dispatch(RecordedRequest request) {
                        if (request.getPath() != null) {
                            switch (request.getPath()) {
                                case "/auth/oauth2/token":
                                    return new MockResponse()
                                            .setResponseCode(200)
                                            .setBody("{\"access_token\": \"" + accessToken + "\"}");
                                case "/vault-proxy/encrypt/test-key-id":
                                    return new MockResponse().setResponseCode(200).setBody(body);
                                default:
                            }
                        }
                        return new MockResponse().setResponseCode(404);
                    }
                };
        server.setDispatcher(dispatcher);
        String tokenUrl = server.url("/auth/oauth2/token").toString();
        String encryptionServiceApiBaseUrl = server.url("/vault-proxy").toString();

        AuthConfig authConfig =
                new AuthConfig(tokenUrl, "test-client-id", "test-client-secret", "test-scope");
        EncryptionServiceApiConfig encryptionServiceApiConfig =
                createEncryptionServiceApiConfig(encryptionServiceApiBaseUrl, authConfig);

        EncryptionServiceVaultProxy encryptionService =
                spy(new EncryptionServiceVaultProxy(encryptionServiceApiConfig).withClock(clock));

        // when
        Map<String, String> payload = Map.of("a", "decryptedA");
        assertThrows(
                EncryptionException.class,
                () -> encryptionService.encrypt("test-key-id", payload),
                "expected " + EncryptionException.class + " to be thrown");

        // then
        verify(encryptionService).verifyToken(null);
        verify(encryptionService).getTokenFromIdp();

        RecordedRequest request1 = server.takeRequest();
        assertEquals("/auth/oauth2/token", request1.getPath());

        RecordedRequest request2 = server.takeRequest();
        assertEquals("/vault-proxy/encrypt/test-key-id", request2.getPath());
        assertEquals("Bearer " + accessToken, request2.getHeader("Authorization"));
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 403})
    void testEncryption_unauthorizedAndForbidden(int responseCode)
            throws AuthException, InterruptedException {
        // given

        // now = curr time = 1672484400 seconds since epoch = 2023-01-01T00:00:00
        Clock clock = Clock.fixed(Instant.ofEpochSecond(1672484400L), ZoneOffset.UTC);
        // exp = expire at = 4102398000 seconds since epoch = 2100-01-01T00:00:00
        String accessToken = createJwtToken(4102398000L);

        final Dispatcher dispatcher =
                new Dispatcher() {

                    @Override
                    public @NotNull MockResponse dispatch(RecordedRequest request) {
                        if (request.getPath() != null) {
                            switch (request.getPath()) {
                                case "/auth/oauth2/token":
                                    return new MockResponse()
                                            .setResponseCode(200)
                                            .setBody("{\"access_token\": \"" + accessToken + "\"}");
                                case "/vault-proxy/encrypt/test-key-id":
                                    return new MockResponse().setResponseCode(responseCode);
                                default:
                            }
                        }
                        return new MockResponse().setResponseCode(404);
                    }
                };
        server.setDispatcher(dispatcher);
        String tokenUrl = server.url("/auth/oauth2/token").toString();
        String encryptionServiceApiBaseUrl = server.url("/vault-proxy").toString();

        AuthConfig authConfig =
                new AuthConfig(tokenUrl, "test-client-id", "test-client-secret", "test-scope");
        EncryptionServiceApiConfig encryptionServiceApiConfig =
                createEncryptionServiceApiConfig(encryptionServiceApiBaseUrl, authConfig);
        Logger logger = mock(Logger.class);
        when(logger.isTraceEnabled()).thenReturn(false);

        EncryptionServiceVaultProxy encryptionService =
                spy(
                        new EncryptionServiceVaultProxy(encryptionServiceApiConfig)
                                .withClock(clock)
                                .withLogger(logger));

        // when
        Map<String, String> payload = Map.of("a", "decryptedA");
        assertThrows(
                EncryptionException.class,
                () -> encryptionService.encrypt("test-key-id", payload),
                "expected " + EncryptionException.class + " to be thrown");

        // then
        verify(encryptionService).verifyToken(null);
        verify(encryptionService).getTokenFromIdp();
        verify(encryptionService).invalidateToken();
        verify(logger, never()).trace(anyString());

        RecordedRequest request1 = server.takeRequest();
        assertEquals("/auth/oauth2/token", request1.getPath());

        RecordedRequest request2 = server.takeRequest();
        assertEquals("/vault-proxy/encrypt/test-key-id", request2.getPath());
        assertEquals("Bearer " + accessToken, request2.getHeader("Authorization"));
    }

    @ParameterizedTest
    @ValueSource(ints = {301, 400, 419, 500, 501, 502})
    void testEncryption_unsuccessful(int responseCode) throws AuthException, InterruptedException {
        // given

        // now = curr time = 1672484400 seconds since epoch = 2023-01-01T00:00:00
        Clock clock = Clock.fixed(Instant.ofEpochSecond(1672484400L), ZoneOffset.UTC);
        // exp = expire at = 4102398000 seconds since epoch = 2100-01-01T00:00:00
        String accessToken = createJwtToken(4102398000L);

        final Dispatcher dispatcher =
                new Dispatcher() {

                    @Override
                    public @NotNull MockResponse dispatch(RecordedRequest request) {
                        if (request.getPath() != null) {
                            switch (request.getPath()) {
                                case "/auth/oauth2/token":
                                    return new MockResponse()
                                            .setResponseCode(200)
                                            .setBody("{\"access_token\": \"" + accessToken + "\"}");
                                case "/vault-proxy/encrypt/test-key-id":
                                    return new MockResponse().setResponseCode(responseCode);
                                default:
                            }
                        }
                        return new MockResponse().setResponseCode(404);
                    }
                };
        server.setDispatcher(dispatcher);
        String tokenUrl = server.url("/auth/oauth2/token").toString();
        String encryptionServiceApiBaseUrl = server.url("/vault-proxy").toString();

        AuthConfig authConfig =
                new AuthConfig(tokenUrl, "test-client-id", "test-client-secret", "test-scope");
        EncryptionServiceApiConfig encryptionServiceApiConfig =
                createEncryptionServiceApiConfig(encryptionServiceApiBaseUrl, authConfig);
        Logger logger = mock(Logger.class);
        when(logger.isTraceEnabled()).thenReturn(true);

        EncryptionServiceVaultProxy encryptionService =
                spy(
                        new EncryptionServiceVaultProxy(encryptionServiceApiConfig)
                                .withClock(clock)
                                .withLogger(logger));

        // when + then
        Map<String, String> payload = Map.of("a", "decryptedA");
        assertThrows(
                EncryptionException.class,
                () -> encryptionService.encrypt("test-key-id", payload),
                "expected " + EncryptionException.class + " to be thrown");

        // then
        verify(encryptionService).verifyToken(null);
        verify(encryptionService).getTokenFromIdp();
        verify(encryptionService, never()).invalidateToken();
        verify(logger).trace(anyString());

        RecordedRequest request1 = server.takeRequest();
        assertEquals("/auth/oauth2/token", request1.getPath());

        RecordedRequest request2 = server.takeRequest();
        assertEquals("/vault-proxy/encrypt/test-key-id", request2.getPath());
        assertEquals("Bearer " + accessToken, request2.getHeader("Authorization"));
    }

    @Test
    void testDecryption_emptyMapToEncrypt() throws AuthException {
        // given
        AuthConfig authConfig =
                new AuthConfig(
                        "http://localhost/auth", "test-client-id", "test-client-secret&=", "api://test-scope");
        EncryptionServiceApiConfig encryptionServiceApiConfig =
                createEncryptionServiceApiConfig("http://localhost/vault-proxy", authConfig);
        EncryptionServiceVaultProxy encryptionService =
                spy(new EncryptionServiceVaultProxy(encryptionServiceApiConfig));

        // when
        Map<String, EncryptionProcessingResult> result =
                encryptionService.decrypt("test-encryption-key-id", Map.of());

        // then
        assertTrue(result.isEmpty());
        verify(encryptionService, never()).getTokenFromIdp();
    }

    @Test
    void testDecryption_happyPath() throws AuthException, InterruptedException {
        // given

        // now = curr time = 1672484400 seconds since epoch = 2023-01-01T00:00:00
        Clock clock = Clock.fixed(Instant.ofEpochSecond(1672484400L), ZoneOffset.UTC);
        // exp = expire at = 4102398000 seconds since epoch = 2100-01-01T00:00:00
        String accessToken = createJwtToken(4102398000L);

        final Dispatcher dispatcher =
                new Dispatcher() {

                    @Override
                    public @NotNull MockResponse dispatch(RecordedRequest request) {
                        if (request.getPath() != null) {
                            switch (request.getPath()) {
                                case "/auth/oauth2/token":
                                    return new MockResponse()
                                            .setResponseCode(200)
                                            .setBody("{\"access_token\": \"" + accessToken + "\"}");
                                case "/vault-proxy/decrypt/test-key-id":
                                    return new MockResponse().setResponseCode(200).setBody("{\"a\": \"decryptedA\"}");
                                default:
                            }
                        }
                        return new MockResponse().setResponseCode(404);
                    }
                };
        server.setDispatcher(dispatcher);
        String tokenUrl = server.url("/auth/oauth2/token").toString();
        String encryptionServiceApiBaseUrl = server.url("/vault-proxy").toString();

        AuthConfig authConfig =
                new AuthConfig(tokenUrl, "test-client-id", "test-client-secret", "test-scope");
        EncryptionServiceApiConfig encryptionServiceApiConfig =
                createEncryptionServiceApiConfig(encryptionServiceApiBaseUrl, authConfig);

        EncryptionServiceVaultProxy encryptionService =
                spy(new EncryptionServiceVaultProxy(encryptionServiceApiConfig).withClock(clock));

        // when
        Map<String, EncryptionProcessingResult> output =
                encryptionService.decrypt("test-key-id", Map.of("a", "encryptedA"));

        // then
        assertEquals(1, output.size());
        assertTrue(output.containsKey("a"));
        assertNotNull(output.get("a"));

        EncryptionProcessingResult result = output.get("a");
        assertTrue(result.isSuccessful());
        assertEquals("decryptedA", result.getValue());

        verify(encryptionService).verifyToken(null);
        verify(encryptionService).getTokenFromIdp();

        RecordedRequest request1 = server.takeRequest();
        assertEquals("/auth/oauth2/token", request1.getPath());

        RecordedRequest request2 = server.takeRequest();
        assertEquals("/vault-proxy/decrypt/test-key-id", request2.getPath());
        assertEquals("Bearer " + accessToken, request2.getHeader("Authorization"));
    }

    @Test
    void testDecryption_partialFailure() throws AuthException, InterruptedException {
        // given

        // now = curr time = 1672484400 seconds since epoch = 2023-01-01T00:00:00
        Clock clock = Clock.fixed(Instant.ofEpochSecond(1672484400L), ZoneOffset.UTC);
        // exp = expire at = 4102398000 seconds since epoch = 2100-01-01T00:00:00
        String accessToken = createJwtToken(4102398000L);

        final Dispatcher dispatcher =
                new Dispatcher() {

                    @Override
                    public @NotNull MockResponse dispatch(RecordedRequest request) {
                        if (request.getPath() != null) {
                            switch (request.getPath()) {
                                case "/auth/oauth2/token":
                                    return new MockResponse()
                                            .setResponseCode(200)
                                            .setBody("{\"access_token\": \"" + accessToken + "\"}");
                                case "/vault-proxy/decrypt/test-key-id":
                                    return new MockResponse()
                                            .setResponseCode(200)
                                            .setBody(
                                                    "{"
                                                            + "\"a\": \"encryptedA\","
                                                            + "\"failedToDecrypt\": {\"b\": \"something went wrong!\"}"
                                                            + "}");
                                default:
                            }
                        }
                        return new MockResponse().setResponseCode(404);
                    }
                };
        server.setDispatcher(dispatcher);
        String tokenUrl = server.url("/auth/oauth2/token").toString();
        String encryptionServiceApiBaseUrl = server.url("/vault-proxy").toString();

        AuthConfig authConfig =
                new AuthConfig(tokenUrl, "test-client-id", "test-client-secret", "test-scope");
        EncryptionServiceApiConfig encryptionServiceApiConfig =
                createEncryptionServiceApiConfig(encryptionServiceApiBaseUrl, authConfig);

        EncryptionServiceVaultProxy encryptionService =
                spy(new EncryptionServiceVaultProxy(encryptionServiceApiConfig).withClock(clock));

        // when
        Map<String, EncryptionProcessingResult> output =
                encryptionService.decrypt("test-key-id", Map.of("a", "encryptedA", "b", "encryptedB"));

        // then
        assertEquals(2, output.size());
        assertTrue(output.containsKey("a"));
        assertTrue(output.containsKey("b"));

        EncryptionProcessingResult resultA = output.get("a");
        assertTrue(resultA.isSuccessful());
        assertEquals("encryptedA", resultA.getValue());

        EncryptionProcessingResult resultB = output.get("b");
        assertFalse(resultB.isSuccessful());
        assertEquals("something went wrong!", resultB.getReason());

        verify(encryptionService).verifyToken(null);
        verify(encryptionService).getTokenFromIdp();

        RecordedRequest request1 = server.takeRequest();
        assertEquals("/auth/oauth2/token", request1.getPath());

        RecordedRequest request2 = server.takeRequest();
        assertEquals("/vault-proxy/decrypt/test-key-id", request2.getPath());
        assertEquals("Bearer " + accessToken, request2.getHeader("Authorization"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "false", "\"hello\""})
    void testDecryption_invalidResponseBody(String body) throws AuthException, InterruptedException {
        // given

        // now = curr time = 1672484400 seconds since epoch = 2023-01-01T00:00:00
        Clock clock = Clock.fixed(Instant.ofEpochSecond(1672484400L), ZoneOffset.UTC);
        // exp = expire at = 4102398000 seconds since epoch = 2100-01-01T00:00:00
        String accessToken = createJwtToken(4102398000L);

        final Dispatcher dispatcher =
                new Dispatcher() {

                    @Override
                    public @NotNull MockResponse dispatch(RecordedRequest request) {
                        if (request.getPath() != null) {
                            switch (request.getPath()) {
                                case "/auth/oauth2/token":
                                    return new MockResponse()
                                            .setResponseCode(200)
                                            .setBody("{\"access_token\": \"" + accessToken + "\"}");
                                case "/vault-proxy/decrypt/test-key-id":
                                    return new MockResponse().setResponseCode(200).setBody(body);
                                default:
                            }
                        }
                        return new MockResponse().setResponseCode(404);
                    }
                };
        server.setDispatcher(dispatcher);
        String tokenUrl = server.url("/auth/oauth2/token").toString();
        String encryptionServiceApiBaseUrl = server.url("/vault-proxy").toString();

        AuthConfig authConfig =
                new AuthConfig(tokenUrl, "test-client-id", "test-client-secret", "test-scope");
        EncryptionServiceApiConfig encryptionServiceApiConfig =
                createEncryptionServiceApiConfig(encryptionServiceApiBaseUrl, authConfig);

        EncryptionServiceVaultProxy encryptionService =
                spy(new EncryptionServiceVaultProxy(encryptionServiceApiConfig).withClock(clock));

        // when
        Map<String, String> payload = Map.of("a", "encryptedA");
        assertThrows(
                DecryptionException.class,
                () -> encryptionService.decrypt("test-key-id", payload),
                "expected " + DecryptionException.class + " to be thrown");

        // then
        verify(encryptionService).verifyToken(null);
        verify(encryptionService).getTokenFromIdp();

        RecordedRequest request1 = server.takeRequest();
        assertEquals("/auth/oauth2/token", request1.getPath());

        RecordedRequest request2 = server.takeRequest();
        assertEquals("/vault-proxy/decrypt/test-key-id", request2.getPath());
        assertEquals("Bearer " + accessToken, request2.getHeader("Authorization"));
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 403})
    void testDecryption_unauthorizedAndForbidden(int responseCode)
            throws AuthException, InterruptedException {
        // given

        // now = curr time = 1672484400 seconds since epoch = 2023-01-01T00:00:00
        Clock clock = Clock.fixed(Instant.ofEpochSecond(1672484400L), ZoneOffset.UTC);
        // exp = expire at = 4102398000 seconds since epoch = 2100-01-01T00:00:00
        String accessToken = createJwtToken(4102398000L);

        final Dispatcher dispatcher =
                new Dispatcher() {

                    @Override
                    public @NotNull MockResponse dispatch(RecordedRequest request) {
                        if (request.getPath() != null) {
                            switch (request.getPath()) {
                                case "/auth/oauth2/token":
                                    return new MockResponse()
                                            .setResponseCode(200)
                                            .setBody("{\"access_token\": \"" + accessToken + "\"}");
                                case "/vault-proxy/decrypt/test-key-id":
                                    return new MockResponse().setResponseCode(responseCode);
                                default:
                            }
                        }
                        return new MockResponse().setResponseCode(404);
                    }
                };
        server.setDispatcher(dispatcher);
        String tokenUrl = server.url("/auth/oauth2/token").toString();
        String encryptionServiceApiBaseUrl = server.url("/vault-proxy").toString();

        AuthConfig authConfig =
                new AuthConfig(tokenUrl, "test-client-id", "test-client-secret", "test-scope");
        EncryptionServiceApiConfig encryptionServiceApiConfig =
                createEncryptionServiceApiConfig(encryptionServiceApiBaseUrl, authConfig);
        Logger logger = mock(Logger.class);
        when(logger.isTraceEnabled()).thenReturn(false);

        EncryptionServiceVaultProxy encryptionService =
                spy(
                        new EncryptionServiceVaultProxy(encryptionServiceApiConfig)
                                .withClock(clock)
                                .withLogger(logger));

        // when
        Map<String, String> payload = Map.of("a", "encryptedA");
        assertThrows(
                DecryptionException.class,
                () -> encryptionService.decrypt("test-key-id", payload),
                "expected " + DecryptionException.class + " to be thrown");

        // then
        verify(encryptionService).verifyToken(null);
        verify(encryptionService).getTokenFromIdp();
        verify(encryptionService).invalidateToken();
        verify(logger, never()).trace(anyString());

        RecordedRequest request1 = server.takeRequest();
        assertEquals("/auth/oauth2/token", request1.getPath());

        RecordedRequest request2 = server.takeRequest();
        assertEquals("/vault-proxy/decrypt/test-key-id", request2.getPath());
        assertEquals("Bearer " + accessToken, request2.getHeader("Authorization"));
    }

    @ParameterizedTest
    @ValueSource(ints = {301, 400, 419, 500, 501, 502})
    void testDecryption_unsuccessful(int responseCode) throws AuthException, InterruptedException {
        // given

        // now = curr time = 1672484400 seconds since epoch = 2023-01-01T00:00:00
        Clock clock = Clock.fixed(Instant.ofEpochSecond(1672484400L), ZoneOffset.UTC);
        // exp = expire at = 4102398000 seconds since epoch = 2100-01-01T00:00:00
        String accessToken = createJwtToken(4102398000L);

        final Dispatcher dispatcher =
                new Dispatcher() {

                    @Override
                    public @NotNull MockResponse dispatch(RecordedRequest request) {
                        if (request.getPath() != null) {
                            switch (request.getPath()) {
                                case "/auth/oauth2/token":
                                    return new MockResponse()
                                            .setResponseCode(200)
                                            .setBody("{\"access_token\": \"" + accessToken + "\"}");
                                case "/vault-proxy/decrypt/test-key-id":
                                    return new MockResponse().setResponseCode(responseCode);
                                default:
                            }
                        }
                        return new MockResponse().setResponseCode(404);
                    }
                };
        server.setDispatcher(dispatcher);
        String tokenUrl = server.url("/auth/oauth2/token").toString();
        String encryptionServiceApiBaseUrl = server.url("/vault-proxy").toString();

        AuthConfig authConfig =
                new AuthConfig(tokenUrl, "test-client-id", "test-client-secret", "test-scope");
        EncryptionServiceApiConfig encryptionServiceApiConfig =
                createEncryptionServiceApiConfig(encryptionServiceApiBaseUrl, authConfig);
        Logger logger = mock(Logger.class);
        when(logger.isTraceEnabled()).thenReturn(true);

        EncryptionServiceVaultProxy encryptionService =
                spy(
                        new EncryptionServiceVaultProxy(encryptionServiceApiConfig)
                                .withClock(clock)
                                .withLogger(logger));

        // when
        Map<String, String> payload = Map.of("a", "encryptedA");
        assertThrows(
                DecryptionException.class,
                () -> encryptionService.decrypt("test-key-id", payload),
                "expected " + DecryptionException.class + " to be thrown");

        // then
        verify(encryptionService).verifyToken(null);
        verify(encryptionService).getTokenFromIdp();
        verify(encryptionService, never()).invalidateToken();
        verify(logger).trace(anyString());

        RecordedRequest request1 = server.takeRequest();
        assertEquals("/auth/oauth2/token", request1.getPath());

        RecordedRequest request2 = server.takeRequest();
        assertEquals("/vault-proxy/decrypt/test-key-id", request2.getPath());
        assertEquals("Bearer " + accessToken, request2.getHeader("Authorization"));
    }

    @Test
    void testAccessTokenRetrievalFailure() {
        // given
        final Dispatcher dispatcher =
                new Dispatcher() {

                    @Override
                    public @NotNull MockResponse dispatch(RecordedRequest request) {

                        if ("/auth/oauth2/token".equals(request.getPath())) {
                            return new MockResponse().setResponseCode(500);
                        }
                        return new MockResponse().setResponseCode(404);
                    }
                };
        server.setDispatcher(dispatcher);
        String tokenUrl = server.url("/auth/oauth2/token").toString();
        String encryptionServiceApiBaseUrl = server.url("/vault-proxy").toString();

        AuthConfig authConfig =
                new AuthConfig(tokenUrl, "test-client-id", "test-client-secret", "test-scope");
        EncryptionServiceApiConfig encryptionServiceApiConfig =
                createEncryptionServiceApiConfig(encryptionServiceApiBaseUrl, authConfig);

        EncryptionServiceVaultProxy encryptionService =
                spy(
                        new EncryptionServiceVaultProxy(encryptionServiceApiConfig)
                                .withClock(Clock.fixed(Instant.ofEpochSecond(1672484400L), ZoneOffset.UTC)));

        // when + then
        Map<String, String> encryptionPayload = Map.of("a", "decryptedA");
        assertThrows(
                EncryptionException.class,
                () -> encryptionService.encrypt("test-key-id", encryptionPayload),
                "expected " + EncryptionException.class + " to be thrown");

        Map<String, String> decryptionPayload = Map.of("a", "encryptedA");
        assertThrows(
                DecryptionException.class,
                () -> encryptionService.decrypt("test-key-id", decryptionPayload),
                "expected " + DecryptionException.class + " to be thrown");
    }

    /**
    * This test will setup the {@link EncryptionServiceVaultProxy} with an expired access token to
    * check that it will observe the expiry and send a request to the IDP to pickup a new one.
    */
    @Test
    void testExpiredAccessToken() throws AuthException, InterruptedException {
        // given

        // now = curr time = 1672484400 seconds since epoch = 2023-01-01T00:00:00
        Clock clock = Clock.fixed(Instant.ofEpochSecond(1672484400L), ZoneOffset.UTC);
        // exp = expire at = 1672484399 seconds since epoch = 2022-12-31T23:59:59
        String expiredToken = createJwtToken(1672484399L);
        // new = new token = 4102398000 seconds since epoch = 2100-01-01T00:00:00
        String newToken = createJwtToken(4102398000L);

        final Dispatcher dispatcher =
                new Dispatcher() {

                    @Override
                    public @NotNull MockResponse dispatch(RecordedRequest request) {
                        if (request.getPath() != null) {
                            switch (request.getPath()) {
                                case "/auth/oauth2/token":
                                    return new MockResponse()
                                            .setResponseCode(200)
                                            .setBody("{\"access_token\": \"" + newToken + "\"}");
                                case "/vault-proxy/encrypt/test-key-id":
                                    return new MockResponse().setResponseCode(200).setBody("{\"a\": \"encryptedA\"}");
                                default:
                            }
                        }
                        return new MockResponse().setResponseCode(404);
                    }
                };
        server.setDispatcher(dispatcher);
        String tokenUrl = server.url("/auth/oauth2/token").toString();
        String encryptionServiceApiBaseUrl = server.url("/vault-proxy").toString();

        AuthConfig authConfig =
                new AuthConfig(tokenUrl, "test-client-id", "test-client-secret", "test-scope");
        EncryptionServiceApiConfig encryptionServiceApiConfig =
                createEncryptionServiceApiConfig(encryptionServiceApiBaseUrl, authConfig);

        EncryptionServiceVaultProxy encryptionService =
                spy(
                        new EncryptionServiceVaultProxy(encryptionServiceApiConfig)
                                .withClock(clock)
                                .withToken(expiredToken));

        // when
        Map<String, EncryptionProcessingResult> output =
                encryptionService.encrypt("test-key-id", Map.of("a", "decryptedA"));

        // then
        assertEquals(1, output.size());
        assertTrue(output.containsKey("a"));
        assertNotNull(output.get("a"));

        EncryptionProcessingResult result = output.get("a");
        assertTrue(result.isSuccessful());
        assertEquals("encryptedA", result.getValue());

        verify(encryptionService).verifyToken(expiredToken);
        verify(encryptionService).getTokenFromIdp();

        RecordedRequest request1 = server.takeRequest();
        assertEquals("/auth/oauth2/token", request1.getPath());

        RecordedRequest request2 = server.takeRequest();
        assertEquals("/vault-proxy/encrypt/test-key-id", request2.getPath());
        assertEquals("Bearer " + newToken, request2.getHeader("Authorization"));
    }

    @Test
    void testValidToken() throws AuthException, InterruptedException {
        // given

        // now = curr time = 1672480800 seconds since epoch = 2022-12-31T23:00:00
        Clock clock = Clock.fixed(Instant.ofEpochSecond(1672480800L), ZoneOffset.UTC);

        // exp = expire at = 1672484400 seconds since epoch = 2023-01-01T00:00:00
        String currentToken = createJwtToken(1672484400L);

        final Dispatcher dispatcher =
                new Dispatcher() {

                    @Override
                    public @NotNull MockResponse dispatch(RecordedRequest request) {

                        if ("/vault-proxy/encrypt/test-key-id".equals(request.getPath())) {
                            return new MockResponse().setResponseCode(200).setBody("{\"a\": \"encryptedA\"}");
                        }
                        return new MockResponse().setResponseCode(404);
                    }
                };
        server.setDispatcher(dispatcher);
        String tokenUrl = server.url("/auth/oauth2/token").toString();
        String encryptionServiceApiBaseUrl = server.url("/vault-proxy").toString();

        AuthConfig authConfig =
                new AuthConfig(tokenUrl, "test-client-id", "test-client-secret", "test-scope");
        EncryptionServiceApiConfig encryptionServiceApiConfig =
                createEncryptionServiceApiConfig(encryptionServiceApiBaseUrl, authConfig);

        EncryptionServiceVaultProxy encryptionService =
                spy(
                        new EncryptionServiceVaultProxy(encryptionServiceApiConfig)
                                .withClock(clock)
                                .withToken(currentToken));

        // when
        Map<String, EncryptionProcessingResult> output =
                encryptionService.encrypt("test-key-id", Map.of("a", "decryptedA"));

        // then
        assertEquals(1, output.size());
        assertTrue(output.containsKey("a"));
        assertNotNull(output.get("a"));

        EncryptionProcessingResult result = output.get("a");
        assertTrue(result.isSuccessful());
        assertEquals("encryptedA", result.getValue());

        verify(encryptionService).verifyToken(currentToken);
        verify(encryptionService, never()).getTokenFromIdp();

        RecordedRequest request1 = server.takeRequest();
        assertEquals("/vault-proxy/encrypt/test-key-id", request1.getPath());
        assertEquals("Bearer " + currentToken, request1.getHeader("Authorization"));
    }

    private String createJwtToken(long expireAt) {
        return String.join(
                ".",
                List.of(
                        Base64.getEncoder()
                                .encodeToString(
                                        "{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8)),

                        // iat = issued at = 1640948400 seconds since epoch = 2022-01-01T00:00:00
                        Base64.getEncoder()
                                .encodeToString(
                                        ("{\"sub\":\"1234567890\",\"iat\":1640948400,\"exp\":" + expireAt + "}")
                                                .getBytes(StandardCharsets.UTF_8)),
                        Base64.getEncoder()
                                .encodeToString("{\"blah\":\"blah\"}".getBytes(StandardCharsets.UTF_8))));
    }

    private EncryptionServiceApiConfig createEncryptionServiceApiConfig(
            String baseUrl, AuthConfig authConfig) {
        EncryptionServiceApiConfig encryptionServiceApiConfig = new EncryptionServiceApiConfig();
        encryptionServiceApiConfig.setBaseUrl(baseUrl);
        encryptionServiceApiConfig.setAuth(authConfig);
        encryptionServiceApiConfig.setCachedAccessTokenPreemptiveExpirySeconds(300);

        return encryptionServiceApiConfig;
    }

    public static Map<String, String> splitQuery(String query) {
        Map<String, String> queryPairs = new LinkedHashMap<>();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            queryPairs.put(
                    URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8),
                    pair.substring(idx + 1));
        }
        return queryPairs;
    }
}
