package nz.co.twg.service.{{cookiecutter.java_package_name}};

import nz.co.twg.service.{{cookiecutter.java_package_name}}.config.features.launchdarkly.EnableLaunchDarkly;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableLaunchDarkly
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
