package nz.co.twg.service.{{cookiecutter.java_package_name}}.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import io.micrometer.core.aop.CountedAspect;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import javax.validation.Validator;
import nz.co.twg.common.http.EncryptDecryptHttpMessageConverter;
import nz.co.twg.common.http.SdemResponseValidatingMessageConverter;
import nz.co.twg.schema.encryption.PojoEncryptorDecryptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Application configuration. */
@Configuration
public class ApplicationConfig {

    private final PojoEncryptorDecryptor pojoEncryptorDecryptor;

    private final Validator validator;

    @Value("${twg.sdem.validate-http-responses:true}")
    private boolean sdemValidateHttpResponses;

    /** Constructor. */
    public ApplicationConfig(
            PojoEncryptorDecryptor pojoEncryptorDecryptor,
            Validator validator,
            ObjectMapper objectMapper) {
        this.pojoEncryptorDecryptor = pojoEncryptorDecryptor;
        this.validator = validator;

        // configure the Spring provided object mapper with additional configuration
        new ObjectMapperSupplier().decorate(objectMapper);
    }

    /**
    * This CORS policy allows teams to call the API using the OpenAPI UI in backstage - from both dev
    * and prod instances. Do not remove this as we want all teams to be able to explore APIs in
    * backstage for dev and test environments.
    *
    * @return -
    */
    @Bean
    public WebMvcConfigurer webMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry
                        .addMapping("/**")
                        .allowedOrigins("https://backstage-dev.twg.co.nz", "https://backstage.twg.co.nz");
            }

            /**
            * These are the converters that are translating an inbound request into a model object and
            * then translating a model object to an outbound response. The logic here will decorate the
            * Jackson (JSON) handling {@link HttpMessageConverter} objects so that they perform
            * encryption and decryption as well as enforce validation on outbound messages that have the
            * <code>@Sdem</code> annotation.
            */
            @Override
            public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
                converters.replaceAll(ApplicationConfig.this::decorateMessageConverter);
            }
        };
    }

    /** Enables the use of @Counted annotation for custom metrics. */
    @Bean
    public CountedAspect countedAspect(MeterRegistry registry) {
        return new CountedAspect(registry);
    }

    /** Enables the use of @Timed annotation for custom metrics. */
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    private HttpMessageConverter<?> decorateMessageConverter(HttpMessageConverter<?> converter) {
        Preconditions.checkState(null != validator, "the validator has not been configured.");
        Preconditions.checkState(
                null != pojoEncryptorDecryptor, "the pojo encryptor / decryptor has not been configured.");
        if (converter instanceof AbstractJackson2HttpMessageConverter) {
            if (sdemValidateHttpResponses) {
                converter = new SdemResponseValidatingMessageConverter<>(validator, converter);
            }
            converter = new EncryptDecryptHttpMessageConverter<>(pojoEncryptorDecryptor, converter);
        }

        return converter;
    }
}
