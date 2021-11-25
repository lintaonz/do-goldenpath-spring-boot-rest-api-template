package nz.co.twg.service.{{cookiecutter.java_package_name}}.services;

import nz.co.twg.common.features.Features;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SampleService {

    private final Features features;

    @Autowired
    public SampleService(Features features) {
        this.features = features;
    }

    public boolean isFeatureActive(String key) {
        return features.isActive(key);
    }

    public String featureDependentLogic() {
        if (features.isActive("feature-A")) {
            return "Apple";
        } else {
            return "Banana";
        }
    }
}
