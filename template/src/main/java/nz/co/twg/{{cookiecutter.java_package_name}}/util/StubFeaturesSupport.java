package nz.co.twg.{{cookiecutter.java_package_name}}.util;

import org.apache.commons.lang3.NotImplementedException;

public class StubFeaturesSupport implements FeaturesSupport {

    @Override
    public void configure(String key, boolean value) {
        throw new NotImplementedException();
    }

    @Override
    public void remove(String key) {
        throw new NotImplementedException();
    }

    @Override
    public void clear() {
        throw new NotImplementedException();
    }
}
