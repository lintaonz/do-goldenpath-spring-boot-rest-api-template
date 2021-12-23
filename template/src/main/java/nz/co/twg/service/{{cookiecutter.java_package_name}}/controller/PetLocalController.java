package nz.co.twg.service.{{cookiecutter.java_package_name}}.controller;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import nz.co.twg.common.features.Features;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.FeatureFlag;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.openapi.server.api.PetsApi;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.openapi.server.model.PetV1;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Sample controller with example usages for feature flags. */
@RestController
@RequestMapping("/api/local")
public class PetLocalController implements PetsApi {

    private final Features features;

    public PetLocalController(Features features) {
        this.features = features;
    }

    @Override
    public ResponseEntity<List<PetV1>> listPets(Long limit) {

        List<PetV1> pets =
                List.of(
                        createLocalPet(1L, "Caspurr", "cat", new BigDecimal("10.97")),
                        createLocalPet(2L, "Pluto", "dog", new BigDecimal("10.12")));
        return new ResponseEntity<>(pets, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<PetV1> showPetById(String petId) {
        PetV1 pet =
                createLocalPet(Long.parseLong(petId), "Dumbo", "elephant", new BigDecimal("50.001"));
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
        pet.setMicrochipDate(LocalDate.now());
        pet.setCostPerDay(costPerDay);

        FeatureFlag flag = FeatureFlag.{{cookiecutter.artifact_id|upper|replace("-", "_")}}_UPPERCASE_NAME;
        String petName = features.isActive(flag) ? name.toUpperCase(Locale.ROOT) : name;
        pet.setName(petName);

        return pet;
    }
}
