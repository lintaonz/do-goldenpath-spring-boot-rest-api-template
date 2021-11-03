package nz.co.twg.{{cookiecutter.java_package_name}}.componenttest.util;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ServiceBase {
    private static final Logger logger = LoggerFactory.getLogger(ServiceBase.class);

    private static DatabaseConfig databaseConfig;
    private static final String hostname = "localhost";
    private static String appPort;
    private static String actuatorPort;
    private static String wiremockPort;
    private static String dbPort;

    protected String getHostname() {
        return hostname;
    }

    protected String getAppPort() {
        return appPort;
    }

    protected String getActuatorPort() {
        return actuatorPort;
    }

    protected String getWiremockPort() {
        return wiremockPort;
    }

    protected String getDbPort() {
        return dbPort;
    }

    public static DatabaseConfig getDatabaseConfig() {
        return databaseConfig;
    }

    @BeforeAll
    public static void beforeAll() throws InterruptedException, SQLException {

        // The application is available at
        //    localhost:30000   - when running on local dev machine

        // The application actuator is available at
        //    localhost:30001   - when running on local dev machine

        // setup database first, postgres is available at
        //    localhost:30005   - when running on local dev machine

        // setup mock service, wiremock is available at
        //     http://localhost:30090   - when running on local dev machine

        // Use the defaults one that will be available on local dev
        appPort = System.getProperty("app_port", "8080");
        actuatorPort = System.getProperty("actuator_port", "8050");
        wiremockPort = System.getProperty("wiremock_port", "30090");
        dbPort = System.getProperty("db_port", "30005");

        logger.info("app_port=" + appPort);
        logger.info("actuator_port=" + actuatorPort);
        logger.info("wiremock_port=" + wiremockPort);
        logger.info("db_port=" + dbPort);

        RestAssured.baseURI = "http://" + hostname;
        RestAssured.port = Integer.parseInt(appPort);
        RestAssured.basePath = "/";
        RestAssured.config =
                RestAssuredConfig.config()
                        .httpClient(
                                HttpClientConfig.httpClientConfig()
                                        .setParam("http.socket.timeout", 1000)
                                        .setParam("http.connection.timeout", 1000));
        Awaitility.await()
                .atMost(2, TimeUnit.MINUTES)
                .pollInterval(5, TimeUnit.SECONDS)
                .pollDelay(0, TimeUnit.SECONDS)
                .ignoreExceptions()
                .untilAsserted(
                        () ->
                                assertThat(
                                                given()
                                                        .port(Integer.parseInt(actuatorPort))
                                                        .contentType("application/json")
                                                        .when()
                                                        .get("/health/readiness")
                                                        .then()
                                                        .extract()
                                                        .response()
                                                        .statusCode())
                                        .isEqualTo(200));

        // Create DB connection
        try {
            databaseConfig = new DatabaseConfig(hostname, dbPort);
        } catch (Exception e) {
            logger.error("Exception encountered setting up database resources for tests", e);
        }

        WireMock.configureFor(hostname, Integer.parseInt(wiremockPort));
    }

    @AfterAll
    public static void afterAll() {
        try {
            DatabaseConfig databaseConfig = ServiceBase.getDatabaseConfig();
            if (databaseConfig != null) {
                Connection connection = databaseConfig.getConnection();
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                }
            }
        } catch (Exception e) {
            logger.error("Exception encountered cleaning up database resources for tests", e);
        }
    }
}
