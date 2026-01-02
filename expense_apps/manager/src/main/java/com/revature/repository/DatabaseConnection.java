package com.revature.repository;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Database connection utility for SQLite database.
 * Handles connection management for the shared expense manager database.
 */
public class DatabaseConnection {
    private final String databasePath;

    public DatabaseConnection() {
        try (InputStream input = new FileInputStream("./src/main/resources/config.properties")) {
            Properties prop = new Properties();

            // load a properties file
            prop.load(input);

            // get the property value and print it out
            System.setProperty("databasePath", prop.getProperty("databasePath"));
            //testing print statement
//            System.out.println(prop.getProperty("databasePath"));

        } catch (IOException ex) {
            System.out.println("DB FAILED");
            ex.printStackTrace();
        }

        this.databasePath = System.getenv("DATABASE_PATH") != null
            ? System.getenv("DATABASE_PATH")
            : System.getProperty("databasePath");
    }
    
    public DatabaseConnection(String databasePath) {
        this.databasePath = databasePath;
    }
    
    /**
     * Get a database connection.
     * @return SQLite database connection
     * @throws SQLException if connection fails
     */
    public Connection getConnection() throws SQLException {
        String url = "jdbc:sqlite:" + databasePath;
        return DriverManager.getConnection(url);
    }
}