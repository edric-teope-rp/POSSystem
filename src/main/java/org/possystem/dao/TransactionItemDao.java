package org.possystem.dao;

import org.possystem.database.DatabaseManager;
import org.possystem.entity.TransactionItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO class for TransactionItem entity.
 * Handles all database operations for the transaction_items table.
 */
public class TransactionItemDao {

    public int insert(TransactionItem item) throws SQLException {
        String sql = """
                INSERT INTO transaction_items
                (transaction_id, upc, name, quantity, unit_price, subtotal, status)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        Connection conn = DatabaseManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        stmt.setInt(1, item.transactionId());
        stmt.setString(2, item.upc());
        stmt.setString(3, item.name());
        stmt.setInt(4, item.quantity());
        stmt.setDouble(5, item.unitPrice());
        stmt.setDouble(6, item.subtotal());
        stmt.setString(7, item.status());
        stmt.executeUpdate();

        ResultSet generatedKeys = stmt.getGeneratedKeys();
        if (generatedKeys.next()) {
            return generatedKeys.getInt(1);
        }
        return -1;
    }

    public List<TransactionItem> findByTransactionId(int transactionId) throws SQLException {
        String sql = "SELECT * FROM transaction_items WHERE transaction_id = ?";
        Connection conn = DatabaseManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, transactionId);
        ResultSet rs = stmt.executeQuery();

        List<TransactionItem> items = new ArrayList<>();
        while (rs.next()) {
            items.add(new TransactionItem(
                    rs.getInt("id"),
                    rs.getInt("transaction_id"),
                    rs.getString("upc"),
                    rs.getString("name"),
                    rs.getInt("quantity"),
                    rs.getDouble("unit_price"),
                    rs.getDouble("subtotal"),
                    rs.getString("status")
            ));
        }
        return items;
    }

    public void updateStatus(int id, String status) throws SQLException {
        String sql = "UPDATE transaction_items SET status = ? WHERE id = ?";
        Connection conn = DatabaseManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, status);
        stmt.setInt(2, id);
        stmt.executeUpdate();
    }

    public void updateQuantity(int id, int quantity, double subtotal) throws SQLException {
        String sql = "UPDATE transaction_items SET quantity = ?, subtotal = ? WHERE id = ?";
        Connection conn = DatabaseManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, quantity);
        stmt.setDouble(2, subtotal);
        stmt.setInt(3, id);
        stmt.executeUpdate();
    }

    public TransactionItem findActiveItemByUpc(int transactionId, String upc) throws SQLException {
        String sql = "SELECT * FROM transaction_items WHERE transaction_id = ? AND upc = ? AND status = 'ACTIVE'";
        Connection conn = DatabaseManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, transactionId);
        stmt.setString(2, upc);
        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            return new TransactionItem(
                    rs.getInt("id"),
                    rs.getInt("transaction_id"),
                    rs.getString("upc"),
                    rs.getString("name"),
                    rs.getInt("quantity"),
                    rs.getDouble("unit_price"),
                    rs.getDouble("subtotal"),
                    rs.getString("status")
            );
        }
        return null;
    }
}