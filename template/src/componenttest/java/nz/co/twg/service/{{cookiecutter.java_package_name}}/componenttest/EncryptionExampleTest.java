package nz.co.twg.service.{{cookiecutter.java_package_name}}.componenttest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.componenttest.util.DecryptionMockData;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.componenttest.util.EncryptionMockData;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.componenttest.util.ServiceBase;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.componenttest.util.WireMockHelper;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.config.ObjectMapperSupplier;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
* One of the endpoints accepts and returns an encrypted value. Here it is possible to check the
* value is decrypted in the controller method, is modified and is returned back again encrypted.
*
* <p>Note that in this environment, it is expected that the encryption is mocked and is simply a
* Base64 encoding transformation.
*/
class EncryptionExampleTest extends ServiceBase {

    private final HttpClient client = HttpClient.newHttpClient();

    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        objectMapper = new ObjectMapperSupplier().get();

        WireMock wireMock = new WireMock(Integer.parseInt(getWiremockPort()));
        wireMock.resetMappings();
        WireMock.configureFor(wireMock);
    }

    @Test
    void testRequestResponseCycleWithEncryption_happyDays() throws Exception {
        // given
        String tag = "XYZ999";
        BigDecimal costPerDay = new BigDecimal("10.5");

        String tagEncrypted = Base64.getEncoder().encodeToString(objectMapper.writeValueAsBytes(tag));
        String costPerDayEncrypted =
                Base64.getEncoder().encodeToString(objectMapper.writeValueAsBytes(costPerDay));

        String outputTag = "XYZ999-1";
        BigDecimal outputCostPerDay = new BigDecimal("20.5");
        String outputTagEncrypted =
                Base64.getEncoder().encodeToString(objectMapper.writeValueAsBytes(outputTag));
        String outputCostPerDayEncrypted =
                Base64.getEncoder().encodeToString(objectMapper.writeValueAsBytes(outputCostPerDay));

        WireMockHelper.stubAuthenticationEndpoint();
        WireMockHelper.stubDecryption(
                List.of(
                        new DecryptionMockData("tagEncrypted", tagEncrypted, tag),
                        new DecryptionMockData("costPerDayEncrypted", costPerDayEncrypted, costPerDay)));

        WireMockHelper.stubEncryption(
                List.of(
                        new EncryptionMockData("tag", outputTag, outputTagEncrypted),
                        new EncryptionMockData("costPerDay", outputCostPerDay, outputCostPerDayEncrypted)));

        String payload =
                objectMapper.writeValueAsString(
                        Map.of(
                                "name", "Snowy",
                                "tagEncrypted", tagEncrypted,
                                "costPerDayEncrypted", costPerDayEncrypted));

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(String.format("http://%s:%s/api/pets", getHostName(), getAppPort())))
                        .header("Content-Type", "application/json")
                        .method("POST", HttpRequest.BodyPublishers.ofString(payload))
                        .build();

        // when
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // then
        Assertions.assertThat(response.statusCode()).isEqualTo(200);
        JsonNode resultNode = objectMapper.readTree(response.body());
        Assertions.assertThat(resultNode.get("id").isNumber()).isTrue();
        Assertions.assertThat(resultNode.get("name").asText()).isEqualTo("SNOWY");
        Assertions.assertThat(resultNode.get("costPerDay")).isNull();
        // ^ should be JsonIgnored
        Assertions.assertThat(resultNode.get("tag")).isNull();
        // ^ should be JsonIgnored

        String resultTag =
                objectMapper.readValue(
                        Base64.getDecoder().decode(resultNode.get("tagEncrypted").asText()), String.class);
        Assertions.assertThat(resultTag).isEqualTo("XYZ999-1");

        BigDecimal resultCostPerDay =
                objectMapper.readValue(
                        Base64.getDecoder().decode(resultNode.get("costPerDayEncrypted").asText()),
                        BigDecimal.class);
        Assertions.assertThat(resultCostPerDay).isEqualTo(new BigDecimal("20.5"));
    }

    @Test
    void testRequestResponseCycleWithEncryption_invalidRequest() throws Exception {
        // given
        String tag = "XYZ999";
        BigDecimal costPerDay = new BigDecimal("101.5");
        // ^ too large

        String tagEncrypted = Base64.getEncoder().encodeToString(objectMapper.writeValueAsBytes(tag));
        String costPerDayEncrypted =
                Base64.getEncoder().encodeToString(objectMapper.writeValueAsBytes(costPerDay));

        WireMockHelper.stubAuthenticationEndpoint();

        WireMockHelper.stubDecryption(
                List.of(
                        new DecryptionMockData("tagEncrypted", tagEncrypted, tag),
                        new DecryptionMockData("costPerDayEncrypted", costPerDayEncrypted, costPerDay)));

        String payload =
                objectMapper.writeValueAsString(
                        Map.of(
                                "name", "Snowy",
                                "tagEncrypted", tagEncrypted,
                                "costPerDayEncrypted", costPerDayEncrypted));

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(String.format("http://%s:%s/api/pets", getHostName(), getAppPort())))
                        .header("Content-Type", "application/json")
                        .method("POST", HttpRequest.BodyPublishers.ofString(payload))
                        .build();

        // when
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // then
        Assertions.assertThat(response.statusCode()).isEqualTo(400);
        // ^ bad request.
    }

    /**
    * In this example, we know that the "cost per day" has a maximum of 100 and the API will add +10
    * on to this value in the response. This way it is possible to fabricate a request that will
    * yield an invalid response.
    */
    @Test
    void testRequestResponseCycleWithEncryption_invalidResponse() throws Exception {
        // given
        String tag = "XYZ999";
        BigDecimal costPerDay = new BigDecimal("99.5");
        // ^ will exceed 100 when +10 is added.

        String tagEncrypted = Base64.getEncoder().encodeToString(objectMapper.writeValueAsBytes(tag));
        String costPerDayEncrypted =
                Base64.getEncoder().encodeToString(objectMapper.writeValueAsBytes(costPerDay));

        String outputTag = "XYZ999-1";
        BigDecimal outputCostPerDay = new BigDecimal("109.5");
        String outputTagEncrypted =
                Base64.getEncoder().encodeToString(objectMapper.writeValueAsBytes(outputTag));
        String outputCostPerDayEncrypted =
                Base64.getEncoder().encodeToString(objectMapper.writeValueAsBytes(outputCostPerDay));

        WireMockHelper.stubAuthenticationEndpoint();

        WireMockHelper.stubDecryption(
                List.of(
                        new DecryptionMockData("tagEncrypted", tagEncrypted, tag),
                        new DecryptionMockData("costPerDayEncrypted", costPerDayEncrypted, costPerDay)));

        WireMockHelper.stubEncryption(
                List.of(
                        new EncryptionMockData("tag", outputTag, outputTagEncrypted),
                        new EncryptionMockData("costPerDay", outputCostPerDay, outputCostPerDayEncrypted)));

        String payload =
                objectMapper.writeValueAsString(
                        Map.of(
                                "name", "Snowy",
                                "tagEncrypted", tagEncrypted,
                                "costPerDayEncrypted", costPerDayEncrypted));

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(String.format("http://%s:%s/api/pets", getHostName(), getAppPort())))
                        .header("Content-Type", "application/json")
                        .method("POST", HttpRequest.BodyPublishers.ofString(payload))
                        .build();

        // when
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // then
        Assertions.assertThat(response.statusCode()).isEqualTo(500);
        // ^ the application producing a bad response is a 500.
    }
}
