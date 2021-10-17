package nz.co.twg.{{cookiecutter.java_package_name}}.controllers;

import nz.co.twg.{{cookiecutter.java_package_name}}.services.SampleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sample")
public class SampleController {

    private final SampleService sampleService;

    public SampleController(SampleService sampleService) {
        this.sampleService = sampleService;
    }

    @GetMapping("featureDependentEndpoint")
    public String featureDependentEndpoint() {
        return this.sampleService.featureDependentLogic();
    }
}
