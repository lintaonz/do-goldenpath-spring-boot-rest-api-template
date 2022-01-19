package nz.co.twg.service.{{cookiecutter.java_package_name}}.componenttest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import nz.co.twg.common.features.FeaturesSupport;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.FeatureFlag;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.componenttest.util.ActuatorFeaturesSupport;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.componenttest.util.ServiceBase;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.openapi.server.model.PetV1;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** An example test featuring testing of feature toggle dependent code */
class FeatureToggleExampleTest extends ServiceBase {

    private final FeaturesSupport featuresSupport =
            new ActuatorFeaturesSupport(getHostname(), getActuatorPort());

    private final HttpClient client = HttpClient.newHttpClient();

    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        featuresSupport.clear();
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
                        .uri(
                                URI.create(
                                        String.format("http://%s:%s/api/local/pets/1", getHostname(), getAppPort())))
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
                        .uri(
                                URI.create(
                                        String.format("http://%s:%s/api/local/pets/1", getHostname(), getAppPort())))
                        .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String result = response.body();
        PetV1 pet = objectMapper.reader().readValue(result, PetV1.class);

        // then
        assertEquals("Dumbo", pet.getName());
    }
}
