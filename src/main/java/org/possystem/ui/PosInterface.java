package org.possystem.ui;

import org.possystem.entity.PriceBook;
import org.possystem.entity.TransactionItem;
import org.possystem.service.PriceBookService;
import org.possystem.service.TransactionService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Main POS Interface with 3 zones:
 * - Quick Keys (12 product buttons)
 * - Actions (transaction controls)
 * - Current Sale (cart display)
 */
public class PosInterface extends JFrame {

    // Services
    private final PriceBookService priceBookService;
    private final TransactionService transactionService;

    // UPC Input Components
    private JTextField upcTextField;
    private JButton searchButton;
    private JLabel itemInfoLabel;
    private JButton addToCartButton;

    // Quick Keys Panel
    private JPanel quickKeysPanel;
    private List<JButton> quickKeyButtons;

    // Current Sale Table
    private JTable saleTable;
    private DefaultTableModel tableModel;

    // Totals Display
    private JLabel subtotalLabel;
    private JLabel taxLabel;
    private JLabel totalLabel;

    // Actions Panel Buttons
    private JButton voidTransactionButton;
    private JButton payCashButton;
    private JButton payCardButton;
    private JButton deleteSelectedButton;

    // State
    private PriceBook searchedItem; // Item from manual search

    public PosInterface() {
        this.priceBookService = new PriceBookService();
        this.transactionService = new TransactionService();

        setupFrame();
        initializeComponents();
        layoutComponents();
        attachEventHandlers();

        // Create initial transaction
        try {
            transactionService.createTransaction();
            System.out.println("Initial transaction created");
        } catch (SQLException e) {
            showError("Failed to create initial transaction: " + e.getMessage());
        }

        refreshSaleDisplay();
    }

    private void setupFrame() {
        setTitle("POS System - Point of Sale");
        setSize(1400, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    private void initializeComponents() {
        // UPC Input Components
        upcTextField = new JTextField(20);
        searchButton = new JButton("Search");
        itemInfoLabel = new JLabel("");
        addToCartButton = new JButton("Add to Cart");
        addToCartButton.setVisible(false);

        // Quick Keys Panel
        quickKeysPanel = new JPanel(new GridLayout(3, 4, 10, 10));
        quickKeysPanel.setBorder(BorderFactory.createTitledBorder("Quick Keys"));
        quickKeyButtons = new ArrayList<>();

        // Load featured items for quick keys
        loadQuickKeys();

        // Current Sale Table
        String[] columns = {"Select", "ID", "Name", "Qty", "Price", "Line Total"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 0) return Boolean.class; // Checkbox column
                return String.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0 || column == 3; // Select and Qty editable
            }
        };
        saleTable = new JTable(tableModel);
        saleTable.getColumnModel().getColumn(0).setMaxWidth(50); // Select checkbox
        saleTable.getColumnModel().getColumn(1).setMaxWidth(50); // ID
        saleTable.getColumnModel().getColumn(3).setMaxWidth(50); // Qty

        // Totals Display
        subtotalLabel = new JLabel("Subtotal: $0.00");
        taxLabel = new JLabel("Tax (7%): $0.00");
        totalLabel = new JLabel("Total: $0.00");
        totalLabel.setFont(new Font("Arial", Font.BOLD, 18));

        // Actions Panel Buttons
        voidTransactionButton = new JButton("Void Transaction");
        payCashButton = new JButton("Pay Cash");
        payCardButton = new JButton("Pay Card");
        deleteSelectedButton = new JButton("Delete Selected");

        // Style action buttons
        voidTransactionButton.setBackground(new Color(255, 100, 100));
        payCashButton.setBackground(new Color(100, 200, 100));
        payCardButton.setBackground(new Color(100, 150, 255));
        deleteSelectedButton.setBackground(new Color(255, 150, 100));
    }

