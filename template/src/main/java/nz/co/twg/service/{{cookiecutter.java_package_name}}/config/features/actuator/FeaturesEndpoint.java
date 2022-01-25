package nz.co.twg.service.{{cookiecutter.java_package_name}}.config.features.actuator;

import java.util.Map;
import nz.co.twg.common.features.Features;
import nz.co.twg.common.features.FeaturesSupport;
import nz.co.twg.common.features.NoOpFeaturesSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.stereotype.Component;

/** A feature centric actuator endpoint for management of feature toggles. */
@Component
@Endpoint(id = "features")
public class FeaturesEndpoint {

    private final Features features;

    private final FeaturesSupport featuresSupport;

    public FeaturesEndpoint(
            Features features, @Autowired(required = false) FeaturesSupport featuresSupport) {
        this.features = features;
        this.featuresSupport = featuresSupport != null ? featuresSupport : new NoOpFeaturesSupport();
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
