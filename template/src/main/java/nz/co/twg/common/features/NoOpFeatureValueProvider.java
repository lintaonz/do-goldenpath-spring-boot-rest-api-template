package nz.co.twg.common.features;

import java.util.Collections;
import java.util.Map;
import java.util.function.BiConsumer;

/** An empty implementation of the {@link FeatureValueProvider} */
public final class NoOpFeatureValueProvider implements FeatureValueProvider {

    @Override
    public Map<String, Boolean> getAllBoolean(String subject) {
        return Collections.emptyMap();
    }

    @Override
    public boolean getBoolean(String key, String subject, boolean defaultValue) {
        return defaultValue;
    }

    @Override
    public void onChangeBoolean(String key, String subject, BiConsumer<Boolean, Boolean> consumer) {
        // do nothing
    }
}
