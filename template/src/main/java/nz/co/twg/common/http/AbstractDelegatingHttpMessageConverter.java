package nz.co.twg.common.http;

import com.google.common.base.Preconditions;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;

/**
* Abstract class for {@link HttpMessageConverter} that delegates to another {@link
* HttpMessageConverter}.
*
* @param <T> the generic type.
*/
public abstract class AbstractDelegatingHttpMessageConverter<T> implements HttpMessageConverter<T> {

    protected final HttpMessageConverter<T> delegate;

    protected AbstractDelegatingHttpMessageConverter(HttpMessageConverter<T> delegate) {
        this.delegate = Preconditions.checkNotNull(delegate);
    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return delegate.canRead(clazz, mediaType);
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return delegate.canWrite(clazz, mediaType);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return delegate.getSupportedMediaTypes();
    }

    @Override
    public List<MediaType> getSupportedMediaTypes(Class<?> clazz) {
        return delegate.getSupportedMediaTypes(clazz);
    }
}
