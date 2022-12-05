package nz.co.twg.service.{{cookiecutter.java_package_name}}.config.features.launchdarkly;

import com.launchdarkly.sdk.server.Components;
import com.launchdarkly.sdk.server.LDClient;
import com.launchdarkly.sdk.server.LDConfig;
import com.launchdarkly.sdk.server.integrations.FileData;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import nz.co.twg.common.features.FeatureValueProvider;
import nz.co.twg.common.features.FeaturesSupport;
import nz.co.twg.common.features.NoOpFeaturesSupport;
import nz.co.twg.common.features.StaticSubjectProvider;
import nz.co.twg.common.features.SubjectProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;

/** LaunchDarkly configuration. */
public class LaunchDarklyConfig {

    private static final Logger logger = LoggerFactory.getLogger(LaunchDarklyConfig.class);

    /** Create the LaunchDarkly client based on the provided {@link LaunchDarklyProperties}. */
    @Bean
    public LDClient launchDarklyClient(LaunchDarklyProperties properties) throws IOException {
        LDConfig.Builder configBuilder = new LDConfig.Builder().offline(properties.isOffline());

        LaunchDarklyProperties.DataSource dataSource = properties.getDataSource();
        if (dataSource.getType() == LaunchDarklyProperties.DataSource.Type.FILE) {
            logger.info("datasource is file");
            logger.info("location: {}", dataSource.getFile().getLocation());
            logger.info("auto-reload: {}", dataSource.getFile().isAutoReload());
            ensureParentDirectoryExists(dataSource.getFile().getLocation());

            configBuilder.dataSource(
                    FileData.dataSource()
                            .filePaths(dataSource.getFile().getLocation())
                            .autoUpdate(dataSource.getFile().isAutoReload()));
            // if file source, then don't publish events
            configBuilder.events(Components.noEvents());
        } else {
            logger.info("datasource is stream");
        }

        // never send diagnostic data
        configBuilder.diagnosticOptOut(true);

        return new LDClient(properties.getSdkKey(), configBuilder.build());
    }

    @Bean
    public FeatureValueProvider featureValueProvider(LDClient ldClient) {
        return new LaunchDarklyFeatureValueProvider(ldClient);
    }

    @Bean
    public SubjectProvider subjectProvider(LaunchDarklyProperties properties) {
        return new StaticSubjectProvider(properties.getDefaultUser());
    }

    /**
    * {@link FeaturesSupport} is only meant to be valid in a testing environment, therefore only
    * initialise a {@link LaunchDarklyFeaturesSupport} if the data source is configured to be read
    * from a file.
    */
    @Bean
    public FeaturesSupport featuresSupport(LaunchDarklyProperties properties) throws IOException {
        if (properties.getDataSource().getType() == LaunchDarklyProperties.DataSource.Type.FILE) {
            ensureParentDirectoryExists(properties.getDataSource().getFile().getLocation());
            return new LaunchDarklyFeaturesSupport(properties.getDataSource().getFile().getLocation());
        }
        return new NoOpFeaturesSupport();
    }

    void ensureParentDirectoryExists(String path) throws IOException {
        Path parentDir = Paths.get(path).getParent();
        if (Files.notExists(parentDir)) {
            Files.createDirectories(parentDir);
        }
    }
}
