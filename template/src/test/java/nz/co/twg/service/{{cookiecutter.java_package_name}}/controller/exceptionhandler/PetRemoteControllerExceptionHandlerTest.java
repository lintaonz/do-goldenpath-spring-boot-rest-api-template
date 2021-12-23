package nz.co.twg.service.{{cookiecutter.java_package_name}}.controller.exceptionhandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.Request;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.openapi.server.model.ErrorV1;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class PetRemoteControllerExceptionHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // object under test
    private PetRemoteControllerExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new PetRemoteControllerExceptionHandler(objectMapper);
    }

    @Test
    void testHandleFeignException_happyPath() {
        // given
        Request request = mock(Request.class);
        FeignException exception =
                new FeignException.BadRequest(
                        "bad stuff",
                        request,
                        "{\"code\": 400, \"message\": \"wrong input\"}".getBytes(StandardCharsets.UTF_8),
                        Collections.emptyMap());

        // when
        ResponseEntity<ErrorV1> error = handler.handleFeignException(exception);

        // then
        assertEquals(400, error.getStatusCodeValue());
        assertNotNull(error.getBody());
        assertEquals(400L, error.getBody().getCode());
        assertEquals("wrong input", error.getBody().getMessage());
    }

    @Test
    void testHandleFeignException_invalidBody() {
        // given
        Request request = mock(Request.class);
        FeignException exception =
                new FeignException.BadRequest(
                        "bad stuff",
                        request,
                        // not suppose to be an array
                        "[]".getBytes(StandardCharsets.UTF_8),
                        Collections.emptyMap());

        // when
        ResponseEntity<ErrorV1> error = handler.handleFeignException(exception);

        // then
        assertEquals(500, error.getStatusCodeValue());
        assertNotNull(error.getBody());
        assertEquals(500L, error.getBody().getCode());
    }

    @Test
    void testHandleFeignException_noBody() {
        // given
        Request request = mock(Request.class);
        FeignException exception =
                new FeignException.BadRequest(
                        "bad stuff",
                        request,
                        // not suppose to be an null
                        null,
                        Collections.emptyMap());

        // when
        ResponseEntity<ErrorV1> error = handler.handleFeignException(exception);

        // then
        assertEquals(500, error.getStatusCodeValue());
        assertNotNull(error.getBody());
        assertEquals(500L, error.getBody().getCode());
        assertEquals("bad stuff", error.getBody().getMessage());
    }

    @Test
    void testHandleFeignException_connectionRefused() {
        // given
        Request request = mock(Request.class);
        FeignException exception =
                new FeignException.FeignServerException(
                        -1, // negative 1 status code indicates connection refuse
                        "connection refused",
                        request,
                        // not suppose to be an null
                        null,
                        Collections.emptyMap());

        // when
        ResponseEntity<ErrorV1> error = handler.handleFeignException(exception);

        // then
        assertEquals(500, error.getStatusCodeValue());
        assertNotNull(error.getBody());
        assertEquals(500L, error.getBody().getCode());
        assertEquals("connection refused", error.getBody().getMessage());
    }
}
