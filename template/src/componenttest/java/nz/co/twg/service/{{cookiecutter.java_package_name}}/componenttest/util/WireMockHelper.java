package nz.co.twg.service.{{cookiecutter.java_package_name}}.componenttest.util;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.function.Function;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.config.ObjectMapperSupplier;
import org.springframework.http.MediaType;

public class WireMockHelper {

    public static final String HEADER_AUTHORIZATION = "Authorization";

    public static final String VALID_ENCRYPTION_SERVICE_ACCESS_TOKEN =
            // {
            //  "alg": "RS256",
            //  "typ": "JWT",
            //  "kid": "test-kid"
            // }
            "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6InRlc3Qta2lkIn0."
                    +
                    // {
                    //  "aud": "my-service",
                    //  "iss": "mock-idp",
                    //  "iat": 1640948400, <---- issued at - 2022-01-01T00:00:00
                    //  "nbf": 1640948400, <---- not valid before - 2022-01-01T00:00:00
                    //  "exp": 2145870000, <---- expire at - 2038-01-01T00:00:00
                    //  "roles": [
                    //    "encrypt.getting-started",
                    //    "decrypt.getting-started"
                    //  ]
                    // }
                    "eyJhdWQiOiJteS1zZXJ2aWNlIiwiaXNzIjoibW9jay1pZHAiLCJpYXQiOjE2NDA5NDg0MDAsIm5iZiI6MTY0MDk0ODQwMCwiZXhwIjoyMTQ1ODcwMDAwLCJyb2xlcyI6WyJlbmNyeXB0LmdldHRpbmctc3RhcnRlZCIsImRlY3J5cHQuZ2V0dGluZy1zdGFydGVkIl19."
                    + "LGZdQrluYDkYXHpb7vhrbEHV7mxbC50yTpqS7R5AqscBkM93Ul5htGnQNwhwfr35kHZJ6p5X6l9fREiju8Ro8i4dP7JHoWbl5qtKZ-9HnaiSLzzoBk7O5DLDU2EjhXP1lkzSdYqUYafg-cdNAcoo7yd97G7JHJCM9zngStwDj-SjNANhoXeEQNsSIeZJaoTwISdfdJ4YeFafIi9-EPcEisrOpwmsVK32_G_bonsGolXM-hquNChrIFH_09-UTb16AuDxyrcc-htb5GL4SBOBuvmTfWu5SmZl70wq0cHHPELKpWeWLTgAsnZ9yGgjwPwtxJ7modwH5OccVwfFJszWCw";

    /**
    * The application will authenticate with an identity provider in order to get a JWT access token
    * to send on to the TWG encryption service. This method will setup an endpoint on WireMock in
    * order to return an access token.
    */
    public static void stubAuthenticationEndpoint() {
        stubFor(
                WireMock.post(urlEqualTo("/auth/oauth2/token"))
                        .withRequestBody(
                                equalTo(
                                        "grant_type=client_credentials&"
                                                + "client_id=test-client-id&"
                                                + "client_secret=test-client-secret&"
                                                + "scope=test-scope"))
                        .willReturn(
                                WireMock.aResponse()
                                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                        .withBody(
                                                "{\"access_token\": \"" + VALID_ENCRYPTION_SERVICE_ACCESS_TOKEN + "\"}")
                                        .withStatus(200)));
    }

    /**
    * This method will setup WireMock stubs for simulating the TWG Encryption Service when it
    * encrypts some data. Each piece of data to encrypt is described by one of the objects in the
    * supplied list.
    */
    public static void stubEncryption(List<EncryptionMockData> encryptions) {
        ObjectMapper objectMapper = new ObjectMapperSupplier().get();
        stubFor(
                WireMock.post(urlEqualTo("/vault-proxy/encrypt/getting-started"))
                        .withHeader(
                                WireMockHelper.HEADER_AUTHORIZATION,
                                equalTo("Bearer " + WireMockHelper.VALID_ENCRYPTION_SERVICE_ACCESS_TOKEN))
                        .withRequestBody(
                                equalToJson(prepareEncryptionExpectedRequestPayload(objectMapper, encryptions)))
                        .willReturn(
                                WireMock.aResponse()
                                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                        // for encryption, the keys are built based on the decrypted fields
                                        .withBody(prepareEncryptionExpectedResponsePayload(objectMapper, encryptions))
                                        .withStatus(200)));
    }

