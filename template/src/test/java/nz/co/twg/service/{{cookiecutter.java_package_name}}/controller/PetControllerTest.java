package nz.co.twg.service.{{cookiecutter.java_package_name}}.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Charsets;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.validation.Validator;
import nz.co.twg.common.features.Features;
import nz.co.twg.schema.encryption.PojoEncryptorDecryptor;
import nz.co.twg.schema.wrapper.DecryptedClearValue;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.FeatureFlag;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.openapi.clients.thirdpartyapi.api.AnimalsApiClient;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.openapi.clients.thirdpartyapi.model.AnimalV1;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.openapi.server.model.PetV1;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class PetControllerTest {

    @Mock private Logger logger;

    @Mock private Features features;

    @Mock private PojoEncryptorDecryptor pojoEncryptorDecryptor;

    @Mock private Validator validator;

    @Mock private AnimalsApiClient animalsApiClient;

    // object under test
    @InjectMocks private PetController controller;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testListPets() {
        // given
        // spotless:off
        FeatureFlag flag = FeatureFlag.{{cookiecutter.artifact_id|upper|replace("-", "_")}}_UPPERCASE_NAME;
        // spotless:on
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
        // spotless:off
        FeatureFlag flag = FeatureFlag.{{cookiecutter.artifact_id|upper|replace("-", "_")}}_UPPERCASE_NAME;
        // spotless:on
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
    void testListPets_doNotIncludeDogsFromThirdParty() {
        // given
        // spotless:off
        FeatureFlag flag = FeatureFlag.{{cookiecutter.artifact_id|upper|replace("-", "_")}}_INCLUDE_DOGS_FROM_THIRD_PARTY;
        // spotless:on
        when(features.isActive(flag)).thenReturn(false);

        // when
        ResponseEntity<List<PetV1>> response = controller.listPets(null);
        List<PetV1> pets = response.getBody();

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(pets);
        assertEquals(2, pets.size());
        verify(logger, never()).info("will load third party dogs");
    }

    @Test
    void testListPets_includeDogsFromThirdParty() {
        // given
        // spotless:off
        FeatureFlag flag = FeatureFlag.{{cookiecutter.artifact_id|upper|replace("-", "_")}}_INCLUDE_DOGS_FROM_THIRD_PARTY;
        // spotless:on
        when(validator.validate(Mockito.any())).thenReturn(Set.of());
        when(pojoEncryptorDecryptor.decrypt(Mockito.any())).thenAnswer(a -> a.getArguments()[0]);
        when(features.isActive(flag)).thenReturn(true);
        when(animalsApiClient.getAnimalsByType("dog"))
                .thenReturn(
                        ResponseEntity.ok(List.of(createAnimal(10, "Dexter", "dog", new BigDecimal("15.97")))));

        // when
        ResponseEntity<List<PetV1>> response = controller.listPets(null);
        List<PetV1> pets = response.getBody();

        // then
        Mockito.verify(validator).validate(Mockito.any());
        Mockito.verify(pojoEncryptorDecryptor).decrypt(Mockito.any());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(pets);
        assertEquals(3, pets.size());
        verify(logger).info("will load third party dogs");
    }

    @Test
    void testShowPetById() {
        // given
        // spotless:off
        FeatureFlag flag = FeatureFlag.{{cookiecutter.artifact_id|upper|replace("-", "_")}}_UPPERCASE_NAME;
        // spotless:on
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
        // spotless:off
        FeatureFlag flag = FeatureFlag.{{cookiecutter.artifact_id|upper|replace("-", "_")}}_UPPERCASE_NAME;
        // spotless:on
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
        // given
        PetV1 pet = new PetV1();
        pet.setName("Fluffy");
        pet.setTag(DecryptedClearValue.modified("ABC123"));
        pet.setId(null);
        pet.setCostPerDay(DecryptedClearValue.of(new BigDecimal("12.123")));
        pet.setDateOfBirth(OffsetDateTime.of(2022, 9, 20, 19, 18, 17, 0, ZoneOffset.UTC));
        pet.setMicrochipDate(LocalDate.of(2022, 9, 25));

        // when
        ResponseEntity<PetV1> response = controller.createPets(pet);

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        PetV1 responsePet = response.getBody();

        Assertions.assertThat(responsePet).isNotNull();
        Assertions.assertThat(responsePet.getTag().get()).isEqualTo("ABC123-1");
    }

    private boolean isUpperCase(String s) {
        return s.equals(s.toUpperCase(Locale.ROOT));
    }

    private AnimalV1 createAnimal(long id, String name, String tag, BigDecimal costPerDay) {
        Base64.Encoder encoder = Base64.getEncoder();

        AnimalV1 animal = new AnimalV1();

        animal.id(id);
        animal.setName(name);
        animal.setDateOfBirth(OffsetDateTime.now(ZoneOffset.UTC));
        animal.setMicrochipDate(LocalDate.now());

        animal.setTag(DecryptedClearValue.modified(tag));
        animal.setTagEncrypted(encoder.encodeToString(tag.getBytes(Charsets.UTF_8)));

        animal.setCostPerDayEncrypted(
                encoder.encodeToString(costPerDay.toString().getBytes(Charsets.UTF_8)));
        animal.setCostPerDay(DecryptedClearValue.of(costPerDay));

        return animal;
    }
}
