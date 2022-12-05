package nz.co.twg.service.{{cookiecutter.java_package_name}}.config.features.launchdarkly;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.launchdarkly.sdk.server.LDClient;
import java.io.IOException;
import nz.co.twg.common.features.NoOpFeaturesSupport;
import nz.co.twg.common.features.StaticSubjectProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LaunchDarklyConfigTest {

    // object under test
    private LaunchDarklyConfig config;

    @BeforeEach
    void setup() throws IOException {
        config = spy(new LaunchDarklyConfig());
        doNothing().when(config).ensureParentDirectoryExists(any());
    }

    @Test
    void testLaunchDarklyClient_file() throws IOException {
        // given
        LaunchDarklyProperties properties = createFileBasedProperties();

        // when
        config.launchDarklyClient(properties);

        // then
        verify(config).ensureParentDirectoryExists(properties.getDataSource().getFile().getLocation());
    }

    @Test
    void testLaunchDarklyClient_stream() throws IOException {
        // given
        LaunchDarklyProperties properties = createStreamBasedProperties();

        // when
        config.launchDarklyClient(properties);

        // then
        verify(config, never()).ensureParentDirectoryExists(anyString());
    }

    @Test
    void testFeatureValueProvider() throws IOException {
        // given
        LDClient ldClient = mock(LDClient.class);

        // when
        var featureValueProvider = config.featureValueProvider(ldClient);

        // then
        assertTrue(featureValueProvider instanceof LaunchDarklyFeatureValueProvider);
    }

    @Test
    void testSubjectProvider() throws IOException {
        // given
        LaunchDarklyProperties properties = createStreamBasedProperties();

        // when
        var subjectProvider = config.subjectProvider(properties);

        // then
        assertTrue(subjectProvider instanceof StaticSubjectProvider);
    }

    @Test
    void testFeatureSupport_stream() throws IOException {
        // given
        LaunchDarklyProperties properties = createStreamBasedProperties();

        // when
        var featureSupport = config.featuresSupport(properties);

        // then
        assertTrue(featureSupport instanceof NoOpFeaturesSupport);
    }

    @Test
    void testFeatureSupport_file() throws IOException {
        // given
        LaunchDarklyProperties properties = createFileBasedProperties();

        // when
        var featureSupport = config.featuresSupport(properties);

        // then
        assertTrue(featureSupport instanceof LaunchDarklyFeaturesSupport);
    }

    private LaunchDarklyProperties createFileBasedProperties() {
        var file = new LaunchDarklyProperties.DataSource.FileConfig();
        file.setLocation(System.getProperty("user.home"));
        file.setAutoReload(true);

        var datasource = new LaunchDarklyProperties.DataSource();
        datasource.setType(LaunchDarklyProperties.DataSource.Type.FILE);
        datasource.setFile(file);

        var properties = new LaunchDarklyProperties();
        properties.setDataSource(datasource);
        properties.setDefaultUser("test-user");
        properties.setSdkKey("test-sdk-key");
        properties.setOffline(true);

        return properties;
    }

    private LaunchDarklyProperties createStreamBasedProperties() {
        var datasource = new LaunchDarklyProperties.DataSource();
        datasource.setType(LaunchDarklyProperties.DataSource.Type.STREAM);

        var properties = new LaunchDarklyProperties();
        properties.setDataSource(datasource);
        properties.setDefaultUser("test-user");
        properties.setSdkKey("test-sdk-key");
        properties.setOffline(false);

        return properties;
    }
}
