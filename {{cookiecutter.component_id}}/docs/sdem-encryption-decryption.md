# SDEM Encryption & Decryption

The ability to encrypt and decrypt PII/PSI/Confidential information has been built into the project. This ability is
driven by the SDEM schema and configured to be actioned in code logic in the Spring `HttpMessageConverter` as messages
are received, are processed and are responded to.
The following documentation will describe how everything is linked together and provides some examples on how to
handle encrypted/decrypted data.

For a more comprehensive walk-through of the system and design, read the confluence pages
[here](https://thewarehouse.atlassian.net/l/cp/Cu0wBeG1).

## SDEM & Generated Models

The SDEM schema allows for declaration of encrypted properties. These encrypted properties will have
the following characteristics;

- The property name ends with `Encrypted` suffix.
- The property contains the `x-twg-encryption-key` attribute, with the value being the Key ID required to
  encrypt/decrypt
  the clear/encrypted value.
- The property contains the `x-twg-clear` attribute, with the value being a nested object with attributes describing the
  shape of the non-encrypted value (such as `type`, `format`, `pattern`, `minimum`, `maximum`, etc).

A sample description for an encrypted property will look like the following:

```json
{
  ...
  "properties": {
    ...
    "taxCodeEncrypted": {
      "type": "string",
      "pattern": "^[A-Za-z0-9+/=:]+$",
      "x-twg-clear": {
        "type": "string",
        "maxLength": 10,
        "minLength": 1
      },
      "x-twg-encryption-key": "customer-personal"
    },
    ...
  }
}
```

The SDEM schema file will be converted to Java POJO model objects via the `openapi-generator-maven-plugin` plugin.
An extension has been developed to enhance the code generation to cater for these encrypted properties. The
plugin extension has been configured as below in the `pom.xml`.

The full implementation of the TWG custom OpenAPI Generator rules (generation logic) can be found in the
[do-lib-twg-openapigenerator](https://bitbucket.org/twgnz/do-lib-twg-openapigenerator) project.

```xml

<plugin>
    <groupId>org.openapitools</groupId>
    <artifactId>openapi-generator-maven-plugin</artifactId>
    <version>6.0.1</version>
    ...
    <dependencies>
        <dependency>
            <groupId>nz.co.twg.{{cookiecutter.prefix}}</groupId>
            <artifactId>do-lib-twg-openapigenerator</artifactId>
            <version>LATEST</version> <!-- Visit TWG Nexus and find the latest version available. -->
        </dependency>
    </dependencies>
    ...
    <executions>
        <execution>
            <!-- Generates OpenAPI interface for current service -->
            <id>server-api</id>
            <configuration>
                ...
                <generatorName>twg-spring</generatorName>
                <templateResourcePath>twg-spring-templates</templateResourcePath>
                <library>spring-boot</library>
            </configuration>
            <goals>
                <goal>generate</goal>
            </goals>
        </execution>
        <execution>
            <!-- Generates OpenFeign clients for inter-service communications -->
            <id>client-api</id>
            <configuration>
                ...
                <generatorName>twg-spring</generatorName>
                <templateResourcePath>twg-spring-templates</templateResourcePath>
                <library>spring-cloud</library>
            </configuration>
            <goals>
                <goal>generate</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

Note: Due to the limitation of the `openapi-generator-maven-plugin`, `$ref` within the `x-twg-clear` property is NOT
supported.

### Generated Model and its Characteristic

- The generated classes will be annotated with the `@Sdem` annotation. This is a marker annotation for the
  encryption logic so that it knows which classes to inspect during object traversal when looking for items to
  encrypt/decrypt.

  ```java
  @Sdem
  public class MyGeneratedClass {
    ...
  }
  ```

- The encrypted fields will be annotated with the `@TwgEncrypted` annotation. This annotation indicates the value is
  an encrypted field. The annotation contains some metadata to facilitate the data decryption process.

  ```java
  @TwgEncrypted(key = "customer-personal", decryptedProperty = "taxCode", actualType = String.class)
  private String taxCodeEncrypted;
  ```

- A corresponding field will be generated, and annotated with the `@TwgDecryption` annotation. The annotation's purpose
  is near identical to the `@TwgEncryption`, but is to facilitate data encryption. The decrypted value will be
  wrapped inside a wrapper class `DecryptedClearValue` (see later description), and the field will be annotated
  with `@JsonIgnore`, so the decrypted value will never appear in the JSON payload.

  ```java
  @JsonIgnore
  @TwgDecrypted(key = "customer-personal", encryptedProperty = "taxCodeEncrypted", actualType = String.class)
  @Size(min = 1, max = 10)
  private DecryptedClearValue<String> taxCode;
  ```

These TWG specific annotations can be found in the
[do-lib-twg-annotation](https://bitbucket.org/twgnz/do-lib-twg-annotation) project.

## The Encryption/Decryption Logic

The core logic for encryption and decryption is implemented the
[do-lib-twg-common](https://bitbucket.org/twgnz/do-lib-twg-common) project.

The 3 main files to be aware of are:

- `nz.co.twg.schema.spi.EncryptionService` interface.
- `nz.co.twg.schema.encryption.PojoEncryptorDecryptor` class.
- `nz.co.twg.schema.wrapper.DecryptedClearValue` wrapper class.

### The `EncryptionService` Interface

This is the interface for downstream applications to implement, be it a stub implementation for testing, or a
fully-fledged implementation that talks to an external service for encryption/decryption.

This interface is utilized by the `PojoEncryptorDecryptor` for the actual encryption/decryption work, but
can technically be used as a standalone service.

### The `PojoEncryptorDecryptor` Class

This class orchestrates the encryption/decryption logic for an instance of the SDEM model.

The encryption/decryption logic includes:

- Traversing the objects annotated with `@Sdem`. This is for performance reasons as the logic can avoid the cost of
  traversing objects that are not capable of carrying the encrypted/decrypted properties. For example, non-SDEM
  objects such as `String` could be safely skipped.
- Discovery of `@TwgEncrypted` annotated fields during decryption.
- Discovery of `@TwgDecrypted` annotated fields during encryption.
- Encoding of raw value to be JSON encoded string for cross language compatibility.
    - e.g.
        - `true` -> `"true"`
        - `1.0` -> `"1.0"`
        - `"my string"` -> `"\"my string \""`
- Type casting the decrypted value to the expected type
- Batching of data encryption/decryption so the memory consumption during the process is controlled and the quantity of
  requests to downstream systems such as the TWG Encryption Service is constrained.
- Invoking of the `EncryptionService` implementation for the actual encryption/decryption.
- The setting of the encrypted and decrypted field on the SDEM model.
    - Decrypted fields will be assigned a `DecryptedClearValue` based on the outcome of the decryption.

### The `DecryptedClearValue` Wrapper Class

The decrypted value (or the "clear" value) -- such as `String`, `Boolean` or `BigDecimal` -- will be wrapped in this
class. This is to cater for various situations that could happen during the decryption process. Each of these
situations corresponds to a `State` in this wrapper.

- The encrypted field does not have a value; (`EMPTY`).
- The application is not entitled to decrypt the field with the stated key ID, and the field is therefore skipped; (`NOT_DECRYPTED`).
- The application failed to decrypt the value due to some unforeseen error; (`ERROR`).
- The application successfully decrypted the value; (`DECRYPTED`).
- The application modified the decrypted value, so the new value should be re-encrypted and updated; (`MODIFIED`).

Access to the data wrapped by the `DecryptedClearValue` is possible by calling the `get()` method on the wrapper.
Data access is allowed for the following states:

- `EMPTY` - Returns null
- `DECRYPTED` - Returns the clear, decrypted value
- `MODIFIED` - Returns the modified value

Data access is NOT allowed for the following states, and an unchecked `DataAccessException` will be raised if
a `DecryptedClearValue` is accessed in one of these states;

- `NOT_DECRYPTED`
- `ERROR`

#### Retrieving a Decrypted Value

```java
// getTaxCode() returns a DecryptedClearValue.
// use the get() method to retrieve the clear value.
myClass.getTaxCode().get();
```

#### Setting a new value

```java
// By setting the value to be MODIFIED, will trigger the
// encryption logic to later kick in and set the encrypted
// value on the taxCodeEncrypted field.
myClass.setTaxCode(DecryptedClearValue.modified("TAX123"));
```

#### Error handling

```java
try {
    String taxCode=myClass.getTaxCode().get();
    // additional logic here
} catch(DataAccessException e) {
    // error handling here
}
```

## Spring Configuration

The Spring application properties relevant to the SDEM encryption and decryption are under the `twg.sdem` section.

```yaml
twg:
  sdem:
    validate-http-responses: true
    encryption:
      allowed-encryption-key-ids:
        - "getting-started"
      error-on-decryption-failure: true
      service-api-config:
        # URLs must not have trailing /
        base-url: "http://localhost:8090/vault-proxy"
        # the amount of time to deduct from the actual token expiry time to avoid
        # the situation where the token expires immediately after the validation deemed it still valid.
        cached-access-token-preemptive-expiry-seconds: 300
        auth:
          token-url: "http://localhost:8090/auth/oauth2/token"
          client-id: "test-client-id"
          client-secret: "test-client-secret"
          scope: "test-scope"
```

- The `validate-http-responses` configures whether JSR303 validation happens on the HTTP responses for models annotated
  with `@Sdem`. Response validation is not provided by Spring by default, but to maintain consistency with other TWG
  project archetypes (e.g. KStream), validation on outbound payload is implemented, but kept optional.
- The `allowed-encryption-key-ids` allows the application to control, by specifying the allowed encryption key ids,
  what fields are decrypted by the encryption logic.
  In the event where the application does not need to make use of the encrypted fields, setting this to `[]` can
  remove round trips to the vault proxy encryption microservice, increasing performance.
- The `error-on-decryption-failure` controls when the exception gets handled in the case that failures arise
  during decryption.
  For a more comprehensive description about this property, see the Javadoc in the `SdemEncryptionConfig` class.
- The `service-api-config` configuration sets up necessary information when communicating with the vault
  proxy encryption microservice known as "TWG Encryption Service".

All necessary beans for the encryption service will be created in the
`nz.co.twg.service.dogoldenpathspringbootkafka.config.SdemEncryptionConfig` Spring configuration class.

## Wiring to Spring's `HttpMessageConverter`

While the `PojoEncryptorDecryptor` Bean is available for any logic in the application (ie; `@Autowired`) to use,
handling encryption and decryption manually can be tedious and error-prone. This is because everything coming in may
require decryption, and everything going out should also have any updated values re-encrypted.
To make things easier for the developer, the encryption and decryption mechanism are also configured to occur
automatically during the inbound and outbound phase as HTTP requests are received and responded respectively.

These encryption and decryption operations are done in the `nz.co.twg.common.http.EncryptDecryptHttpMessageConverter`
class and are configured via the `ApplicationConfig#decorateMessageConverter` method. The decorating of message
converters happens in the `WebMvcConfigurer` bean.

## Validation for Decrypted/Encrypted Values

On data inbound and optionally, outbound, there is a check to ensure the payload conforms to the expected JSON Schema.
Once a payload is decrypted, another validation step is required to validate the clear (unencrypted) fields.

JSR303 validations are described by annotations on the clear (unencrypted) fields to describe what validation checks
to perform. The inbound checks are enforced by Spring's inbuilt validation, while the outbound checks are enforced by
the `SdemResponseValidatingMessageConverter` class (configurable via `twg.sdem.validate-http-responses` app property).

Since all the decrypted values are wrapped within the `DecryptedClearValue` wrapper, the JSR303 validations
will not work for fields of this type out of the box. Instead, `ConstraintValidator` implementation classes are
provided in the [do-lib-twg-common](https://bitbucket.org/twgnz/do-lib-twg-common) project to support the
`DecryptedClearValue` wrapper. These custom constraint validators are activated in the
`src/main/resources/META-INF/validation.xml` resource within the project.

## Encryption / Decryption and Validation for Delegated API Calls

Encryption, decryption, and validation for inbound and outbound payload are automatically handled. However, any
delegated API calls done through a custom `RestTemplate` or the OpenFeign generated clients will need to handle
encryption, decryption and validations manually. This can be easily achieved by implementing the following 2 methods;

```java
// initialized
private PojoEncryptorDecryptor pojoEncryptorDecryptor;
private Validator validator;

// call this prior to sending
private <T> T encryptAndValidate(T obj) {
    pojoEncryptorDecryptor.encrypt(obj);
    Set<ConstraintViolation<T>> violations = validator.validate(obj);
    if (!CollectionUtils.isEmpty(violations)) {
        throw new ConstraintViolationException(violations);
    }
    return obj;
}

// call this after receiving
private <T> T decryptAndValidate(T obj) {
    pojoEncryptorDecryptor.decrypt(obj);
    Set<ConstraintViolation<T>> violations = validator.validate(obj);
    if (!CollectionUtils.isEmpty(violations)) {
        throw new ConstraintViolationException(violations);
    }
    return obj;
}
```
