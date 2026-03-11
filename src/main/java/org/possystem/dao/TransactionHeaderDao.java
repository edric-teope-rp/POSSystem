package org.possystem.dao;

import org.possystem.database.DatabaseManager;
import org.possystem.entity.TransactionHeader;

import java.sql.*;

/**
 * DAO class for TransactionHeader entity.
 * Handles all database operations for the transaction_header table.
 */
public class TransactionHeaderDao {

    public int insert(TransactionHeader header) throws SQLException {
        String sql = """
                INSERT INTO transaction_header
                (subtotal, discount, tax, total, tender_type, amount_tendered, change_amount, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        Connection conn = DatabaseManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        stmt.setDouble(1, header.subtotal());
        stmt.setDouble(2, header.discount());
        stmt.setDouble(3, header.tax());
        stmt.setDouble(4, header.total());
        stmt.setString(5, header.tenderType());
        stmt.setDouble(6, header.amountTendered());
        stmt.setDouble(7, header.changeAmount());
        stmt.setString(8, header.status());
        stmt.executeUpdate();

        ResultSet generatedKeys = stmt.getGeneratedKeys();
        if (generatedKeys.next()) {
            return generatedKeys.getInt(1);
        }
        return -1;
    }

    public void updateStatus(int id, String status) throws SQLException {
        String sql = "UPDATE transaction_header SET status = ? WHERE id = ?";
        Connection conn = DatabaseManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, status);
        stmt.setInt(2, id);
        stmt.executeUpdate();
    }

    public void updateTotals(int id, double subtotal, double discount, double tax, double total) throws SQLException {
        String sql = """
                UPDATE transaction_header
                SET subtotal = ?, discount = ?, tax = ?, total = ?
                WHERE id = ?
                """;
        Connection conn = DatabaseManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setDouble(1, subtotal);
        stmt.setDouble(2, discount);
        stmt.setDouble(3, tax);
        stmt.setDouble(4, total);
        stmt.setInt(5, id);
        stmt.executeUpdate();
    }

    public void updateTender(int id, String tenderType, double amountTendered, double changeAmount) throws SQLException {
        String sql = """
                UPDATE transaction_header
                SET tender_type = ?, amount_tendered = ?, change_amount = ?
                WHERE id = ?
                """;
        Connection conn = DatabaseManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, tenderType);
        stmt.setDouble(2, amountTendered);
        stmt.setDouble(3, changeAmount);
        stmt.setInt(4, id);
        stmt.executeUpdate();
    }
}