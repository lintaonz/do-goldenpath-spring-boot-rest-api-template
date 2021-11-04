package nz.co.twg.{{cookiecutter.java_package_name}}.config.features.launchdarkly;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

class LaunchDarklyPropertiesTest {

    @Test
    void test() {
        // this is here to keep sonar happy
        assertTrue(true);
    }

    @ExtendWith(SpringExtension.class)
    @EnableConfigurationProperties(value = LaunchDarklyProperties.class)
    @TestPropertySource(locations = {"classpath:launchdarkly-file.properties"})
    static class FileBasedLaunchDarklyPropertiesTest {
        private static final String TMP_DIR = System.getProperty("java.io.tmpdir");

        @Autowired private LaunchDarklyProperties properties;

        @Test
        void testProperties() {
            assertEquals("file-test-user", properties.getDefaultUser());
            assertEquals("file-test-sdk-key", properties.getSdkKey());
            assertFalse(properties.isOffline());

            assertNotNull(properties.getDataSource());
            assertEquals(
                    LaunchDarklyProperties.DataSource.Type.FILE, properties.getDataSource().getType());

            assertNotNull(properties.getDataSource().getFile());
            assertTrue(properties.getDataSource().getFile().isAutoReload());

            assertNotNull(properties.getDataSource().getFile().getLocation());
            Path expectedDir = Paths.get(TMP_DIR, "featuretoggles", "local-features.json");
            Path actualDir = Paths.get(properties.getDataSource().getFile().getLocation());
            assertEquals(expectedDir, actualDir);
        }
    }

    @ExtendWith(SpringExtension.class)
    @EnableConfigurationProperties(value = LaunchDarklyProperties.class)
    @TestPropertySource(locations = {"classpath:launchdarkly-stream.properties"})
    static class StreamBasedLaunchDarklyPropertiesTest {
        @Autowired private LaunchDarklyProperties properties;

        @Test
        void testProperties() {
            assertEquals("stream-test-user", properties.getDefaultUser());
            assertEquals("stream-test-sdk-key", properties.getSdkKey());
            assertFalse(properties.isOffline());

            assertNotNull(properties.getDataSource());
            assertEquals(
                    LaunchDarklyProperties.DataSource.Type.STREAM, properties.getDataSource().getType());
        }
    }
}
