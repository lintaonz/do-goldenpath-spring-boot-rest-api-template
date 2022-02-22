package nz.co.twg.service.{{cookiecutter.java_package_name}}.controller;

import static java.util.stream.Collectors.toList;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import nz.co.twg.common.features.Features;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.FeatureFlag;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.openapi.clients.thirdpartyapi.api.AnimalsApiClient;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.openapi.clients.thirdpartyapi.model.AnimalV1;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.openapi.server.api.PetsApi;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.openapi.server.model.PetV1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Sample controller with example usages for feature flags. */
@RestController
@RequestMapping("/api")
public class PetController implements PetsApi {

    private static final Logger logger = LoggerFactory.getLogger(PetController.class);

    private final Features features;

    private final AnimalsApiClient animalsClient;

    public PetController(Features features, AnimalsApiClient animalsClient) {
        this.features = features;
        this.animalsClient = animalsClient;
    }

    @Override
    public ResponseEntity<List<PetV1>> listPets(Long limit) {
        logger.info("will list pets");
        List<PetV1> pets =
                new ArrayList<>(
                        List.of(
                                createLocalPet(1L, "Caspurr", "cat", new BigDecimal("10.97")),
                                createLocalPet(2L, "Pluto", "dog", new BigDecimal("10.12"))));

        // spotless:off
        FeatureFlag thirdPartyDogs = FeatureFlag.{{cookiecutter.artifact_id|upper|replace("-", "_")}}_INCLUDE_DOGS_FROM_THIRD_PARTY;
        boolean thirdPartyAnimalsFeature = features.isActive(thirdPartyDogs);
        // spotless:on

        // If the third party feature is enabled, call the third party api to get all the dogs
        // and add them to the list of pets
        if (thirdPartyAnimalsFeature) {
            logger.info("will load third party dogs");
            List<AnimalV1> animals = animalsClient.getAnimalsByType("dog").getBody();
            List<PetV1> thirdPartyPets =
                    animals != null
                            ? animals.stream().map(this::mapAnimalsToPets).collect(toList())
                            : Collections.emptyList();

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
    public ResponseEntity<PetV1> showPetById(String petId) {
        PetV1 pet =
                createLocalPet(Long.parseLong(petId), "Dumbo", "elephant", new BigDecimal("50.001"));

        // spotless:off
        FeatureFlag uppercaseName = FeatureFlag.{{cookiecutter.artifact_id|upper|replace("-", "_")}}_UPPERCASE_NAME;
        boolean upperCaseNameFeature = features.isActive(uppercaseName);
        // spotless:on
        if (upperCaseNameFeature) {
            pet.setName(pet.getName().toUpperCase());
        }

        return new ResponseEntity<>(pet, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> createPets() {
        return ResponseEntity.created(URI.create("")).build();
    }

    private PetV1 createLocalPet(long id, String name, String tag, BigDecimal costPerDay) {
        PetV1 pet = new PetV1();
        pet.id(id);
        pet.setTag(tag);
        pet.setDateOfBirth(OffsetDateTime.now(ZoneOffset.UTC));
        pet.setMicrochipDate(LocalDate.now(ZoneOffset.UTC));
        pet.setCostPerDay(costPerDay);
        pet.setName(name);

        return pet;
    }

    private PetV1 mapAnimalsToPets(AnimalV1 animal) {
        PetV1 pet = new PetV1();
        pet.setId(animal.getId());
        pet.setName(animal.getName());
        pet.setTag(animal.getTag());
        pet.setDateOfBirth(animal.getDateOfBirth());
        pet.setMicrochipDate(animal.getMicrochipDate());
        pet.setCostPerDay(animal.getCostPerDay());

        return pet;
    }
}
