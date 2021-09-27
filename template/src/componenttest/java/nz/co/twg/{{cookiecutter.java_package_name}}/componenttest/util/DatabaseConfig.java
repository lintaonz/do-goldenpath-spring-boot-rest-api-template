package nz.co.twg.{{cookiecutter.java_package_name}}.componenttest.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {

	private static Connection connection;

	public DatabaseConfig() throws SQLException
	{
		String dbUser = System.getProperty("SPRING_DATASOURCE_USERNAME", "myuser");
		//Jenkins pulls from command line args. Reading from env variable
		String dbUrl = System.getProperty("SPRING_DATASOURCE_URL", "jdbc:postgresql://localhost:"+System.getProperty("db_port","30005")+"/mydb");
		String dbPassword = System.getProperty("SPRING_DATASOURCE_PASSWORD", "mypassword");
		connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
	}

	/*
	 * public static DatabaseConfig getInstance() throws SQLException {
	 * if(databaseConfig==null) { databaseConfig=new DatabaseConfig(); } return
	 * databaseConfig;
	 *
	 * }
	 */

	public Connection getConnection() {
		return connection;
	}

	public void setConnection(Connection con) {
		DatabaseConfig.connection = con;
	}
}
