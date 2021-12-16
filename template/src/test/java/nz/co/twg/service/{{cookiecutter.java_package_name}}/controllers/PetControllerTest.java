package nz.co.twg.service.{{cookiecutter.java_package_name}}.controllers;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.openapi.model.PetV1;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class PetControllerTest {

    // object under test
    private final PetController petApi = new PetController();

    @Test
    void testListPets() {
        // when
        ResponseEntity<List<PetV1>> response = petApi._listPets(Long.MAX_VALUE);
        List<PetV1> pets = response.getBody();

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(pets);
        assertEquals(2, pets.size());
    }

    @Test
    void testShowPetById() {
        // when
        ResponseEntity<PetV1> response = petApi._showPetById("100");
        PetV1 pet = response.getBody();

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(pet);
        assertEquals(100L, pet.getId());
    }

    @Test
    void testCreatePet() {
        // when
        ResponseEntity<Void> response = petApi._createPets();

        // then
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
    }
}
