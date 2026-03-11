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

        String sql = "INSERT INTO price_book (upc, name, price, is_featured, quick_key_position) VALUES (?, ?, ?, ?, ?)";
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

                // Split by tab character (use -1 limit to preserve trailing empty fields)
                String[] fields = line.split("\t", -1);

                // Validate line has exactly 5 fields
                if (fields.length != 5) {
                    System.err.println("Warning: Skipping line " + lineNumber + " - expected 5 fields, found " + fields.length);
                    skippedCount++;
                    continue;
                }

                String upc = fields[0].trim();
                String name = fields[1].trim();
                String priceStr = fields[2].trim();
                String isFeaturedStr = fields[3].trim();
                String quickKeyPositionStr = fields[4].trim();

                // Validate and parse fields
                try {
                    double price = Double.parseDouble(priceStr);
                    boolean isFeatured = Boolean.parseBoolean(isFeaturedStr);
                    Integer quickKeyPosition = null;
                    if (!quickKeyPositionStr.isEmpty() && !quickKeyPositionStr.equalsIgnoreCase("null")) {
                        quickKeyPosition = Integer.parseInt(quickKeyPositionStr);
                    }

                    stmt.setString(1, upc);
                    stmt.setString(2, name);
                    stmt.setDouble(3, price);
                    stmt.setBoolean(4, isFeatured);
                    if (quickKeyPosition != null) {
                        stmt.setInt(5, quickKeyPosition);
                    } else {
                        stmt.setNull(5, java.sql.Types.INTEGER);
                    }
                    stmt.addBatch();
                    productCount++;
                } catch (NumberFormatException e) {
                    System.err.println("Warning: Skipping line " + lineNumber + " - invalid data: " + e.getMessage());
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