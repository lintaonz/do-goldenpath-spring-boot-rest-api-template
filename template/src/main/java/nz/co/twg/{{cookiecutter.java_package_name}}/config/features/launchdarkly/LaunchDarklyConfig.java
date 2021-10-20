package nz.co.twg.{{cookiecutter.java_package_name}}.config.features.launchdarkly;

import com.launchdarkly.sdk.server.Components;
import com.launchdarkly.sdk.server.LDClient;
import com.launchdarkly.sdk.server.LDConfig;
import com.launchdarkly.sdk.server.integrations.FileData;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import nz.co.twg.features.FeatureValueProvider;
import nz.co.twg.features.FeaturesSupport;
import nz.co.twg.features.NoOpFeaturesSupport;
import nz.co.twg.features.StaticSubjectProvider;
import nz.co.twg.features.SubjectProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;

public class LaunchDarklyConfig {

    private static final Logger logger = LoggerFactory.getLogger(LaunchDarklyConfig.class);

    public LaunchDarklyConfig(LaunchDarklyProperties properties) throws IOException {
        if (properties.getDataSource().getType() == LaunchDarklyProperties.DataSource.Type.FILE) {
            // ensure the directory with the file exists
            Path parentDir = Paths.get(properties.getDataSource().getFile().getLocation()).getParent();
            Files.createDirectories(parentDir);
        }
    }

    @Bean
    public LDClient launchDarklyClient(LaunchDarklyProperties properties) {
        LDConfig.Builder configBuilder = new LDConfig.Builder().offline(properties.isOffline());

        LaunchDarklyProperties.DataSource dataSource = properties.getDataSource();
        if (dataSource.getType() == LaunchDarklyProperties.DataSource.Type.FILE) {
            logger.info("datasource is file");
            logger.info("location: {}", dataSource.getFile().getLocation());
            logger.info("auto-reload: {}", dataSource.getFile().isAutoReload());

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

    @Bean
    public FeaturesSupport featuresSupport(LaunchDarklyProperties properties) {
        if (properties.getDataSource().getType() == LaunchDarklyProperties.DataSource.Type.FILE) {
            return new LaunchDarklyFeaturesSupport(properties.getDataSource().getFile().getLocation());
        }
        return new NoOpFeaturesSupport();
    }
}
