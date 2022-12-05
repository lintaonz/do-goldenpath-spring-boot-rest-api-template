package nz.co.twg.service.{{cookiecutter.java_package_name}}.config.features;

import nz.co.twg.common.features.FeatureValueProvider;
import nz.co.twg.common.features.Features;
import nz.co.twg.common.features.NoOpFeatureValueProvider;
import nz.co.twg.common.features.StaticSubjectProvider;
import nz.co.twg.common.features.SubjectProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Features configurations. */
@Configuration
public class FeaturesConfig {

    private static final Logger logger = LoggerFactory.getLogger(FeaturesConfig.class);

    /**
    * Create the {@link Features} bean.
    *
    * @param featureValueProvider value provider
    * @param subjectProvider subject provider
    */
    @Bean
    public Features features(
            @Autowired(required = false) FeatureValueProvider featureValueProvider,
            @Autowired(required = false) SubjectProvider subjectProvider) {
        if (featureValueProvider != null && subjectProvider != null) {
            return new Features(featureValueProvider, subjectProvider);
        }
        logger.warn(
                "one of featureValueProvider or subjectProvider is missing. fallback to no-op implementation.");
        return new Features(
                new NoOpFeatureValueProvider(), new StaticSubjectProvider("not configured"));
    }
}
