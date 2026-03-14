package org.possystem.ui;

import org.possystem.service.PriceBookService;
import org.possystem.service.TransactionService;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

/**
 * POS Interface - Main frame coordinator
 */
public class PosInterface extends JFrame {

    // Services
    private final PriceBookService priceBookService;
    private final TransactionService transactionService;

    // UI Panels
    private QuickKeysPanel quickKeysPanel;
    private CurrentSalePanel currentSalePanel;
    private ActionsPanel actionsPanel;
    private JSplitPane mainSplitPane;

    public PosInterface() {
        this.priceBookService = new PriceBookService();
        this.transactionService = new TransactionService();

        setupFrame();
        initializeComponents();
        layoutComponents();

        // Create initial transaction
        try {
            transactionService.createTransaction();
        } catch (SQLException e) {
            showError("Failed to create initial transaction: " + e.getMessage());
        }

        refreshSaleDisplay();
    }

    private void setupFrame() {
        setTitle("POS System - Point of Sale");
        setSize(1600, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    private void initializeComponents() {
        // Create the three main panels
        quickKeysPanel = new QuickKeysPanel(priceBookService, item -> {
            try {
                transactionService.addItem(item.upc(), item.name(), item.price());
                refreshSaleDisplay();
                // Return focus to barcode scanner after adding item
                actionsPanel.returnFocusToScanner();
            } catch (SQLException e) {
                showError("Failed to add item: " + e.getMessage());
            }
        });

        currentSalePanel = new CurrentSalePanel(transactionService, this::updateDeleteSelectedButton);

        actionsPanel = new ActionsPanel(priceBookService, transactionService, this::refreshSaleDisplay);

        // Wire up callbacks
        actionsPanel.setDeleteSelectedCallback(this::handleDeleteSelected);
        actionsPanel.setChangeQtyCallback(this::handleChangeQuantity);
        actionsPanel.setTransactionFinalizedCallback(this::handleTransactionFinalized);
        actionsPanel.setTransactionResumedCallback(this::handleTransactionResumed);
    }

    private void layoutComponents() {
        setLayout(new BorderLayout(10, 10));

        // CENTER/RIGHT: Split for Quick Keys (top) and Actions (bottom)
        JPanel rightPanel = new JPanel(new BorderLayout(10, 10));

        // Create vertical split pane for Quick Keys (60%) and Actions (40%)
        JSplitPane verticalSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, quickKeysPanel, actionsPanel);
        verticalSplitPane.setResizeWeight(0.6);
        verticalSplitPane.setDividerSize(8);
        verticalSplitPane.setContinuousLayout(true);
        verticalSplitPane.setOneTouchExpandable(false);

        // Custom horizontal divider
        verticalSplitPane.setUI(new javax.swing.plaf.basic.BasicSplitPaneUI() {
            @Override
            public javax.swing.plaf.basic.BasicSplitPaneDivider createDefaultDivider() {
                return new javax.swing.plaf.basic.BasicSplitPaneDivider(this) {
                    private boolean isHovered = false;

                    {
                        addMouseListener(new java.awt.event.MouseAdapter() {
                            @Override
                            public void mouseEntered(java.awt.event.MouseEvent e) {
                                isHovered = true;
                                repaint();
                            }

                            @Override
                            public void mouseExited(java.awt.event.MouseEvent e) {
                                isHovered = false;
                                repaint();
                            }
                        });
                    }

                    @Override
                    public void paint(Graphics g) {
                        Graphics2D g2d = (Graphics2D) g.create();
                        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                        int width = getWidth();
                        int height = getHeight();

                        Color bgColor = isHovered ? new Color(100, 149, 237) : new Color(180, 180, 180);
                        g2d.setColor(bgColor);
                        g2d.fillRect(0, 0, width, height);

                        g2d.setColor(isHovered ? Color.WHITE : new Color(120, 120, 120));
                        int centerY = height / 2;
                        int gripSpacing = 4;
                        int gripDotSize = 3;
                        int startX = width / 2 - (gripSpacing * 4);

                        for (int i = 0; i < 9; i++) {
                            int x = startX + (i * gripSpacing);
                            if (x >= 10 && x <= width - 10) {
                                g2d.fillOval(x, centerY - gripDotSize / 2, gripDotSize, gripDotSize);
                            }
                        }

                        if (isHovered) {
                            g2d.setColor(Color.WHITE);
                            int arrowX = width / 2;
                            int[] xPointsUp = {arrowX, arrowX - 2, arrowX - 4};
                            int[] yPointsUp = {centerY - 3, centerY - 1, centerY - 3};
                            g2d.fillPolygon(xPointsUp, yPointsUp, 3);
                            int[] xPointsUp2 = {arrowX, arrowX + 2, arrowX + 4};
                            int[] yPointsUp2 = {centerY - 3, centerY - 1, centerY - 3};
                            g2d.fillPolygon(xPointsUp2, yPointsUp2, 3);

                            int[] xPointsDown = {arrowX, arrowX - 2, arrowX - 4};
                            int[] yPointsDown = {centerY + 3, centerY + 1, centerY + 3};
                            g2d.fillPolygon(xPointsDown, yPointsDown, 3);
                            int[] xPointsDown2 = {arrowX, arrowX + 2, arrowX + 4};
                            int[] yPointsDown2 = {centerY + 3, centerY + 1, centerY + 3};
                            g2d.fillPolygon(xPointsDown2, yPointsDown2, 3);
                        }

                        g2d.dispose();
                    }
                };
            }
        });

        rightPanel.add(verticalSplitPane, BorderLayout.CENTER);

        // Create resizable split pane with Current Sale on left and Quick Keys/Actions on right
        mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, currentSalePanel, rightPanel);
        mainSplitPane.setDividerLocation(CurrentSalePanel.DEFAULT_WIDTH);
        mainSplitPane.setDividerSize(8);
        mainSplitPane.setContinuousLayout(true);
        mainSplitPane.setOneTouchExpandable(false);

        // Custom vertical divider
        mainSplitPane.setUI(new javax.swing.plaf.basic.BasicSplitPaneUI() {
            @Override
            public javax.swing.plaf.basic.BasicSplitPaneDivider createDefaultDivider() {
                return new javax.swing.plaf.basic.BasicSplitPaneDivider(this) {
                    private boolean isHovered = false;

                    {
                        addMouseListener(new java.awt.event.MouseAdapter() {
                            @Override
                            public void mouseEntered(java.awt.event.MouseEvent e) {
                                isHovered = true;
                                repaint();
                            }

                            @Override
                            public void mouseExited(java.awt.event.MouseEvent e) {
                                isHovered = false;
                                repaint();
                            }
                        });
                    }

                    @Override
                    public void paint(Graphics g) {
                        Graphics2D g2d = (Graphics2D) g.create();
                        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                        int width = getWidth();
                        int height = getHeight();

                        Color bgColor = isHovered ? new Color(100, 149, 237) : new Color(180, 180, 180);
                        g2d.setColor(bgColor);
                        g2d.fillRect(0, 0, width, height);

                        g2d.setColor(isHovered ? Color.WHITE : new Color(120, 120, 120));
                        int centerX = width / 2;
                        int gripSpacing = 4;
                        int gripDotSize = 3;
                        int startY = height / 2 - (gripSpacing * 4);

                        for (int i = 0; i < 9; i++) {
                            int y = startY + (i * gripSpacing);
                            if (y >= 10 && y <= height - 10) {
                                g2d.fillOval(centerX - gripDotSize / 2, y, gripDotSize, gripDotSize);
                            }
                        }

                        if (isHovered) {
                            g2d.setColor(Color.WHITE);
                            int arrowY = height / 2;
                            int[] xPointsLeft = {centerX - 3, centerX - 1, centerX - 3};
                            int[] yPointsLeft = {arrowY, arrowY - 2, arrowY - 4};
                            g2d.fillPolygon(xPointsLeft, yPointsLeft, 3);
                            int[] xPointsLeft2 = {centerX - 3, centerX - 1, centerX - 3};
                            int[] yPointsLeft2 = {arrowY, arrowY + 2, arrowY + 4};
                            g2d.fillPolygon(xPointsLeft2, yPointsLeft2, 3);

                            int[] xPointsRight = {centerX + 3, centerX + 1, centerX + 3};
                            int[] yPointsRight = {arrowY, arrowY - 2, arrowY - 4};
                            g2d.fillPolygon(xPointsRight, yPointsRight, 3);
                            int[] xPointsRight2 = {centerX + 3, centerX + 1, centerX + 3};
                            int[] yPointsRight2 = {arrowY, arrowY + 2, arrowY + 4};
                            g2d.fillPolygon(xPointsRight2, yPointsRight2, 3);
                        }

                        g2d.dispose();
                    }
                };
            }
        });

        // Enforce min/max width constraints
        mainSplitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, evt -> {
            int location = mainSplitPane.getDividerLocation();
            if (location < CurrentSalePanel.MIN_WIDTH) {
                mainSplitPane.setDividerLocation(CurrentSalePanel.MIN_WIDTH);
            } else if (location > CurrentSalePanel.MAX_WIDTH) {
                mainSplitPane.setDividerLocation(CurrentSalePanel.MAX_WIDTH);
            }
        });

