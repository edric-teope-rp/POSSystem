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
                    rs.getDouble("price")
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
                    rs.getDouble("price")
            ));
        }
        return items;
    }
}