    /**
    * This method will setup WireMock stubs for simulating the TWG Encryption Service when it
    * decrypts some data. Each piece of data to decrypt is described by one of the objects in the
    * supplied list.
    */
    public static void stubDecryption(List<DecryptionMockData> decryptions) {
        ObjectMapper objectMapper = new ObjectMapperSupplier().get();
        stubFor(
                WireMock.post(urlEqualTo("/vault-proxy/decrypt/getting-started"))
                        .withHeader(
                                WireMockHelper.HEADER_AUTHORIZATION,
                                equalTo("Bearer " + WireMockHelper.VALID_ENCRYPTION_SERVICE_ACCESS_TOKEN))
                        .withRequestBody(
                                equalToJson(prepareDecryptionExpectedRequestPayload(objectMapper, decryptions)))
                        .willReturn(
                                WireMock.aResponse()
                                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                        // for decryption, the keys are built based on the encrypted fields
                                        .withBody(prepareDecryptionExpectedResponsePayload(objectMapper, decryptions))
                                        .withStatus(200)));
    }

    private static String prepareEncryptionExpectedRequestPayload(
            ObjectMapper objectMapper, List<EncryptionMockData> encryptions) {
        return prepareEncryptionExpectedPayload(
                objectMapper,
                encryptions,
                emd -> {
                    try {
                        return objectMapper.writeValueAsString(emd.getSource());
                    } catch (IOException ioe) {
                        throw new UncheckedIOException(ioe);
                    }
                });
    }

    private static String prepareEncryptionExpectedResponsePayload(
            ObjectMapper objectMapper, List<EncryptionMockData> encryptions) {
        return prepareEncryptionExpectedPayload(
                objectMapper, encryptions, EncryptionMockData::getTarget);
    }

    private static String prepareEncryptionExpectedPayload(
            ObjectMapper objectMapper,
            List<EncryptionMockData> encryptions,
            Function<EncryptionMockData, String> valueFn) {
        StringWriter output = new StringWriter();

        try {
            JsonGenerator generator = objectMapper.createGenerator(output);
            generator.writeStartObject();
            encryptions.forEach(
                    emd -> {
                        try {
                            generator.writeStringField(emd.getUniqueId(), valueFn.apply(emd));
                        } catch (IOException ioe) {
                            throw new UncheckedIOException(ioe);
                        }
                    });
            generator.writeEndObject();
            generator.flush();
            output.flush();
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }

        return output.toString();
    }

    private static String prepareDecryptionExpectedRequestPayload(
            ObjectMapper objectMapper, List<DecryptionMockData> decryptions) {
        return prepareDecryptionExpectedPayload(
                objectMapper, decryptions, DecryptionMockData::getSource);
    }

    private static String prepareDecryptionExpectedResponsePayload(
            ObjectMapper objectMapper, List<DecryptionMockData> decryptions) {
        return prepareDecryptionExpectedPayload(
                objectMapper,
                decryptions,
                dmd -> {
                    try {
                        return objectMapper.writeValueAsString(dmd.getTarget());
                    } catch (IOException ioe) {
                        throw new UncheckedIOException(ioe);
                    }
                });
    }

    private static String prepareDecryptionExpectedPayload(
            ObjectMapper objectMapper,
            List<DecryptionMockData> decryptions,
            Function<DecryptionMockData, String> valueFn) {
        StringWriter output = new StringWriter();

        try {
            JsonGenerator generator = objectMapper.createGenerator(output);
            generator.writeStartObject();
            decryptions.forEach(
                    emd -> {
                        try {
                            generator.writeStringField(emd.getUniqueId(), valueFn.apply(emd));
                        } catch (IOException ioe) {
                            throw new UncheckedIOException(ioe);
                        }
                    });
            generator.writeEndObject();
            generator.flush();
            output.flush();
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }

        return output.toString();
    }
}
