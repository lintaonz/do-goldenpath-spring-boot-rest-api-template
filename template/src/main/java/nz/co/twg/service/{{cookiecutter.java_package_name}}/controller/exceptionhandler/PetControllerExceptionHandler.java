package nz.co.twg.service.{{cookiecutter.java_package_name}}.controller.exceptionhandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import javax.validation.ConstraintViolationException;
import nz.co.twg.common.http.SdemResponseValidatingMessageConverter;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.controller.PetController;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.openapi.server.model.ErrorV1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/** Exception handler for {@link FeignException}s. */
@ControllerAdvice(assignableTypes = PetController.class)
public class PetControllerExceptionHandler {

    private final Logger logger = LoggerFactory.getLogger(PetControllerExceptionHandler.class);

    private final ObjectMapper objectMapper;

    public PetControllerExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
    * Handler for the {@link FeignException}. This will unpack the client error payload and wrap it
    * in the service's own error payload.
    *
    * @param ex the {@link FeignException}
    * @return a {@link ResponseEntity} of {@link ErrorV1} type
    */
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
                    body,
                    nz.co.twg.service.{{cookiecutter.java_package_name}}.openapi.clients.thirdpartyapi.model.ErrorV1.class
                );
                // spotless:on
                HttpStatus status =
                        Optional.ofNullable(HttpStatus.resolve(clientError.getCode().intValue()))
                                .orElse(HttpStatus.INTERNAL_SERVER_ERROR);
                return handle(status, clientError.getMessage());
            } catch (Exception e) {
                return handle(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "failed to parse downstream client error: "
                                + e.getMessage()
                                + "; status ["
                                + ex.status()
                                + "]");
            }
        }
        return handle(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    /**
    * The inbound payload is validated by Spring and is considered as a "method argument" for the
    * controller body and therefore throws {@link MethodArgumentNotValidException}.
    */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorV1> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex) {
        logger.error("caught inbound payload validation error", ex);
        return handle(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
    * Handler for {@link ConstraintViolationException}. <br>
    * Can be thrown when the outbound HTTP payload is validated by {@link
    * SdemResponseValidatingMessageConverter}. <br>
    * Can also be thrown by any other internal processing utilising JSR303 validations.
    */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorV1> handleConstraintViolationException(
            ConstraintViolationException ex) {
        logger.error(
                "caught validation error either in outbound response validation or in internal processing",
                ex);
        return handle(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    /** Generic catch all exception handler. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorV1> handleException(Exception ex) {
        logger.error("caught unexpected exception", ex);
        return handle(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    private ResponseEntity<ErrorV1> handle(HttpStatus httpStatus, String message) {
        ErrorV1 error = new ErrorV1().code((long) httpStatus.value()).message(message);
        return ResponseEntity.status(httpStatus).contentType(MediaType.APPLICATION_JSON).body(error);
    }
}
