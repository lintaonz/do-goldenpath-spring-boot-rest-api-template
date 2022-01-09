package nz.co.twg.service.{{cookiecutter.java_package_name}}.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Locale;
import nz.co.twg.common.features.Features;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.FeatureFlag;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.openapi.server.model.PetV1;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class PetLocalControllerTest {

    @Mock private Features features;

    // object under test
    private PetLocalController controller;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        this.controller = new PetLocalController(features);
    }

    @Test
    void testListPets() {
        // given
        FeatureFlag flag = FeatureFlag.{{cookiecutter.artifact_id|upper|replace("-", "_")}}_UPPERCASE_NAME;
        when(features.isActive(flag)).thenReturn(false);

        // when
        ResponseEntity<List<PetV1>> response = controller.listPets(null);
        List<PetV1> pets = response.getBody();

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(pets);
        assertEquals(2, pets.size());
        assertFalse(isUpperCase(pets.get(0).getName()));
        assertFalse(isUpperCase(pets.get(1).getName()));
    }

    @Test
    void testListPets_uppercase() {
        // given
        FeatureFlag flag = FeatureFlag.{{cookiecutter.artifact_id|upper|replace("-", "_")}}_UPPERCASE_NAME;
        when(features.isActive(flag)).thenReturn(true);

        // when
        ResponseEntity<List<PetV1>> response = controller.listPets(null);
        List<PetV1> pets = response.getBody();

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(pets);
        assertEquals(2, pets.size());
        assertTrue(isUpperCase(pets.get(0).getName()));
        assertTrue(isUpperCase(pets.get(1).getName()));
    }

    @Test
    void testShowPetById() {
        // given
        FeatureFlag flag = FeatureFlag.{{cookiecutter.artifact_id|upper|replace("-", "_")}}_UPPERCASE_NAME;
        when(features.isActive(flag)).thenReturn(false);

        // when
        ResponseEntity<PetV1> response = controller.showPetById("100");
        PetV1 pet = response.getBody();

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(pet);
        assertEquals(100L, pet.getId());
        assertFalse(isUpperCase(pet.getName()));
    }

    @Test
    void testShowPetById_uppercase() {
        // given
        FeatureFlag flag = FeatureFlag.{{cookiecutter.artifact_id|upper|replace("-", "_")}}_UPPERCASE_NAME;
        when(features.isActive(flag)).thenReturn(true);

        // when
        ResponseEntity<PetV1> response = controller.showPetById("100");
        PetV1 pet = response.getBody();

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(pet);
        assertEquals(100L, pet.getId());
        assertTrue(isUpperCase(pet.getName()));
    }

    @Test
    void testCreatePet() {
        // when
        ResponseEntity<Void> response = controller.createPets();

        // then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNull(response.getBody());
    }

    private boolean isUpperCase(String s) {
        return s.equals(s.toUpperCase(Locale.ROOT));
    }
}
