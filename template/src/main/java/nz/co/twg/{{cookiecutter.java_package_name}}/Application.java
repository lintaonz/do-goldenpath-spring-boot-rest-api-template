package nz.co.twg.{{cookiecutter.java_package_name}};

import nz.co.twg.{{cookiecutter.java_package_name}}.config.launchdarkly.EnableLaunchDarkly;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableLaunchDarkly
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