    private void loadQuickKeys() {
        try {
            List<PriceBook> featuredItems = priceBookService.getFeaturedItems();
            System.out.println("Loading " + featuredItems.size() + " quick key items");

            for (PriceBook item : featuredItems) {
                JButton btn = new JButton("<html><center>" +
                    item.name().substring(0, Math.min(20, item.name().length())) +
                    "<br>$" + String.format("%.2f", item.price()) + "</center></html>");
                btn.setPreferredSize(new Dimension(120, 80));

                // Add click handler
                btn.addActionListener(e -> addItemToSale(item));

                quickKeyButtons.add(btn);
                quickKeysPanel.add(btn);
            }

            // Fill empty slots if less than 12 items
            while (quickKeyButtons.size() < 12) {
                JButton emptyBtn = new JButton("---");
                emptyBtn.setEnabled(false);
                quickKeysPanel.add(emptyBtn);
            }

        } catch (SQLException e) {
            showError("Failed to load quick keys: " + e.getMessage());
        }
    }

    private void layoutComponents() {
        setLayout(new BorderLayout(10, 10));

        // TOP: UPC Input Panel
        JPanel upcPanel = createUpcInputPanel();
        add(upcPanel, BorderLayout.NORTH);

        // LEFT: Quick Keys
        add(quickKeysPanel, BorderLayout.WEST);

        // CENTER: Current Sale
        JPanel centerPanel = createCurrentSalePanel();
        add(centerPanel, BorderLayout.CENTER);

        // RIGHT: Actions Panel
        JPanel actionsPanel = createActionsPanel();
        add(actionsPanel, BorderLayout.EAST);
    }

    private JPanel createUpcInputPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Barcode / UPC Entry"));

        panel.add(new JLabel("UPC:"));
        panel.add(upcTextField);
        panel.add(searchButton);
        panel.add(itemInfoLabel);
        panel.add(addToCartButton);

