package nz.co.twg.{{cookiecutter.java_package_name}}.config.features.launchdarkly;

import java.lang.annotation.*;
import org.springframework.context.annotation.Import;

/** Enable LaunchDarkly as the feature toggle framework */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({LaunchDarklyConfig.class})
public @interface EnableLaunchDarkly {}
