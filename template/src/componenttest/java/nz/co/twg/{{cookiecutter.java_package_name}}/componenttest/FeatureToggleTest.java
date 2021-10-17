package nz.co.twg.{{cookiecutter.java_package_name}}.componenttest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import nz.co.twg.{{cookiecutter.java_package_name}}.componenttest.util.ActuatorFeaturesSupport;
import nz.co.twg.{{cookiecutter.java_package_name}}.componenttest.util.ServiceBase;
import nz.co.twg.{{cookiecutter.java_package_name}}.util.FeaturesSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** An example test featuring testing of feature toggle dependent code */
class FeatureToggleTest extends ServiceBase {

    private final FeaturesSupport featuresSupport =
            new ActuatorFeaturesSupport(getHostname(), getActuatorPort());

    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach
    public void setup() {
        featuresSupport.clear();
    }

    @Test
    void testFeatureA_on() throws Exception {
        // given
        featuresSupport.configure("feature-A", true);
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(
                                        String.format(
                                                "http://%s:%s/sample/featureDependentEndpoint",
                                                getHostname(), getAppPort())))
                        .build();

        // when
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String result = response.body();

        // then
        assertEquals("Apple", result);
    }

    @Test
    void testFeatureA_off() throws Exception {
        // given
        featuresSupport.configure("feature-A", false);
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(
                                        String.format(
                                                "http://%s:%s/sample/featureDependentEndpoint",
                                                getHostname(), getAppPort())))
                        .build();

        // when
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String result = response.body();

        // then
        assertEquals("Banana", result);
    }
}
