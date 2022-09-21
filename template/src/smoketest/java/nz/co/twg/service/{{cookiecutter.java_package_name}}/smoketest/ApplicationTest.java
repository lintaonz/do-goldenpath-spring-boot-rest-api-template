package nz.co.twg.service.{{cookiecutter.java_package_name}}.smoketest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.smoketest.util.ServiceBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApplicationTest extends ServiceBase {

    private final HttpClient client = HttpClient.newHttpClient();

    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    /**
    * Test to call the health endpoint on actuator to verify. You could use this to do deep health
    * checks if needed.
    *
    * @throws Exception
    */
    @Test
    void testApplicationHealth() throws Exception {
        // given

        String endpoint = String.format("%s/health", getActuatorBaseUrl());
        // when
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(String.format(endpoint))).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
    }

    /**
    * Test to call a custom actuator endpoint to verify. Use this if you want to do more than just
    * deep health checks. You could implement the extra verification in 'VerifierEndpoint' actuator
    * class and just call it tests like this to let the app do all the necessary steps and return you
    * result.
    *
    * @throws Exception
    */
    @Test
    void testApplicationVerifier() throws Exception {
        // given

        String endpoint = String.format("%s/verifier/startup", getActuatorBaseUrl());
        // when
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(String.format(endpoint))).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        String responseBody = response.body();
        Boolean result = objectMapper.reader().readValue(responseBody, Boolean.class);

        // then
        assertEquals(true, result);
    }
}
