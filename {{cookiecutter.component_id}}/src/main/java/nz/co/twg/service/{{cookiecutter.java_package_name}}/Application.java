package nz.co.twg.service.{{cookiecutter.java_package_name}};

import nz.co.twg.service.{{cookiecutter.java_package_name}}.config.features.launchdarkly.EnableLaunchDarkly;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/** Main application entry point. */
@SpringBootApplication
@EnableLaunchDarkly
@EnableFeignClients
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
