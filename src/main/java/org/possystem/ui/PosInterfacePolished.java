package org.possystem.ui;

import org.possystem.entity.PriceBook;
import org.possystem.entity.TransactionItem;
import org.possystem.service.PriceBookService;
import org.possystem.service.TransactionService;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Polished POS Interface with improved layout and controls
 */
public class PosInterfacePolished extends JFrame {

    // Services
    private final PriceBookService priceBookService;
    private final TransactionService transactionService;

    // UPC Input Components
    private JTextField upcTextField;
    private JButton searchButton;
    private JLabel itemInfoLabel;
    private JButton addToCartButton;

    // Quick Keys with Pagination and Search
    private JPanel quickKeysPanel;
    private JPanel quickKeysGridPanel;
    private List<PriceBook> allProducts;
    private List<PriceBook> filteredProducts;
    private int currentPage = 0;
    private static final int ITEMS_PER_PAGE_FULL = 12;  // 3x4 grid
    private static final int ITEMS_PER_PAGE_COMPACT = 6; // 3x2 grid
    private static final int SUGGESTION_THRESHOLD = 3;
    private int currentItemsPerPage = ITEMS_PER_PAGE_FULL;
    private JButton prevPageButton;
    private JButton nextPageButton;
    private JLabel pageLabel;
    private JTextField searchField;
    private JPanel suggestionScrollPanel;
    private JPanel suggestionContainer;
    private JComboBox<String> priceFilterCombo;
    private String currentSortOrder = "None";

    // Current Sale Components
    private JPanel salePanel;
    private JTable saleTable;
    private SaleTableModel saleTableModel;

    // Totals Display
    private JLabel subtotalLabel;
    private JLabel taxLabel;
    private JLabel totalLabel;

    // Actions Panel Buttons
    private JButton voidTransactionButton;
    private JButton payExactButton;
    private JButton payNextDollarButton;
    private JButton payCardButton;
    private JButton deleteSelectedButton;

    // State
    private PriceBook searchedItem;

