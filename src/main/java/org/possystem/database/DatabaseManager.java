package org.possystem.database;

import org.h2.tools.Server;
import java.sql.*;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:h2:~/possystemdb";
    private static final String DB_USER = "sa";
    private static final String DB_PASS = "";
    private static Connection connection;

    public static void initialize() {
        try {
            Server webServer = Server.createWebServer(
                    "-web", "-webAllowOthers", "-webPort", "8082"
            );
            webServer.start();
            System.out.println("H2 Console available at: http://localhost:8082");

            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            System.out.println("Database connected successfully!");

            createTables();
            DataSeeder.seed();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void createTables() throws SQLException {
        Statement stmt = connection.createStatement();

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS price_book (
                upc                 VARCHAR(50) PRIMARY KEY,
                name                VARCHAR(100) NOT NULL,
                price               DOUBLE NOT NULL,
                is_featured         BOOLEAN DEFAULT FALSE,
                quick_key_position  INT DEFAULT NULL
            )
        """);

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS transaction_header (
                id                      INT AUTO_INCREMENT PRIMARY KEY,
                transaction_datetime    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                subtotal                DOUBLE NOT NULL,
                discount                DOUBLE NOT NULL,
                tax                     DOUBLE NOT NULL,
                total                   DOUBLE NOT NULL,
                tender_type             VARCHAR(20) NOT NULL,
                amount_tendered         DOUBLE NOT NULL,
                change_amount           DOUBLE NOT NULL,
                status                  VARCHAR(20) DEFAULT 'PENDING'
            )
        """);

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS transaction_items (
                id              INT AUTO_INCREMENT PRIMARY KEY,
                transaction_id  INT NOT NULL,
                upc             VARCHAR(50) NOT NULL,
                name            VARCHAR(100) NOT NULL,
                quantity        INT NOT NULL,
                unit_price      DOUBLE NOT NULL,
                subtotal        DOUBLE NOT NULL,
                status          VARCHAR(20) DEFAULT 'ACTIVE',
                FOREIGN KEY (transaction_id) REFERENCES transaction_header(id),
                FOREIGN KEY (upc) REFERENCES price_book(upc)
            )
        """);

        System.out.println("Tables created successfully!");
    }

    public static Connection getConnection() {
        return connection;
    }
}