        // Add custom header bar
        add(createHeaderPanel(), BorderLayout.NORTH);
        add(mainSplitPane, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(45, 45, 48));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        headerPanel.setPreferredSize(new Dimension(0, 50));

        JLabel titleLabel = new JLabel("POS System");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        JButton closeButton = createCloseButton();
        headerPanel.add(closeButton, BorderLayout.EAST);

        // Add window dragging functionality
        final Point[] mouseDownCompCoords = {null};

        headerPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                mouseDownCompCoords[0] = e.getPoint();
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                mouseDownCompCoords[0] = null;
            }
        });

        headerPanel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseDragged(java.awt.event.MouseEvent e) {
                if (mouseDownCompCoords[0] != null) {
                    Point currCoords = e.getLocationOnScreen();
                    setLocation(currCoords.x - mouseDownCompCoords[0].x,
                               currCoords.y - mouseDownCompCoords[0].y);
                }
            }
        });

        return headerPanel;
    }

    private JButton createCloseButton() {
        JButton closeButton = new JButton() {
            private boolean isHovered = false;

            {
                setFocusPainted(false);
                setBorderPainted(false);
                setContentAreaFilled(false);
                setOpaque(false);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                setPreferredSize(new Dimension(40, 40));
                setToolTipText("Close");

                addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseEntered(java.awt.event.MouseEvent e) {
                        isHovered = true;
                        repaint();
                    }

                    @Override
                    public void mouseExited(java.awt.event.MouseEvent e) {
                        isHovered = false;
                        repaint();
                    }
                });

                addActionListener(e -> {
                    int result = JOptionPane.showConfirmDialog(
                        PosInterface.this,
                        "Are you sure you want to close the POS System?",
                        "Confirm Close",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                    );
                    if (result == JOptionPane.YES_OPTION) {
                        System.exit(0);
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int size = Math.min(getWidth(), getHeight());
                int padding = size / 3;

                if (isHovered) {
                    g2d.setColor(new Color(232, 17, 35));
                    g2d.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 4, 4);
                }

                g2d.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2d.setColor(isHovered ? Color.WHITE : new Color(200, 200, 200));

                int x1 = padding;
                int y1 = padding;
                int x2 = size - padding;
                int y2 = size - padding;

                g2d.drawLine(x1, y1, x2, y2);
                g2d.drawLine(x2, y1, x1, y2);

                g2d.dispose();
            }
        };

        return closeButton;
    }

    private void refreshSaleDisplay() {
        currentSalePanel.refreshDisplay();
        updateDeleteSelectedButton();
    }

    private void updateDeleteSelectedButton() {
        List<Integer> selectedIds = currentSalePanel.getSelectedItemIds();
        int checkboxSelectionCount = selectedIds.size();
        boolean hasRowSelection = currentSalePanel.hasRowSelection();

        // Enable Delete button if any checkboxes selected
        actionsPanel.setDeleteSelectedEnabled(checkboxSelectionCount > 0);

        // Enable Change Qty button if:
        // - Exactly 1 checkbox selected, OR
        // - No checkboxes selected but a row is selected
        boolean enableChangeQty = (checkboxSelectionCount == 1) ||
                                  (checkboxSelectionCount == 0 && hasRowSelection);
        actionsPanel.setChangeQtyEnabled(enableChangeQty);
    }

    private void handleDeleteSelected() {
        var selectedIds = currentSalePanel.getSelectedItemIds();

        if (selectedIds.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No items selected", "Info",
                JOptionPane.INFORMATION_MESSAGE);
            actionsPanel.returnFocusToScanner();
            return;
        }

        // Create custom confirmation dialog
        boolean confirmed = showConfirmDialog(
            "Confirm Delete",
            "Delete " + selectedIds.size() + " selected item(s)?",
            "This action will remove the selected items from the cart."
        );

        if (confirmed) {
            try {
                transactionService.deleteSelectedItems(selectedIds);
                refreshSaleDisplay();
            } catch (SQLException e) {
                showError("Failed to delete items: " + e.getMessage());
            }
        }

        // Return focus to scanner after dialog closes (whether Yes or No)
        actionsPanel.returnFocusToScanner();
    }

    private void handleChangeQuantity() {
        // Try checkbox selection first, then fall back to row selection
        List<Integer> checkboxSelectedIds = currentSalePanel.getSelectedItemIds();
        Integer selectedId = null;

        if (!checkboxSelectedIds.isEmpty()) {
            // Use checkbox selection
            if (checkboxSelectedIds.size() > 1) {
                JOptionPane.showMessageDialog(this,
                    "Multiple items selected.\n\nPlease select only ONE item to change quantity.",
                    "Multiple Selection",
                    JOptionPane.WARNING_MESSAGE);
                actionsPanel.returnFocusToScanner();
                return;
            }
            selectedId = checkboxSelectedIds.get(0);
        } else {
            // Fall back to row selection
            selectedId = currentSalePanel.getRowSelectedItemId();
        }

        if (selectedId == null) {
            JOptionPane.showMessageDialog(this,
                "No item selected.\n\nPlease select an item from the cart to change its quantity.",
                "No Selection",
                JOptionPane.INFORMATION_MESSAGE);
            actionsPanel.returnFocusToScanner();
            return;
        }

        try {
            // Get current sale items to find the selected one
            List<org.possystem.entity.TransactionItem> items = transactionService.getCurrentSaleItems();
            final int itemId = selectedId;

            // Find the selected item
            org.possystem.entity.TransactionItem selectedItem = items.stream()
                .filter(item -> item.id() == itemId && item.status().equals("ACTIVE"))
                .findFirst()
                .orElse(null);

            if (selectedItem == null) {
                showError("Selected item not found");
                actionsPanel.returnFocusToScanner();
                return;
            }

            // Show custom input dialog
            String input = showInputDialog(
                "Change Quantity",
                "Update Item Quantity",
                selectedItem.name(),
                "Current Quantity: " + selectedItem.quantity(),
                String.valueOf(selectedItem.quantity())
            );

            // User cancelled
            if (input == null) {
                actionsPanel.returnFocusToScanner();
                return;
            }

            // Validate input
            try {
                int newQty = Integer.parseInt(input.trim());
                if (newQty < 1) {
                    showErrorDialog("Invalid Quantity", "Quantity Too Low", "Quantity must be at least 1.");
                    actionsPanel.returnFocusToScanner();
                    return;
                }

                // Update quantity
                transactionService.updateQuantity(selectedItem.id(), newQty, selectedItem.unitPrice());

                // Refresh display
                refreshSaleDisplay();

                // Return focus to barcode scanner
                actionsPanel.returnFocusToScanner();

            } catch (NumberFormatException e) {
                showErrorDialog("Invalid Quantity", "Invalid Input", "Please enter a whole number.");
                actionsPanel.returnFocusToScanner();
            }
        } catch (SQLException e) {
            showError("Failed to change quantity: " + e.getMessage());
            actionsPanel.returnFocusToScanner();
        }
    }

    private void handleTransactionFinalized() {
        // Disable Quick Keys (product selection, search, filtering)
        quickKeysPanel.setProductSelectionEnabled(false);

        // Disable Current Sale editing (quantity controls, delete buttons)
        currentSalePanel.setEditingEnabled(false);
    }

    private void handleTransactionResumed() {
        // Re-enable Quick Keys (product selection, search, filtering)
        quickKeysPanel.setProductSelectionEnabled(true);

        // Re-enable Current Sale editing (quantity controls, delete buttons)
        currentSalePanel.setEditingEnabled(true);
    }

    private boolean showConfirmDialog(String title, String message, String details) {
        // Get screen dimensions for responsive sizing
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenHeight = screenSize.height;
        float scaleFactor = screenHeight / 1080.0f;

        int headerFontSize = Math.round(24 * scaleFactor);
        int bodyFontSize = Math.round(18 * scaleFactor);
        int buttonFontSize = Math.round(18 * scaleFactor);
        int buttonHeight = Math.round(50 * scaleFactor);

        JDialog confirmDialog = new JDialog(this, title, Dialog.ModalityType.APPLICATION_MODAL);
        confirmDialog.setSize(400, 280);
        confirmDialog.setMinimumSize(new Dimension(350, 280));
        confirmDialog.setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Header with warning color
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(255, 193, 7));  // Amber/warning color
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel headerLabel = new JLabel(message);
        headerLabel.setFont(new Font("Arial", Font.BOLD, headerFontSize));
        headerLabel.setForeground(Color.WHITE);
        headerPanel.add(headerLabel, BorderLayout.CENTER);

        // Details panel
        JPanel detailsPanel = new JPanel(new BorderLayout());
        detailsPanel.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));

        JLabel detailsLabel = new JLabel("<html><center>" + details + "</center></html>");
        detailsLabel.setFont(new Font("Arial", Font.PLAIN, bodyFontSize));
        detailsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        detailsPanel.add(detailsLabel, BorderLayout.CENTER);

        // Buttons panel
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 10, 0));

        final boolean[] result = {false};

        JButton noButton = new JButton("No");
        noButton.setFont(new Font("Arial", Font.BOLD, buttonFontSize));
        noButton.setPreferredSize(new Dimension(0, buttonHeight));
        noButton.setBackground(new Color(220, 53, 69));  // Red
        noButton.setForeground(Color.WHITE);
        noButton.addActionListener(e -> {
            result[0] = false;
            confirmDialog.dispose();
        });
        applyRoundedStyle(noButton);

        JButton yesButton = new JButton("Yes");
        yesButton.setFont(new Font("Arial", Font.BOLD, buttonFontSize));
        yesButton.setPreferredSize(new Dimension(0, buttonHeight));
        yesButton.setBackground(new Color(40, 167, 69));  // Green
        yesButton.setForeground(Color.WHITE);
        yesButton.addActionListener(e -> {
            result[0] = true;
            confirmDialog.dispose();
        });
        applyRoundedStyle(yesButton);

        buttonPanel.add(noButton);
        buttonPanel.add(yesButton);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(detailsPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        confirmDialog.add(mainPanel);
        confirmDialog.setVisible(true);

        return result[0];
    }

    private String showInputDialog(String title, String message, String itemName, String currentInfo, String defaultValue) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenHeight = screenSize.height;
        float scaleFactor = screenHeight / 1080.0f;

        int headerFontSize = Math.round(24 * scaleFactor);
        int labelBoldFontSize = Math.round(19 * scaleFactor);
        int labelFontSize = Math.round(18 * scaleFactor);
        int inputFontSize = Math.round(20 * scaleFactor);
        int buttonFontSize = Math.round(18 * scaleFactor);
        int inputFieldHeight = Math.round(45 * scaleFactor);

        int dialogWidth = Math.min(450, screenSize.width - 100);
        int dialogHeight = Math.min(650, screenSize.height - 100);

        JDialog inputDialog = new JDialog(this, title, Dialog.ModalityType.APPLICATION_MODAL);
        inputDialog.setSize(dialogWidth, dialogHeight);
        inputDialog.setMinimumSize(new Dimension(400, 550));
        inputDialog.setLocationRelativeTo(null); // Center on screen
        inputDialog.setLayout(new BorderLayout());

        // Header Panel with blue info color
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(23, 162, 184));  // Info blue
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel headerLabel = new JLabel(message);
        headerLabel.setFont(new Font("Arial", Font.BOLD, headerFontSize));
        headerLabel.setForeground(Color.WHITE);
        headerPanel.add(headerLabel, BorderLayout.CENTER);

        inputDialog.add(headerPanel, BorderLayout.NORTH);

        // Center Panel with item info and input field
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10)); // Reduced left padding

        // Item name label - same width as input box, left-aligned text
        JLabel itemLabel = new JLabel("Item: " + itemName);
        itemLabel.setFont(new Font("Arial", Font.BOLD, labelBoldFontSize));
        itemLabel.setMaximumSize(new Dimension(380, 30)); // Match input box width
        itemLabel.setPreferredSize(new Dimension(380, 30));
        itemLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(itemLabel);

        centerPanel.add(Box.createVerticalStrut(10));

        // Current quantity label - same width as input box, left-aligned text
        JLabel currentLabel = new JLabel(currentInfo);
        currentLabel.setFont(new Font("Arial", Font.PLAIN, labelFontSize));
        currentLabel.setForeground(new Color(100, 100, 100));
        currentLabel.setMaximumSize(new Dimension(380, 25)); // Match input box width
        currentLabel.setPreferredSize(new Dimension(380, 25));
        currentLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(currentLabel);

        centerPanel.add(Box.createVerticalStrut(15));

        // New quantity label - same width as input box, left-aligned text
        JLabel newQtyLabel = new JLabel("Enter New Quantity:");
        newQtyLabel.setFont(new Font("Arial", Font.PLAIN, labelFontSize));
        newQtyLabel.setMaximumSize(new Dimension(380, 25)); // Match input box width
        newQtyLabel.setPreferredSize(new Dimension(380, 25));
        newQtyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(newQtyLabel);

        centerPanel.add(Box.createVerticalStrut(5));

        // Input text field (read-only, keypad only) - same width as keypad
        JTextField inputField = new JTextField(defaultValue);
        inputField.setFont(new Font("Arial", Font.BOLD, 24));
        inputField.setHorizontalAlignment(JTextField.CENTER);
        inputField.setMaximumSize(new Dimension(380, 50)); // Match keypad width
        inputField.setPreferredSize(new Dimension(380, 50));
        inputField.setAlignmentX(Component.CENTER_ALIGNMENT);
        inputField.setEditable(false);  // Only keypad can input
        centerPanel.add(inputField);

        centerPanel.add(Box.createVerticalStrut(8)); // Reduced spacing

        // Numeric keypad (no decimal point for quantities)
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
        JButton btn7 = createQuantityKeypadButton("7", inputField);
        keypadPanel.add(btn7, gbc);

        gbc.gridx = 1;
        JButton btn8 = createQuantityKeypadButton("8", inputField);
        keypadPanel.add(btn8, gbc);

        gbc.gridx = 2;
        JButton btn9 = createQuantityKeypadButton("9", inputField);
        keypadPanel.add(btn9, gbc);

        gbc.gridx = 3; gbc.gridheight = 2; // Backspace spans 2 rows
        JButton backspaceBtn = createQuantityKeypadButton("←", inputField);
        backspaceBtn.setBackground(new Color(255, 193, 7)); // Amber
        keypadPanel.add(backspaceBtn, gbc);

        // Row 2: 4, 5, 6
        gbc.gridheight = 1; // Reset to 1 row
        gbc.gridx = 0; gbc.gridy = 1;
        JButton btn4 = createQuantityKeypadButton("4", inputField);
        keypadPanel.add(btn4, gbc);

        gbc.gridx = 1;
        JButton btn5 = createQuantityKeypadButton("5", inputField);
        keypadPanel.add(btn5, gbc);

        gbc.gridx = 2;
        JButton btn6 = createQuantityKeypadButton("6", inputField);
        keypadPanel.add(btn6, gbc);

        // Row 3: 1, 2, 3, Clear (spans 2 rows)
        gbc.gridx = 0; gbc.gridy = 2;
        JButton btn1 = createQuantityKeypadButton("1", inputField);
        keypadPanel.add(btn1, gbc);

        gbc.gridx = 1;
        JButton btn2 = createQuantityKeypadButton("2", inputField);
        keypadPanel.add(btn2, gbc);

        gbc.gridx = 2;
        JButton btn3 = createQuantityKeypadButton("3", inputField);
        keypadPanel.add(btn3, gbc);

        gbc.gridx = 3; gbc.gridheight = 2; // Clear spans 2 rows
        JButton clearBtn = createQuantityKeypadButton("Clear", inputField);
        clearBtn.setBackground(new Color(220, 53, 69)); // Red
        keypadPanel.add(clearBtn, gbc);

        // Row 4: empty, 0, empty
        gbc.gridheight = 1; // Reset to 1 row
        gbc.gridx = 0; gbc.gridy = 3;
        JPanel emptyPanel1 = new JPanel();
        emptyPanel1.setBackground(Color.WHITE);
        keypadPanel.add(emptyPanel1, gbc);

        gbc.gridx = 1;
        JButton btn0 = createQuantityKeypadButton("0", inputField);
        keypadPanel.add(btn0, gbc);

        gbc.gridx = 2;
        JPanel emptyPanel2 = new JPanel();
        emptyPanel2.setBackground(Color.WHITE);
        keypadPanel.add(emptyPanel2, gbc);

        centerPanel.add(keypadPanel);

        inputDialog.add(centerPanel, BorderLayout.CENTER);

        // Button Panel - match keypad width
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 15, 0));

        // Track user input
        final String[] userInput = {null};

        // Cancel button (red, left side)
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setBackground(new Color(220, 53, 69));
        cancelButton.setForeground(Color.WHITE);
        cancelButton.setFont(new Font("Arial", Font.BOLD, 16));
        cancelButton.setPreferredSize(new Dimension(185, 50)); // Match keypad width proportionally
        cancelButton.setFocusPainted(false);
        cancelButton.setBorderPainted(false);
        cancelButton.setOpaque(true);
        applyRoundedStyle(cancelButton);

        cancelButton.addActionListener(e -> {
            userInput[0] = null;
            inputDialog.dispose();
        });

        // OK button (green, right side)
        JButton okButton = new JButton("OK");
        okButton.setBackground(new Color(40, 167, 69));
        okButton.setForeground(Color.WHITE);
        okButton.setFont(new Font("Arial", Font.BOLD, 16));
        okButton.setPreferredSize(new Dimension(185, 50)); // Match keypad width proportionally
        okButton.setFocusPainted(false);
        okButton.setBorderPainted(false);
        okButton.setOpaque(true);
        applyRoundedStyle(okButton);

        okButton.addActionListener(e -> {
            userInput[0] = inputField.getText();
            inputDialog.dispose();
        });

        buttonPanel.add(cancelButton);
        buttonPanel.add(okButton);

        inputDialog.add(buttonPanel, BorderLayout.SOUTH);

        inputDialog.setVisible(true);

        return userInput[0];
    }

    private JButton createQuantityKeypadButton(String key, JTextField inputField) {
        JButton keyButton = new JButton(key);
        keyButton.setFont(new Font("Arial", Font.BOLD, 18));
        keyButton.setFocusPainted(false);
        keyButton.setBackground(new Color(248, 249, 250)); // Light gray
        keyButton.setForeground(Color.BLACK);

        keyButton.addActionListener(e -> {
            String currentText = inputField.getText();
            if (key.equals("Clear")) {
                inputField.setText("1"); // Default to 1 for quantities
            } else if (key.equals("←")) {
                // Backspace - remove last character
                if (currentText.length() > 1) {
                    inputField.setText(currentText.substring(0, currentText.length() - 1));
                } else {
                    // If only one character left, replace with "1"
                    inputField.setText("1");
                }
            } else {
                // Number button (0-9)
                // Remove leading zeros (except for just "0")
                if (currentText.equals("0") || currentText.equals("1") && currentText.length() == 1) {
                    inputField.setText(key);
                } else {
                    inputField.setText(currentText + key);
                }
            }
        });

        return keyButton;
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

        JDialog errorDialog = new JDialog(this, title, Dialog.ModalityType.APPLICATION_MODAL);
        errorDialog.setSize(dialogWidth, dialogHeight);
        errorDialog.setMinimumSize(new Dimension(320, 250));
        errorDialog.setLocationRelativeTo(this);
        errorDialog.setLayout(new BorderLayout());

        // Header Panel with red error color
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(220, 53, 69));  // Error red
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel headerLabel = new JLabel(message);
        headerLabel.setFont(new Font("Arial", Font.BOLD, headerFontSize));
        headerLabel.setForeground(Color.WHITE);
        headerPanel.add(headerLabel, BorderLayout.CENTER);

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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PosInterface pos = new PosInterface();
            pos.setVisible(true);
        });
    }
}
