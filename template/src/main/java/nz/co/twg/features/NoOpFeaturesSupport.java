package nz.co.twg.features;

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
