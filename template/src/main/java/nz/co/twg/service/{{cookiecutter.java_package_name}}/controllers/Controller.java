package nz.co.twg.service.{{cookiecutter.java_package_name}}.controllers;

import java.util.ArrayList;
import java.util.List;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.openapi.api.PetsApi;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.openapi.model.Pet;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Controller implements PetsApi {

    @Override
    public ResponseEntity<List<Pet>> _listPets(Integer limit) {

        List<Pet> pets = new ArrayList<>();

        Pet pet1 = new Pet();
        pet1.setId(1L);
        pet1.setName("pet1");

        Pet pet2 = new Pet();
        pet1.setId(2L);
        pet1.setName("pet2");

        pets.add(pet1);
        pets.add(pet2);

        return new ResponseEntity<>(pets, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Pet> _showPetById(String petId) {

        Pet pet = new Pet();
        pet.id(Long.parseLong(petId));
        pet.setName("pet" + petId);

        return new ResponseEntity<>(pet, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> _createPets() {
        return ResponseEntity.noContent().build();
    }
}
