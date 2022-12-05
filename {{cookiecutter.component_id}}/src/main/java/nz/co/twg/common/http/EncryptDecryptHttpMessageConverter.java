package nz.co.twg.common.http;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import nz.co.twg.schema.encryption.PojoEncryptorDecryptor;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

/**
* This delegating {@link HttpMessageConverter} implementation will wrap another {@link
* HttpMessageConverter}. Once it has asked the delegate to read the inbound HTTP request then it
* will decrypt the fields of the resultant model. Conversely when a model is output, it will first
* encrypt the fields of the model and then request that the delegate output the model.
*/
public class EncryptDecryptHttpMessageConverter<T>
        extends AbstractDelegatingHttpMessageConverter<T> {

    private final PojoEncryptorDecryptor pojoEncryptorDecryptor;

    public EncryptDecryptHttpMessageConverter(
            PojoEncryptorDecryptor pojoEncryptorDecryptor, HttpMessageConverter<T> delegate) {
        super(delegate);
        this.pojoEncryptorDecryptor = Preconditions.checkNotNull(pojoEncryptorDecryptor);
    }

    @Override
    public T read(Class<? extends T> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        T model = delegate.read(clazz, inputMessage);
        if (model instanceof List || model instanceof Set) {
            ((Collection<?>) model).forEach(pojoEncryptorDecryptor::decrypt);
        } else {
            pojoEncryptorDecryptor.decrypt(model);
        }
        return model;
    }

    @Override
    public void write(T t, MediaType contentType, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        if (t instanceof List || t instanceof Set) {
            ((Collection<?>) t).forEach(pojoEncryptorDecryptor::encrypt);
        } else {
            pojoEncryptorDecryptor.encrypt(t);
        }
        delegate.write(t, contentType, outputMessage);
    }
}
