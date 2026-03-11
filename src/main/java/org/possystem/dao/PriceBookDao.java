package org.possystem.dao;

import org.possystem.database.DatabaseManager;
import org.possystem.entity.PriceBook;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO class for PriceBook entity.
 * Handles all database operations for the price_book table.
 */
public class PriceBookDao {

    public Optional<PriceBook> findByUpc(String upc) throws SQLException {
        String sql = "SELECT * FROM price_book WHERE upc = ?";
        Connection conn = DatabaseManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, upc);
        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            return Optional.of(new PriceBook(
                    rs.getString("upc"),
                    rs.getString("name"),
                    rs.getDouble("price"),
                    rs.getBoolean("is_featured"),
                    (Integer) rs.getObject("quick_key_position")
            ));
        }
        return Optional.empty();
    }

    public List<PriceBook> findAll() throws SQLException {
        String sql = "SELECT * FROM price_book";
        Connection conn = DatabaseManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery();

        List<PriceBook> items = new ArrayList<>();
        while (rs.next()) {
            items.add(new PriceBook(
                    rs.getString("upc"),
                    rs.getString("name"),
                    rs.getDouble("price"),
                    rs.getBoolean("is_featured"),
                    (Integer) rs.getObject("quick_key_position")
            ));
        }
        return items;
    }

    public List<PriceBook> findFeaturedItems() throws SQLException {
        String sql = "SELECT * FROM price_book WHERE is_featured = TRUE ORDER BY quick_key_position";
        Connection conn = DatabaseManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery();

        List<PriceBook> items = new ArrayList<>();
        while (rs.next()) {
            items.add(new PriceBook(
                    rs.getString("upc"),
                    rs.getString("name"),
                    rs.getDouble("price"),
                    rs.getBoolean("is_featured"),
                    (Integer) rs.getObject("quick_key_position")
            ));
        }
        return items;
    }

    public void update(String upc, String name, double price, boolean isFeatured) throws SQLException {
        String sql = "UPDATE price_book SET name = ?, price = ?, is_featured = ? WHERE upc = ?";
        Connection conn = DatabaseManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, name);
        stmt.setDouble(2, price);
        stmt.setBoolean(3, isFeatured);
        stmt.setString(4, upc);

        int rowsAffected = stmt.executeUpdate();
        if (rowsAffected == 0) {
            throw new SQLException("Product not found with UPC: " + upc);
        }
    }
}