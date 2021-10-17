package nz.co.twg.{{cookiecutter.java_package_name}}.util;

/** Helper class for mutating feature flags. FOR TESTING ONLY */
public interface FeaturesSupport {

    void configure(String key, boolean value);

    void remove(String key);

    void clear();
}
