package nz.co.twg.service.{{cookiecutter.java_package_name}}.config;

import io.micrometer.core.aop.CountedAspect;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Application configuration. */
@Configuration
public class ApplicationConfig {
    /**
    * This CORS policy allows teams to call the API using the OpenAPI UI in backstage - from both dev
    * and prod instances. Do not remove this as we want all teams to be able to explore APIs in
    * backstage for dev and test environments.
    *
    * @return -
    */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry
                        .addMapping("/**")
                        .allowedOrigins("https://backstage-dev.twg.co.nz", "https://backstage.twg.co.nz");
            }
        };
    }

    /** Enables the use of @Counted annotation for custom metrics */
    @Bean
    public CountedAspect countedAspect(MeterRegistry registry) {
        return new CountedAspect(registry);
    }

    /** Enables the use of @Timed annotation for custom metrics */
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
}
