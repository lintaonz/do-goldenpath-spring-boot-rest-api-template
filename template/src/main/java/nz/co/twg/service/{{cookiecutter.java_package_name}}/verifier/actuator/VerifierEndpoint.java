package nz.co.twg.service.{{cookiecutter.java_package_name}}.verifier.actuator;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.stereotype.Component;

/**
* Endpoint to expose a verifier which can be used to verify the application. e.g. To use in smoke
* tests.
*/
@Component
@Endpoint(id = "verifier")
public class VerifierEndpoint {

    /** Get all verifier values. */
    @ReadOperation
    public Map<String, Boolean> getAll() {
        Map<String, Boolean> verifierResult = new HashMap<>();
        verifierResult.put("startup", true);
        verifierResult.put("integration-x", true);
        return verifierResult;
    }

    /** Get verifier value. */
    @ReadOperation
    public boolean get(@Selector String key) {
        boolean response = false;
        if ("startup".equals(key) || "integration-x".equals(key)) {
            response = true;
        }
        return response;
    }
}