    public PosInterfacePolished() {
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
        setSize(1600, 900);
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
        quickKeysPanel = new JPanel(new BorderLayout(5, 5));
        quickKeysPanel.setBorder(BorderFactory.createTitledBorder("Quick Keys / All Products"));
        quickKeysGridPanel = new JPanel(new GridLayout(4, 3, 10, 10)); // 4 rows x 3 columns

        // Search field with auto-suggest
        searchField = new JTextField(30);
        searchField.setFont(new Font("Arial", Font.PLAIN, 14));

        // Create suggestion scroll panel (vertical list)
        suggestionContainer = new JPanel();
        suggestionContainer.setLayout(new BoxLayout(suggestionContainer, BoxLayout.Y_AXIS));
        suggestionContainer.setBackground(Color.WHITE);

        JScrollPane suggestionScrollPane = new JScrollPane(suggestionContainer);
        suggestionScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        suggestionScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        suggestionScrollPane.setPreferredSize(new Dimension(300, 200));
        suggestionScrollPane.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Suggestions"),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        suggestionScrollPanel = new JPanel(new BorderLayout());
        suggestionScrollPanel.add(suggestionScrollPane, BorderLayout.CENTER);
        suggestionScrollPanel.setVisible(false); // Hidden by default

        // Price filter combo box
        String[] filterOptions = {"No Sorting", "Price: Low to High", "Price: High to Low"};
        priceFilterCombo = new JComboBox<>(filterOptions);

        // Pagination buttons
        prevPageButton = new JButton("◀ Prev");
        nextPageButton = new JButton("Next ▶");
        pageLabel = new JLabel("Page 1", SwingConstants.CENTER);

        // Load all products for pagination
        loadAllProducts();

        // Current Sale Table
        salePanel = new JPanel(new BorderLayout(5, 5));
        salePanel.setBorder(BorderFactory.createTitledBorder("Current Sale"));
        salePanel.setPreferredSize(new Dimension(620, 0));

        // Create custom table model
        saleTableModel = new SaleTableModel();
        saleTable = new JTable(saleTableModel);
        saleTable.setRowHeight(45);
        saleTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        saleTable.getTableHeader().setReorderingAllowed(false);

        // Set column widths (proportional)
        saleTable.getColumnModel().getColumn(0).setPreferredWidth(40);  // Checkbox
        saleTable.getColumnModel().getColumn(1).setPreferredWidth(250); // Name
        saleTable.getColumnModel().getColumn(2).setPreferredWidth(150); // Qty (with - + buttons)
        saleTable.getColumnModel().getColumn(2).setMinWidth(150);       // Prevent wrapping of + button
        saleTable.getColumnModel().getColumn(3).setPreferredWidth(80);  // Price
        saleTable.getColumnModel().getColumn(4).setPreferredWidth(90);  // Line Total
        saleTable.getColumnModel().getColumn(5).setPreferredWidth(50);  // Delete
        saleTable.getColumnModel().getColumn(5).setMaxWidth(50);        // Fixed width for delete icon

        // Set custom header renderer and listener for select all checkbox
        saleTable.getColumnModel().getColumn(0).setHeaderRenderer(new SelectAllHeaderRenderer());
        saleTable.getTableHeader().addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int column = saleTable.columnAtPoint(e.getPoint());
                if (column == 0) {
                    toggleSelectAll();
                    saleTable.getTableHeader().repaint();
                }
            }
        });

        // Add mouse listener for single-click delete
        saleTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = saleTable.rowAtPoint(e.getPoint());
                int column = saleTable.columnAtPoint(e.getPoint());

                // Check if delete column (column 5) was clicked
                if (column == 5 && row >= 0) {
                    TransactionItem item = saleTableModel.getItemAt(row);
                    handleSingleClickDelete(item);
                }
            }
        });

        // Set custom renderers and editors
        saleTable.getColumnModel().getColumn(0).setCellRenderer(new CheckBoxRenderer());
        saleTable.getColumnModel().getColumn(0).setCellEditor(new CheckBoxEditor());
        saleTable.getColumnModel().getColumn(2).setCellRenderer(new QuantityControlRenderer());
        saleTable.getColumnModel().getColumn(2).setCellEditor(new QuantityControlEditor());
        saleTable.getColumnModel().getColumn(5).setCellRenderer(new DeleteButtonRenderer());

        // Totals Display
        subtotalLabel = new JLabel("Subtotal: $0.00");
        taxLabel = new JLabel("Tax (7%): $0.00");
        totalLabel = new JLabel("Total: $0.00");
        totalLabel.setFont(new Font("Arial", Font.BOLD, 20));

        // Actions Panel Buttons
        voidTransactionButton = new JButton("Void Transaction");
        payExactButton = new JButton("Pay Exact Dollar");
        payNextDollarButton = new JButton("Pay Next Dollar");
        payCardButton = new JButton("Pay Card");
        deleteSelectedButton = new JButton("Delete Selected");

        // Style action buttons
        voidTransactionButton.setBackground(new Color(255, 100, 100));
        payExactButton.setBackground(new Color(100, 200, 100));
        payNextDollarButton.setBackground(new Color(150, 220, 150));
        payCardButton.setBackground(new Color(100, 150, 255));
        deleteSelectedButton.setBackground(new Color(255, 150, 100));
    }

    private void loadAllProducts() {
        try {
            allProducts = priceBookService.getAllItems();
            filteredProducts = new ArrayList<>(allProducts);
            System.out.println("Loaded " + allProducts.size() + " products for pagination");
            updateQuickKeysPage();
        } catch (SQLException e) {
            showError("Failed to load products: " + e.getMessage());
        }
    }

    private void updateQuickKeysPage() {
        quickKeysGridPanel.removeAll();

        // Adjust grid layout based on current mode
        int rows = suggestionScrollPanel.isVisible() ? 2 : 4; // 2 rows when suggestions shown, 4 rows otherwise
        int cols = 3;
        quickKeysGridPanel.setLayout(new GridLayout(rows, cols, 10, 10));

        int startIdx = currentPage * currentItemsPerPage;
        int endIdx = Math.min(startIdx + currentItemsPerPage, filteredProducts.size());

        for (int i = startIdx; i < endIdx; i++) {
            PriceBook item = filteredProducts.get(i);
            String displayName = item.name().length() > 25
                ? item.name().substring(0, 22) + "..."
                : item.name();

            JButton btn = new JButton("<html><center>" +
                displayName + "<br>$" + String.format("%.2f", item.price()) +
                "</center></html>");
            btn.setPreferredSize(new Dimension(120, 80));
            btn.addActionListener(e -> addItemToSale(item));
            quickKeysGridPanel.add(btn);
        }

        // Fill empty slots
        int displayed = endIdx - startIdx;
        for (int i = displayed; i < currentItemsPerPage; i++) {
            JButton emptyBtn = new JButton("---");
            emptyBtn.setEnabled(false);
            quickKeysGridPanel.add(emptyBtn);
        }

        // Update pagination controls
        int totalPages = (int) Math.ceil((double) filteredProducts.size() / currentItemsPerPage);
        pageLabel.setText(String.format("Page %d of %d (%d items)",
            currentPage + 1, Math.max(1, totalPages), filteredProducts.size()));
        prevPageButton.setEnabled(currentPage > 0);
        nextPageButton.setEnabled(currentPage < totalPages - 1);

        quickKeysGridPanel.revalidate();
        quickKeysGridPanel.repaint();
    }

    private void layoutComponents() {
        setLayout(new BorderLayout(10, 10));

        // TOP: UPC Input Panel
        JPanel upcPanel = createUpcInputPanel();
        add(upcPanel, BorderLayout.NORTH);

        // LEFT: Current Sale (narrower)
        add(createCurrentSalePanel(), BorderLayout.WEST);

        // CENTER/RIGHT: Split for Quick Keys (top) and Actions (bottom)
        JPanel rightPanel = new JPanel(new BorderLayout(10, 10));

        // Upper: Quick Keys with Search and Pagination
        JPanel quickKeysContainer = new JPanel(new BorderLayout(5, 5));

        // Search and filter panel
        JPanel searchFilterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        searchFilterPanel.add(new JLabel("Search:"));
        searchFilterPanel.add(searchField);
        searchFilterPanel.add(new JLabel("Sort:"));
        searchFilterPanel.add(priceFilterCombo);
        quickKeysContainer.add(searchFilterPanel, BorderLayout.NORTH);

        // Main content area with grid and suggestions
        JPanel mainContentPanel = new JPanel(new BorderLayout(10, 10));

        // Grid panel wrapper
        JPanel gridWrapper = new JPanel(new BorderLayout());
        gridWrapper.add(quickKeysGridPanel, BorderLayout.CENTER);

        // Pagination panel
        JPanel paginationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        paginationPanel.add(prevPageButton);
        paginationPanel.add(pageLabel);
        paginationPanel.add(nextPageButton);
        gridWrapper.add(paginationPanel, BorderLayout.SOUTH);

        mainContentPanel.add(gridWrapper, BorderLayout.CENTER);
        mainContentPanel.add(suggestionScrollPanel, BorderLayout.EAST);

        quickKeysContainer.add(mainContentPanel, BorderLayout.CENTER);
        quickKeysPanel.add(quickKeysContainer, BorderLayout.CENTER);
        rightPanel.add(quickKeysPanel, BorderLayout.CENTER);

        // Lower: Actions Panel
        rightPanel.add(createActionsPanel(), BorderLayout.SOUTH);

        add(rightPanel, BorderLayout.CENTER);
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
        panel.setPreferredSize(new Dimension(620, 0));

        // Table with scroll (vertical only, no horizontal)
        JScrollPane scrollPane = new JScrollPane(saleTable);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
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
        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Actions"));
        panel.setPreferredSize(new Dimension(0, 180));

        panel.add(deleteSelectedButton);
        panel.add(voidTransactionButton);
        panel.add(payExactButton);
        panel.add(payNextDollarButton);
        panel.add(payCardButton);
        panel.add(new JLabel("")); // Empty spacer

        // Initially disable Delete Selected
        deleteSelectedButton.setEnabled(false);

        return panel;
    }

    private void attachEventHandlers() {
        // UPC TextField - Enter key triggers auto-add
        upcTextField.addActionListener(e -> handleUpcEnter());

        // Search button - Manual search
        searchButton.addActionListener(e -> handleManualSearch());

        // Add to Cart button
        addToCartButton.addActionListener(e -> handleManualAddToCart());

        // Pagination
        prevPageButton.addActionListener(e -> {
            if (currentPage > 0) {
                currentPage--;
                updateQuickKeysPage();
            }
        });

        nextPageButton.addActionListener(e -> {
            int totalPages = (int) Math.ceil((double) filteredProducts.size() / currentItemsPerPage);
            if (currentPage < totalPages - 1) {
                currentPage++;
                updateQuickKeysPage();
            }
        });

        // Search field with auto-suggest
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                SwingUtilities.invokeLater(() -> handleSearchInput());
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                SwingUtilities.invokeLater(() -> handleSearchInput());
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                SwingUtilities.invokeLater(() -> handleSearchInput());
            }
        });

        // Close suggestions when pressing Escape
        searchField.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE) {
                    if (suggestionScrollPanel.isVisible()) {
                        suggestionScrollPanel.setVisible(false);
                        currentItemsPerPage = ITEMS_PER_PAGE_FULL;
                        currentPage = 0;
                        updateQuickKeysPage();
                    }
                }
            }
        });

        // Search field Enter key
        searchField.addActionListener(e -> handleSearchSubmit());

        // Price filter
        priceFilterCombo.addActionListener(e -> handlePriceFilterChange());

        // Action buttons
        deleteSelectedButton.addActionListener(e -> handleDeleteSelected());
        voidTransactionButton.addActionListener(e -> handleVoidTransaction());
        payExactButton.addActionListener(e -> handlePayExactDollar());
        payNextDollarButton.addActionListener(e -> handlePayNextDollar());
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

    private void handleSearchInput() {
        String searchText = searchField.getText().trim().toLowerCase();

        if (searchText.isEmpty()) {
            if (suggestionScrollPanel.isVisible()) {
                suggestionScrollPanel.setVisible(false);
                currentItemsPerPage = ITEMS_PER_PAGE_FULL;
                currentPage = 0; // Reset to first page
                updateQuickKeysPage();
            }
            return;
        }

        // Filter products for suggestions (limit to 15)
        List<PriceBook> suggestions = allProducts.stream()
            .filter(p -> p.name().toLowerCase().contains(searchText) ||
                        p.upc().toLowerCase().contains(searchText))
            .limit(15)
            .toList();

        if (suggestions.isEmpty() || suggestions.size() < SUGGESTION_THRESHOLD) {
            if (suggestionScrollPanel.isVisible()) {
                suggestionScrollPanel.setVisible(false);
                currentItemsPerPage = ITEMS_PER_PAGE_FULL;
                currentPage = 0; // Reset to first page
                updateQuickKeysPage();
            }
            return;
        }

        // Build suggestion cards
        suggestionContainer.removeAll();

        for (PriceBook product : suggestions) {
            JPanel card = createSuggestionCard(product);
            suggestionContainer.add(card);
        }

        // Show suggestion scroll panel and switch to compact grid
        if (!suggestionScrollPanel.isVisible()) {
            suggestionScrollPanel.setVisible(true);
            currentItemsPerPage = ITEMS_PER_PAGE_COMPACT;
            currentPage = 0; // Reset to first page
            updateQuickKeysPage();
        }

        suggestionContainer.revalidate();
        suggestionContainer.repaint();
    }

    private JPanel createSuggestionCard(PriceBook product) {
        JPanel card = new JPanel();
        card.setLayout(new BorderLayout(5, 5));
        card.setPreferredSize(new Dimension(280, 60));
        card.setMaximumSize(new Dimension(280, 60));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        card.setBackground(Color.WHITE);
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Product info panel
        JPanel infoPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        infoPanel.setOpaque(false);

        // Product name (truncated)
        String displayName = product.name().length() > 30
            ? product.name().substring(0, 27) + "..."
            : product.name();
        JLabel nameLabel = new JLabel(displayName);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 12));

        // UPC and Price
        JLabel detailsLabel = new JLabel(String.format("UPC: %s | $%.2f",
            product.upc(), product.price()));
        detailsLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        detailsLabel.setForeground(new Color(100, 100, 100));

        infoPanel.add(nameLabel);
        infoPanel.add(detailsLabel);

        card.add(infoPanel, BorderLayout.CENTER);

        // Price label (prominent)
        JLabel priceLabel = new JLabel(String.format("$%.2f", product.price()));
        priceLabel.setFont(new Font("Arial", Font.BOLD, 14));
        priceLabel.setForeground(new Color(0, 120, 0));
        card.add(priceLabel, BorderLayout.EAST);

        // Add hover effect
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                card.setBackground(new Color(240, 248, 255));
                card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(100, 149, 237), 2),
                    BorderFactory.createEmptyBorder(7, 9, 7, 9)
                ));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                card.setBackground(Color.WHITE);
                card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                    BorderFactory.createEmptyBorder(8, 10, 8, 10)
                ));
            }

            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                showProductActionMenu(product, card);
            }
        });

        return card;
    }

    private void handleSearchSubmit() {
        String searchText = searchField.getText().trim().toLowerCase();

        // Hide suggestions and return to full grid
        if (suggestionScrollPanel.isVisible()) {
            suggestionScrollPanel.setVisible(false);
            currentItemsPerPage = ITEMS_PER_PAGE_FULL;
        }

        if (searchText.isEmpty()) {
            filteredProducts = new ArrayList<>(allProducts);
        } else {
            filteredProducts = allProducts.stream()
                .filter(p -> p.name().toLowerCase().contains(searchText) ||
                            p.upc().toLowerCase().contains(searchText))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        }

        // Apply current sort order
        applySortOrder();

        // Reset to first page
        currentPage = 0;
        updateQuickKeysPage();

        System.out.println("Search results: " + filteredProducts.size() + " products found");
    }

    private void handlePriceFilterChange() {
        currentSortOrder = (String) priceFilterCombo.getSelectedItem();
        applySortOrder();
        currentPage = 0;
        updateQuickKeysPage();
    }

    private void applySortOrder() {
        if (currentSortOrder == null) return;

        switch (currentSortOrder) {
            case "Price: Low to High":
                filteredProducts.sort((a, b) -> Double.compare(a.price(), b.price()));
                break;
            case "Price: High to Low":
                filteredProducts.sort((a, b) -> Double.compare(b.price(), a.price()));
                break;
            default:
                // No sorting
                break;
        }
    }

    private void showProductActionMenu(PriceBook product, Component sourceComponent) {
        JPopupMenu actionMenu = new JPopupMenu();

        JMenuItem addToCartItem = new JMenuItem("Add to Cart");
        addToCartItem.addActionListener(e -> {
            addItemToSale(product);
            suggestionScrollPanel.setVisible(false);
        });

        JMenuItem viewDetailsItem = new JMenuItem("View Details");
        viewDetailsItem.addActionListener(e -> showProductDetails(product));

        JMenuItem editProductItem = new JMenuItem("Edit Product");
        editProductItem.addActionListener(e -> showProductEditDialog(product));

        actionMenu.add(addToCartItem);
        actionMenu.add(viewDetailsItem);
        actionMenu.add(editProductItem);

        // Show menu at the clicked card location
        actionMenu.show(sourceComponent, sourceComponent.getWidth() / 2, sourceComponent.getHeight() / 2);
    }

    private void showProductDetails(PriceBook product) {
        String details = String.format(
            "Product Details\n\n" +
            "UPC: %s\n" +
            "Name: %s\n" +
            "Price: $%.2f\n" +
            "Featured: %s\n" +
            "Quick Key Position: %s",
            product.upc(),
            product.name(),
            product.price(),
            product.isFeatured() ? "Yes" : "No",
            product.quickKeyPosition() != null ? product.quickKeyPosition().toString() : "Not assigned"
        );

        JOptionPane.showMessageDialog(this, details, "Product Details",
            JOptionPane.INFORMATION_MESSAGE);
    }

    private void showProductEditDialog(PriceBook product) {
        JDialog editDialog = new JDialog(this, "Edit Product", true);
        editDialog.setSize(400, 300);
        editDialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridLayout(5, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // UPC (read-only)
        panel.add(new JLabel("UPC:"));
        JTextField upcField = new JTextField(product.upc());
        upcField.setEditable(false);
        panel.add(upcField);

        // Name
        panel.add(new JLabel("Name:"));
        JTextField nameField = new JTextField(product.name());
        panel.add(nameField);

        // Price
        panel.add(new JLabel("Price:"));
        JTextField priceField = new JTextField(String.format("%.2f", product.price()));
        panel.add(priceField);

        // Featured
        panel.add(new JLabel("Featured:"));
        JCheckBox featuredCheckbox = new JCheckBox();
        featuredCheckbox.setSelected(product.isFeatured());
        panel.add(featuredCheckbox);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");

        saveButton.addActionListener(e -> {
            try {
                String newName = nameField.getText().trim();
                double newPrice = Double.parseDouble(priceField.getText().trim());
                boolean newFeatured = featuredCheckbox.isSelected();

                if (newName.isEmpty()) {
                    showError("Product name cannot be empty");
                    return;
                }

                if (newPrice < 0) {
                    showError("Price cannot be negative");
                    return;
                }

                // Update product
                priceBookService.updateProduct(product.upc(), newName, newPrice, newFeatured);

                JOptionPane.showMessageDialog(editDialog, "Product updated successfully!",
                    "Success", JOptionPane.INFORMATION_MESSAGE);

                // Reload products
                loadAllProducts();
                handleSearchSubmit(); // Refresh current view

                editDialog.dispose();
            } catch (NumberFormatException ex) {
                showError("Invalid price format");
            } catch (SQLException ex) {
                showError("Failed to update product: " + ex.getMessage());
            }
        });

        cancelButton.addActionListener(e -> editDialog.dispose());

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        editDialog.setLayout(new BorderLayout());
        editDialog.add(panel, BorderLayout.CENTER);
        editDialog.add(buttonPanel, BorderLayout.SOUTH);

        editDialog.setVisible(true);
    }

    private void handleDeleteSelected() {
        List<Integer> selectedIds = saleTableModel.getSelectedItemIds();

        if (selectedIds.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No items selected", "Info",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete " + selectedIds.size() + " selected item(s)?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                System.out.println("Deleting selected items: " + selectedIds);
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
                JOptionPane.showMessageDialog(this, "Transaction voided", "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException e) {
                showError("Failed to void transaction: " + e.getMessage());
            }
        }
    }

    private void handlePayExactDollar() {
        try {
            double total = transactionService.getTransactionTotal();
            if (total == 0) {
                JOptionPane.showMessageDialog(this, "Cart is empty", "Info",
                    JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            System.out.println("Processing exact dollar payment: $" + total);

            // Get transaction details
            List<TransactionItem> items = transactionService.getCurrentSaleItems();
            double subtotal = transactionService.getTransactionSubtotal();
            double tax = total - subtotal;

            transactionService.processCash(total);

            // Show receipt and auto-clear
            showReceiptDialogQuick(items, subtotal, tax, total, total, 0, "CASH - EXACT");
        } catch (SQLException e) {
            showError("Payment failed: " + e.getMessage());
        }
    }

    private void handlePayNextDollar() {
        try {
            double total = transactionService.getTransactionTotal();
            if (total == 0) {
                JOptionPane.showMessageDialog(this, "Cart is empty", "Info",
                    JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            double nextDollar = Math.ceil(total);

            String input = JOptionPane.showInputDialog(this,
                String.format("Total: $%.2f\nNext Dollar: $%.2f\nEnter amount tendered:",
                    total, nextDollar),
                "Next Dollar Payment",
                JOptionPane.QUESTION_MESSAGE);

            if (input != null && !input.trim().isEmpty()) {
                double tendered = Double.parseDouble(input);

                if (tendered < total) {
                    showError("Insufficient payment. Required: $" + String.format("%.2f", total));
                    return;
                }

                System.out.println("Processing next dollar payment: $" + tendered);

                List<TransactionItem> items = transactionService.getCurrentSaleItems();
                double subtotal = transactionService.getTransactionSubtotal();
                double tax = total - subtotal;
                double change = tendered - total;

                transactionService.processCash(tendered);

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
                JOptionPane.showMessageDialog(this, "Cart is empty", "Info",
                    JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this,
                String.format("Process card payment of $%.2f?", total),
                "Card Payment",
                JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                System.out.println("Processing card payment");

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

    private void refreshSaleDisplay() {
        try {
            List<TransactionItem> items = transactionService.getCurrentSaleItems();

            // Update table model
            saleTableModel.setItems(items);

            // Update Delete Selected button state
            updateDeleteSelectedButton();

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

    private void updateDeleteSelectedButton() {
        boolean hasSelection = saleTableModel.hasSelection();
        deleteSelectedButton.setEnabled(hasSelection);
    }

    private void toggleSelectAll() {
        boolean newState = !saleTableModel.isAllSelected();
        saleTableModel.selectAll(newState);
        saleTable.repaint();
        saleTable.getTableHeader().repaint(); // Update header checkbox
        updateDeleteSelectedButton();
    }

    private void handleSingleClickDelete(TransactionItem item) {
        int confirm = JOptionPane.showConfirmDialog(
            this,
            String.format("Delete \"%s\" from the cart?", item.name()),
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                System.out.println("Deleting item: " + item.id());
                List<Integer> ids = new ArrayList<>();
                ids.add(item.id());
                transactionService.deleteSelectedItems(ids);
                refreshSaleDisplay();
            } catch (SQLException e) {
                showError("Failed to delete item: " + e.getMessage());
            }
        }
    }

    // ==== Custom Header Renderer ====
    private class SelectAllHeaderRenderer extends JCheckBox implements TableCellRenderer {
        public SelectAllHeaderRenderer() {
            setHorizontalAlignment(JLabel.CENTER);
            setOpaque(true);
            setBackground(UIManager.getColor("TableHeader.background"));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            setSelected(saleTableModel != null && saleTableModel.isAllSelected());
            return this;
        }
    }

    // ==== Custom Table Model ====
    private class SaleTableModel extends javax.swing.table.AbstractTableModel {
        private final String[] columnNames = {"", "Item Name", "Qty", "Price", "Line Total", ""};
        private List<TransactionItem> items = new ArrayList<>();
        private List<Boolean> selections = new ArrayList<>();

        public void setItems(List<TransactionItem> newItems) {
            this.items = new ArrayList<>();
            this.selections = new ArrayList<>();
            for (TransactionItem item : newItems) {
                if (item.status().equals("ACTIVE")) {
                    this.items.add(item);
                    this.selections.add(false);
                }
            }
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return items.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Object getValueAt(int row, int column) {
            if (row >= items.size()) return null;
            TransactionItem item = items.get(row);
            return switch (column) {
                case 0 -> selections.get(row);
                case 1 -> item.name();
                case 2 -> item; // Pass entire item for quantity controls
                case 3 -> String.format("$%.2f", item.unitPrice());
                case 4 -> String.format("$%.2f", item.subtotal());
                case 5 -> "🗑";
                default -> null;
            };
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column == 0 || column == 2; // Only checkbox and quantity are editable, not delete
        }

        @Override
        public void setValueAt(Object value, int row, int column) {
            if (column == 0) {
                selections.set(row, (Boolean) value);
                fireTableCellUpdated(row, column);
                saleTable.getTableHeader().repaint(); // Update header checkbox when individual items change
                updateDeleteSelectedButton();
            }
        }

        @Override
        public Class<?> getColumnClass(int column) {
            if (column == 0) return Boolean.class;
            if (column == 2) return TransactionItem.class;
            return String.class;
        }

        public TransactionItem getItemAt(int row) {
            return items.get(row);
        }

        public List<Integer> getSelectedItemIds() {
            List<Integer> ids = new ArrayList<>();
            for (int i = 0; i < items.size(); i++) {
                if (selections.get(i)) {
                    ids.add(items.get(i).id());
                }
            }
            return ids;
        }

        public boolean hasSelection() {
            return selections.stream().anyMatch(selected -> selected);
        }

        public boolean isAllSelected() {
            if (selections.isEmpty()) return false;
            return selections.stream().allMatch(selected -> selected);
        }

        public void selectAll(boolean selected) {
            for (int i = 0; i < selections.size(); i++) {
                selections.set(i, selected);
            }
            fireTableDataChanged();
        }
    }

    // ==== Custom Cell Renderers ====
    private class CheckBoxRenderer extends JCheckBox implements TableCellRenderer {
        public CheckBoxRenderer() {
            setHorizontalAlignment(JLabel.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            setSelected(value != null && (Boolean) value);
            return this;
        }
    }

    private class QuantityControlRenderer extends JPanel implements TableCellRenderer {
        private final JButton minusBtn;
        private final JLabel qtyLabel;
        private final JButton plusBtn;

        public QuantityControlRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 5, 2));
            minusBtn = new JButton("-");
            minusBtn.setPreferredSize(new Dimension(40, 30));
            qtyLabel = new JLabel("1");
            qtyLabel.setPreferredSize(new Dimension(40, 30));
            qtyLabel.setHorizontalAlignment(JLabel.CENTER);
            plusBtn = new JButton("+");
            plusBtn.setPreferredSize(new Dimension(40, 30));

            add(minusBtn);
            add(qtyLabel);
            add(plusBtn);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            if (value instanceof TransactionItem item) {
                qtyLabel.setText(String.valueOf(item.quantity()));
            }
            return this;
        }
    }

    private class DeleteButtonRenderer extends JPanel implements TableCellRenderer {
        private final Color trashRed = new Color(220, 53, 69);

        public DeleteButtonRenderer() {
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int iconSize = 20;
            int x = (width - iconSize) / 2;
            int y = (height - iconSize) / 2;

            // Draw trash can icon
            g2d.setColor(trashRed);
            g2d.setStroke(new BasicStroke(2));

            // Trash can body (rectangle)
            g2d.drawRect(x + 3, y + 6, 14, 12);

            // Trash can lid (horizontal line)
            g2d.drawLine(x + 2, y + 5, x + 18, y + 5);

            // Trash can handle (small rectangle on top)
            g2d.drawRect(x + 7, y + 2, 6, 3);

            // Vertical lines inside (trash detail)
            g2d.drawLine(x + 7, y + 9, x + 7, y + 15);
            g2d.drawLine(x + 10, y + 9, x + 10, y + 15);
            g2d.drawLine(x + 13, y + 9, x + 13, y + 15);

            g2d.dispose();
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            return this;
        }
    }

    // ==== Custom Cell Editors ====
    private class CheckBoxEditor extends DefaultCellEditor {
        public CheckBoxEditor() {
            super(new JCheckBox());
            ((JCheckBox) getComponent()).setHorizontalAlignment(JLabel.CENTER);
        }
    }

    private class QuantityControlEditor extends AbstractCellEditor implements TableCellEditor {
        private final JPanel panel;
        private final JButton minusBtn;
        private final JTextField qtyField;
        private final JButton plusBtn;
        private TransactionItem currentItem;

        public QuantityControlEditor() {
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 2));

            minusBtn = new JButton("-");
            minusBtn.setPreferredSize(new Dimension(40, 30));
            minusBtn.addActionListener(e -> {
                adjustQuantity(-1);
            });

            qtyField = new JTextField(3);
            qtyField.setHorizontalAlignment(JTextField.CENTER);
            qtyField.setPreferredSize(new Dimension(40, 30));
            qtyField.addActionListener(e -> {
                updateQuantityFromField();
            });

            plusBtn = new JButton("+");
            plusBtn.setPreferredSize(new Dimension(40, 30));
            plusBtn.addActionListener(e -> {
                adjustQuantity(1);
            });

            panel.add(minusBtn);
            panel.add(qtyField);
            panel.add(plusBtn);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            if (value instanceof TransactionItem item) {
                currentItem = item;
                qtyField.setText(String.valueOf(item.quantity()));
            }
            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            return currentItem;
        }

        private void adjustQuantity(int delta) {
            if (currentItem == null) return;
            int currentQty = currentItem.quantity();
            int newQty = Math.max(1, currentQty + delta);
            updateQuantity(newQty);
        }

        private void updateQuantityFromField() {
            if (currentItem == null) return;
            try {
                int newQty = Integer.parseInt(qtyField.getText());
                if (newQty >= 1) {
                    updateQuantity(newQty);
                } else {
                    qtyField.setText(String.valueOf(currentItem.quantity()));
                }
            } catch (NumberFormatException e) {
                qtyField.setText(String.valueOf(currentItem.quantity()));
            }
        }

        private void updateQuantity(int newQty) {
            try {
                System.out.println("Updating item " + currentItem.id() + " quantity to " + newQty);
                transactionService.updateQuantity(currentItem.id(), newQty, currentItem.unitPrice());
                fireEditingStopped();
                refreshSaleDisplay();
            } catch (SQLException e) {
                showError("Failed to update quantity: " + e.getMessage());
            }
        }
    }

    private void showReceiptDialogQuick(List<TransactionItem> items, double subtotal,
                                        double tax, double total, double tendered,
                                        double change, String paymentType) {
        JDialog receiptDialog = new JDialog(this, "Receipt", true);
        receiptDialog.setSize(400, 600);
        receiptDialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Receipt content
        JTextArea receiptArea = new JTextArea(buildReceiptText(items, subtotal, tax,
            total, tendered, change, paymentType));
        receiptArea.setEditable(false);
        receiptArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(receiptArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton newTransactionBtn = new JButton("New Transaction");
        JButton closeBtn = new JButton("Close");

        newTransactionBtn.addActionListener(e -> {
            receiptDialog.dispose();
            startNewTransaction();
        });

        closeBtn.addActionListener(e -> receiptDialog.dispose());

        buttonPanel.add(newTransactionBtn);
        buttonPanel.add(closeBtn);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        receiptDialog.add(panel);
        receiptDialog.setVisible(true);
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
        JTextArea receiptArea = new JTextArea(buildReceiptText(items, subtotal, tax,
            total, tendered, change, paymentType));
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

    private String buildReceiptText(List<TransactionItem> items, double subtotal,
                                    double tax, double total, double tendered,
                                    double change, String paymentType) {
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
        return receipt.toString();
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
            PosInterfacePolished pos = new PosInterfacePolished();
            pos.setVisible(true);
        });
    }
}
