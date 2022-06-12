package nz.co.twg.service.{{cookiecutter.java_package_name}}.componenttest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
import nz.co.twg.service.{{cookiecutter.java_package_name}}.FeatureFlag;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.componenttest.util.ActuatorFeaturesSupport;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.componenttest.util.ServiceBase;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.openapi.clients.thirdpartyapi.model.AnimalV1;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.openapi.server.model.PetV1;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** An example test featuring testing of feature toggle dependent code */
class FeatureFlagAndWireMockExampleTest extends ServiceBase {

    private final FeaturesSupport featuresSupport =
            new ActuatorFeaturesSupport(getHostName(), getActuatorPort());

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
    void testUppercaseNameFeature_on() throws Exception {
        // given
        // spotless:off
        FeatureFlag flag = FeatureFlag.{{cookiecutter.artifact_id|upper|replace("-", "_")}}_UPPERCASE_NAME;
        // spotless:on
        featuresSupport.configure(flag.toString(), true);

        // when
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(String.format("http://%s:%s/api/pets/1", getHostName(), getAppPort())))
                        .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String result = response.body();
        PetV1 pet = objectMapper.reader().readValue(result, PetV1.class);

        // then
        assertEquals("DUMBO", pet.getName());
    }

    @Test
    void testUppercaseNameFeature_off() throws Exception {
        // given
        // spotless:off
        FeatureFlag flag = FeatureFlag.{{cookiecutter.artifact_id|upper|replace("-", "_")}}_UPPERCASE_NAME;
        // spotless:on
        featuresSupport.configure(flag.toString(), false);

        // when
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(String.format("http://%s:%s/api/pets/1", getHostName(), getAppPort())))
                        .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String result = response.body();
        PetV1 pet = objectMapper.reader().readValue(result, PetV1.class);

        // then
        assertEquals("Dumbo", pet.getName());
    }

    @Test
    void testIncludeDogsFromThirdParty_on() throws Exception {
        // given

        // spotless:off
        FeatureFlag flag = FeatureFlag.{{cookiecutter.artifact_id|upper|replace("-", "_")}}_INCLUDE_DOGS_FROM_THIRD_PARTY;
        // spotless:on
        featuresSupport.configure(flag.toString(), true);

        AnimalV1 stubAnimal = createAnimal(1L, "Danny", "dog", new BigDecimal("9.13"));
        // wiremock stubbing
        stubFor(
                get(urlEqualTo("/animals/dog"))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(objectMapper.writeValueAsString(List.of(stubAnimal)))));

        // when
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(String.format("http://%s:%s/api/pets", getHostName(), getAppPort())))
                        .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String result = response.body();
        TypeReference<List<PetV1>> typeRef = new TypeReference<>() {};
        List<PetV1> pets = objectMapper.readValue(result, typeRef);

        // then
        assertEquals(3, pets.size());
        assertEquals("Caspurr", pets.get(0).getName());
        assertEquals("Pluto", pets.get(1).getName());
        assertEquals("Danny", pets.get(2).getName());
    }

    private AnimalV1 createAnimal(long id, String name, String tag, BigDecimal costPerDay) {
        var animal = new AnimalV1();
        animal.setId(id);
        animal.setName(name);
        animal.setTag(tag);
        animal.setDateOfBirth(OffsetDateTime.now(ZoneOffset.UTC));
        animal.setMicrochipDate(LocalDate.now(ZoneOffset.UTC));
        animal.setCostPerDay(costPerDay);
        return animal;
    }
}
