package nz.co.twg.{{cookiecutter.java_package_name}}.config.actuator;

import java.util.Map;
import nz.co.twg.{{cookiecutter.java_package_name}}.util.FeaturesSupport;
import nz.co.twg.features.Features;
import org.springframework.boot.actuate.endpoint.annotation.*;
import org.springframework.stereotype.Component;

/** A feature centric actuator endpoint for management of feature toggles */
@Component
@Endpoint(id = "features")
public class FeaturesEndpoint {

    private final FeaturesSupport featuresSupport;

    private final Features features;

    public FeaturesEndpoint(FeaturesSupport featuresSupport, Features features) {
        this.featuresSupport = featuresSupport;
        this.features = features;
    }

    @ReadOperation
    public Map<String, Boolean> getAll() {
        return this.features.getAll();
    }

    @ReadOperation
    public boolean get(@Selector String key) {
        return this.features.isActive(key);
    }

    @WriteOperation
    public void configure(@Selector String key, boolean value) {
        this.featuresSupport.configure(key, value);
    }

    @DeleteOperation
    public void delete(@Selector String key) {
        this.featuresSupport.remove(key);
    }

    @DeleteOperation
    public void deleteAll() {
        this.featuresSupport.clear();
    }
}
