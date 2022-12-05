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
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import nz.co.twg.common.features.FeaturesSupport;
import nz.co.twg.schema.wrapper.DecryptedClearValue;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.FeatureFlag;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.componenttest.util.ActuatorFeaturesSupport;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.componenttest.util.DecryptionMockData;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.componenttest.util.EncryptionMockData;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.componenttest.util.ServiceBase;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.componenttest.util.WireMockHelper;
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

        WireMockHelper.stubAuthenticationEndpoint();

        // when the application sends an outbound payload it will want to encrypt
        // the data and these calls would be made from the application to the
        // TWG encryption service to perform those encryptions.

        WireMockHelper.stubEncryption(
                List.of(
                        new EncryptionMockData("costPerDay", new BigDecimal("50.001"), "MzIxLjMy"),
                        // ^ target is 321.32
                        new EncryptionMockData("tag", "DUMBO123", "IlRBTkdPNSI=")
                        // ^ target is TANGO5
                        ));

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

        WireMockHelper.stubAuthenticationEndpoint();

        // when the application sends an outbound payload it will want to encrypt
        // the data and these calls would be made from the application to the
        // TWG encryption service to perform those encryptions.

        WireMockHelper.stubEncryption(
                List.of(
                        new EncryptionMockData("costPerDay", new BigDecimal("50.001"), "MzIxLjMy"),
                        // ^ target is 321.32
                        new EncryptionMockData("tag", "DUMBO123", "IlRBTkdPNSI=")
                        // ^ target is TANGO5
                        ));

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

        AnimalV1 stubAnimal = createAnimal(1L, "Danny", "HUND999", new BigDecimal("9.13"));
        // wiremock stubbing
        stubFor(
                get(urlEqualTo("/animals/dog"))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(objectMapper.writeValueAsString(List.of(stubAnimal)))));

        WireMockHelper.stubAuthenticationEndpoint();

        // this decryption is used for when the application server fetches data from
        // the animals downstream API and wants to decrypt it.

        WireMockHelper.stubDecryption(
                List.of(
                        new DecryptionMockData("tagEncrypted", "SFVORDk5OQ==", "HUND888"),
                        new DecryptionMockData("costPerDayEncrypted", "OS4xMw==", new BigDecimal("18.17"))));

        // there are three payloads being encrypted from the backend because
        // the encryption is being done on a `List` response type from the backend
        // API.  These three objects in the `List` being returned will each need
        // to be encrypted individually and hence the need for the three mocks.

        WireMockHelper.stubEncryption(
                List.of(
                        new EncryptionMockData("costPerDay", new BigDecimal("10.97"), "MzIxLjMy"),
                        new EncryptionMockData("tag", "CAT001", "IlRBTkdPNSI=")));
        // ^ encrypted value is "TANGO5"

        WireMockHelper.stubEncryption(
                List.of(
                        new EncryptionMockData("costPerDay", new BigDecimal("10.12"), "MzIxLjMy"),
                        new EncryptionMockData("tag", "DOG001", "IlRBTkdPNSI=")));
        // ^ encrypted value is "TANGO5"

        WireMockHelper.stubEncryption(
                List.of(new EncryptionMockData("tag", "HUND888001", "IlRBTkdPNSI=")));
        // ^ encrypted value is "TANGO5"

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

        PetV1 petDanny =
                pets.stream()
                        .filter(p -> p.getName().equals("Danny"))
                        .findFirst()
                        .orElseThrow(() -> new AssertionError("unable to find the animal 'Danny'"));

        // this was returned by the TWG encryption service wiremock when the application
        // attempted to encrypt the tag that came from the "animals"

        assertEquals("IlRBTkdPNSI=", petDanny.getTagEncrypted());
    }

    private AnimalV1 createAnimal(long id, String name, String tag, BigDecimal costPerDay) {
        Base64.Encoder encoder = Base64.getEncoder();

        var animal = new AnimalV1();
        animal.setId(id);
        animal.setName(name);
        animal.setTagEncrypted(encoder.encodeToString(tag.getBytes(StandardCharsets.UTF_8)));
        animal.setDateOfBirth(OffsetDateTime.now(ZoneOffset.UTC));
        animal.setMicrochipDate(LocalDate.now(ZoneOffset.UTC));

        animal.setCostPerDayEncrypted(
                encoder.encodeToString(costPerDay.toString().getBytes(StandardCharsets.UTF_8)));

        // note that this one will not get serialized to JSON
        animal.setCostPerDay(DecryptedClearValue.of(costPerDay));

        return animal;
    }
}
