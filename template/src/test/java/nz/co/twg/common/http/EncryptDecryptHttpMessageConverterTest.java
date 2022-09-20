package nz.co.twg.common.http;

import java.util.List;
import nz.co.twg.schema.encryption.PojoEncryptorDecryptor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;

class EncryptDecryptHttpMessageConverterTest {

    @Test
    void testRead() throws Exception {
        // GIVEN
        SomeModel model = new SomeModel();
        HttpInputMessage inputMessage = Mockito.mock(HttpInputMessage.class);

        PojoEncryptorDecryptor pojoEncryptorDecryptor = Mockito.mock(PojoEncryptorDecryptor.class);
        HttpMessageConverter<SomeModel> delegate = Mockito.mock(HttpMessageConverter.class);
        EncryptDecryptHttpMessageConverter<SomeModel> converter =
                new EncryptDecryptHttpMessageConverter<>(pojoEncryptorDecryptor, delegate);

        Mockito.when(delegate.read(SomeModel.class, inputMessage)).thenReturn(model);

        // WHEN
        Object output = converter.read(SomeModel.class, inputMessage);

        // THEN
        Mockito.verify(delegate).read(SomeModel.class, inputMessage);
        Assertions.assertThat(output).isEqualTo(model);
        Mockito.verify(pojoEncryptorDecryptor).decrypt(model);
    }

    @Test
    void testWrite() throws Exception {
        // GIVEN
        SomeModel model = new SomeModel();
        HttpOutputMessage outputMessage = Mockito.mock(HttpOutputMessage.class);

        PojoEncryptorDecryptor pojoEncryptorDecryptor = Mockito.mock(PojoEncryptorDecryptor.class);
        HttpMessageConverter<SomeModel> delegate = Mockito.mock(HttpMessageConverter.class);
        EncryptDecryptHttpMessageConverter<SomeModel> converter =
                new EncryptDecryptHttpMessageConverter<>(pojoEncryptorDecryptor, delegate);

        // WHEN
        converter.write(model, MediaType.APPLICATION_JSON, outputMessage);

        // THEN
        Mockito.verify(delegate).write(model, MediaType.APPLICATION_JSON, outputMessage);
        Mockito.verify(pojoEncryptorDecryptor).encrypt(model);
    }

    @Test
    void testWrite_list() throws Exception {
        // GIVEN
        List<SomeModel> model = List.of(new SomeModel(), new SomeModel());
        HttpOutputMessage outputMessage = Mockito.mock(HttpOutputMessage.class);

        PojoEncryptorDecryptor pojoEncryptorDecryptor = Mockito.mock(PojoEncryptorDecryptor.class);
        HttpMessageConverter<List<SomeModel>> delegate = Mockito.mock(HttpMessageConverter.class);
        EncryptDecryptHttpMessageConverter<List<SomeModel>> converter =
                new EncryptDecryptHttpMessageConverter<>(pojoEncryptorDecryptor, delegate);

        // WHEN
        converter.write(model, MediaType.APPLICATION_JSON, outputMessage);

        // THEN
        Mockito.verify(delegate).write(model, MediaType.APPLICATION_JSON, outputMessage);
        Mockito.verify(pojoEncryptorDecryptor, Mockito.times(2)).encrypt(Mockito.any(SomeModel.class));
    }

    private static class SomeModel {}
}
