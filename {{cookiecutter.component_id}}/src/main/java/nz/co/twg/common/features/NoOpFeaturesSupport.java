package nz.co.twg.common.features;

/** A {@link FeaturesSupport} implementation that does nothing. */
public final class NoOpFeaturesSupport implements FeaturesSupport {

    @Override
    public void configure(String key, boolean value) {
        // do nothing
    }

    @Override
    public void remove(String key) {
        // do nothing
    }

    @Override
    public void clear() {
        // do nothing
    }
}
