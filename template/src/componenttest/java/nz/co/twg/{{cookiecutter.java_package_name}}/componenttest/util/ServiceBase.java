package nz.co.twg.{{cookiecutter.java_package_name}}.componenttest.util;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.restassured.RestAssured;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

public class ServiceBase {
    private static DatabaseConfig databaseConfig;

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
        String appPort = System.getProperty("app_port", "30000");
        String actuatorPort = System.getProperty("actuator_port", "30001");
        String wiremockPort = System.getProperty("wiremock_port", "30090");
        String dbPort = System.getProperty("db_port", "30005");

        System.out.println("app_port=" + appPort);
        System.out.println("actuator_port=" + actuatorPort);
        System.out.println("wiremock_port=" + wiremockPort);
        System.out.println("db_port=" + dbPort);

        // Create DB connection
        try {
            databaseConfig = new DatabaseConfig();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        RestAssured.baseURI = "http://localhost";
        RestAssured.port = Integer.parseInt(appPort);
        RestAssured.basePath = "/";

        WireMock.configureFor("localhost", Integer.parseInt(wiremockPort));

        Awaitility.await()
                .atMost(2, TimeUnit.MINUTES)
                .pollInterval(5, TimeUnit.SECONDS)
                .pollDelay(0, TimeUnit.SECONDS)
                .ignoreExceptions()
                .until(
                        () ->
                                given()
                                        .port(Integer.parseInt(actuatorPort))
                                        .contentType("application/json")
                                        .when()
                                        .get("/actuator/health/readiness")
                                        .then()
                                        .extract()
                                        .response()
                                        .statusCode(),
                        equalTo(200));
    }

    @AfterAll
    public static void afterAll() throws SQLException {
        ServiceBase.getDatabaseConfig().getConnection().close();
    }
}
