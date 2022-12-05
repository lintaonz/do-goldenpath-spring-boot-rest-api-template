package nz.co.twg.service.{{cookiecutter.java_package_name}}.config.features.launchdarkly;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;

/** Enable LaunchDarkly as the feature toggle framework. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(LaunchDarklyConfig.class)
public @interface EnableLaunchDarkly {}
