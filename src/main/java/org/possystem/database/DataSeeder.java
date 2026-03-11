package org.possystem.database;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.*;

/**
 * DataSeeder class to pre-populate the price_book table
 * with products on first run.
 * Checks if data already exists before inserting
 * to avoid duplicates on restart.
 */
public class DataSeeder {

    public static void seed() throws SQLException {
        Connection conn = DatabaseManager.getConnection();

        Statement checkStmt = conn.createStatement();
        ResultSet rs = checkStmt.executeQuery("SELECT COUNT(*) FROM price_book");
        rs.next();
        int count = rs.getInt(1);

        if (count > 0) {
            System.out.println("Price book already populated, skipping seed.");
            return;
        }

        // Load TSV file from resources
        InputStream inputStream = DataSeeder.class.getClassLoader().getResourceAsStream("pricebook.tsv");
        if (inputStream == null) {
            throw new SQLException("Could not find pricebook.tsv in resources folder");
        }

        String sql = "INSERT INTO price_book (upc, name, price) VALUES (?, ?, ?)";
        PreparedStatement stmt = conn.prepareStatement(sql);

        int productCount = 0;
        int skippedCount = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                // Skip empty lines
                if (line.trim().isEmpty()) {
                    continue;
                }

                // Split by tab character
                String[] fields = line.split("\t");

                // Validate line has exactly 3 fields
                if (fields.length != 3) {
                    System.err.println("Warning: Skipping line " + lineNumber + " - expected 3 fields, found " + fields.length);
                    skippedCount++;
                    continue;
                }

                String upc = fields[0].trim();
                String name = fields[1].trim();
                String priceStr = fields[2].trim();

                // Validate and parse price
                try {
                    double price = Double.parseDouble(priceStr);

                    stmt.setString(1, upc);
                    stmt.setString(2, name);
                    stmt.setDouble(3, price);
                    stmt.addBatch();
                    productCount++;
                } catch (NumberFormatException e) {
                    System.err.println("Warning: Skipping line " + lineNumber + " - invalid price: " + priceStr);
                    skippedCount++;
                }
            }
        } catch (Exception e) {
            throw new SQLException("Error reading pricebook.tsv: " + e.getMessage(), e);
        }

        stmt.executeBatch();
        System.out.println("Price book seeded with " + productCount + " products successfully!");
        if (skippedCount > 0) {
            System.out.println("Warning: " + skippedCount + " lines were skipped due to formatting errors.");
        }
    }
}