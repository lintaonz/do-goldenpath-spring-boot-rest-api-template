package nz.co.twg.{{cookiecutter.java_package_name}}.config.features.launchdarkly;

import com.launchdarkly.sdk.LDUser;
import com.launchdarkly.sdk.server.FeatureFlagsState;
import com.launchdarkly.sdk.server.LDClient;
import java.util.LinkedHashMap;
import java.util.Map;
import nz.co.twg.features.FeatureValueProvider;

/** {@link FeatureValueProvider} with LaunchDarkly as its backing implementation */
public final class LaunchDarklyFeatureValueProvider implements FeatureValueProvider {

    private final LDClient ldClient;

    public LaunchDarklyFeatureValueProvider(LDClient ldClient) {
        this.ldClient = ldClient;
    }

    @Override
    public Map<String, Boolean> getAllBoolean(String subject) {
        Map<String, Boolean> values = new LinkedHashMap<>();
        FeatureFlagsState flagsState = ldClient.allFlagsState(new LDUser(subject));
        flagsState
                .toValuesMap()
                .forEach(
                        (k, v) -> {
                            if (!v.isInt() && !v.isNumber() && !v.isString() && !v.isNull()) {
                                values.put(k, v.booleanValue());
                            }
                        });
        return values;
    }

    @Override
    public boolean getBoolean(String key, String subject, boolean defaultValue) {
        return ldClient.boolVariation(key, new LDUser(subject), defaultValue);
    }
}
