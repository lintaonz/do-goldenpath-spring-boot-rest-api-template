package nz.co.twg.service.{{cookiecutter.java_package_name}}.controller.exceptionhandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.controller.PetController;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.openapi.server.model.ErrorV1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice(assignableTypes = PetController.class)
public class PetControllerExceptionHandler {

    private final Logger logger = LoggerFactory.getLogger(PetControllerExceptionHandler.class);

    private final ObjectMapper objectMapper;

    public PetControllerExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorV1> handleFeignException(FeignException ex) {

        logger.error("caught feign client exception", ex);
        Optional<ByteBuffer> responseBodyOptional = ex.responseBody();
        if (ex.status() != -1 && responseBodyOptional.isPresent()) {
            ByteBuffer responseBody = responseBodyOptional.get();
            try {
                String body = StandardCharsets.UTF_8.decode(responseBody).toString();
                // spotless:off
                var clientError = objectMapper.readValue(
                    body, nz.co.twg.service.{{cookiecutter.java_package_name}}.openapi.clients.thirdpartyapi.model.ErrorV1.class);
                // spotless:on
                ErrorV1 error = new ErrorV1().code(clientError.getCode()).message(clientError.getMessage());
                return ResponseEntity.status(HttpStatus.valueOf(clientError.getCode().intValue()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(error);
            } catch (Exception e) {
                return handle500(
                        "failed to parse downstream client error: "
                                + e.getMessage()
                                + "; status ["
                                + ex.status()
                                + "]");
            }
        }
        return handle500(ex.getMessage());
    }

    private ResponseEntity<ErrorV1> handle500(String message) {
        ErrorV1 error =
                new ErrorV1().code((long) HttpStatus.INTERNAL_SERVER_ERROR.value()).message(message);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(error);
    }
}
