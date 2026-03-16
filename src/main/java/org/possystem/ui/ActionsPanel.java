package org.possystem.ui;

import org.possystem.entity.PriceBook;
import org.possystem.entity.TransactionItem;
import org.possystem.service.PriceBookService;
import org.possystem.service.TransactionService;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.List;

/**
 * Actions Panel - Transaction actions and payment buttons
 */
public class ActionsPanel extends JPanel {

    private final PriceBookService priceBookService;
    private final TransactionService transactionService;
    private final Runnable onSaleRefresh;
    private Runnable onTransactionFinalized;
    private Runnable onTransactionResumed;

    // Barcode Scanner Components
    private JTextField barcodeScannerField;  // Hidden field for scanner input
    private JPanel scanIndicatorPanel;  // Visual indicator for scan success
    private Timer scanCompleteTimer;  // Detects when scanning is complete
    private Timer indicatorTimer;  // Controls indicator visibility

    // Scan detection and duplicate prevention
    private static final int SCAN_COMPLETE_DELAY_MS = 100;  // Time to wait after typing stops
    private static final int DUPLICATE_SCAN_WINDOW_MS = 800;  // Ignore duplicates within this time
    private static final int INDICATOR_FLASH_DURATION_MS = 500;  // How long indicator shows
    private String lastScannedUPC = "";
    private long lastScanTime = 0;

    // Action buttons
    private JButton changeQtyButton;
    private JButton voidTransactionButton;
    private JButton payCashButton;
    private JButton payCardButton;
    private JButton deleteSelectedButton;
    private JButton totalButton;
    private JButton paymentVoidButton;

    public ActionsPanel(PriceBookService priceBookService, TransactionService transactionService,
                       Runnable onSaleRefresh) {
        this.priceBookService = priceBookService;
        this.transactionService = transactionService;
        this.onSaleRefresh = onSaleRefresh;

        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        initializeComponents();
        layoutComponents();
        attachEventHandlers();

        // Initialize focus for barcode scanner (needs to be done after layout)
        SwingUtilities.invokeLater(() -> barcodeScannerField.requestFocusInWindow());
    }

