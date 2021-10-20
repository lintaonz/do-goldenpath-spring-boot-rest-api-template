package nz.co.twg.features;

public final class NoOpFeaturesSupport implements FeaturesSupport {

    @Override
    public void configure(String key, boolean value) {}

    @Override
    public void remove(String key) {}

    @Override
    public void clear() {}
}
