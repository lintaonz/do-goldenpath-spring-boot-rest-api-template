package nz.co.twg.service.{{cookiecutter.java_package_name}}.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.Set;
import nz.co.twg.common.encryption.EncryptionServiceApiConfig;
import nz.co.twg.common.encryption.EncryptionServiceVaultProxy;
import nz.co.twg.schema.encryption.PojoEncryptorDecryptor;
import nz.co.twg.schema.spi.EncryptionService;
import nz.co.twg.schema.wrapper.DecryptedClearValue;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configuration for beans related to SDEM encryption/decryption. */
@Configuration
@ConfigurationProperties(prefix = "twg.sdem.encryption")
public class SdemEncryptionConfig {

    /**
    * This list of encryption key ID the application is entitled to. <br>
    * Any fields using key ID not within this list will be skipped for decryption, with the {@link
    * DecryptedClearValue} set to {@link DecryptedClearValue.State#NOT_DECRYPTED} state. <br>
    * In the event an encryption is required for a field, but the required key ID is not within this
    * list, an exception will be raised. <br>
    * Overridden by value configured in application.yaml.
    */
    private Set<String> allowedEncryptionKeyIds = new HashSet<>();

    /**
    * This flag controls how errors are processed as payloads arrive via Kafka into the application
    * and are decrypted. Actual encryption / decryption of data won’t be handled by this application
    * but is instead handled by a downstream encryption / decryption remote service. Failures may
    * arise from the remote service not being online or the encrypted data may not be in the right
    * format or the application's authentication with the remote decryption service not being
    * authorized to decrypt the data.
    *
    * <p>In the case that the flag is <code>true</code> then any failure that arises during
    * decryption will be raised in the Spring MVC system.
    *
    * <p>In the case that the flag is <code>false</code> then any failure that arises during
    * decryption will be deferred to the point where the software actually attempts to access the
    * clear-field. The clear field will be provided in a {@link DecryptedClearValue} wrapper and in
    * the case that the field experienced an error during decryption, it will be in state {@link
    * DecryptedClearValue.State#ERROR} and attempting to use the {@link DecryptedClearValue#get()}
    * method will yield a runtime exception. This mechanism is intended for situations where
    * engineers of an application proactively want to handle failure in some specific way at the
    * field level.
    *
    * <p>An example of where field-level error handling would make sense is with a fictitious payload
    * such as;
    *
    * <pre>{
    *     "orderId": "ABC-DEF-HIJ",
    *     "status": "DELIVERED",
    *     "recipientEmailEncrypted": "vault:v1:hi238r723rhad03u03="
    * }
    * </pre>
    *
    * <p>In this example, the processing of the message MUST persist that the order was delivered. A
    * secondary function is to email the user to let them know, but if the user can't be emailed then
    * that is acceptable. In this case, not being able to decrypt the recipient's email could fail
    * and the overall processing of the message would be OK.
    *
    * <pre>try {
    *     mailService.send(...., payload.getRecipientEmail().get());
    * } catch (Throwable th) {
    *     ...log and continue...
    * }
    * </pre>
    *
    * <p>If the <code>errorOnDecryptionFailure</code> had been set to <code>true</code> and the field
    * <code>recipientEmailEncrypted</code> were unable to be decrypted then the business logic would
    * never have been able to be reached.
    *
    * <p>For most applications, this flag would be expected to be set to <code>true</code> as it
    * provides the simplest error handling path. The setting for this field would usually be set in
    * the application's <code>application.yml</code> file.
    */
    private boolean errorOnDecryptionFailure = false;

    /**
    * The configuration for Vault Proxy Encryption Microservice. Contains IdP info for access token
    * retrieval.
    */
    private EncryptionServiceApiConfig serviceApiConfig;

    /**
    * The {@link PojoEncryptorDecryptor} bean used to encrypt/decrypt the inbound and outbound pojo
    * when passing through the http converter.
    *
    * @param encryptionService the encryption service for the actual encryption/decryption work
    * @return the {@link PojoEncryptorDecryptor}
    */
    @Bean
    public PojoEncryptorDecryptor pojoEncryptorDecryptor(
            EncryptionService encryptionService, ObjectMapper objectMapper) {
        return new PojoEncryptorDecryptor(
                        encryptionService, allowedEncryptionKeyIds, errorOnDecryptionFailure)
                .withObjectMapper(objectMapper);
    }

    /**
    * The encryption service for the actual encryption/decryption work.
    *
    * @return the {@link EncryptionService}
    */
    @Bean
    public EncryptionService encryptionService() {
        return new EncryptionServiceVaultProxy(serviceApiConfig);
    }

    public void setAllowedEncryptionKeyIds(Set<String> allowedEncryptionKeyIds) {
        this.allowedEncryptionKeyIds = allowedEncryptionKeyIds;
    }

    public void setErrorOnDecryptionFailure(boolean errorOnDecryptionFailure) {
        this.errorOnDecryptionFailure = errorOnDecryptionFailure;
    }

    public void setServiceApiConfig(EncryptionServiceApiConfig serviceApiConfig) {
        this.serviceApiConfig = serviceApiConfig;
    }
}
