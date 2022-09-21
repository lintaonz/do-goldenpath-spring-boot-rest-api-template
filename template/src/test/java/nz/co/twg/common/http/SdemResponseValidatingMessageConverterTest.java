package nz.co.twg.common.http;

import java.util.List;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import nz.co.twg.schema.annotation.Sdem;
import org.assertj.core.api.Assertions;
import org.hibernate.validator.internal.engine.path.PathImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;

class SdemResponseValidatingMessageConverterTest {

    @Test
    void testRead() throws Exception {
        // GIVEN
        SomeSdemModel model = new SomeSdemModel();
        HttpInputMessage inputMessage = Mockito.mock(HttpInputMessage.class);

        Validator validator = Mockito.mock(Validator.class);
        HttpMessageConverter<SomeSdemModel> delegate = Mockito.mock(HttpMessageConverter.class);
        SdemResponseValidatingMessageConverter<SomeSdemModel> converter =
                new SdemResponseValidatingMessageConverter<>(validator, delegate);

        Mockito.when(delegate.read(SomeSdemModel.class, inputMessage)).thenReturn(model);

        // WHEN
        Object output = converter.read(SomeSdemModel.class, inputMessage);

        // THEN
        Mockito.verify(delegate).read(SomeSdemModel.class, inputMessage);
        Assertions.assertThat(output).isEqualTo(model);
        Mockito.verifyNoInteractions(validator);
    }

    @Test
    void testWrite_sdemWithFailure() {
        // GIVEN
        SomeSdemModel model = new SomeSdemModel();

        ConstraintViolation<SomeSdemModel> constraintViolation =
                Mockito.mock(ConstraintViolation.class);
        Mockito.when(constraintViolation.getPropertyPath())
                .thenReturn(PathImpl.createPathFromString("name"));
        Mockito.when(constraintViolation.getMessage()).thenReturn("Peppermint");

        Validator validator = Mockito.mock(Validator.class);
        Mockito.when(validator.validate(model)).thenReturn(Set.of(constraintViolation));

        HttpMessageConverter<SomeSdemModel> delegate = Mockito.mock(HttpMessageConverter.class);
        SdemResponseValidatingMessageConverter<SomeSdemModel> converter =
                new SdemResponseValidatingMessageConverter<>(validator, delegate);
        HttpOutputMessage outputMessage = Mockito.mock(HttpOutputMessage.class);

        // WHEN
        org.junit.jupiter.api.Assertions.assertThrows(
                ConstraintViolationException.class,
                () -> converter.write(model, MediaType.APPLICATION_JSON, outputMessage));

        // THEN
        // exception is thrown.
    }

    @Test
    void testWrite_sdem() throws Exception {
        // GIVEN
        SomeSdemModel model = new SomeSdemModel();
        HttpOutputMessage outputMessage = Mockito.mock(HttpOutputMessage.class);

        Validator validator = Mockito.mock(Validator.class);
        HttpMessageConverter<SomeSdemModel> delegate = Mockito.mock(HttpMessageConverter.class);
        SdemResponseValidatingMessageConverter<SomeSdemModel> converter =
                new SdemResponseValidatingMessageConverter<>(validator, delegate);

        // WHEN
        converter.write(model, MediaType.APPLICATION_JSON, outputMessage);

        // THEN
        Mockito.verify(delegate).write(model, MediaType.APPLICATION_JSON, outputMessage);
        Mockito.verify(validator).validate(model);
    }

    @Test
    void testWrite_sdemList() throws Exception {
        // GIVEN
        List<SomeSdemModel> model = List.of(new SomeSdemModel(), new SomeSdemModel());
        HttpOutputMessage outputMessage = Mockito.mock(HttpOutputMessage.class);

        Validator validator = Mockito.mock(Validator.class);
        HttpMessageConverter<List<SomeSdemModel>> delegate = Mockito.mock(HttpMessageConverter.class);
        SdemResponseValidatingMessageConverter<List<SomeSdemModel>> converter =
                new SdemResponseValidatingMessageConverter<>(validator, delegate);

        // WHEN
        converter.write(model, MediaType.APPLICATION_JSON, outputMessage);

        // THEN
        Mockito.verify(delegate).write(model, MediaType.APPLICATION_JSON, outputMessage);
        Mockito.verify(validator, Mockito.times(2)).validate(Mockito.any(SomeSdemModel.class));
    }

    @Test
    void testWrite_nonSdem() throws Exception {
        // GIVEN
        SomeNonSdemModel model = new SomeNonSdemModel();
        HttpOutputMessage outputMessage = Mockito.mock(HttpOutputMessage.class);

        Validator validator = Mockito.mock(Validator.class);
        HttpMessageConverter<SomeNonSdemModel> delegate = Mockito.mock(HttpMessageConverter.class);
        SdemResponseValidatingMessageConverter<SomeNonSdemModel> converter =
                new SdemResponseValidatingMessageConverter<>(validator, delegate);

        // WHEN
        converter.write(model, MediaType.APPLICATION_JSON, outputMessage);

        // THEN
        Mockito.verify(delegate).write(model, MediaType.APPLICATION_JSON, outputMessage);
        Mockito.verifyNoInteractions(validator);
    }

    @Test
    void testCanWrite() throws Exception {
        // GIVEN
        Validator validator = Mockito.mock(Validator.class);
        HttpMessageConverter<SomeNonSdemModel> delegate = Mockito.mock(HttpMessageConverter.class);
        Mockito.when(delegate.canWrite(SomeNonSdemModel.class, MediaType.APPLICATION_JSON))
                .thenReturn(true);

        SdemResponseValidatingMessageConverter<SomeNonSdemModel> converter =
                new SdemResponseValidatingMessageConverter<>(validator, delegate);

        // WHEN
        boolean result = converter.canWrite(SomeNonSdemModel.class, MediaType.APPLICATION_JSON);

        // THEN
        Assertions.assertThat(result).isTrue();
        Mockito.verify(delegate).canWrite(SomeNonSdemModel.class, MediaType.APPLICATION_JSON);
    }

    @Sdem
    private static class SomeSdemModel {}

    private static class SomeNonSdemModel {}
}
