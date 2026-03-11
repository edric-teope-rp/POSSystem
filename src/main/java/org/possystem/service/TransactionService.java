package org.possystem.service;

import org.possystem.dao.TransactionHeaderDao;
import org.possystem.dao.TransactionItemDao;
import org.possystem.entity.TransactionHeader;
import org.possystem.entity.TransactionItem;
import org.possystem.event.PosEvent;
import org.possystem.event.PosEventDispatcher;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service class for Transaction operations.
 * Implements PosEventDispatcher to fire events
 * throughout the transaction lifecycle.
 */
public class TransactionService implements PosEventDispatcher {

    private static final double TAX_RATE = 0.07;

    private final TransactionHeaderDao transactionHeaderDao;
    private final TransactionItemDao transactionItemDao;

    private int currentTransactionId = -1;

    public TransactionService() {
        this.transactionHeaderDao = new TransactionHeaderDao();
        this.transactionItemDao = new TransactionItemDao();
    }

    public void createTransaction() throws SQLException {
        TransactionHeader header = new TransactionHeader(
                0,
                LocalDateTime.now(),
                0.0,
                0.0,
                0.0,
                0.0,
                "",
                0.0,
                0.0,
                "PENDING"
        );
        currentTransactionId = transactionHeaderDao.insert(header);
        dispatchEvent(PosEvent.TRANSACTION_CREATED, currentTransactionId);
    }

    public void addItem(String upc, String name, double unitPrice) throws SQLException {
        TransactionItem item = new TransactionItem(
                0,
                currentTransactionId,
                upc,
                name,
                1,
                unitPrice,
                unitPrice,
                "ACTIVE"
        );
        int itemId = transactionItemDao.insert(item);
        TransactionItem savedItem = new TransactionItem(
                itemId,
                currentTransactionId,
                upc,
                name,
                1,
                unitPrice,
                unitPrice,
                "ACTIVE"
        );
        dispatchEvent(PosEvent.ITEM_ADDED, savedItem);
    }

    public void voidItem(int itemId) throws SQLException {
        transactionItemDao.updateStatus(itemId, "VOIDED");
        dispatchEvent(PosEvent.ITEM_VOIDED, itemId);
    }

    public void voidTransaction() throws SQLException {
        transactionHeaderDao.updateStatus(currentTransactionId, "VOIDED");
        dispatchEvent(PosEvent.TRANSACTION_VOIDED, currentTransactionId);
        currentTransactionId = -1;
    }

    public void updateQuantity(int itemId, int quantity, double unitPrice) throws SQLException {
        double subtotal = quantity * unitPrice;
        transactionItemDao.updateQuantity(itemId, quantity, subtotal);
        dispatchEvent(PosEvent.QUANTITY_UPDATED, itemId);
    }

    public void totalTransaction() throws SQLException {
        List<TransactionItem> items = transactionItemDao.findByTransactionId(currentTransactionId);

        // Calculate subtotal from active items only
        double subtotal = items.stream()
                .filter(item -> item.status().equals("ACTIVE"))
                .mapToDouble(TransactionItem::subtotal)
                .sum();

        double tax = subtotal * TAX_RATE;
        double total = subtotal + tax;

        transactionHeaderDao.updateTotals(currentTransactionId, subtotal, 0.0, tax, total);
        dispatchEvent(PosEvent.TRANSACTION_TOTALLED, total);
    }

    public void processCash(double amountTendered) throws SQLException {
        List<TransactionItem> items = transactionItemDao.findByTransactionId(currentTransactionId);
        double subtotal = items.stream()
                .filter(item -> item.status().equals("ACTIVE"))
                .mapToDouble(TransactionItem::subtotal)
                .sum();
        double total = subtotal + (subtotal * TAX_RATE);
        double changeAmount = amountTendered - total;

        transactionHeaderDao.updateTender(currentTransactionId, "CASH", amountTendered, changeAmount);
        transactionHeaderDao.updateStatus(currentTransactionId, "COMPLETED");
        dispatchEvent(PosEvent.PAYMENT_PROCESSED, changeAmount);
        currentTransactionId = -1;
    }

    public void processCard(String cardId, String cvv, String expiration) throws SQLException {
        transactionHeaderDao.updateTender(currentTransactionId, "CARD", 0.0, 0.0);
        transactionHeaderDao.updateStatus(currentTransactionId, "COMPLETED");
        dispatchEvent(PosEvent.PAYMENT_PROCESSED, null);
        currentTransactionId = -1;
    }

    public int getCurrentTransactionId() {
        return currentTransactionId;
    }
}