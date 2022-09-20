package nz.co.twg.common.http;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import nz.co.twg.schema.annotation.Sdem;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

/**
* If an SDEM model is being written out via an API then this will be able to ensure that it gets
* validated before it is written.
*/
public class SdemResponseValidatingMessageConverter<T>
        extends AbstractDelegatingHttpMessageConverter<T> {

    private final Validator validator;

    public SdemResponseValidatingMessageConverter(
            Validator validator, HttpMessageConverter<T> delegate) {
        super(delegate);
        this.validator = Preconditions.checkNotNull(validator);
    }

    @Override
    public T read(Class<? extends T> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        return delegate.read(clazz, inputMessage);
    }

    @Override
    public void write(T t, MediaType contentType, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        if (t instanceof List || t instanceof Set) {
            ((Collection<?>) t).forEach(this::validateForWrite);
        } else {
            validateForWrite(t);
        }
        delegate.write(t, contentType, outputMessage);
    }

    private void validateForWrite(Object o) {
        if (null != o.getClass().getAnnotation(Sdem.class)) {
            Set<ConstraintViolation<Object>> violations = validator.validate(o);
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }
        }
    }
}
