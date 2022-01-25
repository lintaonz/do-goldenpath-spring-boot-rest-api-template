package nz.co.twg.service.{{cookiecutter.java_package_name}}.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.entity.Pet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** This is an example test for database integration test. */
class PetRepositoryTest extends AbstractDatabaseTest {

    @Autowired PetRepository petRepository;

    @BeforeEach
    public void setUp() {
        useTransaction((entityManager) -> petRepository.deleteAll());
    }

    @AfterEach
    public void tearDown() {
        useTransaction((entityManager) -> petRepository.deleteAll());
    }

    @Test
    void testSaveAndCount() {
        // given
        assertEquals(0, petRepository.count());
        useTransaction(
                (entityManager) -> {
                    entityManager.persist(new Pet(1L, "alice", "dog"));
                    entityManager.persist(new Pet(2L, "bob", "dog"));
                    entityManager.persist(new Pet(3L, "carl", "cat"));
                });

        // when
        long count = petRepository.count();

        // then
        assertEquals(3, count);
    }

    @Test
    void testFindByTag() {
        // given
        assertEquals(0, petRepository.count());

        Pet pet1 = new Pet();
        pet1.setId(1L);
        pet1.setName("alice");
        pet1.setTag("dog");

        Pet pet2 = new Pet();
        pet2.setId(2L);
        pet2.setName("bob");
        pet2.setTag("dog");

        Pet pet3 = new Pet();
        pet3.setId(3L);
        pet3.setName("carl");
        pet3.setTag("cat");

        useTransaction(
                (entityManager) -> {
                    entityManager.persist(pet1);
                    entityManager.persist(pet2);
                    entityManager.persist(pet3);
                });

        // when
        List<Pet> result = petRepository.findByTag("dog");

        // then
        assertEquals(2, result.size());

        Map<Long, Pet> petById = result.stream().collect(Collectors.toMap(Pet::getId, x -> x));

        Pet result1 = petById.get(1L);
        assertNotNull(result1);
        assertEquals("alice", result1.getName());
        assertEquals("dog", result1.getTag());

        Pet result2 = petById.get(2L);
        assertNotNull(result2);
        assertEquals("bob", result2.getName());
        assertEquals("dog", result2.getTag());

        Pet result3 = petById.get(3L);
        assertNull(result3);
    }
}
