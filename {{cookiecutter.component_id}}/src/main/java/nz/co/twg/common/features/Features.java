package nz.co.twg.common.features;

import java.util.Map;
import java.util.function.BiConsumer;

/**
* An implementation agnostic wrapper around feature flag frameworks to buffer against wide-spread
* usage of vendor specific API/SPI.
*/
public class Features {

    /** provide the value/status of the feature flag. */
    private final FeatureValueProvider featureValueProvider;

    /** provides the subject who queried the feature flag. */
    private final SubjectProvider subjectProvider;

    public Features(FeatureValueProvider featureValueProvider, SubjectProvider subjectProvider) {
        this.featureValueProvider = featureValueProvider;
        this.subjectProvider = subjectProvider;
    }

    public Map<String, Boolean> getAll() {
        return featureValueProvider.getAllBoolean(subjectProvider.get());
    }

    public boolean isActive(String key) {
        return isActive(key, false);
    }

    public boolean isActive(Enum<?> key) {
        return isActive(key, false);
    }

    public boolean isActive(String key, boolean defaultValue) {
        return featureValueProvider.getBoolean(key, subjectProvider.get(), defaultValue);
    }

    public boolean isActive(Enum<?> key, boolean defaultValue) {
        return featureValueProvider.getBoolean(key.name(), subjectProvider.get(), defaultValue);
    }

    public void registerChangeListener(String key, BiConsumer<Boolean, Boolean> consumer) {
        featureValueProvider.onChangeBoolean(key, subjectProvider.get(), consumer);
    }

    public void registerChangeListener(Enum<?> key, BiConsumer<Boolean, Boolean> consumer) {
        featureValueProvider.onChangeBoolean(key.name(), subjectProvider.get(), consumer);
    }
}
