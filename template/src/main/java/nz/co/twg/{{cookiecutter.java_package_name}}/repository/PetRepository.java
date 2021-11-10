package nz.co.twg.{{cookiecutter.java_package_name}}.repository;

import java.util.List;
import nz.co.twg.{{cookiecutter.java_package_name}}.entity.Pet;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PetRepository extends CrudRepository<Pet, Long> {

    List<Pet> findByTag(String tag);
}
