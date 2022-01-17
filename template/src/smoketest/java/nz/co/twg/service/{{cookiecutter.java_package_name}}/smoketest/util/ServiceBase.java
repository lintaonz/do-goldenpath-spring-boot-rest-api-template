package nz.co.twg.service.{{cookiecutter.java_package_name}}.smoketest.util;

import java.sql.SQLException;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ServiceBase {
    private static final Logger logger = LoggerFactory.getLogger(ServiceBase.class);

    private static String applicationBaseUrl;
    private static String actuatorBaseUrl;

    public static String getApplicationBaseUrl() {
        return applicationBaseUrl;
    }

    public static String getActuatorBaseUrl() {
        return actuatorBaseUrl;
    }

    @BeforeAll
    public static void beforeAll() throws InterruptedException, SQLException {

        // Use the defaults one that will be available on local dev
        applicationBaseUrl = System.getProperty("application_base_url", "http://localhost:8080");
        actuatorBaseUrl = System.getProperty("actuator_base_url", "http://localhost:8050");
    }
}
