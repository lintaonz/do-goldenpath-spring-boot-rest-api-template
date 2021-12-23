package nz.co.twg.service.{{cookiecutter.java_package_name}}.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.openapi.client.api.PetsApiClient;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.openapi.server.model.PetV1;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class PetRemoteControllerTest {

    @Mock private PetsApiClient client;

    // object under test
    private PetRemoteController controller;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        this.controller = new PetRemoteController(client);
    }

    @Test
    void testListPets() {
        // given
        var pet1 = createClientPet(1L, "pet1", "cat", new BigDecimal("10.10"));
        var pet2 = createClientPet(2L, "pet2", "dog", new BigDecimal("10.10"));

        var pets = List.of(pet1, pet2);
        when(client.listPets(any())).thenReturn(ResponseEntity.ok(pets));

        // when
        ResponseEntity<List<PetV1>> response = controller.listPets(null);
        List<PetV1> result = response.getBody();

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("pet1", result.get(0).getName());
        assertEquals("pet2", result.get(1).getName());
    }

    @Test
    void testListPets_nullClientResponse() {
        // given
        when(client.listPets(any())).thenReturn(ResponseEntity.ok().build());

        // when
        ResponseEntity<List<PetV1>> response = controller.listPets(null);
        List<PetV1> result = response.getBody();

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void testShowPetById() {
        // given
        var pet = createClientPet(100L, "pet1", "cat", new BigDecimal("10.10"));
        when(client.showPetById(any())).thenReturn(ResponseEntity.ok(pet));

        // when
        ResponseEntity<PetV1> response = controller.showPetById("100");
        PetV1 result = response.getBody();

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals("pet1", result.getName());
        verify(client).showPetById("100");
    }

    @Test
    void testShowPetById_nullClientResponse() {
        // given
        when(client.showPetById(any())).thenReturn(ResponseEntity.ok().build());

        // when
        try {
            controller.showPetById("100");
            fail("expected " + IllegalStateException.class + " to be thrown.");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    @Test
    void testCreatePets() {
        // when + then
        try {
            controller.createPets();
            fail("expected " + NotImplementedException.class + " to be thrown.");
        } catch (NotImplementedException e) {
            // expected
        }
    }

    private nz.co.twg.service.{{cookiecutter.java_package_name}}.openapi.client.model.PetV1
            createClientPet(long id, String name, String tag, BigDecimal costPerDay) {
        var pet = new nz.co.twg.service.{{cookiecutter.java_package_name}}.openapi.client.model.PetV1();
        pet.setId(id);
        pet.setName(name);
        pet.setTag(tag);
        pet.setDateOfBirth(OffsetDateTime.now(ZoneOffset.UTC));
        pet.setMicrochipDate(LocalDate.now(ZoneOffset.UTC));
        pet.setCostPerDay(costPerDay);
        return pet;
    }
}
