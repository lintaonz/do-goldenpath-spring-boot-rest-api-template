package nz.co.twg.service.{{cookiecutter.java_package_name}}.controller;

import static java.util.stream.Collectors.toList;

import java.util.Collections;
import java.util.List;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.openapi.client.api.PetsApiClient;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.openapi.server.api.PetsApi;
import nz.co.twg.service.{{cookiecutter.java_package_name}}.openapi.server.model.PetV1;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Sample controller for performing an outbound API call using the generated OpenFeign client */
@RestController
@RequestMapping("/api/remote")
public class PetRemoteController implements PetsApi {

    private final PetsApiClient petsClient;

    public PetRemoteController(PetsApiClient petsClient) {
        this.petsClient = petsClient;
    }

    @Override
    public ResponseEntity<List<PetV1>> listPets(Long limit) {

        var body = petsClient.listPets(limit).getBody();
        List<PetV1> pets =
                body != null
                        ? body.stream().map(this::mapRemotePet).collect(toList())
                        : Collections.emptyList();
        return new ResponseEntity<>(pets, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<PetV1> showPetById(String petId) {
        var response = petsClient.showPetById(petId);
        if (response.getBody() != null) {
            PetV1 pet = mapRemotePet(response.getBody());
            return new ResponseEntity<>(pet, HttpStatus.OK);
        }
        throw new IllegalStateException("illegal state: empty response body");
    }

    @Override
    public ResponseEntity<Void> createPets() {
        throw new NotImplementedException("not implemented.");
    }

    // spotless:off
    private PetV1 mapRemotePet(
            nz.co.twg.service.{{cookiecutter.java_package_name}}.openapi.client.model.PetV1 client) {
        PetV1 server = new PetV1();
        BeanUtils.copyProperties(client, server);
        return server;
    }
    // spotless:on
}
