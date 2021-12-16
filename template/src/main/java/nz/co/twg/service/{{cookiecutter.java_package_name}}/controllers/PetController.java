package nz.co.twg.service.{{cookiecutter.java_package_name}}.controllers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.openapi.api.PetsApi;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.openapi.model.PetV1;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PetController implements PetsApi {

    @Override
    public ResponseEntity<List<PetV1>> _listPets(Long limit) {

        List<PetV1> pets = new ArrayList<>();

        PetV1 pet1 = new PetV1();
        pet1.setId(1L);
        pet1.setName("pet1");
        pet1.setTag("cat");
        pet1.setDateOfBirth(OffsetDateTime.now(ZoneOffset.UTC));
        pet1.setMicrochipDate(LocalDate.now());
        pet1.setCostPerDay(new BigDecimal("10.97"));

        PetV1 pet2 = new PetV1();
        pet2.setId(2L);
        pet2.setName("pet2");
        pet2.setTag("dog");
        pet2.setDateOfBirth(OffsetDateTime.now(ZoneOffset.UTC));
        pet2.setMicrochipDate(LocalDate.now());
        pet2.setCostPerDay(new BigDecimal("10.12"));

        pets.add(pet1);
        pets.add(pet2);

        return new ResponseEntity<>(pets, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<PetV1> _showPetById(String petId) {

        PetV1 pet = new PetV1();
        pet.id(Long.parseLong(petId));
        pet.setName("pet" + petId);
        pet.setTag("elephant");
        pet.setDateOfBirth(OffsetDateTime.now(ZoneOffset.UTC));
        pet.setMicrochipDate(LocalDate.now());
        pet.setCostPerDay(new BigDecimal("10.00"));

        return new ResponseEntity<>(pet, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> _createPets() {
        return ResponseEntity.noContent().build();
    }
}
