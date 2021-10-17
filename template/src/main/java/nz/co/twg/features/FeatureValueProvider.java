package nz.co.twg.features;

import java.util.Map;

/**
* Provide the value/status of the feature flag. An implementation of this interface should be done
* for each specific feature-flag frameworks e.g. LaunchDarkly, Togglz, etc
*/
public interface FeatureValueProvider {

    /** @param subject the subject who queried the feature flag */
    Map<String, Boolean> getAllBoolean(String subject);

    /**
    * @param key the key of the feature flag
    * @param subject the subject who queried the feature flag
    * @param defaultValue the default value
    */
    boolean getBoolean(String key, String subject, boolean defaultValue);
}
