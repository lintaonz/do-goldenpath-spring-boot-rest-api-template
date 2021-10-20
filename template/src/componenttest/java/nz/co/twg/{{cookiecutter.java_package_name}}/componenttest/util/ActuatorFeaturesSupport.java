package nz.co.twg.{{cookiecutter.java_package_name}}.componenttest.util;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.core.MediaType;
import nz.co.twg.features.FeaturesSupport;
import org.awaitility.Awaitility;
import org.springframework.web.util.UriComponentsBuilder;

/** A helper class to query feature statuses from the actuator endpoint */
public final class ActuatorFeaturesSupport implements FeaturesSupport {

    private final String port;

    private final URI baseUri;

    private final ObjectMapper objectMapper;

    private final HttpClient client;

    public ActuatorFeaturesSupport(String hostname, String port) {
        this.port = port;
        this.baseUri =
                UriComponentsBuilder.newInstance().scheme("http").host(hostname).port(port).build().toUri();
        this.objectMapper = new ObjectMapper();
        this.client = HttpClient.newHttpClient();
    }

    @Override
    public void configure(String key, boolean value) {
        URI uri = UriComponentsBuilder.fromUri(baseUri).pathSegment("features", key).build().toUri();
        HttpRequest request;
        try {
            request =
                    HttpRequest.newBuilder()
                            .header("Content-Type", MediaType.APPLICATION_JSON)
                            .POST(
                                    HttpRequest.BodyPublishers.ofString(
                                            objectMapper.writeValueAsString(Collections.singletonMap("value", value))))
                            .uri(uri)
                            .build();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        HttpResponse<String> response = doApiCall(request);
        if (response.statusCode() != 204) {
            throw new AssertionError("POST api call to " + uri + " returned " + response.statusCode());
        }
        awaitUntilMatched(key, value);
    }

    @Override
    public void remove(String key) {
        URI uri = UriComponentsBuilder.fromUri(baseUri).pathSegment("features", key).build().toUri();
        HttpRequest request = HttpRequest.newBuilder().DELETE().uri(uri).build();
        HttpResponse<String> response = doApiCall(request);
        if (response.statusCode() != 204) {
            throw new AssertionError("DELETE api call to " + uri + " returned " + response.statusCode());
        }
        awaitUntilMatched(key, null);
    }

    @Override
    public void clear() {
        URI uri = UriComponentsBuilder.fromUri(baseUri).pathSegment("features").build().toUri();
        HttpRequest request = HttpRequest.newBuilder().DELETE().uri(uri).build();
        HttpResponse<String> response = doApiCall(request);
        if (response.statusCode() != 204) {
            throw new AssertionError("DELETE api call to " + uri + " returned " + response.statusCode());
        }
        awaitUntilCleared();
    }

    private HttpResponse<String> doApiCall(HttpRequest request) {
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new AssertionError(
                    "[" + request.method() + "] api call to " + request.uri() + " failed", e);
        }
    }

    /** verify with the /features endpoint until the key matches the expected value */
    private void awaitUntilMatched(String key, Boolean expectedValue) {
        Awaitility.await()
                .atMost(6, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .pollDelay(0, TimeUnit.SECONDS)
                .ignoreExceptions()
                .untilAsserted(
                        () ->
                                assertThat(
                                                given()
                                                        .port(Integer.parseInt(port))
                                                        .contentType("application/json")
                                                        .when()
                                                        .get("/features")
                                                        .then()
                                                        .extract()
                                                        .response()
                                                        .jsonPath()
                                                        .getMap(".")
                                                        .get(key))
                                        .isEqualTo(expectedValue));
    }

    /** verify with the /features endpoint to check if the clearing succeeded */
    private void awaitUntilCleared() {
        Awaitility.await()
                .atMost(6, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .pollDelay(0, TimeUnit.SECONDS)
                .ignoreExceptions()
                .untilAsserted(
                        () ->
                                assertThat(
                                                given()
                                                        .port(Integer.parseInt(port))
                                                        .contentType("application/json")
                                                        .when()
                                                        .get("/features")
                                                        .then()
                                                        .extract()
                                                        .response()
                                                        .jsonPath()
                                                        .getMap(".")
                                                        .size())
                                        .isEqualTo(0));
    }
}
