package nz.co.twg.service.{{cookiecutter.java_package_name}}.controller;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import nz.co.twg.common.features.Features;
import nz.co.twg.schema.encryption.PojoEncryptorDecryptor;
import nz.co.twg.schema.wrapper.DecryptedClearValue;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.FeatureFlag;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.openapi.clients.thirdpartyapi.api.AnimalsApiClient;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.openapi.clients.thirdpartyapi.model.AnimalV1;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.openapi.server.api.PetsApi;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.openapi.server.model.PetV1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Sample controller with example usages for feature flags. */
@RestController
@RequestMapping("/api")
public class PetController implements PetsApi {

    private static final Logger logger = LoggerFactory.getLogger(PetController.class);

    private final Features features;

    private final AnimalsApiClient animalsClient;

    private final PojoEncryptorDecryptor pojoEncryptorDecryptor;

    private final Validator validator;

    public PetController(
            Features features,
            AnimalsApiClient animalsClient,
            PojoEncryptorDecryptor pojoEncryptorDecryptor,
            Validator validator) {
        this.features = features;
        this.animalsClient = animalsClient;
        this.pojoEncryptorDecryptor = pojoEncryptorDecryptor;
        this.validator = validator;
    }

    /**
    * The @Timed annotated method will automatically capture the time taken for the method to execute
    * and report them in the /metrics endpoint.
    */
    @Override
    @Timed(value = "twg.endpoints.listpets.time", description = "TWG endpoints - listPets time taken")
    public ResponseEntity<List<PetV1>> listPets(Long limit) {

        logger.info("will list pets");
        List<PetV1> pets =
                new ArrayList<>(
                        List.of(
                                createLocalPet(1L, "Caspurr", "CAT001", new BigDecimal("10.97")),
                                createLocalPet(2L, "Pluto", "DOG001", new BigDecimal("10.12"))));

        // spotless:off
        FeatureFlag thirdPartyDogs = FeatureFlag.{{cookiecutter.artifact_id|upper|replace("-", "_")}}_INCLUDE_DOGS_FROM_THIRD_PARTY;
        boolean thirdPartyAnimalsFeature = features.isActive(thirdPartyDogs);
        // spotless:on

        // If the third party feature is enabled, call the third party api to get all the dogs
        // and add them to the list of pets
        if (thirdPartyAnimalsFeature) {
            logger.info("will load third party dogs");
            List<AnimalV1> animals = animalsClient.getAnimalsByType("dog").getBody();

            // Note that there is no automation around encryption / decryption with the client
            // nor is there any automation with regard to the validation.  For this reason it
            // is handled here manually.

            List<PetV1> thirdPartyPets =
                    Optional.ofNullable(animals).orElse(List.of()).stream()
                            .map(this::decryptAndValidate)
                            .map(this::mapAnimalsToPets)
                            .collect(Collectors.toUnmodifiableList());
            pets.addAll(thirdPartyPets);
        }

        // spotless:off
        FeatureFlag uppercaseName = FeatureFlag.{{cookiecutter.artifact_id|upper|replace("-", "_")}}_UPPERCASE_NAME;
        boolean upperCaseNameFeature = features.isActive(uppercaseName);
        // spotless:on

        // if the feature for upper case name is enabled, change the name of all the pets to uppercase.
        if (upperCaseNameFeature) {
            for (PetV1 pet : pets) {
                pet.setName(pet.getName().toUpperCase());
            }
        }
        logger.info("did list pets");
        return new ResponseEntity<>(pets, HttpStatus.OK);
    }

    @Override
    @Counted(
            value = "twg.endpoints.showPetById.counts",
            description = "TWG endpoints - showPetById counts called")
    public ResponseEntity<PetV1> showPetById(String petId) {
        PetV1 pet =
                createLocalPet(Long.parseLong(petId), "Dumbo", "DUMBO123", new BigDecimal("50.001"));

        // spotless:off
        FeatureFlag uppercaseName = FeatureFlag.{{cookiecutter.artifact_id|upper|replace("-", "_")}}_UPPERCASE_NAME;
        boolean upperCaseNameFeature = features.isActive(uppercaseName);
        // spotless:on
        if (upperCaseNameFeature) {
            pet.setName(pet.getName().toUpperCase());
        }

        return new ResponseEntity<>(pet, HttpStatus.OK);
    }

    /**
    * For demonstration purposes to show the model being read, decrypted and then re-encrypted on a
    * modified outbound response model.
    */
    @Override
    public ResponseEntity<PetV1> createPets(PetV1 petV1) {
        PetV1 responsePet = new PetV1();
        responsePet.setName(petV1.getName().toUpperCase(Locale.ROOT));

        if (null != petV1.getTag()) {
            responsePet.setTag(DecryptedClearValue.modified(petV1.getTag().get() + "-1"));
        }

        responsePet.setId(Math.abs(UUID.randomUUID().getLeastSignificantBits()));

        if (null != petV1.getCostPerDay()) {
            responsePet.setCostPerDay(
                    DecryptedClearValue.modified(petV1.getCostPerDay().get().add(BigDecimal.TEN)));
        }

        responsePet.setDateOfBirth(OffsetDateTime.of(2022, 9, 20, 19, 18, 17, 0, ZoneOffset.UTC));
        responsePet.setMicrochipDate(LocalDate.of(2022, 9, 25));

        return ResponseEntity.of(Optional.of(responsePet));
    }

    private PetV1 createLocalPet(long id, String name, String tag, BigDecimal costPerDay) {
        PetV1 pet = new PetV1();
        pet.id(id);
        pet.setTag(DecryptedClearValue.modified(tag));
        pet.setDateOfBirth(OffsetDateTime.now(ZoneOffset.UTC));
        pet.setMicrochipDate(LocalDate.now(ZoneOffset.UTC));
        pet.setCostPerDay(DecryptedClearValue.modified(costPerDay));
        pet.setName(name);

        return pet;
    }

    private PetV1 mapAnimalsToPets(AnimalV1 animal) {
        String petTag = animal.getTag().get().toUpperCase(Locale.ROOT) + "001";

        PetV1 pet = new PetV1();
        pet.setId(animal.getId());
        pet.setName(animal.getName());
        pet.setDateOfBirth(animal.getDateOfBirth());
        pet.setMicrochipDate(animal.getMicrochipDate());

        // the animal tag is not encrypted so we write it into the pet model
        // as a modified value so that it will re-encrypted on the way out.
        pet.setTag(DecryptedClearValue.modified(petTag));

        // note that the "cost per day" is being transferred in the encrypted
        // and decrypted form.
        pet.setCostPerDayEncrypted(animal.getCostPerDayEncrypted());
        pet.setCostPerDay(animal.getCostPerDay());

        return pet;
    }

    /**
    * The REST client does not automatically encrypt / decrypt and validate data that is used in a
    * request or response payload. For this reason, this method is used to perform a decryption on
    * the data and to validate it before it is used.
    */
    private <T> T decryptAndValidate(T model) {
        model = pojoEncryptorDecryptor.decrypt(model);
        Set<ConstraintViolation<T>> violations = validator.validate(model);
        if (!CollectionUtils.isEmpty(violations)) {
            throw new ConstraintViolationException(violations);
        }
        return model;
    }
}
