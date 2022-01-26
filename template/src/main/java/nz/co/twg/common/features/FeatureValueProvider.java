package nz.co.twg.common.features;

import java.util.Map;
import java.util.function.BiConsumer;

/**
* Provide the value/status of the feature flag. An implementation of this interface should be done
* for each specific feature-flag frameworks e.g. LaunchDarkly, Togglz, etc
*/
public interface FeatureValueProvider {

    /**
    * Get feature flags with boolean type.
    *
    * @param subject the subject who queried the feature flag
    */
    Map<String, Boolean> getAllBoolean(String subject);

    /**
    * Get the boolean feature flag.
    *
    * @param key the key of the feature flag
    * @param subject the subject who queried the feature flag
    * @param defaultValue the default value
    */
    boolean getBoolean(String key, String subject, boolean defaultValue);

    /**
    * Register an on change listener on a specific boolean feature flag.
    *
    * @param key the key of the feature flag
    * @param subject the subject who queried the feature flag
    * @param consumer a consumer that returns the old and new value of the change event
    */
    void onChangeBoolean(String key, String subject, BiConsumer<Boolean, Boolean> consumer);
}
