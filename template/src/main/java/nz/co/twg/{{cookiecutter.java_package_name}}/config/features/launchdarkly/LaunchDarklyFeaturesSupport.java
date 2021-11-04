package nz.co.twg.{{cookiecutter.java_package_name}}.config.features.launchdarkly;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import nz.co.twg.features.FeaturesSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* A helper class to update the feature source file consumed by launch darkly. When the given file
* is modified, LaunchDarkly SDK will automatically pick up the new changes and update itself. <br>
* Note: Due to the limitation of LaunchDarkly only officially endorse file based import for
* testing, and the services deployed in a kube cluster shared by many tests, the concurrency nature
* of the tests will be reduced for any tests relying on feature toggles.
*/
public class LaunchDarklyFeaturesSupport implements FeaturesSupport {

    private final Logger logger = LoggerFactory.getLogger(LaunchDarklyFeaturesSupport.class);

    private final Path fileLocation;

    private ObjectMapper objectMapper;

    public LaunchDarklyFeaturesSupport(String fileLocation) {
        this.fileLocation = Paths.get(fileLocation);
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public synchronized void configure(String key, boolean value) {
        logger.info("will set feature [{}] to [{}]", key, value);
        ensureFileExists();
        FlagValuesWrapper flagValues = read();
        flagValues.getFlagValues().put(key, value);
        write(flagValues);
        logger.info("did set feature [{}] to [{}]", key, value);
    }

    @Override
    public synchronized void remove(String key) {
        logger.info("will remove feature [{}]", key);
        ensureFileExists();
        FlagValuesWrapper flagValues = read();
        flagValues.getFlagValues().remove(key);
        write(flagValues);
        logger.info("did remove feature [{}]", key);
    }

    @Override
    public synchronized void clear() {
        logger.info("will clear all features");
        write(new FlagValuesWrapper());
        logger.info("did clear all features");
    }

    // exposed for testing
    void ensureFileExists() {
        if (!this.fileLocation.toFile().exists()) {
            write(new FlagValuesWrapper());
        }
    }

    // exposed for testing
    FlagValuesWrapper read() {
        try {
            return objectMapper.readValue(Files.readString(this.fileLocation), FlagValuesWrapper.class);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "unexpected exception - reading from file [" + this.fileLocation + "]", e);
        }
    }

    // exposed for testing
    void write(FlagValuesWrapper flagValues) {
        try {
            if (this.fileLocation.toFile().exists()) {
                Path parentDir = this.fileLocation.getParent();
                Files.createDirectories(parentDir);
            }
            Files.writeString(
                    this.fileLocation, objectMapper.writeValueAsString(flagValues), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "unexpected exception - writing to file [" + this.fileLocation + "]", e);
        }
    }

    // exposed for testing
    void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** The object structure expected by LaunchDarkly file input */
    static class FlagValuesWrapper {

        private Map<String, Boolean> flagValues;

        public FlagValuesWrapper() {
            flagValues = new LinkedHashMap<>();
        }

        public Map<String, Boolean> getFlagValues() {
            return flagValues;
        }

        public void setFlagValues(Map<String, Boolean> flagValues) {
            this.flagValues = flagValues;
        }
    }
}
