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
    private JButton clearUpcButton;
    private JButton searchUpcButton;
    private JPanel upcFieldPanel;

    // Quick Keys with Pagination and Search
    private JPanel quickKeysPanel;
    private JPanel quickKeysGridPanel;
    private List<PriceBook> allProducts;
    private List<PriceBook> filteredProducts;
    private int currentPage = 0;
    private static final int SUGGESTION_THRESHOLD = 3;
    private int currentItemsPerPage = 12; // Will be calculated dynamically

    // Button dimensions for grid calculation
    private static final int BUTTON_WIDTH = 120;
    private static final int BUTTON_HEIGHT = 80;
    private static final int GRID_SPACING = 10;
    private JButton prevPageButton;
    private JButton nextPageButton;
    private JLabel pageLabel;
    private JTextField searchField;
    private JButton searchIconButton;
    private JButton clearSearchButton;
    private JPanel searchFieldPanel;
    private JPanel suggestionScrollPanel;
    private JPanel suggestionContainer;
    private JComboBox<String> priceFilterCombo;
    private String currentSortOrder = "None";

    // Current Sale Components
    private JPanel salePanel;
    private JTable saleTable;
    private SaleTableModel saleTableModel;
    private JSplitPane mainSplitPane;

    // Current Sale width constraints
    private static final int CURRENT_SALE_MIN_WIDTH = 400;
    private static final int CURRENT_SALE_MAX_WIDTH = 800;
    private static final int CURRENT_SALE_DEFAULT_WIDTH = 620;

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
    private JButton totalButton;
    private JButton paymentVoidButton;

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
        setExtendedState(JFrame.MAXIMIZED_BOTH); // Start maximized (but with menu bar visible)
    }

    private void initializeComponents() {
        // UPC Input Components
        upcTextField = new JTextField(20);
        upcTextField.setFont(new Font("Arial", Font.PLAIN, 16));
        // Add right padding to make room for both icons (X and magnifying glass)
        upcTextField.setBorder(BorderFactory.createCompoundBorder(
            upcTextField.getBorder(),
            BorderFactory.createEmptyBorder(0, 5, 0, 65) // Right padding for both X button and search icon
        ));

        // Create clear button (X icon) for UPC field
        clearUpcButton = new JButton() {
            private boolean isHovered = false;

            {
                setFocusPainted(false);
                setBorderPainted(false);
                setContentAreaFilled(false);
                setOpaque(false);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                setToolTipText("Clear UPC");
                setVisible(false); // Hidden by default

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
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                // Set color based on hover state
                g2d.setColor(isHovered ? Color.RED : Color.GRAY);

                int size = Math.min(getWidth(), getHeight());
                int padding = size / 4;
                int x1 = padding;
                int y1 = padding;
                int x2 = size - padding;
                int y2 = size - padding;

                // Draw X (two diagonal lines)
                g2d.drawLine(x1, y1, x2, y2); // Top-left to bottom-right
                g2d.drawLine(x2, y1, x1, y2); // Top-right to bottom-left

                g2d.dispose();
            }
        };

        // Create search icon button (magnifying glass) for UPC field
        searchUpcButton = new JButton() {
            private boolean isHovered = false;

            {
                setFocusPainted(false);
                setBorderPainted(false);
                setContentAreaFilled(false);
                setOpaque(false);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                setToolTipText("Search UPC");
                setVisible(true); // Always visible

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
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Set color based on hover state
                Color iconColor = isHovered ? new Color(100, 149, 237) : new Color(100, 100, 100);
                g2d.setColor(iconColor);

                int size = Math.min(getWidth(), getHeight());
                int padding = 4;

                // Circle size (lens)
                int circleSize = (int) (size * 0.55);
                int centerX = size / 2 - 2;
                int centerY = size / 2 - 2;

                // Draw magnifying glass circle
                g2d.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2d.drawOval(centerX - circleSize / 2, centerY - circleSize / 2, circleSize, circleSize);

                // Draw magnifying glass handle
                g2d.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int handleStartX = centerX + (int) (circleSize / 2 * 0.6);
                int handleStartY = centerY + (int) (circleSize / 2 * 0.6);
                int handleLength = (int) (circleSize * 0.6);
                int handleEndX = handleStartX + (int) (handleLength * 0.707);
                int handleEndY = handleStartY + (int) (handleLength * 0.707);
                g2d.drawLine(handleStartX, handleStartY, handleEndX, handleEndY);

                g2d.dispose();
            }
        };

        // Create panel to hold UPC field with icon buttons overlay
        upcFieldPanel = new JPanel(null); // Use null layout for absolute positioning
        upcFieldPanel.setOpaque(false);

        // Quick Keys Panel
        quickKeysPanel = new JPanel(new BorderLayout(5, 5));
        quickKeysPanel.setBorder(BorderFactory.createTitledBorder("Quick Keys / All Products"));
        quickKeysGridPanel = new JPanel(new GridLayout(4, 3, 10, 10)); // 4 rows x 3 columns

        // Search field with auto-suggest - will match filter dropdown height
        searchField = new JTextField(30);
        searchField.setFont(new Font("Arial", Font.PLAIN, 16)); // Larger font for readability
        // Add right padding to make room for both icons (X and magnifying glass)
        searchField.setBorder(BorderFactory.createCompoundBorder(
            searchField.getBorder(),
            BorderFactory.createEmptyBorder(0, 5, 0, 65) // Right padding for both X button and search icon
        ));

        // Create clear button (X icon) that appears inside search field
        clearSearchButton = new JButton() {
            private boolean isHovered = false;

            {
                setFocusPainted(false);
                setBorderPainted(false);
                setContentAreaFilled(false);
                setOpaque(false);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                setToolTipText("Clear search");
                setVisible(false); // Hidden by default

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
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                // Set color based on hover state
                g2d.setColor(isHovered ? Color.RED : Color.GRAY);

                int size = Math.min(getWidth(), getHeight());
                int padding = size / 4;
                int x1 = padding;
                int y1 = padding;
                int x2 = size - padding;
                int y2 = size - padding;

                // Draw X (two diagonal lines)
                g2d.drawLine(x1, y1, x2, y2); // Top-left to bottom-right
                g2d.drawLine(x2, y1, x1, y2); // Top-right to bottom-left

                g2d.dispose();
            }
        };

        // Create search icon button (magnifying glass) on left side
        searchIconButton = new JButton() {
            private boolean isHovered = false;

            {
                setFocusPainted(false);
                setBorderPainted(false);
                setContentAreaFilled(false);
                setOpaque(false);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                setToolTipText("Search");
                setVisible(true); // Always visible

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
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Set color based on hover state
                Color iconColor = isHovered ? new Color(100, 149, 237) : new Color(100, 100, 100);
                g2d.setColor(iconColor);

                int size = Math.min(getWidth(), getHeight());
                int padding = 4;

                // Circle size (lens)
                int circleSize = (int) (size * 0.55);
                int centerX = size / 2 - 2;
                int centerY = size / 2 - 2;

                // Draw magnifying glass circle
                g2d.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2d.drawOval(centerX - circleSize / 2, centerY - circleSize / 2, circleSize, circleSize);

                // Draw magnifying glass handle
                g2d.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int handleStartX = centerX + (int) (circleSize / 2 * 0.6);
                int handleStartY = centerY + (int) (circleSize / 2 * 0.6);
                int handleLength = (int) (circleSize * 0.6);
                int handleEndX = handleStartX + (int) (handleLength * 0.707);
                int handleEndY = handleStartY + (int) (handleLength * 0.707);
                g2d.drawLine(handleStartX, handleStartY, handleEndX, handleEndY);

                g2d.dispose();
            }
        };

        // Create panel to hold search field with icon buttons overlay
        searchFieldPanel = new JPanel(null); // Use null layout for absolute positioning
        searchFieldPanel.setOpaque(false);

        // Create suggestion scroll panel (vertical list)
        suggestionContainer = new JPanel();
        suggestionContainer.setLayout(new BoxLayout(suggestionContainer, BoxLayout.Y_AXIS));
        suggestionContainer.setBackground(Color.WHITE);

        JScrollPane suggestionScrollPane = new JScrollPane(suggestionContainer);
        suggestionScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        suggestionScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        suggestionScrollPane.setPreferredSize(new Dimension(300, 200));
        suggestionScrollPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Create header panel with title and close button
        JPanel suggestionHeader = new JPanel(new BorderLayout(5, 0));
        suggestionHeader.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        JLabel suggestionTitle = new JLabel("Suggestions");
        suggestionTitle.setFont(new Font("Arial", Font.BOLD, 14));

        // Create custom close button with painted X icon
        JButton closeSuggestionsBtn = new JButton() {
            private boolean isHovered = false;

            {
                setPreferredSize(new Dimension(30, 30));
                setMinimumSize(new Dimension(30, 30));
                setMaximumSize(new Dimension(30, 30));
                setFocusPainted(false);
                setBorderPainted(false);
                setContentAreaFilled(false);
                setToolTipText("Close suggestions");
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

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
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                // Set color based on hover state
                g2d.setColor(isHovered ? new Color(200, 0, 0) : Color.RED);

                int padding = 8;
                int x1 = padding;
                int y1 = padding;
                int x2 = getWidth() - padding;
                int y2 = getHeight() - padding;

                // Draw X (two diagonal lines)
                g2d.drawLine(x1, y1, x2, y2); // Top-left to bottom-right
                g2d.drawLine(x2, y1, x1, y2); // Top-right to bottom-left

                g2d.dispose();
            }
        };

        closeSuggestionsBtn.addActionListener(e -> {
            suggestionScrollPanel.setVisible(false);
            searchField.setText(""); // Clear search field
            currentPage = 0;
            updateQuickKeysPage(); // Recalculate grid
        });

        suggestionHeader.add(suggestionTitle, BorderLayout.WEST);
        suggestionHeader.add(closeSuggestionsBtn, BorderLayout.EAST);

        suggestionScrollPanel = new JPanel(new BorderLayout());
        suggestionScrollPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        suggestionScrollPanel.add(suggestionHeader, BorderLayout.NORTH);
        suggestionScrollPanel.add(suggestionScrollPane, BorderLayout.CENTER);
        suggestionScrollPanel.setVisible(false); // Hidden by default

        // Filter combo box - let it render at natural height
        String[] filterOptions = {
            "No Filter",
            "─────────────────",  // Visual separator
            "Name: A to Z",
            "Name: Z to A",
            "─────────────────",  // Visual separator
            "Price: Low to High",
            "Price: High to Low"
        };
        priceFilterCombo = new JComboBox<>(filterOptions);
        priceFilterCombo.setFont(new Font("Arial", Font.PLAIN, 16)); // Larger font for readability

        // Customize renderer to draw full-width separator and show checkmark for selected item
        priceFilterCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {

                // Check if this is the separator
                if (value != null && value.toString().startsWith("───")) {
                    // Create custom separator panel with full-width line
                    JPanel separator = new JPanel() {
                        @Override
                        protected void paintComponent(Graphics g) {
                            super.paintComponent(g);
                            Graphics2D g2d = (Graphics2D) g.create();
                            g2d.setColor(Color.GRAY);
                            g2d.setStroke(new BasicStroke(1));
                            int y = getHeight() / 2;
                            g2d.drawLine(0, y, getWidth(), y);
                            g2d.dispose();
                        }
                    };
                    separator.setPreferredSize(new Dimension(0, 10));
                    separator.setBackground(new Color(240, 240, 240));
                    return separator;
                }

                // Regular item
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value,
                    index, isSelected, cellHasFocus);

                // Add checkmark to left of currently selected item (except "No Filter")
                Object selectedItem = priceFilterCombo.getSelectedItem();
                if (value != null && value.equals(selectedItem) && !value.equals("No Filter")) {
                    label.setText("✓  " + value.toString());
                    label.setFont(label.getFont().deriveFont(Font.BOLD));
                }

                return label;
            }
        });

        // Make separators non-selectable in the popup list
        priceFilterCombo.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {
                JComboBox<?> combo = (JComboBox<?>) e.getSource();
                Object popup = combo.getUI().getAccessibleChild(combo, 0);
                if (popup instanceof javax.swing.plaf.basic.ComboPopup) {
                    JList<?> list = ((javax.swing.plaf.basic.ComboPopup) popup).getList();
                    list.setSelectionModel(new javax.swing.DefaultListSelectionModel() {
                        @Override
                        public void setSelectionInterval(int index0, int index1) {
                            // Check if trying to select a separator
                            Object item = combo.getItemAt(index0);
                            if (item != null && item.toString().startsWith("───")) {
                                // Don't allow selection, do nothing
                                return;
                            }
                            super.setSelectionInterval(index0, index1);
                        }
                    });
                }
            }

            @Override
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {}

            @Override
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {}
        });

        // Fallback: Prevent separator from being selected if somehow it gets through
        priceFilterCombo.addActionListener(e -> {
            String selected = (String) priceFilterCombo.getSelectedItem();
            if (selected != null && selected.startsWith("───")) {
                // If separator is selected, revert to previous selection
                priceFilterCombo.setSelectedItem(currentSortOrder != null ? currentSortOrder : "No Filter");
            }
        });

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
        voidTransactionButton = new JButton("Void Basket");
        payExactButton = new JButton("Exact Dollar");
        payNextDollarButton = new JButton("Next Dollar");
        payCardButton = new JButton("Card");
        deleteSelectedButton = new JButton("Void Line/s");
        totalButton = new JButton("Total");

        // Style action buttons with colors, rounded corners, and better text visibility
        // Set Void Basket to red (warning)
        voidTransactionButton.setBackground(new Color(220, 53, 69));
        voidTransactionButton.setOpaque(true);
        voidTransactionButton.setBorderPainted(false);
        voidTransactionButton.setForeground(Color.WHITE);
        voidTransactionButton.setFont(new Font("Arial", Font.BOLD, 16));

        // Set payment buttons to green
        payExactButton.setBackground(new Color(40, 167, 69));
        payExactButton.setOpaque(true);
        payExactButton.setBorderPainted(false);
        payExactButton.setForeground(Color.WHITE);
        payExactButton.setFont(new Font("Arial", Font.BOLD, 16));

        payNextDollarButton.setBackground(new Color(40, 167, 69));
        payNextDollarButton.setOpaque(true);
        payNextDollarButton.setBorderPainted(false);
        payNextDollarButton.setForeground(Color.WHITE);
        payNextDollarButton.setFont(new Font("Arial", Font.BOLD, 16));

        payCardButton.setBackground(new Color(40, 167, 69));
        payCardButton.setOpaque(true);
        payCardButton.setBorderPainted(false);
        payCardButton.setForeground(Color.WHITE);
        payCardButton.setFont(new Font("Arial", Font.BOLD, 16));

        // Delete Selected - orange
        deleteSelectedButton.setBackground(new Color(255, 150, 100));
        deleteSelectedButton.setOpaque(true);
        deleteSelectedButton.setBorderPainted(false);
        deleteSelectedButton.setForeground(Color.WHITE); // White text
        deleteSelectedButton.setFont(new Font("Arial", Font.BOLD, 14));

        // Total - bright green
        totalButton.setBackground(new Color(50, 205, 50));
        totalButton.setOpaque(true);
        totalButton.setBorderPainted(false);
        totalButton.setForeground(Color.WHITE); // White text
        totalButton.setFont(new Font("Arial", Font.BOLD, 16));

        // Apply text outlines to all action buttons for better visibility
        applyTextOutline(voidTransactionButton);
        applyTextOutline(payExactButton);
        applyTextOutline(payNextDollarButton);
        applyTextOutline(payCardButton);
        applyTextOutline(deleteSelectedButton);
        applyTextOutline(totalButton);

        // Initially disable payment buttons until Total is pressed
        payExactButton.setEnabled(false);
        payNextDollarButton.setEnabled(false);
        payCardButton.setEnabled(false);

        // Total button is always enabled - will show dialog if cart is empty
    }

    private void loadAllProducts() {
        try {
            allProducts = priceBookService.getAllItems();
            filteredProducts = new ArrayList<>(allProducts);
            applyDefaultSort(); // Apply priority product sort on initial load
            updateQuickKeysPage();
        } catch (SQLException e) {
            showError("Failed to load products: " + e.getMessage());
        }
    }

    /**
     * Apply default sorting: Featured products first, then by quick key position
     */
    private void applyDefaultSort() {
        filteredProducts.sort((a, b) -> {
            // First, sort by isFeatured (featured products first)
            if (a.isFeatured() != b.isFeatured()) {
                return a.isFeatured() ? -1 : 1; // Featured products come first
            }

            // Then, sort by quickKeyPosition (lower position = higher priority)
            Integer posA = a.quickKeyPosition();
            Integer posB = b.quickKeyPosition();

            // Handle null positions (put them at the end)
            if (posA == null && posB == null) return 0;
            if (posA == null) return 1;
            if (posB == null) return -1;

            return Integer.compare(posA, posB);
        });
    }

    /**
     * Dynamically calculate grid dimensions based on available space
     */
    private int[] calculateGridDimensions() {
        // Get the parent container's dimensions (gridWrapper)
        Container parent = quickKeysGridPanel.getParent();
        if (parent == null || parent.getWidth() <= 0 || parent.getHeight() <= 0) {
            // Return default values if parent not yet laid out
            return new int[]{4, 3}; // rows, cols
        }

        int availableWidth = parent.getWidth() - 20; // Account for borders/padding
        int availableHeight = parent.getHeight() - 60; // Account for pagination panel

        // Reduce height if suggestions panel is visible
        if (suggestionScrollPanel.isVisible()) {
            availableHeight = Math.max(availableHeight, 200); // Ensure minimum height
        }

        // Calculate how many columns and rows can fit
        int maxCols = Math.max(2, (availableWidth + GRID_SPACING) / (BUTTON_WIDTH + GRID_SPACING));
        int maxRows = Math.max(2, (availableHeight + GRID_SPACING) / (BUTTON_HEIGHT + GRID_SPACING));

        // Limit to reasonable bounds
        maxCols = Math.min(maxCols, 6); // Max 6 columns
        maxRows = Math.min(maxRows, 6); // Max 6 rows

        return new int[]{maxRows, maxCols};
    }

    private void updateQuickKeysPage() {
        quickKeysGridPanel.removeAll();

        // Calculate grid dimensions dynamically
        int[] gridDims = calculateGridDimensions();
        int rows = gridDims[0];
        int cols = gridDims[1];
        currentItemsPerPage = rows * cols;

        quickKeysGridPanel.setLayout(new GridLayout(rows, cols, GRID_SPACING, GRID_SPACING));

        int startIdx = currentPage * currentItemsPerPage;
        int endIdx = Math.min(startIdx + currentItemsPerPage, filteredProducts.size());

        for (int i = startIdx; i < endIdx; i++) {
            PriceBook item = filteredProducts.get(i);
            String displayName = item.name().length() > 25
                ? item.name().substring(0, 22) + "..."
                : item.name();

            JButton btn = new JButton("<html><center>" +
                displayName + "<br><font color='green'>$" + String.format("%.2f", item.price()) + "</font>" +
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

        // CENTER/RIGHT: Split for Quick Keys (top) and Actions (bottom)
        JPanel rightPanel = new JPanel(new BorderLayout(10, 10));

        // Upper: Quick Keys with Search and Pagination
        JPanel quickKeysContainer = new JPanel(new BorderLayout(5, 5));

        // Search and filter panel with dynamic sizing
        JPanel searchFilterPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER; // Center components vertically
        gbc.gridy = 0;

        // Match search field height to combo box natural height
        int comboHeight = priceFilterCombo.getPreferredSize().height;
        searchField.setPreferredSize(new Dimension(300, comboHeight));
        searchField.setMinimumSize(new Dimension(200, comboHeight));
        searchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, comboHeight));

        // Position clear button inside search field (right side)
        int buttonSize = comboHeight - 4;
        clearSearchButton.setPreferredSize(new Dimension(buttonSize, buttonSize));
        clearSearchButton.setSize(buttonSize, buttonSize);

        // Setup search field panel with overlay button
        searchFieldPanel.setPreferredSize(new Dimension(300, comboHeight));
        searchFieldPanel.setMinimumSize(new Dimension(200, comboHeight));
        searchFieldPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, comboHeight));

        // Add component listener to position elements when panel is resized
        searchFieldPanel.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int panelWidth = searchFieldPanel.getWidth();
                int panelHeight = searchFieldPanel.getHeight();

                // Search field takes full width
                searchField.setBounds(0, 0, panelWidth, panelHeight);

                int btnSize = panelHeight - 8;
                int btnY = (panelHeight - btnSize) / 2;

                // Both icons on the right side:
                // Magnifying glass icon positioned on the far right
                int searchIconX = panelWidth - btnSize - 6;
                searchIconButton.setBounds(searchIconX, btnY, btnSize, btnSize);

                // Clear X button positioned to the left of magnifying glass
                int clearBtnX = searchIconX - btnSize - 4;
                clearSearchButton.setBounds(clearBtnX, btnY, btnSize, btnSize);
            }
        });

        searchFieldPanel.add(searchField);
        searchFieldPanel.add(searchIconButton);
        searchFieldPanel.add(clearSearchButton);

        // Ensure buttons are on top (higher z-order)
        searchFieldPanel.setComponentZOrder(searchIconButton, 0);
        searchFieldPanel.setComponentZOrder(clearSearchButton, 0);

        // Search label - touch-friendly font
        gbc.gridx = 0;
        gbc.weightx = 0;
        JLabel searchLabel = new JLabel("Search:");
        searchLabel.setFont(new Font("Arial", Font.BOLD, 16));
        searchFilterPanel.add(searchLabel, gbc);

        // Search field panel (equal width distribution)
        gbc.gridx = 1;
        gbc.weightx = 0.5; // Take 50% of available space
        searchFilterPanel.add(searchFieldPanel, gbc);

        // Filter label - touch-friendly font
        gbc.gridx = 2;
        gbc.weightx = 0;
        JLabel filterLabel = new JLabel("Filter:");
        filterLabel.setFont(new Font("Arial", Font.BOLD, 16));
        searchFilterPanel.add(filterLabel, gbc);

        // Filter dropdown (equal width distribution) - render at natural height
        gbc.gridx = 3;
        gbc.weightx = 0.5; // Take 50% of available space
        searchFilterPanel.add(priceFilterCombo, gbc);

        quickKeysContainer.add(searchFilterPanel, BorderLayout.NORTH);

        // Main content area with grid and suggestions
        JPanel mainContentPanel = new JPanel(new BorderLayout(10, 10));

        // Grid panel wrapper
        JPanel gridWrapper = new JPanel(new BorderLayout());
        gridWrapper.add(quickKeysGridPanel, BorderLayout.CENTER);

        // Add component listener for responsive grid sizing
        gridWrapper.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                // Recalculate grid when component is resized
                int oldItemsPerPage = currentItemsPerPage;
                int[] gridDims = calculateGridDimensions();
                int newItemsPerPage = gridDims[0] * gridDims[1];

                if (newItemsPerPage != oldItemsPerPage) {
                    // Reset to page 0 if items per page changed significantly
                    currentPage = 0;
                    updateQuickKeysPage();
                }
            }
        });

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

        // Create vertical split pane for Quick Keys (60%) and Actions (40%)
        JPanel actionsPanel = createActionsPanel();
        JSplitPane verticalSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, quickKeysPanel, actionsPanel);
        verticalSplitPane.setResizeWeight(0.6); // Quick Keys gets 60%, Actions gets 40%
        verticalSplitPane.setDividerSize(8);
        verticalSplitPane.setContinuousLayout(true);
        verticalSplitPane.setOneTouchExpandable(false);

        // Replace default divider with custom visual divider (horizontal orientation)
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

                        // Background color changes on hover
                        Color bgColor = isHovered ? new Color(100, 149, 237) : new Color(180, 180, 180);
                        g2d.setColor(bgColor);
                        g2d.fillRect(0, 0, width, height);

                        // Draw horizontal grip marks (dots)
                        g2d.setColor(isHovered ? Color.WHITE : new Color(120, 120, 120));
                        int centerY = height / 2;
                        int gripSpacing = 4;
                        int gripDotSize = 3;
                        int startX = width / 2 - (gripSpacing * 4); // 4 dots left of center

                        // Draw 9 grip dots horizontally
                        for (int i = 0; i < 9; i++) {
                            int x = startX + (i * gripSpacing);
                            if (x >= 10 && x <= width - 10) {
                                g2d.fillOval(x, centerY - gripDotSize / 2, gripDotSize, gripDotSize);
                            }
                        }

                        // Add subtle double arrows on hover to indicate draggability (up/down)
                        if (isHovered) {
                            g2d.setColor(Color.WHITE);
                            int arrowX = width / 2;
                            // Up arrow
                            int[] xPointsUp = {arrowX, arrowX - 2, arrowX - 4};
                            int[] yPointsUp = {centerY - 3, centerY - 1, centerY - 3};
                            g2d.fillPolygon(xPointsUp, yPointsUp, 3);
                            int[] xPointsUp2 = {arrowX, arrowX + 2, arrowX + 4};
                            int[] yPointsUp2 = {centerY - 3, centerY - 1, centerY - 3};
                            g2d.fillPolygon(xPointsUp2, yPointsUp2, 3);

                            // Down arrow
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
        JPanel currentSalePanel = createCurrentSalePanel();
        mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, currentSalePanel, rightPanel);
        mainSplitPane.setDividerLocation(CURRENT_SALE_DEFAULT_WIDTH);
        mainSplitPane.setDividerSize(8);
        mainSplitPane.setContinuousLayout(true);
        mainSplitPane.setOneTouchExpandable(false);

        // Replace default divider with custom visual divider
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

                        // Background color changes on hover
                        Color bgColor = isHovered ? new Color(100, 149, 237) : new Color(180, 180, 180);
                        g2d.setColor(bgColor);
                        g2d.fillRect(0, 0, width, height);

                        // Draw vertical grip marks (dots)
                        g2d.setColor(isHovered ? Color.WHITE : new Color(120, 120, 120));
                        int centerX = width / 2;
                        int gripSpacing = 4;
                        int gripDotSize = 3;
                        int startY = height / 2 - (gripSpacing * 4); // 4 dots above center

                        // Draw 9 grip dots vertically
                        for (int i = 0; i < 9; i++) {
                            int y = startY + (i * gripSpacing);
                            if (y >= 10 && y <= height - 10) {
                                g2d.fillOval(centerX - gripDotSize / 2, y, gripDotSize, gripDotSize);
                            }
                        }

                        // Add subtle double arrows on hover to indicate draggability
                        if (isHovered) {
                            g2d.setColor(Color.WHITE);
                            int arrowY = height / 2;
                            // Left arrow
                            int[] xPointsLeft = {centerX - 3, centerX - 1, centerX - 3};
                            int[] yPointsLeft = {arrowY, arrowY - 2, arrowY - 4};
                            g2d.fillPolygon(xPointsLeft, yPointsLeft, 3);
                            int[] xPointsLeft2 = {centerX - 3, centerX - 1, centerX - 3};
                            int[] yPointsLeft2 = {arrowY, arrowY + 2, arrowY + 4};
                            g2d.fillPolygon(xPointsLeft2, yPointsLeft2, 3);

                            // Right arrow
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

        // Add property change listener to enforce min/max width constraints
        mainSplitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, evt -> {
            int location = mainSplitPane.getDividerLocation();
            if (location < CURRENT_SALE_MIN_WIDTH) {
                mainSplitPane.setDividerLocation(CURRENT_SALE_MIN_WIDTH);
            } else if (location > CURRENT_SALE_MAX_WIDTH) {
                mainSplitPane.setDividerLocation(CURRENT_SALE_MAX_WIDTH);
            }
        });

        // Add custom header bar
        add(createHeaderPanel(), BorderLayout.NORTH);
        add(mainSplitPane, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(45, 45, 48)); // Dark gray background
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        headerPanel.setPreferredSize(new Dimension(0, 50));

        // Title label
        JLabel titleLabel = new JLabel("POS System");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        // Close button
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
                        PosInterfacePolished.this,
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

                // Background circle on hover
                if (isHovered) {
                    g2d.setColor(new Color(232, 17, 35)); // Red background on hover
                    g2d.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 4, 4);
                }

                // Draw X
                g2d.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2d.setColor(isHovered ? Color.WHITE : new Color(200, 200, 200));

                int x1 = padding;
                int y1 = padding;
                int x2 = size - padding;
                int y2 = size - padding;

                g2d.drawLine(x1, y1, x2, y2); // Top-left to bottom-right
                g2d.drawLine(x2, y1, x1, y2); // Top-right to bottom-left

                g2d.dispose();
            }
        };

        // Add only the close button to the header
        headerPanel.add(closeButton, BorderLayout.EAST);

        // Add window dragging functionality to header
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

    private JPanel createUpcInputPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // UPC label
        JLabel upcLabel = new JLabel("UPC:");
        upcLabel.setFont(new Font("Arial", Font.BOLD, 16));

        // Match UPC field height to combo box natural height (same as Quick Keys search field)
        int fieldHeight = priceFilterCombo.getPreferredSize().height;
        upcTextField.setPreferredSize(new Dimension(300, fieldHeight));
        upcTextField.setMinimumSize(new Dimension(200, fieldHeight));

        // Setup UPC field panel with overlay buttons
        upcFieldPanel.setPreferredSize(new Dimension(300, fieldHeight));
        upcFieldPanel.setMinimumSize(new Dimension(200, fieldHeight));

        // Add component listener to position elements when panel is resized
        upcFieldPanel.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int panelWidth = upcFieldPanel.getWidth();
                int panelHeight = upcFieldPanel.getHeight();

                // UPC field takes full width
                upcTextField.setBounds(0, 0, panelWidth, panelHeight);

                int btnSize = panelHeight - 8;
                int btnY = (panelHeight - btnSize) / 2;

                // Both icons on the right side:
                // Magnifying glass icon positioned on the far right
                int searchIconX = panelWidth - btnSize - 6;
                searchUpcButton.setBounds(searchIconX, btnY, btnSize, btnSize);

                // Clear X button positioned to the left of magnifying glass
                int clearBtnX = searchIconX - btnSize - 4;
                clearUpcButton.setBounds(clearBtnX, btnY, btnSize, btnSize);
            }
        });

        upcFieldPanel.add(upcTextField);
        upcFieldPanel.add(searchUpcButton);
        upcFieldPanel.add(clearUpcButton);

        // Ensure buttons are on top (higher z-order)
        upcFieldPanel.setComponentZOrder(searchUpcButton, 0);
        upcFieldPanel.setComponentZOrder(clearUpcButton, 0);

        panel.add(upcLabel, BorderLayout.WEST);
        panel.add(upcFieldPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createCurrentSalePanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Current Sale"));
        panel.setMinimumSize(new Dimension(CURRENT_SALE_MIN_WIDTH, 0));

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
        // Main actions container with BorderLayout
        JPanel actionsContainer = new JPanel(new BorderLayout(5, 5));
        actionsContainer.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Center panel - Transaction Actions gets more space, Payment gets less
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // LEFT: Transaction Actions (with UPC search) - 50% width
        JPanel transactionSubZone = new JPanel(new BorderLayout(5, 5));
        transactionSubZone.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Transaction Actions"),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        // UPC panel at the top of Transaction Actions
        JPanel upcPanel = createUpcInputPanel();
        transactionSubZone.add(upcPanel, BorderLayout.NORTH);

        // Transaction buttons in dynamic grid layout (horizontal - 1 row, 3 columns)
        JPanel transactionButtonsGrid = new JPanel(new GridLayout(1, 3, 10, 10));
        transactionButtonsGrid.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Buttons will automatically resize to fill available space
        transactionButtonsGrid.add(deleteSelectedButton);
        transactionButtonsGrid.add(voidTransactionButton);
        transactionButtonsGrid.add(totalButton);

        transactionSubZone.add(transactionButtonsGrid, BorderLayout.CENTER);

        // RIGHT: Payment - takes less space (fixed width)
        JPanel paymentSubZone = new JPanel(new BorderLayout(5, 5));
        paymentSubZone.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Payment"),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        paymentSubZone.setPreferredSize(new Dimension(320, 0)); // Fixed width, Transaction Actions gets remaining space

        // Payment buttons in dynamic grid layout (buttons resize with available space)
        JPanel paymentButtonsGrid = new JPanel(new GridLayout(2, 2, 10, 10));
        paymentButtonsGrid.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Add Void Basket button to Payment zone (colored red)
        paymentVoidButton = new JButton("Void Basket");
        paymentVoidButton.setBackground(new Color(220, 53, 69)); // Red - warning color
        paymentVoidButton.setOpaque(true);
        paymentVoidButton.setBorderPainted(false);
        paymentVoidButton.setForeground(Color.WHITE);
        paymentVoidButton.setFont(new Font("Arial", Font.BOLD, 16));
        applyTextOutline(paymentVoidButton); // Apply text outline for better visibility
        paymentVoidButton.addActionListener(e -> handleVoidTransaction());
        paymentVoidButton.setEnabled(false); // Disabled until Total is pressed

        // Buttons will automatically resize to fill available space
        paymentButtonsGrid.add(payExactButton);
        paymentButtonsGrid.add(payNextDollarButton);
        paymentButtonsGrid.add(payCardButton);
        paymentButtonsGrid.add(paymentVoidButton);

        paymentSubZone.add(paymentButtonsGrid, BorderLayout.CENTER);

        // Add both sub-zones to center panel (Transaction Actions gets more space)
        centerPanel.add(transactionSubZone, BorderLayout.CENTER);
        centerPanel.add(paymentSubZone, BorderLayout.EAST);

        actionsContainer.add(centerPanel, BorderLayout.CENTER);

        // Initially disable Delete Selected
        deleteSelectedButton.setEnabled(false);

        return actionsContainer;
    }

    private void attachEventHandlers() {
        // UPC TextField - Enter key adds item directly to cart
        upcTextField.addActionListener(e -> handleUpcSearch());

        // UPC search icon button - trigger search (same as Enter key)
        searchUpcButton.addActionListener(e -> handleUpcSearch());

        // UPC clear button action - clear UPC field
        clearUpcButton.addActionListener(e -> upcTextField.setText(""));

        // Show/hide clear button based on UPC field content
        upcTextField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                updateUpcClearButtonVisibility();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                updateUpcClearButtonVisibility();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                updateUpcClearButtonVisibility();
            }

            private void updateUpcClearButtonVisibility() {
                clearUpcButton.setVisible(!upcTextField.getText().isEmpty());
            }
        });

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

        // Show/hide clear button based on search field content
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                updateClearButtonVisibility();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                updateClearButtonVisibility();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                updateClearButtonVisibility();
            }

            private void updateClearButtonVisibility() {
                clearSearchButton.setVisible(!searchField.getText().isEmpty());
            }
        });

        // Search icon button action - trigger search (same as Enter key)
        searchIconButton.addActionListener(e -> handleSearchSubmit());

        // Clear button action - clear search and revert to default view
        clearSearchButton.addActionListener(e -> {
            searchField.setText("");
            suggestionScrollPanel.setVisible(false);
            filteredProducts = new ArrayList<>(allProducts);
            applyDefaultSort(); // Revert to priority products (featured + position)
            currentPage = 0;
            updateQuickKeysPage();
        });

        // Close suggestions when pressing Escape
        searchField.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE) {
                    if (suggestionScrollPanel.isVisible()) {
                        suggestionScrollPanel.setVisible(false);
                        currentPage = 0;
                        updateQuickKeysPage(); // Will recalculate grid dynamically
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
        totalButton.addActionListener(e -> handleTotal());
        payExactButton.addActionListener(e -> handlePayExactDollar());
        payNextDollarButton.addActionListener(e -> handlePayNextDollar());
        payCardButton.addActionListener(e -> handlePayCard());
    }

    // ============ Event Handlers ============

    private void handleUpcSearch() {
        String upc = upcTextField.getText().trim();
        if (!upc.isEmpty()) {
            try {
                var result = priceBookService.getItemByUpc(upc);
                if (result.isPresent()) {
                    PriceBook item = result.get();
                    addItemToSale(item);
                    upcTextField.setText("");
                } else {
                    showError("Item not found: " + upc);
                }
            } catch (SQLException ex) {
                showError("Database error: " + ex.getMessage());
            }
        }
    }

    private void addItemToSale(PriceBook item) {
        try {
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
                currentPage = 0; // Reset to first page
                updateQuickKeysPage(); // Will recalculate grid dynamically
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
                currentPage = 0; // Reset to first page
                updateQuickKeysPage(); // Will recalculate grid dynamically
            }
            return;
        }

        // Build suggestion cards
        suggestionContainer.removeAll();

        for (PriceBook product : suggestions) {
            JPanel card = createSuggestionCard(product);
            suggestionContainer.add(card);
        }

        // Show suggestion scroll panel - grid will recalculate automatically
        if (!suggestionScrollPanel.isVisible()) {
            suggestionScrollPanel.setVisible(true);
            currentPage = 0; // Reset to first page
            updateQuickKeysPage(); // Will recalculate grid dynamically
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

        // Hide suggestions - grid will recalculate automatically
        if (suggestionScrollPanel.isVisible()) {
            suggestionScrollPanel.setVisible(false);
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
    }

    private void handlePriceFilterChange() {
        currentSortOrder = (String) priceFilterCombo.getSelectedItem();
        applySortOrder();
        currentPage = 0;
        updateQuickKeysPage();
    }

    private void applySortOrder() {
        if (currentSortOrder == null) return;

        // Ignore separator if somehow selected
        if (currentSortOrder.startsWith("───")) return;

        switch (currentSortOrder) {
            case "Name: A to Z":
                filteredProducts.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
                break;
            case "Name: Z to A":
                filteredProducts.sort((a, b) -> b.name().compareToIgnoreCase(a.name()));
                break;
            case "Price: Low to High":
                filteredProducts.sort((a, b) -> Double.compare(a.price(), b.price()));
                break;
            case "Price: High to Low":
                filteredProducts.sort((a, b) -> Double.compare(b.price(), a.price()));
                break;
            case "No Filter":
            default:
                // Revert to default: featured products first, then by quick key position
                applyDefaultSort();
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
                transactionService.voidTransaction();
                transactionService.createTransaction();
                refreshSaleDisplay();

                // Reset button states after voiding
                // Re-enable transaction controls
                upcTextField.setEnabled(true);
                searchUpcButton.setEnabled(true);
                clearUpcButton.setEnabled(true);
                voidTransactionButton.setEnabled(true);
                voidTransactionButton.repaint();
                totalButton.setEnabled(true); // Re-enable Total button after voiding
                totalButton.repaint();

                // Disable payment buttons until Total is pressed
                payExactButton.setEnabled(false);
                payExactButton.repaint();
                payNextDollarButton.setEnabled(false);
                payNextDollarButton.repaint();
                payCardButton.setEnabled(false);
                payCardButton.repaint();
                paymentVoidButton.setEnabled(false);
                paymentVoidButton.repaint();

                JOptionPane.showMessageDialog(this, "Transaction voided", "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException e) {
                showError("Failed to void transaction: " + e.getMessage());
            }
        }
    }

    private void handleTotal() {
        try {
            double total = transactionService.getTransactionTotal();
            if (total == 0) {
                JOptionPane.showMessageDialog(this, "Cart is empty. Add items before pressing Total.",
                    "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // Finalize the basket - lock transaction modifications
            // Disable transaction action controls
            upcTextField.setEnabled(false);
            searchUpcButton.setEnabled(false);
            clearUpcButton.setEnabled(false);
            deleteSelectedButton.setEnabled(false);
            voidTransactionButton.setEnabled(false);
            voidTransactionButton.repaint();
            totalButton.setEnabled(false);
            totalButton.repaint();

            // Enable payment buttons
            payExactButton.setEnabled(true);
            payExactButton.repaint();
            payNextDollarButton.setEnabled(true);
            payNextDollarButton.repaint();
            payCardButton.setEnabled(true);
            payCardButton.repaint();
            paymentVoidButton.setEnabled(true);
            paymentVoidButton.repaint();

            JOptionPane.showMessageDialog(this,
                String.format("Total: $%.2f\n\nBasket finalized. Please select payment method.", total),
                "Ready for Payment",
                JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            showError("Failed to calculate total: " + e.getMessage());
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

            // Note: Total button is always enabled, will show dialog if cart is empty

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
            transactionService.createTransaction();
            refreshSaleDisplay();
            upcTextField.setText("");

            // Reset button states for new transaction
            // Re-enable transaction controls
            upcTextField.setEnabled(true);
            searchUpcButton.setEnabled(true);
            clearUpcButton.setEnabled(true);
            voidTransactionButton.setEnabled(true);
            voidTransactionButton.repaint();
            totalButton.setEnabled(true); // Re-enable Total button for new transaction
            totalButton.repaint();

            // Disable payment buttons until Total is pressed
            payExactButton.setEnabled(false);
            payExactButton.repaint();
            payNextDollarButton.setEnabled(false);
            payNextDollarButton.repaint();
            payCardButton.setEnabled(false);
            payCardButton.repaint();
            paymentVoidButton.setEnabled(false);
            paymentVoidButton.repaint();

            JOptionPane.showMessageDialog(this,
                "Ready for new transaction",
                "New Transaction",
                JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            showError("Failed to create new transaction: " + e.getMessage());
        }
    }

    /**
     * Apply text outline effect to button for better visibility with rounded corners
     * Handles enabled/disabled state with color schemes
     */
    private void applyTextOutline(JButton button) {
        // Remove border so we can draw our own rounded one
        button.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        button.setContentAreaFilled(false);

        button.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                JButton btn = (JButton) c;
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int arcSize = 12; // Rounded corner size

                // Determine button color based on enabled/disabled state
                Color buttonColor = btn.getBackground();
                Color displayColor;
                Color textColor;

                if (btn.isEnabled()) {
                    // ENABLED: Use original colors
                    displayColor = buttonColor;
                    textColor = btn.getForeground();
                } else {
                    // DISABLED: Use darker version of original color (50% darker)
                    displayColor = new Color(
                        (int)(buttonColor.getRed() * 0.5),
                        (int)(buttonColor.getGreen() * 0.5),
                        (int)(buttonColor.getBlue() * 0.5)
                    );
                    textColor = new Color(180, 180, 180); // Lighter gray text for disabled
                }

                // Draw button background with rounded edges
                g2d.setColor(displayColor);
                g2d.fillRoundRect(0, 0, btn.getWidth() - 1, btn.getHeight() - 1, arcSize, arcSize);

                // Draw rounded border
                g2d.setColor(displayColor.darker());
                g2d.setStroke(new BasicStroke(2f));
                g2d.drawRoundRect(0, 0, btn.getWidth() - 1, btn.getHeight() - 1, arcSize, arcSize);

                // Draw button text with outline
                String text = btn.getText();
                FontMetrics fm = g2d.getFontMetrics(btn.getFont());
                int textWidth = fm.stringWidth(text);
                int textHeight = fm.getHeight();
                int x = (btn.getWidth() - textWidth) / 2;
                int y = (btn.getHeight() + fm.getAscent() - fm.getDescent()) / 2;

                g2d.setFont(btn.getFont());

                // Draw text outline (stroke) - only if button is enabled
                if (btn.isEnabled()) {
                    g2d.setColor(new Color(0, 0, 0, 150)); // Semi-transparent black outline
                    g2d.setStroke(new BasicStroke(3f));
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dy = -1; dy <= 1; dy++) {
                            if (dx != 0 || dy != 0) {
                                g2d.drawString(text, x + dx, y + dy);
                            }
                        }
                    }
                }

                // Draw text fill with appropriate color
                g2d.setColor(textColor);
                g2d.drawString(text, x, y);

                g2d.dispose();
            }
        });
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PosInterfacePolished pos = new PosInterfacePolished();
            pos.setVisible(true);
        });
    }
}
