package nz.co.twg.service.{{cookiecutter.java_package_name}}.componenttest;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import nz.co.twg.common.features.FeaturesSupport;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.componenttest.util.ActuatorFeaturesSupport;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.componenttest.util.ServiceBase;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.openapi.server.model.ErrorV1;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.openapi.server.model.PetV1;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** An example test featuring testing of outbound api calls mocked by wiremock */
class DelegatedApiCallExampleTest extends ServiceBase {

    private final FeaturesSupport featuresSupport =
            new ActuatorFeaturesSupport(getHostname(), getActuatorPort());

    private final HttpClient client = HttpClient.newHttpClient();

    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        featuresSupport.clear();

        WireMock wireMock = new WireMock(Integer.parseInt(getWiremockPort()));
        wireMock.resetMappings();
        WireMock.configureFor(wireMock);
    }

    @Test
    void testDelegatedApiCall() throws Exception {
        // given
        var stubPet = createClientPet(1L, "Danny", "fox", new BigDecimal("9.13"));
        stubFor(
                get(urlEqualTo("/pets"))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(objectMapper.writeValueAsString(List.of(stubPet)))));

        // when
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(
                                        String.format("http://%s:%s/api/remote/pets", getHostname(), getAppPort())))
                        .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String result = response.body();
        TypeReference<List<PetV1>> typeRef = new TypeReference<>() {};
        List<PetV1> pets = objectMapper.readValue(result, typeRef);

        // then
        assertNotNull(pets);
        assertEquals(1, pets.size());
        assertEquals("Danny", pets.get(0).getName());
        assertEquals("fox", pets.get(0).getTag());
    }

    @Test
    void testDelegatedApiCall_404() throws Exception {
        // given
        var stubError = createClientError(404L, "not found");
        stubFor(
                get(urlEqualTo("/pets/1"))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withStatus(404)
                                        .withBody(objectMapper.writeValueAsString(stubError))));

        // when
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(
                                        String.format("http://%s:%s/api/remote/pets/1", getHostname(), getAppPort())))
                        .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String result = response.body();
        TypeReference<List<PetV1>> typeRef = new TypeReference<>() {};
        ErrorV1 error = objectMapper.readValue(result, ErrorV1.class);

        // then
        assertNotNull(error);
        assertEquals(404L, error.getCode());
        assertEquals("not found", error.getMessage());
    }

    // spotless:off
    private nz.co.twg.service.{{cookiecutter.java_package_name}}.openapi.client.model.PetV1
            createClientPet(long id, String name, String tag, BigDecimal costPerDay) {
        var pet = new nz.co.twg.service.{{cookiecutter.java_package_name}}.openapi.client.model.PetV1();
        pet.setId(id);
        pet.setName(name);
        pet.setTag(tag);
        pet.setDateOfBirth(OffsetDateTime.now(ZoneOffset.UTC));
        pet.setMicrochipDate(LocalDate.now(ZoneOffset.UTC));
        pet.setCostPerDay(costPerDay);
        return pet;
    }

    private nz.co.twg.service.{{cookiecutter.java_package_name}}.openapi.client.model.ErrorV1
            createClientError(long code, String message) {
        return new nz.co.twg.service.{{cookiecutter.java_package_name}}.openapi.client.model.ErrorV1()
                .code(code)
                .message(message);
    }
    // spotless:on
}
