package nz.co.twg.service.{{cookiecutter.java_package_name}}.componenttest.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {

    private static Connection connection;

    public DatabaseConfig(String hostname, String dbPort) throws SQLException {
        // Jenkins pulls from command line args. Reading from env variable
        String dbUrl =
                System.getProperty(
                        "SPRING_DATASOURCE_URL", "jdbc:postgresql://" + hostname + ":" + dbPort + "/mydb");
        String dbUser = System.getProperty("SPRING_DATASOURCE_USERNAME", "myuser");
        String dbPassword = System.getProperty("SPRING_DATASOURCE_PASSWORD", "mypassword");
        connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection con) {
        DatabaseConfig.connection = con;
    }
}