        return panel;
    }

    private JPanel createCurrentSalePanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Current Sale"));

        // Table with scroll
        JScrollPane scrollPane = new JScrollPane(saleTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Totals panel at bottom
        JPanel totalsPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        totalsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        totalsPanel.add(subtotalLabel);
        totalsPanel.add(taxLabel);
        totalsPanel.add(totalLabel);
        panel.add(totalsPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createActionsPanel() {
        JPanel panel = new JPanel(new GridLayout(10, 1, 10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Actions"));
        panel.setPreferredSize(new Dimension(200, 0));

        panel.add(deleteSelectedButton);
        panel.add(new JSeparator());
        panel.add(voidTransactionButton);
        panel.add(new JSeparator());
        panel.add(payCashButton);
        panel.add(payCardButton);

        // Spacer
        panel.add(new JLabel(""));

        return panel;
    }

    private void attachEventHandlers() {
        // UPC TextField - Enter key triggers auto-add
        upcTextField.addActionListener(e -> handleUpcEnter());

        // Search button - Manual search
        searchButton.addActionListener(e -> handleManualSearch());

        // Add to Cart button (appears after manual search)
        addToCartButton.addActionListener(e -> handleManualAddToCart());

        // Table quantity edit
        tableModel.addTableModelListener(e -> {
            if (e.getColumn() == 3) { // Quantity column
                handleQuantityChange(e.getFirstRow());
            }
        });

        // Action buttons
        deleteSelectedButton.addActionListener(e -> handleDeleteSelected());
        voidTransactionButton.addActionListener(e -> handleVoidTransaction());
        payCashButton.addActionListener(e -> handlePayCash());
        payCardButton.addActionListener(e -> handlePayCard());
    }

    // ============ Event Handlers ============

    private void handleUpcEnter() {
        String upc = upcTextField.getText().trim();
        if (upc.isEmpty()) return;

        System.out.println("UPC Enter pressed: " + upc);

        try {
            var result = priceBookService.getItemByUpc(upc);
            if (result.isPresent()) {
                PriceBook item = result.get();
                addItemToSale(item);
                upcTextField.setText("");
                itemInfoLabel.setText("");
                addToCartButton.setVisible(false);
            } else {
                showError("Item not found: " + upc);
            }
        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
        }
    }

    private void handleManualSearch() {
        String upc = upcTextField.getText().trim();
        if (upc.isEmpty()) return;

        System.out.println("Manual search: " + upc);

        try {
            var result = priceBookService.getItemByUpc(upc);
            if (result.isPresent()) {
                searchedItem = result.get();
                itemInfoLabel.setText(String.format("%s - $%.2f",
                    searchedItem.name(), searchedItem.price()));
                addToCartButton.setVisible(true);
            } else {
                itemInfoLabel.setText("Item not found");
                addToCartButton.setVisible(false);
                searchedItem = null;
            }
        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
        }
    }

    private void handleManualAddToCart() {
        if (searchedItem != null) {
            addItemToSale(searchedItem);
            upcTextField.setText("");
            itemInfoLabel.setText("");
            addToCartButton.setVisible(false);
            searchedItem = null;
        }
    }

    private void addItemToSale(PriceBook item) {
        try {
            System.out.println("Adding item to sale: " + item.name());
            transactionService.addItem(item.upc(), item.name(), item.price());
            refreshSaleDisplay();
        } catch (SQLException e) {
            showError("Failed to add item: " + e.getMessage());
        }
    }

    private void handleQuantityChange(int row) {
        try {
            int itemId = Integer.parseInt((String) tableModel.getValueAt(row, 1));
            int newQty = Integer.parseInt((String) tableModel.getValueAt(row, 3));

            // Get item to get unit price
            List<TransactionItem> items = transactionService.getCurrentSaleItems();
            for (TransactionItem item : items) {
                if (item.id() == itemId) {
                    System.out.println("Updating quantity for item " + itemId + " to " + newQty);
                    transactionService.updateQuantity(itemId, newQty, item.unitPrice());
                    refreshSaleDisplay();
                    break;
                }
            }
        } catch (Exception e) {
            showError("Failed to update quantity: " + e.getMessage());
            refreshSaleDisplay(); // Revert
        }
    }

    private void handleDeleteSelected() {
        List<Integer> selectedIds = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Boolean selected = (Boolean) tableModel.getValueAt(i, 0);
            if (selected != null && selected) {
                int itemId = Integer.parseInt((String) tableModel.getValueAt(i, 1));
                selectedIds.add(itemId);
            }
        }

        if (selectedIds.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No items selected", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete " + selectedIds.size() + " selected item(s)?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                System.out.println("Deleting items: " + selectedIds);
                transactionService.deleteSelectedItems(selectedIds);
                refreshSaleDisplay();
            } catch (SQLException e) {
                showError("Failed to delete items: " + e.getMessage());
            }
        }
    }

    private void handleVoidTransaction() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Void entire transaction?",
            "Confirm Void",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                System.out.println("Voiding transaction");
                transactionService.voidTransaction();
                transactionService.createTransaction();
                refreshSaleDisplay();
                JOptionPane.showMessageDialog(this, "Transaction voided", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException e) {
                showError("Failed to void transaction: " + e.getMessage());
            }
        }
    }

    private void handlePayCash() {
        try {
            double total = transactionService.getTransactionTotal();
            if (total == 0) {
                JOptionPane.showMessageDialog(this, "Cart is empty", "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            String input = JOptionPane.showInputDialog(this,
                String.format("Total: $%.2f\nEnter amount tendered:", total),
                "Cash Payment",
                JOptionPane.QUESTION_MESSAGE);

            if (input != null && !input.trim().isEmpty()) {
                double tendered = Double.parseDouble(input);

                if (tendered < total) {
                    showError("Insufficient payment. Required: $" + String.format("%.2f", total));
                    return;
                }

                System.out.println("Processing cash payment: $" + tendered);

                // Get transaction details before processing
                List<TransactionItem> items = transactionService.getCurrentSaleItems();
                double subtotal = transactionService.getTransactionSubtotal();
                double tax = total - subtotal;
                double change = tendered - total;

                transactionService.processCash(tendered);

                // Show receipt dialog
                showReceiptDialog(items, subtotal, tax, total, tendered, change, "CASH");
            }
        } catch (NumberFormatException e) {
            showError("Invalid amount entered");
        } catch (SQLException e) {
            showError("Payment failed: " + e.getMessage());
        }
    }

    private void handlePayCard() {
        try {
            double total = transactionService.getTransactionTotal();
            if (total == 0) {
                JOptionPane.showMessageDialog(this, "Cart is empty", "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this,
                String.format("Process card payment of $%.2f?", total),
                "Card Payment",
                JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                System.out.println("Processing card payment");

                // Get transaction details before processing
                List<TransactionItem> items = transactionService.getCurrentSaleItems();
                double subtotal = transactionService.getTransactionSubtotal();
                double tax = total - subtotal;

                transactionService.processCard("", "", "");

                // Show receipt dialog
                showReceiptDialog(items, subtotal, tax, total, total, 0, "CARD");
            }
        } catch (SQLException e) {
            showError("Payment failed: " + e.getMessage());
        }
    }

    private void refreshSaleDisplay() {
        try {
            List<TransactionItem> items = transactionService.getCurrentSaleItems();

            // Clear table
            tableModel.setRowCount(0);

            // Add active items
            for (TransactionItem item : items) {
                if (item.status().equals("ACTIVE")) {
                    tableModel.addRow(new Object[]{
                        false, // Checkbox
                        String.valueOf(item.id()),
                        item.name(),
                        String.valueOf(item.quantity()),
                        String.format("$%.2f", item.unitPrice()),
                        String.format("$%.2f", item.subtotal())
                    });
                }
            }

            // Update totals
            double subtotal = transactionService.getTransactionSubtotal();
            double total = transactionService.getTransactionTotal();
            double tax = total - subtotal;

            subtotalLabel.setText(String.format("Subtotal: $%.2f", subtotal));
            taxLabel.setText(String.format("Tax (7%%): $%.2f", tax));
            totalLabel.setText(String.format("Total: $%.2f", total));

        } catch (SQLException e) {
            showError("Failed to refresh display: " + e.getMessage());
        }
    }

    private void showReceiptDialog(List<TransactionItem> items, double subtotal,
                                   double tax, double total, double tendered,
                                   double change, String paymentType) {
        JDialog receiptDialog = new JDialog(this, "Receipt", true);
        receiptDialog.setSize(400, 600);
        receiptDialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Receipt content
        StringBuilder receipt = new StringBuilder();
        receipt.append("========== RECEIPT ==========\n\n");
        for (TransactionItem item : items) {
            if (item.status().equals("ACTIVE")) {
                receipt.append(String.format("%-25s x%d\n",
                    item.name().substring(0, Math.min(25, item.name().length())),
                    item.quantity()));
                receipt.append(String.format("  $%.2f ea.        $%.2f\n\n",
                    item.unitPrice(), item.subtotal()));
            }
        }
        receipt.append("============================\n");
        receipt.append(String.format("Subtotal:        $%.2f\n", subtotal));
        receipt.append(String.format("Tax (7%%):        $%.2f\n", tax));
        receipt.append(String.format("TOTAL:           $%.2f\n\n", total));
        receipt.append(String.format("Payment Method: %s\n", paymentType));
        receipt.append(String.format("Tendered:        $%.2f\n", tendered));
        receipt.append(String.format("Change:          $%.2f\n", change));
        receipt.append("\nThank you for your business!\n");

        JTextArea receiptArea = new JTextArea(receipt.toString());
        receiptArea.setEditable(false);
        receiptArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(receiptArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton newTransactionBtn = new JButton("New Transaction");
        JButton printBtn = new JButton("Print");
        JButton closeBtn = new JButton("Close");

        newTransactionBtn.addActionListener(e -> {
            receiptDialog.dispose();
            startNewTransaction();
        });

        printBtn.addActionListener(e -> {
            JOptionPane.showMessageDialog(receiptDialog,
                "Print functionality coming soon!",
                "Info",
                JOptionPane.INFORMATION_MESSAGE);
        });

        closeBtn.addActionListener(e -> receiptDialog.dispose());

        buttonPanel.add(newTransactionBtn);
        buttonPanel.add(printBtn);
        buttonPanel.add(closeBtn);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        receiptDialog.add(panel);
        receiptDialog.setVisible(true);
    }

    private void startNewTransaction() {
        try {
            System.out.println("Starting new transaction");
            transactionService.createTransaction();
            refreshSaleDisplay();
            upcTextField.setText("");
            itemInfoLabel.setText("");
            addToCartButton.setVisible(false);
            JOptionPane.showMessageDialog(this,
                "Ready for new transaction",
                "New Transaction",
                JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            showError("Failed to create new transaction: " + e.getMessage());
        }
    }

    private void showError(String message) {
        System.err.println("ERROR: " + message);
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PosInterface pos = new PosInterface();
            pos.setVisible(true);
        });
    }
}