    private void initializeComponents() {
        // Barcode Scanner Field (hidden, always ready for input)
        barcodeScannerField = new JTextField();
        barcodeScannerField.setOpaque(false);
        barcodeScannerField.setBorder(null);
        barcodeScannerField.setPreferredSize(new Dimension(0, 0));
        barcodeScannerField.setSize(0, 0);

        // Scan Indicator Panel (small green dot, hidden by default)
        scanIndicatorPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Draw small green circle indicator (8x8 dot)
                g2d.setColor(new Color(40, 167, 69));  // Success green
                g2d.fillOval(1, 1, 8, 8);

                // Add subtle glow
                g2d.setColor(new Color(40, 167, 69, 120));
                g2d.fillOval(0, 0, 10, 10);

                g2d.dispose();
            }
        };
        scanIndicatorPanel.setPreferredSize(new Dimension(10, 10));
        scanIndicatorPanel.setMinimumSize(new Dimension(10, 10));
        scanIndicatorPanel.setMaximumSize(new Dimension(10, 10));
        scanIndicatorPanel.setOpaque(false);
        scanIndicatorPanel.setVisible(false);  // Hidden by default

        // Timers for scan detection and indicator control
        scanCompleteTimer = new Timer(SCAN_COMPLETE_DELAY_MS, e -> processScan());
        scanCompleteTimer.setRepeats(false);

        indicatorTimer = new Timer(INDICATOR_FLASH_DURATION_MS, e -> scanIndicatorPanel.setVisible(false));
        indicatorTimer.setRepeats(false);

        // Actions Panel Buttons
        changeQtyButton = new JButton("Change Qty");
        voidTransactionButton = new JButton("Void Basket");
        payCashButton = new JButton("Cash");
        payCardButton = new JButton("Card");
        deleteSelectedButton = new JButton("Void Line/s");
        totalButton = new JButton("Total");

        // Style action buttons
        // Change Qty - blue/neutral
        changeQtyButton.setBackground(new Color(0, 123, 255));
        changeQtyButton.setOpaque(true);
        changeQtyButton.setBorderPainted(false);
        changeQtyButton.setForeground(Color.WHITE);
        changeQtyButton.setFont(new Font("Arial", Font.BOLD, 14));

        voidTransactionButton.setBackground(new Color(220, 53, 69));
        voidTransactionButton.setOpaque(true);
        voidTransactionButton.setBorderPainted(false);
        voidTransactionButton.setForeground(Color.WHITE);
        voidTransactionButton.setFont(new Font("Arial", Font.BOLD, 16));

        payCashButton.setBackground(new Color(40, 167, 69));
        payCashButton.setOpaque(true);
        payCashButton.setBorderPainted(false);
        payCashButton.setForeground(Color.WHITE);
        payCashButton.setFont(new Font("Arial", Font.BOLD, 16));

        payCardButton.setBackground(new Color(40, 167, 69));
        payCardButton.setOpaque(true);
        payCardButton.setBorderPainted(false);
        payCardButton.setForeground(Color.WHITE);
        payCardButton.setFont(new Font("Arial", Font.BOLD, 16));

        deleteSelectedButton.setBackground(new Color(255, 150, 100));
        deleteSelectedButton.setOpaque(true);
        deleteSelectedButton.setBorderPainted(false);
        deleteSelectedButton.setForeground(Color.WHITE);
        deleteSelectedButton.setFont(new Font("Arial", Font.BOLD, 14));

        totalButton.setBackground(new Color(50, 205, 50));
        totalButton.setOpaque(true);
        totalButton.setBorderPainted(false);
        totalButton.setForeground(Color.WHITE);
        totalButton.setFont(new Font("Arial", Font.BOLD, 16));

        // Apply text outlines
        applyTextOutline(changeQtyButton);
        applyTextOutline(voidTransactionButton);
        applyTextOutline(payCashButton);
        applyTextOutline(payCardButton);
        applyTextOutline(deleteSelectedButton);
        applyTextOutline(totalButton);

        // Initially disable payment buttons
        payCashButton.setEnabled(false);
        payCardButton.setEnabled(false);

        // Initially disable buttons that require item selection
        changeQtyButton.setEnabled(false);
        deleteSelectedButton.setEnabled(false);
    }

    private void layoutComponents() {
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // LEFT: Transaction Actions
        JPanel transactionSubZone = new JPanel(new BorderLayout(5, 5));
        transactionSubZone.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Transaction Actions"),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        // Create minimal dedicated space for indicator - tucked in upper right corner
        JPanel indicatorContainer = new JPanel(new BorderLayout());
        indicatorContainer.setOpaque(false);
        indicatorContainer.setPreferredSize(new Dimension(0, 15));  // Minimal fixed height
        indicatorContainer.setMinimumSize(new Dimension(0, 15));
        indicatorContainer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 15));

        // Indicator positioned tight in upper right corner with minimal margins
        JPanel indicatorWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        indicatorWrapper.setOpaque(false);
        indicatorWrapper.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));  // 5px right margin only
        indicatorWrapper.add(scanIndicatorPanel);
        indicatorContainer.add(indicatorWrapper, BorderLayout.EAST);

        transactionSubZone.add(indicatorContainer, BorderLayout.NORTH);

        // Transaction buttons in horizontal layout
        JPanel transactionButtonsPanel = new JPanel(new GridLayout(1, 4, 10, 10));
        transactionButtonsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Add all 4 transaction buttons (Void Line/s -> Void Basket -> Change Qty -> Total)
        transactionButtonsPanel.add(deleteSelectedButton);
        transactionButtonsPanel.add(voidTransactionButton);
        transactionButtonsPanel.add(changeQtyButton);
        transactionButtonsPanel.add(totalButton);

        transactionSubZone.add(transactionButtonsPanel, BorderLayout.CENTER);

        // Add hidden barcode scanner field (invisible, always ready for input)
        transactionSubZone.add(barcodeScannerField, BorderLayout.SOUTH);

        // RIGHT: Payment
        JPanel paymentSubZone = new JPanel(new BorderLayout(5, 5));
        paymentSubZone.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Payment"),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        // Add spacer to match transaction actions indicator height
        JPanel paymentSpacer = new JPanel();
        paymentSpacer.setOpaque(false);
        paymentSpacer.setPreferredSize(new Dimension(0, 15));  // Match indicator height
        paymentSubZone.add(paymentSpacer, BorderLayout.NORTH);

        // Payment buttons in grid layout
        JPanel paymentButtonsGrid = new JPanel(new GridLayout(1, 3, 10, 10));
        paymentButtonsGrid.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Add Back to Cart button to Payment zone
        paymentVoidButton = new JButton("Back to Cart");
        paymentVoidButton.setBackground(new Color(255, 193, 7)); // Yellow/amber for going back
        paymentVoidButton.setOpaque(true);
        paymentVoidButton.setBorderPainted(false);
        paymentVoidButton.setForeground(Color.WHITE);
        paymentVoidButton.setFont(new Font("Arial", Font.BOLD, 16));
        applyTextOutline(paymentVoidButton);
        paymentVoidButton.setEnabled(false);

        paymentButtonsGrid.add(paymentVoidButton);
        paymentButtonsGrid.add(payCardButton);
        paymentButtonsGrid.add(payCashButton);

        paymentSubZone.add(paymentButtonsGrid, BorderLayout.CENTER);

        // Add both sub zones with 60/40 split using GridBagConstraints
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridy = 0;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(0, 0, 0, 10);

        // Transaction Actions: 60%
        gbc.gridx = 0;
        gbc.weightx = 0.6;
        centerPanel.add(transactionSubZone, gbc);

        // Payment: 40%
        gbc.gridx = 1;
        gbc.weightx = 0.4;
        gbc.insets = new Insets(0, 0, 0, 0);
        centerPanel.add(paymentSubZone, gbc);

        add(centerPanel, BorderLayout.CENTER);
    }

    private void attachEventHandlers() {
        // Barcode Scanner Field - Auto-detect when scanning completes
        barcodeScannerField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                onScannerInput();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                onScannerInput();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                onScannerInput();
            }

            private void onScannerInput() {
                // Restart timer on each character - when timer completes, scan is done
                scanCompleteTimer.restart();
            }
        });

        // Action buttons - deleteSelectedButton and changeQtyButton are wired via callbacks
        voidTransactionButton.addActionListener(e -> handleVoidTransaction());
        totalButton.addActionListener(e -> handleTotal());
        payCashButton.addActionListener(e -> handlePayCash());
        payCardButton.addActionListener(e -> handlePayCard());
        paymentVoidButton.addActionListener(e -> handleCancelTotal());
    }

    /**
     * Process barcode scan when scan detection completes
     * Implements duplicate prevention and auto-add functionality
     */
    private void processScan() {
        String scannedUPC = barcodeScannerField.getText().trim();

        // Ignore empty scans
        if (scannedUPC.isEmpty()) {
            return;
        }

        // Duplicate prevention - ignore if same UPC scanned within time window
        long currentTime = System.currentTimeMillis();
        if (scannedUPC.equals(lastScannedUPC) &&
            (currentTime - lastScanTime) < DUPLICATE_SCAN_WINDOW_MS) {
            // Duplicate scan detected - ignore it
            barcodeScannerField.setText("");
            barcodeScannerField.requestFocusInWindow();
            return;
        }

        // Update last scan tracking
        lastScannedUPC = scannedUPC;
        lastScanTime = currentTime;

        // Lookup product in price book
        try {
            var result = priceBookService.getItemByUpc(scannedUPC);
            if (result.isPresent()) {
                // Product found - add to cart
                PriceBook item = result.get();
                addItemToSale(item);

                // Show success indicator (green flash)
                showSuccessIndicator();

                // Clear field for next scan
                barcodeScannerField.setText("");
                barcodeScannerField.requestFocusInWindow();
            } else {
                // Product not found - show warning dialog
                showWarningDialog(
                    "Product Not Found",
                    "Item Not in System",
                    "The scanned barcode does not match any item in the system.<br><br><b>Scanned Code:</b> " + scannedUPC
                );

                // Clear field for next scan
                barcodeScannerField.setText("");
                barcodeScannerField.requestFocusInWindow();
            }
        } catch (SQLException ex) {
            showError("Database error: " + ex.getMessage());
            barcodeScannerField.setText("");
            barcodeScannerField.requestFocusInWindow();
        }
    }

    /**
     * Show green indicator light briefly on successful scan
     */
    private void showSuccessIndicator() {
        scanIndicatorPanel.setVisible(true);
        scanIndicatorPanel.repaint();
        indicatorTimer.restart();
    }

    private void addItemToSale(PriceBook item) {
        try {
            transactionService.addItem(item.upc(), item.name(), item.price());
            if (onSaleRefresh != null) {
                onSaleRefresh.run();
            }
        } catch (SQLException e) {
            showError("Failed to add item: " + e.getMessage());
        }
    }


    private void handleCancelTotal() {
        // Create custom confirmation dialog
        boolean confirmed = showConfirmDialog(
            "Back to Cart",
            "Go back to cart?",
            "This will allow you to continue editing the transaction."
        );

        if (confirmed) {
            // Return to transaction editing mode
            setTransactionControlsEnabled(true);
            setPaymentButtonsEnabled(false);

            // Re-enable Quick Keys and Current Sale editing
            if (onTransactionResumed != null) {
                onTransactionResumed.run();
            }
        }
    }

    private void handleVoidTransaction() {
        try {
            // Check if cart is empty
            double total = transactionService.getTransactionTotal();
            if (total == 0) {
                showInfoDialog("Cart is Empty", "Nothing to Void", "The cart is empty. Add items to start a transaction.");
                return;
            }

            // Create custom confirmation dialog
            boolean confirmed = showConfirmDialog(
                "Confirm Void",
                "Void entire transaction?",
                "This will cancel all items in the current basket and start a new transaction."
            );

            if (confirmed) {
                transactionService.voidTransaction();
                transactionService.createTransaction();
                if (onSaleRefresh != null) {
                    onSaleRefresh.run();
                }

                // Reset button states
                resetButtonStatesForNewTransaction();

                // Re-enable Quick Keys and Current Sale editing
                if (onTransactionResumed != null) {
                    onTransactionResumed.run();
                }
            }
        } catch (SQLException e) {
            showError("Failed to void transaction: " + e.getMessage());
        }
    }

    private void handleTotal() {
        try {
            double total = transactionService.getTransactionTotal();
            if (total == 0) {
                showInfoDialog("Cart is Empty", "Nothing to Total", "The cart is empty. Add items before pressing Total.");
                return;
            }

            // Finalize the basket - disable transaction controls and enable payment buttons
            setTransactionControlsEnabled(false);
            setPaymentButtonsEnabled(true);

            // Disable Quick Keys and Current Sale editing
            if (onTransactionFinalized != null) {
                onTransactionFinalized.run();
            }
        } catch (SQLException e) {
            showError("Failed to calculate total: " + e.getMessage());
        }
    }

    private void handlePayExactDollar() {
        try {
            double total = transactionService.getTransactionTotal();
            if (total == 0) {
                showInfoDialog("Cart is Empty", "Cannot Process Payment", "The cart is empty. Add items to start a transaction.");
                return;
            }

            // Show confirmation dialog
            boolean confirmed = showConfirmDialog(
                "Exact Amount Payment",
                String.format("EXACT AMOUNT: $%.2f", total),
                "Customer pays exactly the total amount. No change given."
            );

            if (confirmed) {
                List<TransactionItem> items = transactionService.getCurrentSaleItems();
                double subtotal = transactionService.getTransactionSubtotal();
                double tax = total - subtotal;

                transactionService.processCash(total);

                showReceiptDialog(items, subtotal, tax, total, total, 0, "CASH - EXACT");
            } else {
                // User clicked "No" - return to Cash Payment Options
                showCashPaymentOptionsDialog(total);
            }
        } catch (SQLException e) {
            showError("Payment failed: " + e.getMessage());
        }
    }

    private void handlePayNextDollar() {
        try {
            double total = transactionService.getTransactionTotal();
            if (total == 0) {
                showInfoDialog("Cart is Empty", "Cannot Process Payment", "The cart is empty. Add items to start a transaction.");
                return;
            }

            double nextDollar = Math.ceil(total);
            double change = nextDollar - total;

            // Show confirmation dialog
            boolean confirmed = showConfirmDialog(
                "Next Dollar Payment",
                String.format("NEXT DOLLAR: $%.2f", nextDollar),
                String.format("Total: $%.2f | Next Dollar: $%.2f | Change back: $%.2f", total, nextDollar, change)
            );

            if (confirmed) {
                List<TransactionItem> items = transactionService.getCurrentSaleItems();
                double subtotal = transactionService.getTransactionSubtotal();
                double tax = total - subtotal;

                transactionService.processCash(nextDollar);

                showReceiptDialog(items, subtotal, tax, total, nextDollar, change, "CASH - NEXT DOLLAR");
            } else {
                // User clicked "No" - return to Cash Payment Options
                showCashPaymentOptionsDialog(total);
            }
        } catch (SQLException e) {
            showError("Payment failed: " + e.getMessage());
        }
    }

    private void handlePayCard() {
        try {
            double total = transactionService.getTransactionTotal();
            if (total == 0) {
                showInfoDialog("Cart is Empty", "Cannot Process Payment", "The cart is empty. Add items to start a transaction.");
                return;
            }

            boolean confirmed = showConfirmDialog(
                "Card Payment",
                String.format("Process card payment of $%.2f?", total),
                "Swipe, insert, or tap card to complete the transaction."
            );

            if (confirmed) {
                List<TransactionItem> items = transactionService.getCurrentSaleItems();
                double subtotal = transactionService.getTransactionSubtotal();
                double tax = total - subtotal;

                transactionService.processCard("", "", "");

                showReceiptDialog(items, subtotal, tax, total, total, 0, "CARD");
            }
        } catch (SQLException e) {
            showError("Payment failed: " + e.getMessage());
        }
    }

    private void handlePayInputAmount() {
        try {
            double total = transactionService.getTransactionTotal();
            if (total == 0) {
                showInfoDialog("Cart is Empty", "Cannot Process Payment", "The cart is empty. Add items to start a transaction.");
                return;
            }

            boolean validInput = false;

            // Loop until valid amount is entered or user cancels
            while (!validInput) {
                // Show custom input dialog with numeric keypad
                String input = showInputAmountDialog(total);

                // User cancelled - return to cash payment options
                if (input == null || input.trim().isEmpty()) {
                    showCashPaymentOptionsDialog(total);
                    return;
                }

                try {
                    double tendered = Double.parseDouble(input.trim());

                    // Validate amount - insufficient payment
                    if (tendered < total) {
                        showErrorDialog(
                            "Insufficient Payment",
                            String.format("Amount tendered ($%.2f) is less than total ($%.2f)", tendered, total),
                            "Please enter an amount greater than or equal to the total."
                        );
                        // Loop back to input dialog
                        continue;
                    }

                    // Validate amount - negative
                    if (tendered < 0) {
                        showErrorDialog(
                            "Invalid Amount",
                            "Amount cannot be negative",
                            "Please enter a valid positive amount."
                        );
                        // Loop back to input dialog
                        continue;
                    }

                    // Valid amount - process payment
                    validInput = true;

                    List<TransactionItem> items = transactionService.getCurrentSaleItems();
                    double subtotal = transactionService.getTransactionSubtotal();
                    double tax = total - subtotal;
                    double change = tendered - total;

                    transactionService.processCash(tendered);

                    showReceiptDialog(items, subtotal, tax, total, tendered, change, "CASH");

                } catch (NumberFormatException e) {
                    showErrorDialog(
                        "Invalid Input",
                        "Please enter a valid number",
                        "Amount must be a numeric value (e.g., 20.00)."
                    );
                    // Loop back to input dialog
                }
            }
        } catch (SQLException e) {
            showError("Payment failed: " + e.getMessage());
        }
    }

    private String showInputAmountDialog(double total) {
        JDialog inputDialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Input Amount", Dialog.ModalityType.APPLICATION_MODAL);
        inputDialog.setUndecorated(true);
        inputDialog.setResizable(false);

        // Calculate responsive sizing
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int dialogWidth = Math.min(450, screenSize.width - 100);
        int dialogHeight = Math.min(600, screenSize.height - 100);
        inputDialog.setSize(dialogWidth, dialogHeight);
        inputDialog.setLocationRelativeTo(null); // Center on screen
        inputDialog.setLayout(new BorderLayout(10, 10));

        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Header Panel with blue background
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(23, 162, 184)); // Blue
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel headerLabel = new JLabel("Input Amount");
        headerLabel.setFont(new Font("Arial", Font.BOLD, 20));
        headerLabel.setForeground(Color.WHITE);
        headerPanel.add(headerLabel, BorderLayout.WEST);

        // Add mouse drag functionality to header
        final java.awt.Point[] mouseDownCompCoords = {null};
        headerPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) {
                mouseDownCompCoords[0] = e.getPoint();
            }
            public void mouseReleased(java.awt.event.MouseEvent e) {
                mouseDownCompCoords[0] = null;
            }
        });
        headerPanel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent e) {
                if (mouseDownCompCoords[0] != null) {
                    java.awt.Point currCoords = e.getLocationOnScreen();
                    inputDialog.setLocation(currCoords.x - mouseDownCompCoords[0].x, currCoords.y - mouseDownCompCoords[0].y);
                }
            }
        });

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Center panel with total and input field
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(Color.WHITE);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));

        // Total label
        JLabel totalLabel = new JLabel(String.format("Total: $%.2f", total));
        totalLabel.setFont(new Font("Arial", Font.BOLD, 18));
        totalLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(totalLabel);

        centerPanel.add(Box.createVerticalStrut(15));

        // Amount tendered label
        JLabel tenderedLabel = new JLabel("Amount Tendered:");
        tenderedLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        tenderedLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(tenderedLabel);

        centerPanel.add(Box.createVerticalStrut(8));

        // Input field
        JTextField inputField = new JTextField("0");
        inputField.setFont(new Font("Arial", Font.BOLD, 24));
        inputField.setHorizontalAlignment(JTextField.CENTER);
        inputField.setMaximumSize(new Dimension(300, 50));
        inputField.setPreferredSize(new Dimension(300, 50));
        inputField.setEditable(false); // Only keypad can input
        inputField.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(inputField);

        centerPanel.add(Box.createVerticalStrut(15));

        // Numeric keypad with GridBagLayout for spanning buttons
        JPanel keypadPanel = new JPanel(new GridBagLayout());
        keypadPanel.setBackground(Color.WHITE);
        keypadPanel.setMaximumSize(new Dimension(380, 240));
        keypadPanel.setPreferredSize(new Dimension(380, 240));
        keypadPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;

        // Row 1: 7, 8, 9, Backspace (spans 2 rows)
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridheight = 1;
        JButton btn7 = createKeypadButton("7", inputField, false);
        keypadPanel.add(btn7, gbc);

        gbc.gridx = 1;
        JButton btn8 = createKeypadButton("8", inputField, false);
        keypadPanel.add(btn8, gbc);

        gbc.gridx = 2;
        JButton btn9 = createKeypadButton("9", inputField, false);
        keypadPanel.add(btn9, gbc);

        gbc.gridx = 3; gbc.gridheight = 2; // Backspace spans 2 rows
        JButton backspaceBtn = createKeypadButton("←", inputField, false);
        backspaceBtn.setBackground(new Color(255, 193, 7)); // Amber
        keypadPanel.add(backspaceBtn, gbc);

        // Row 2: 4, 5, 6
        gbc.gridheight = 1; // Reset to 1 row
        gbc.gridx = 0; gbc.gridy = 1;
        JButton btn4 = createKeypadButton("4", inputField, false);
        keypadPanel.add(btn4, gbc);

        gbc.gridx = 1;
        JButton btn5 = createKeypadButton("5", inputField, false);
        keypadPanel.add(btn5, gbc);

        gbc.gridx = 2;
        JButton btn6 = createKeypadButton("6", inputField, false);
        keypadPanel.add(btn6, gbc);

        // Row 3: 1, 2, 3, Clear (spans 2 rows)
        gbc.gridx = 0; gbc.gridy = 2;
        JButton btn1 = createKeypadButton("1", inputField, false);
        keypadPanel.add(btn1, gbc);

        gbc.gridx = 1;
        JButton btn2 = createKeypadButton("2", inputField, false);
        keypadPanel.add(btn2, gbc);

        gbc.gridx = 2;
        JButton btn3 = createKeypadButton("3", inputField, false);
        keypadPanel.add(btn3, gbc);

        gbc.gridx = 3; gbc.gridheight = 2; // Clear spans 2 rows
        JButton clearBtn = createKeypadButton("Clear", inputField, true);
        clearBtn.setBackground(new Color(220, 53, 69)); // Red
        keypadPanel.add(clearBtn, gbc);

        // Row 4: empty, 0, .
        gbc.gridheight = 1; // Reset to 1 row
        gbc.gridx = 0; gbc.gridy = 3;
        // Empty space
        JPanel emptyPanel = new JPanel();
        emptyPanel.setBackground(Color.WHITE);
        keypadPanel.add(emptyPanel, gbc);

        gbc.gridx = 1;
        JButton btn0 = createKeypadButton("0", inputField, false);
        keypadPanel.add(btn0, gbc);

        gbc.gridx = 2;
        JButton btnDot = createKeypadButton(".", inputField, false);
        keypadPanel.add(btnDot, gbc);

        centerPanel.add(keypadPanel);
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Button panel (Cancel / Submit)
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Track user input
        final String[] userInput = {null};

        // Cancel button
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setBackground(new Color(220, 53, 69));
        cancelButton.setForeground(Color.WHITE);
        cancelButton.setFont(new Font("Arial", Font.BOLD, 16));
        cancelButton.setFocusPainted(false);
        cancelButton.setBorderPainted(false);
        cancelButton.setOpaque(true);
        applyRoundedStyle(cancelButton);
        applyTextOutline(cancelButton);

        cancelButton.addActionListener(e -> {
            userInput[0] = null;
            inputDialog.dispose();
        });

        // Submit button
        JButton submitButton = new JButton("Submit");
        submitButton.setBackground(new Color(40, 167, 69));
        submitButton.setForeground(Color.WHITE);
        submitButton.setFont(new Font("Arial", Font.BOLD, 16));
        submitButton.setFocusPainted(false);
        submitButton.setBorderPainted(false);
        submitButton.setOpaque(true);
        applyRoundedStyle(submitButton);
        applyTextOutline(submitButton);

        submitButton.addActionListener(e -> {
            userInput[0] = inputField.getText();
            inputDialog.dispose();
        });

        buttonPanel.add(cancelButton);
        buttonPanel.add(submitButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        inputDialog.add(mainPanel);
        inputDialog.setVisible(true);

        return userInput[0];
    }

    private JButton createKeypadButton(String key, JTextField inputField, boolean isRed) {
        JButton keyButton = new JButton(key);
        keyButton.setFont(new Font("Arial", Font.BOLD, 18));
        keyButton.setFocusPainted(false);
        keyButton.setBackground(new Color(248, 249, 250)); // Light gray
        keyButton.setForeground(Color.BLACK);

        keyButton.addActionListener(e -> {
            String currentText = inputField.getText();
            if (key.equals("Clear")) {
                inputField.setText("0");
            } else if (key.equals("←")) {
                // Backspace - remove last character
                if (currentText.length() > 1) {
                    inputField.setText(currentText.substring(0, currentText.length() - 1));
                } else {
                    // If only one character left, replace with "0"
                    inputField.setText("0");
                }
            } else if (key.equals(".")) {
                // Only allow one decimal point
                if (!currentText.contains(".")) {
                    // If current text is "0", change to "0."
                    if (currentText.equals("0")) {
                        inputField.setText("0.");
                    } else {
                        inputField.setText(currentText + ".");
                    }
                }
            } else {
                // Number button (0-9)
                if (currentText.equals("0")) {
                    // Replace the leading zero with the pressed number
                    inputField.setText(key);
                } else {
                    // Append to existing text
                    inputField.setText(currentText + key);
                }
            }
        });

        return keyButton;
    }

    private void handlePayCash() {
        try {
            double total = transactionService.getTransactionTotal();
            if (total == 0) {
                showInfoDialog("Cart is Empty", "Cannot Process Payment", "The cart is empty. Add items to start a transaction.");
                return;
            }

            // Show cash payment options dialog
            showCashPaymentOptionsDialog(total);
        } catch (SQLException e) {
            showError("Payment failed: " + e.getMessage());
        }
    }

    private void showCashPaymentOptionsDialog(double total) {
        // Create custom modal dialog
        JDialog cashDialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Cash Payment Options", Dialog.ModalityType.APPLICATION_MODAL);
        cashDialog.setUndecorated(true);
        cashDialog.setResizable(false);

        // Calculate responsive sizing based on screen
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int dialogWidth = Math.min(400, screenSize.width - 100);
        int dialogHeight = Math.min(350, screenSize.height - 100);
        cashDialog.setSize(dialogWidth, dialogHeight);
        cashDialog.setLocationRelativeTo(null); // Center on screen

        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(Color.WHITE);

        // Header panel with blue background
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(23, 162, 184)); // Blue
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel titleLabel = new JLabel("Cash Payment");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        // Add mouse drag functionality to header
        final java.awt.Point[] mouseDownCompCoords = {null};
        headerPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) {
                mouseDownCompCoords[0] = e.getPoint();
            }
            public void mouseReleased(java.awt.event.MouseEvent e) {
                mouseDownCompCoords[0] = null;
            }
        });
        headerPanel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent e) {
                if (mouseDownCompCoords[0] != null) {
                    java.awt.Point currCoords = e.getLocationOnScreen();
                    cashDialog.setLocation(currCoords.x - mouseDownCompCoords[0].x, currCoords.y - mouseDownCompCoords[0].y);
                }
            }
        });

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Message panel
        JPanel messagePanel = new JPanel(new BorderLayout(10, 10));
        messagePanel.setBackground(Color.WHITE);
        messagePanel.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));

        JLabel messageLabel = new JLabel(String.format("<html><div style='text-align: center;'><b>Total: $%.2f</b><br><br>Select a payment option:</div></html>", total));
        messageLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        messagePanel.add(messageLabel, BorderLayout.CENTER);

        mainPanel.add(messagePanel, BorderLayout.CENTER);

        // Button panel with 4 options in a grid
        JPanel buttonPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        // Create the 4 option buttons
        JButton exactAmountBtn = new JButton("Exact Amount");
        JButton nextDollarBtn = new JButton("Next Dollar");
        JButton inputAmountBtn = new JButton("Input Amount");
        JButton cancelBtn = new JButton("Cancel");

        // Style the buttons
        Color greenColor = new Color(40, 167, 69);
        Color redColor = new Color(220, 53, 69);

        // Exact Amount button (green)
        exactAmountBtn.setBackground(greenColor);
        exactAmountBtn.setForeground(Color.WHITE);
        exactAmountBtn.setFont(new Font("Arial", Font.BOLD, 14));
        exactAmountBtn.setFocusPainted(false);
        exactAmountBtn.setBorderPainted(false);
        exactAmountBtn.setOpaque(true);
        applyRoundedStyle(exactAmountBtn);
        applyTextOutline(exactAmountBtn);

        // Next Dollar button (green)
        nextDollarBtn.setBackground(greenColor);
        nextDollarBtn.setForeground(Color.WHITE);
        nextDollarBtn.setFont(new Font("Arial", Font.BOLD, 14));
        nextDollarBtn.setFocusPainted(false);
        nextDollarBtn.setBorderPainted(false);
        nextDollarBtn.setOpaque(true);
        applyRoundedStyle(nextDollarBtn);
        applyTextOutline(nextDollarBtn);

        // Input Amount button (green)
        inputAmountBtn.setBackground(greenColor);
        inputAmountBtn.setForeground(Color.WHITE);
        inputAmountBtn.setFont(new Font("Arial", Font.BOLD, 14));
        inputAmountBtn.setFocusPainted(false);
        inputAmountBtn.setBorderPainted(false);
        inputAmountBtn.setOpaque(true);
        applyRoundedStyle(inputAmountBtn);
        applyTextOutline(inputAmountBtn);

        // Cancel button (red)
        cancelBtn.setBackground(redColor);
        cancelBtn.setForeground(Color.WHITE);
        cancelBtn.setFont(new Font("Arial", Font.BOLD, 14));
        cancelBtn.setFocusPainted(false);
        cancelBtn.setBorderPainted(false);
        cancelBtn.setOpaque(true);
        applyRoundedStyle(cancelBtn);
        applyTextOutline(cancelBtn);

        // Button actions
        exactAmountBtn.addActionListener(e -> {
            cashDialog.dispose();
            handlePayExactDollar();
        });

        nextDollarBtn.addActionListener(e -> {
            cashDialog.dispose();
            handlePayNextDollar();
        });

        inputAmountBtn.addActionListener(e -> {
            cashDialog.dispose();
            handlePayInputAmount();
        });

        cancelBtn.addActionListener(e -> cashDialog.dispose());

        // Add buttons to panel - arranged as: Cancel, Input Amount (top row) | Exact Amount, Next Dollar (bottom row)
        buttonPanel.add(cancelBtn);
        buttonPanel.add(inputAmountBtn);
        buttonPanel.add(exactAmountBtn);
        buttonPanel.add(nextDollarBtn);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Add main panel to dialog
        cashDialog.add(mainPanel);
        cashDialog.setVisible(true);
    }

    private void showReceiptDialog(List<TransactionItem> items, double subtotal,
                                   double tax, double total, double tendered,
                                   double change, String paymentType) {
        JDialog receiptDialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Receipt", Dialog.ModalityType.APPLICATION_MODAL);

        // Calculate dialog size based on content
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        // Count active items to determine content size
        int activeItemCount = 0;
        for (TransactionItem item : items) {
            if (item.status().equals("ACTIVE")) {
                activeItemCount++;
            }
        }

        // Base height calculation: header (50) + footer/button (70) + padding (20)
        // Each item needs approximately 60px (item name line + price line + spacing)
        // Plus fixed receipt sections: header (30) + subtotal/tax/total (120) + payment info (90) + thank you (30)
        int baseHeight = 50 + 70 + 20; // Dialog chrome
        int receiptFixedHeight = 30 + 120 + 90 + 30; // Fixed receipt sections
        int itemsHeight = activeItemCount * 60; // Dynamic based on item count
        int calculatedHeight = baseHeight + receiptFixedHeight + itemsHeight;

        // Set bounds: min 400px, max 800px, calculated based on content
        int dialogHeight = Math.max(400, Math.min(calculatedHeight, Math.min(800, screenSize.height - 100)));

        // Calculate width based on monospaced font character width
        // Receipt format: 47 characters wide
        // Monospaced 12pt font: ~7 pixels per character
        // Text width: 47 × 7 = ~329px
        // Add padding: text area (20px) + content panel (10px) + border (2px) + dialog frame (~20px)
        // Total padding: ~52px
        int charactersWide = 47; // Receipt width
        int charWidth = 7; // Character width
        int textWidth = charactersWide * charWidth; // ~329px
        int padding = 52;
        int dialogWidth = Math.min(textWidth + padding, screenSize.width - 100); // ~381px total

        receiptDialog.setUndecorated(true);
        receiptDialog.setResizable(false);
        receiptDialog.setSize(dialogWidth, dialogHeight);
        receiptDialog.setLocationRelativeTo(null); // Center on screen

        // Main panel with minimal spacing
        JPanel mainPanel = new JPanel(new BorderLayout(0, 0));
        mainPanel.setBackground(Color.WHITE);

        // Header panel with green background (successful transaction)
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(40, 167, 69)); // Green
        headerPanel.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));

        JLabel titleLabel = new JLabel("Transaction Complete");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        // Add mouse drag functionality to header
        final java.awt.Point[] mouseDownCompCoords = {null};
        headerPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) {
                mouseDownCompCoords[0] = e.getPoint();
            }
            public void mouseReleased(java.awt.event.MouseEvent e) {
                mouseDownCompCoords[0] = null;
            }
        });
        headerPanel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent e) {
                if (mouseDownCompCoords[0] != null) {
                    java.awt.Point currCoords = e.getLocationOnScreen();
                    receiptDialog.setLocation(currCoords.x - mouseDownCompCoords[0].x, currCoords.y - mouseDownCompCoords[0].y);
                }
            }
        });

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Receipt content panel - maximize space for receipt
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JTextArea receiptArea = new JTextArea(buildReceiptText(items, subtotal, tax,
            total, tendered, change, paymentType));
        receiptArea.setEditable(false);
        receiptArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        receiptArea.setBackground(new Color(248, 249, 250)); // Light gray background
        receiptArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        receiptArea.setLineWrap(false);
        receiptArea.setWrapStyleWord(false);

        JScrollPane scrollPane = new JScrollPane(receiptArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(222, 226, 230), 1));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        mainPanel.add(contentPanel, BorderLayout.CENTER);

        // Button panel - compact
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        JButton newTransactionBtn = new JButton("New Transaction");

        // Style New Transaction button (green)
        newTransactionBtn.setBackground(new Color(40, 167, 69));
        newTransactionBtn.setForeground(Color.WHITE);
        newTransactionBtn.setFont(new Font("Arial", Font.BOLD, 14));
        newTransactionBtn.setFocusPainted(false);
        newTransactionBtn.setBorderPainted(false);
        newTransactionBtn.setOpaque(true);
        newTransactionBtn.setPreferredSize(new Dimension(180, 40));
        applyRoundedStyle(newTransactionBtn);
        applyTextOutline(newTransactionBtn);

        newTransactionBtn.addActionListener(e -> {
            receiptDialog.dispose();
            startNewTransaction();
        });

        buttonPanel.add(newTransactionBtn);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        receiptDialog.add(mainPanel);
        receiptDialog.setVisible(true);
    }

    private String buildReceiptText(List<TransactionItem> items, double subtotal,
                                    double tax, double total, double tendered,
                                    double change, String paymentType) {
        StringBuilder receipt = new StringBuilder();
        receipt.append("=================== RECEIPT ===================\n\n");
        for (TransactionItem item : items) {
            if (item.status().equals("ACTIVE")) {
                receipt.append(String.format("%-34s x%-2d\n",
                    item.name().substring(0, Math.min(34, item.name().length())),
                    item.quantity()));
                receipt.append(String.format("  $%-8.2f ea.                  $%-8.2f\n\n",
                    item.unitPrice(), item.subtotal()));
            }
        }
        receipt.append("===============================================\n");
        receipt.append(String.format("Subtotal:                           $%-8.2f\n", subtotal));
        receipt.append(String.format("Tax (7%%):                           $%-8.2f\n", tax));
        receipt.append(String.format("TOTAL:                              $%-8.2f\n\n", total));
        receipt.append(String.format("Payment Method: %s\n", paymentType));
        receipt.append(String.format("Tendered:                           $%-8.2f\n", tendered));
        receipt.append(String.format("Change:                             $%-8.2f\n", change));
        receipt.append("\n   Thank you! Please come again soon!\n");
        return receipt.toString();
    }

    private void startNewTransaction() {
        try {
            transactionService.createTransaction();
            if (onSaleRefresh != null) {
                onSaleRefresh.run();
            }
            barcodeScannerField.setText("");

            resetButtonStatesForNewTransaction();

            // Re-enable Quick Keys and Current Sale editing
            if (onTransactionResumed != null) {
                onTransactionResumed.run();
            }
        } catch (SQLException e) {
            showError("Failed to create new transaction: " + e.getMessage());
        }
    }

    private void applyTextOutline(JButton button) {
        button.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        button.setContentAreaFilled(false);

        button.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                JButton btn = (JButton) c;
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int arcSize = 12;

                Color buttonColor = btn.getBackground();
                Color displayColor;
                Color textColor;

                if (btn.isEnabled()) {
                    displayColor = buttonColor;
                    textColor = btn.getForeground();
                } else {
                    displayColor = new Color(
                        (int)(buttonColor.getRed() * 0.5),
                        (int)(buttonColor.getGreen() * 0.5),
                        (int)(buttonColor.getBlue() * 0.5)
                    );
                    textColor = new Color(180, 180, 180);
                }

                g2d.setColor(displayColor);
                g2d.fillRoundRect(0, 0, btn.getWidth() - 1, btn.getHeight() - 1, arcSize, arcSize);

                g2d.setColor(displayColor.darker());
                g2d.setStroke(new BasicStroke(2f));
                g2d.drawRoundRect(0, 0, btn.getWidth() - 1, btn.getHeight() - 1, arcSize, arcSize);

                String text = btn.getText();
                FontMetrics fm = g2d.getFontMetrics(btn.getFont());
                int textWidth = fm.stringWidth(text);
                int x = (btn.getWidth() - textWidth) / 2;
                int y = (btn.getHeight() + fm.getAscent() - fm.getDescent()) / 2;

                g2d.setFont(btn.getFont());

                if (btn.isEnabled()) {
                    g2d.setColor(new Color(0, 0, 0, 150));
                    g2d.setStroke(new BasicStroke(3f));
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dy = -1; dy <= 1; dy++) {
                            if (dx != 0 || dy != 0) {
                                g2d.drawString(text, x + dx, y + dy);
                            }
                        }
                    }
                }

                g2d.setColor(textColor);
                g2d.drawString(text, x, y);

                g2d.dispose();
            }
        });
    }

    private boolean showConfirmDialog(String title, String message, String details) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenHeight = screenSize.height;
        float scaleFactor = screenHeight / 1080.0f;

        int headerFontSize = Math.round(24 * scaleFactor);
        int bodyFontSize = Math.round(18 * scaleFactor);
        int buttonFontSize = Math.round(18 * scaleFactor);

        int dialogWidth = (int) (screenSize.width * 0.25);
        int dialogHeight = (int) (screenSize.height * 0.30);

        JDialog confirmDialog = new JDialog(SwingUtilities.getWindowAncestor(this), title, Dialog.ModalityType.APPLICATION_MODAL);
        confirmDialog.setUndecorated(true);
        confirmDialog.setResizable(false);
        confirmDialog.setSize(dialogWidth, dialogHeight);
        confirmDialog.setMinimumSize(new Dimension(320, 250));
        confirmDialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));
        confirmDialog.setLayout(new BorderLayout());

        // Header Panel with amber warning color
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(255, 193, 7));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel headerLabel = new JLabel(message);
        headerLabel.setFont(new Font("Arial", Font.BOLD, headerFontSize));
        headerLabel.setForeground(Color.WHITE);
        headerPanel.add(headerLabel, BorderLayout.CENTER);

        // Add mouse drag functionality to header
        final java.awt.Point[] mouseDownCompCoords = {null};
        headerPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) {
                mouseDownCompCoords[0] = e.getPoint();
            }
            public void mouseReleased(java.awt.event.MouseEvent e) {
                mouseDownCompCoords[0] = null;
            }
        });
        headerPanel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent e) {
                if (mouseDownCompCoords[0] != null) {
                    java.awt.Point currCoords = e.getLocationOnScreen();
                    confirmDialog.setLocation(currCoords.x - mouseDownCompCoords[0].x, currCoords.y - mouseDownCompCoords[0].y);
                }
            }
        });

        confirmDialog.add(headerPanel, BorderLayout.NORTH);

        // Center Panel with details text
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel detailsLabel = new JLabel("<html><div style='text-align: center;'>" + details + "</div></html>");
        detailsLabel.setFont(new Font("Arial", Font.PLAIN, bodyFontSize));
        detailsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        centerPanel.add(detailsLabel, BorderLayout.CENTER);

        confirmDialog.add(centerPanel, BorderLayout.CENTER);

        // Button Panel
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        // No button (red, left side)
        JButton noButton = new JButton("No");
        noButton.setBackground(new Color(220, 53, 69));
        noButton.setForeground(Color.WHITE);
        noButton.setFont(new Font("Arial", Font.BOLD, buttonFontSize));
        applyRoundedStyle(noButton);

        // Yes button (green, right side - recommended option)
        JButton yesButton = new JButton("Yes");
        yesButton.setBackground(new Color(40, 167, 69));
        yesButton.setForeground(Color.WHITE);
        yesButton.setFont(new Font("Arial", Font.BOLD, buttonFontSize));
        applyRoundedStyle(yesButton);

        buttonPanel.add(noButton);
        buttonPanel.add(yesButton);

        confirmDialog.add(buttonPanel, BorderLayout.SOUTH);

        // Track user choice
        final boolean[] userChoice = {false};

        yesButton.addActionListener(e -> {
            userChoice[0] = true;
            confirmDialog.dispose();
        });

        noButton.addActionListener(e -> {
            userChoice[0] = false;
            confirmDialog.dispose();
        });

        confirmDialog.setVisible(true);

        return userChoice[0];
    }

    private void showInfoDialog(String title, String message, String details) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenHeight = screenSize.height;
        float scaleFactor = screenHeight / 1080.0f;

        int headerFontSize = Math.round(24 * scaleFactor);
        int bodyFontSize = Math.round(18 * scaleFactor);
        int buttonFontSize = Math.round(18 * scaleFactor);
        int buttonWidth = Math.round(130 * scaleFactor);
        int buttonHeight = Math.round(45 * scaleFactor);

        int dialogWidth = (int) (screenSize.width * 0.25);
        int dialogHeight = (int) (screenSize.height * 0.30);

        JDialog infoDialog = new JDialog(SwingUtilities.getWindowAncestor(this), title, Dialog.ModalityType.APPLICATION_MODAL);
        infoDialog.setUndecorated(true);
        infoDialog.setResizable(false);
        infoDialog.setSize(dialogWidth, dialogHeight);
        infoDialog.setMinimumSize(new Dimension(320, 250));
        infoDialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));
        infoDialog.setLayout(new BorderLayout());

        // Header Panel with blue info color
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(23, 162, 184));  // Info blue
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel headerLabel = new JLabel(message);
        headerLabel.setFont(new Font("Arial", Font.BOLD, headerFontSize));
        headerLabel.setForeground(Color.WHITE);
        headerPanel.add(headerLabel, BorderLayout.CENTER);

        // Add mouse drag functionality to header
        final java.awt.Point[] mouseDownCompCoords = {null};
        headerPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) {
                mouseDownCompCoords[0] = e.getPoint();
            }
            public void mouseReleased(java.awt.event.MouseEvent e) {
                mouseDownCompCoords[0] = null;
            }
        });
        headerPanel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent e) {
                if (mouseDownCompCoords[0] != null) {
                    java.awt.Point currCoords = e.getLocationOnScreen();
                    infoDialog.setLocation(currCoords.x - mouseDownCompCoords[0].x, currCoords.y - mouseDownCompCoords[0].y);
                }
            }
        });

        infoDialog.add(headerPanel, BorderLayout.NORTH);

        // Center Panel with details text
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel detailsLabel = new JLabel("<html><div style='text-align: center;'>" + details + "</div></html>");
        detailsLabel.setFont(new Font("Arial", Font.PLAIN, bodyFontSize));
        detailsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        centerPanel.add(detailsLabel, BorderLayout.CENTER);

        infoDialog.add(centerPanel, BorderLayout.CENTER);

        // Button Panel with single OK button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        JButton okButton = new JButton("OK");
        okButton.setBackground(new Color(40, 167, 69));  // Green
        okButton.setForeground(Color.WHITE);
        okButton.setFont(new Font("Arial", Font.BOLD, buttonFontSize));
        okButton.setPreferredSize(new Dimension(buttonWidth, buttonHeight));
        applyRoundedStyle(okButton);

        okButton.addActionListener(e -> infoDialog.dispose());

        buttonPanel.add(okButton);

        infoDialog.add(buttonPanel, BorderLayout.SOUTH);

        infoDialog.setVisible(true);
    }

    private void showWarningDialog(String title, String message, String details) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenHeight = screenSize.height;
        float scaleFactor = screenHeight / 1080.0f;

        int headerFontSize = Math.round(24 * scaleFactor);
        int bodyFontSize = Math.round(18 * scaleFactor);
        int buttonFontSize = Math.round(18 * scaleFactor);
        int buttonWidth = Math.round(130 * scaleFactor);
        int buttonHeight = Math.round(45 * scaleFactor);

        int dialogWidth = (int) (screenSize.width * 0.25);
        int dialogHeight = (int) (screenSize.height * 0.30);

        JDialog warningDialog = new JDialog(SwingUtilities.getWindowAncestor(this), title, Dialog.ModalityType.APPLICATION_MODAL);
        warningDialog.setUndecorated(true);
        warningDialog.setResizable(false);
        warningDialog.setSize(dialogWidth, dialogHeight);
        warningDialog.setMinimumSize(new Dimension(320, 250));
        warningDialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));
        warningDialog.setLayout(new BorderLayout());

        // Header Panel with amber warning color
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(255, 193, 7));  // Amber/warning
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel headerLabel = new JLabel(message);
        headerLabel.setFont(new Font("Arial", Font.BOLD, headerFontSize));
        headerLabel.setForeground(Color.WHITE);
        headerPanel.add(headerLabel, BorderLayout.CENTER);

        // Add mouse drag functionality to header
        final java.awt.Point[] mouseDownCompCoords = {null};
        headerPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) {
                mouseDownCompCoords[0] = e.getPoint();
            }
            public void mouseReleased(java.awt.event.MouseEvent e) {
                mouseDownCompCoords[0] = null;
            }
        });
        headerPanel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent e) {
                if (mouseDownCompCoords[0] != null) {
                    java.awt.Point currCoords = e.getLocationOnScreen();
                    warningDialog.setLocation(currCoords.x - mouseDownCompCoords[0].x, currCoords.y - mouseDownCompCoords[0].y);
                }
            }
        });

        warningDialog.add(headerPanel, BorderLayout.NORTH);

        // Center Panel with details text
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel detailsLabel = new JLabel("<html><div style='text-align: center;'>" + details + "</div></html>");
        detailsLabel.setFont(new Font("Arial", Font.PLAIN, bodyFontSize));
        detailsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        centerPanel.add(detailsLabel, BorderLayout.CENTER);

        warningDialog.add(centerPanel, BorderLayout.CENTER);

        // Button Panel with single OK button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        JButton okButton = new JButton("OK");
        okButton.setBackground(new Color(255, 193, 7));  // Amber to match warning theme
        okButton.setForeground(Color.WHITE);
        okButton.setFont(new Font("Arial", Font.BOLD, buttonFontSize));
        okButton.setPreferredSize(new Dimension(buttonWidth, buttonHeight));
        applyRoundedStyle(okButton);

        okButton.addActionListener(e -> warningDialog.dispose());

        buttonPanel.add(okButton);

        warningDialog.add(buttonPanel, BorderLayout.SOUTH);

        warningDialog.setVisible(true);
    }

    private void applyRoundedStyle(JButton button) {
        button.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        button.setContentAreaFilled(false);
        button.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                JButton btn = (JButton) c;
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int arcSize = 12;

                Color buttonColor = btn.getBackground();
                Color displayColor;
                Color textColor;

                if (btn.isEnabled()) {
                    displayColor = buttonColor;
                    textColor = btn.getForeground();
                } else {
                    displayColor = new Color(
                        (int)(buttonColor.getRed() * 0.5),
                        (int)(buttonColor.getGreen() * 0.5),
                        (int)(buttonColor.getBlue() * 0.5)
                    );
                    textColor = new Color(180, 180, 180);
                }

                g2d.setColor(displayColor);
                g2d.fillRoundRect(0, 0, btn.getWidth() - 1, btn.getHeight() - 1, arcSize, arcSize);

                g2d.setColor(displayColor.darker());
                g2d.setStroke(new BasicStroke(2f));
                g2d.drawRoundRect(0, 0, btn.getWidth() - 1, btn.getHeight() - 1, arcSize, arcSize);

                String text = btn.getText();
                FontMetrics fm = g2d.getFontMetrics(btn.getFont());
                int textWidth = fm.stringWidth(text);
                int x = (btn.getWidth() - textWidth) / 2;
                int y = (btn.getHeight() + fm.getAscent() - fm.getDescent()) / 2;

                g2d.setFont(btn.getFont());
                g2d.setColor(textColor);
                g2d.drawString(text, x, y);

                g2d.dispose();
            }
        });
    }

    private void showError(String message) {
        showErrorDialog("Error", "System Error", message);
    }

    private String showInputDialog(String title, String message, String itemName, String currentInfo, String defaultValue) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenHeight = screenSize.height;
        float scaleFactor = screenHeight / 1080.0f;

        int headerFontSize = Math.round(24 * scaleFactor);
        int labelFontSize = Math.round(18 * scaleFactor);
        int inputFontSize = Math.round(20 * scaleFactor);
        int buttonFontSize = Math.round(18 * scaleFactor);
        int inputFieldHeight = Math.round(45 * scaleFactor);

        int dialogWidth = (int) (screenSize.width * 0.30);
        int dialogHeight = (int) (screenSize.height * 0.30);

        JDialog inputDialog = new JDialog(SwingUtilities.getWindowAncestor(this), title, Dialog.ModalityType.APPLICATION_MODAL);
        inputDialog.setUndecorated(true);
        inputDialog.setResizable(false);
        inputDialog.setSize(dialogWidth, dialogHeight);
        inputDialog.setMinimumSize(new Dimension(350, 280));
        inputDialog.setLocationRelativeTo(this);
        inputDialog.setLayout(new BorderLayout());

        // Header Panel with blue info color
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(23, 162, 184));  // Info blue
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel headerLabel = new JLabel(message);
        headerLabel.setFont(new Font("Arial", Font.BOLD, headerFontSize));
        headerLabel.setForeground(Color.WHITE);
        headerPanel.add(headerLabel, BorderLayout.CENTER);

        // Add mouse drag functionality to header
        final java.awt.Point[] mouseDownCompCoords = {null};
        headerPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) {
                mouseDownCompCoords[0] = e.getPoint();
            }
            public void mouseReleased(java.awt.event.MouseEvent e) {
                mouseDownCompCoords[0] = null;
            }
        });
        headerPanel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent e) {
                if (mouseDownCompCoords[0] != null) {
                    java.awt.Point currCoords = e.getLocationOnScreen();
                    inputDialog.setLocation(currCoords.x - mouseDownCompCoords[0].x, currCoords.y - mouseDownCompCoords[0].y);
                }
            }
        });

        inputDialog.add(headerPanel, BorderLayout.NORTH);

        // Center Panel with input field
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Info label
        JLabel infoLabel = new JLabel(currentInfo);
        infoLabel.setFont(new Font("Arial", Font.PLAIN, labelFontSize));
        infoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(infoLabel);

        centerPanel.add(Box.createVerticalStrut(15));

        // Input label
        JLabel inputLabel = new JLabel(itemName + ":");
        inputLabel.setFont(new Font("Arial", Font.PLAIN, labelFontSize));
        inputLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(inputLabel);

        centerPanel.add(Box.createVerticalStrut(5));

        // Input text field
        JTextField inputField = new JTextField(defaultValue);
        inputField.setFont(new Font("Arial", Font.PLAIN, inputFontSize));
        inputField.setMaximumSize(new Dimension(Integer.MAX_VALUE, inputFieldHeight));
        inputField.setAlignmentX(Component.LEFT_ALIGNMENT);
        inputField.selectAll();
        centerPanel.add(inputField);

        inputDialog.add(centerPanel, BorderLayout.CENTER);

        // Button Panel
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        // Track user input
        final String[] userInput = {null};

        // Cancel button (red, left side)
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setBackground(new Color(220, 53, 69));
        cancelButton.setForeground(Color.WHITE);
        cancelButton.setFont(new Font("Arial", Font.BOLD, buttonFontSize));
        cancelButton.setFocusPainted(false);
        cancelButton.setBorderPainted(false);
        cancelButton.setOpaque(true);
        applyRoundedStyle(cancelButton);
        applyTextOutline(cancelButton);

        cancelButton.addActionListener(e -> {
            userInput[0] = null;
            inputDialog.dispose();
        });

        // Submit button (green, right side)
        JButton submitButton = new JButton("Submit");
        submitButton.setBackground(new Color(40, 167, 69));
        submitButton.setForeground(Color.WHITE);
        submitButton.setFont(new Font("Arial", Font.BOLD, buttonFontSize));
        submitButton.setFocusPainted(false);
        submitButton.setBorderPainted(false);
        submitButton.setOpaque(true);
        applyRoundedStyle(submitButton);
        applyTextOutline(submitButton);

        submitButton.addActionListener(e -> {
            userInput[0] = inputField.getText();
            inputDialog.dispose();
        });

        // Enter key submits
        inputField.addActionListener(e -> submitButton.doClick());

        buttonPanel.add(cancelButton);
        buttonPanel.add(submitButton);

        inputDialog.add(buttonPanel, BorderLayout.SOUTH);

        // Request focus on input field when dialog opens
        inputDialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowOpened(java.awt.event.WindowEvent e) {
                inputField.requestFocusInWindow();
            }
        });

        inputDialog.setVisible(true);

        return userInput[0];
    }

    private void showErrorDialog(String title, String message, String details) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenHeight = screenSize.height;
        float scaleFactor = screenHeight / 1080.0f;

        int headerFontSize = Math.round(24 * scaleFactor);
        int bodyFontSize = Math.round(18 * scaleFactor);
        int buttonFontSize = Math.round(18 * scaleFactor);
        int buttonWidth = Math.round(130 * scaleFactor);
        int buttonHeight = Math.round(45 * scaleFactor);

        int dialogWidth = (int) (screenSize.width * 0.25);
        int dialogHeight = (int) (screenSize.height * 0.30);

        JDialog errorDialog = new JDialog(SwingUtilities.getWindowAncestor(this), title, Dialog.ModalityType.APPLICATION_MODAL);
        errorDialog.setUndecorated(true);
        errorDialog.setResizable(false);
        errorDialog.setSize(dialogWidth, dialogHeight);
        errorDialog.setMinimumSize(new Dimension(320, 250));
        errorDialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));
        errorDialog.setLayout(new BorderLayout());

        // Header Panel with red error color
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(220, 53, 69));  // Error red
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel headerLabel = new JLabel(message);
        headerLabel.setFont(new Font("Arial", Font.BOLD, headerFontSize));
        headerLabel.setForeground(Color.WHITE);
        headerPanel.add(headerLabel, BorderLayout.CENTER);

        // Add mouse drag functionality to header
        final java.awt.Point[] mouseDownCompCoords = {null};
        headerPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) {
                mouseDownCompCoords[0] = e.getPoint();
            }
            public void mouseReleased(java.awt.event.MouseEvent e) {
                mouseDownCompCoords[0] = null;
            }
        });
        headerPanel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent e) {
                if (mouseDownCompCoords[0] != null) {
                    java.awt.Point currCoords = e.getLocationOnScreen();
                    errorDialog.setLocation(currCoords.x - mouseDownCompCoords[0].x, currCoords.y - mouseDownCompCoords[0].y);
                }
            }
        });

        errorDialog.add(headerPanel, BorderLayout.NORTH);

        // Center Panel with details text
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel detailsLabel = new JLabel("<html><div style='text-align: center;'>" + details + "</div></html>");
        detailsLabel.setFont(new Font("Arial", Font.PLAIN, bodyFontSize));
        detailsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        centerPanel.add(detailsLabel, BorderLayout.CENTER);

        errorDialog.add(centerPanel, BorderLayout.CENTER);

        // Button Panel with single OK button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        JButton okButton = new JButton("OK");
        okButton.setBackground(new Color(220, 53, 69));  // Red to match error theme
        okButton.setForeground(Color.WHITE);
        okButton.setFont(new Font("Arial", Font.BOLD, buttonFontSize));
        okButton.setPreferredSize(new Dimension(buttonWidth, buttonHeight));
        applyRoundedStyle(okButton);

        okButton.addActionListener(e -> errorDialog.dispose());

        buttonPanel.add(okButton);

        errorDialog.add(buttonPanel, BorderLayout.SOUTH);

        errorDialog.setVisible(true);
    }

    // Public methods for main interface to control button states
    public void setDeleteSelectedEnabled(boolean enabled) {
        deleteSelectedButton.setEnabled(enabled);
        deleteSelectedButton.repaint();
    }

    public void setChangeQtyEnabled(boolean enabled) {
        changeQtyButton.setEnabled(enabled);
        changeQtyButton.repaint();
    }

    public void setChangeQtyCallback(Runnable callback) {
        // Remove any existing listeners first
        for (ActionListener listener : changeQtyButton.getActionListeners()) {
            changeQtyButton.removeActionListener(listener);
        }
        changeQtyButton.addActionListener(e -> callback.run());
    }

    private void setTransactionControlsEnabled(boolean enabled) {
        barcodeScannerField.setEnabled(enabled);
        if (enabled) {
            // Re-focus scanner field when re-enabled
            SwingUtilities.invokeLater(() -> barcodeScannerField.requestFocusInWindow());
        }
        changeQtyButton.setEnabled(false);
        changeQtyButton.repaint();
        deleteSelectedButton.setEnabled(false);
        deleteSelectedButton.repaint();
        voidTransactionButton.setEnabled(enabled);
        voidTransactionButton.repaint();
        totalButton.setEnabled(enabled);
        totalButton.repaint();
    }

    private void setPaymentButtonsEnabled(boolean enabled) {
        payCashButton.setEnabled(enabled);
        payCashButton.repaint();
        payCardButton.setEnabled(enabled);
        payCardButton.repaint();
        paymentVoidButton.setEnabled(enabled);
        paymentVoidButton.repaint();
    }

    private void resetButtonStatesForNewTransaction() {
        setTransactionControlsEnabled(true);
        setPaymentButtonsEnabled(false);
    }

    public void setDeleteSelectedCallback(Runnable callback) {
        // Remove any existing listeners first
        for (ActionListener listener : deleteSelectedButton.getActionListeners()) {
            deleteSelectedButton.removeActionListener(listener);
        }
        deleteSelectedButton.addActionListener(e -> callback.run());
    }

    public void setTransactionFinalizedCallback(Runnable callback) {
        this.onTransactionFinalized = callback;
    }

    public void setTransactionResumedCallback(Runnable callback) {
        this.onTransactionResumed = callback;
    }

    public void returnFocusToScanner() {
        SwingUtilities.invokeLater(() -> barcodeScannerField.requestFocusInWindow());
    }
}
