package org.possystem.ui;

import org.possystem.entity.PriceBook;
import org.possystem.service.PriceBookService;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Quick Keys Panel - Product grid with search, filters, and pagination
 */
public class QuickKeysPanel extends JPanel {

    private final PriceBookService priceBookService;
    private final Consumer<PriceBook> onItemSelected;

    // Quick Keys with Pagination and Search
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

    public QuickKeysPanel(PriceBookService priceBookService, Consumer<PriceBook> onItemSelected) {
        this.priceBookService = priceBookService;
        this.onItemSelected = onItemSelected;

        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createTitledBorder("Quick Keys / All Products"));

        initializeComponents();
        layoutComponents();
        attachEventHandlers();
        loadAllProducts();
    }

    private void initializeComponents() {
        quickKeysGridPanel = new JPanel(new GridLayout(4, 3, 10, 10));

        // Search field with auto-suggest (read-only, opens keyboard dialog)
        searchField = new JTextField(30);
        searchField.setFont(new Font("Arial", Font.PLAIN, 16));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            searchField.getBorder(),
            BorderFactory.createEmptyBorder(0, 5, 0, 65)
        ));
        searchField.setEditable(false); // Read-only - click opens on-screen keyboard
        searchField.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        searchField.setFocusable(false); // Prevent focus, make click always trigger dialog immediately
        searchField.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                // Use mousePressed instead of mouseClicked for immediate response
                openKeyboardDialog();
            }
        });

        // Create clear button (X icon)
        clearSearchButton = createClearButton();

        // Create search icon button (magnifying glass)
        searchIconButton = createSearchIconButton();

        // Create panel to hold search field with icon buttons overlay
        searchFieldPanel = new JPanel(null);
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

        // Create custom close button
        JButton closeSuggestionsBtn = createCloseSuggestionsButton();

        suggestionHeader.add(suggestionTitle, BorderLayout.WEST);
        suggestionHeader.add(closeSuggestionsBtn, BorderLayout.EAST);

        suggestionScrollPanel = new JPanel(new BorderLayout());
        suggestionScrollPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        suggestionScrollPanel.add(suggestionHeader, BorderLayout.NORTH);
        suggestionScrollPanel.add(suggestionScrollPane, BorderLayout.CENTER);
        suggestionScrollPanel.setVisible(false);

        // Filter combo box
        String[] filterOptions = {
            "No Filter",
            "─────────────────",
            "Name: A to Z",
            "Name: Z to A",
            "─────────────────",
            "Price: Low to High",
            "Price: High to Low"
        };
        priceFilterCombo = new JComboBox<>(filterOptions);
        priceFilterCombo.setFont(new Font("Arial", Font.PLAIN, 16));
        setupFilterComboRenderer();

        // Pagination buttons
        prevPageButton = new JButton("◀ Prev");
        nextPageButton = new JButton("Next ▶");
        pageLabel = new JLabel("Page 1", SwingConstants.CENTER);
    }

    private JButton createClearButton() {
        return new JButton() {
            private boolean isHovered = false;

            {
                setFocusPainted(false);
                setBorderPainted(false);
                setContentAreaFilled(false);
                setOpaque(false);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                setToolTipText("Clear search");
                setVisible(false);

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
                g2d.setColor(isHovered ? Color.RED : Color.GRAY);

                int size = Math.min(getWidth(), getHeight());
                int padding = size / 4;
                int x1 = padding;
                int y1 = padding;
                int x2 = size - padding;
                int y2 = size - padding;

                g2d.drawLine(x1, y1, x2, y2);
                g2d.drawLine(x2, y1, x1, y2);
                g2d.dispose();
            }
        };
    }

    private JButton createSearchIconButton() {
        return new JButton() {
            private boolean isHovered = false;

            {
                setFocusPainted(false);
                setBorderPainted(false);
                setContentAreaFilled(false);
                setOpaque(false);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                setToolTipText("Search");
                setVisible(true);

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

                Color iconColor = isHovered ? new Color(100, 149, 237) : new Color(100, 100, 100);
                g2d.setColor(iconColor);

                int size = Math.min(getWidth(), getHeight());
                int circleSize = (int) (size * 0.55);
                int centerX = size / 2 - 2;
                int centerY = size / 2 - 2;

                g2d.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2d.drawOval(centerX - circleSize / 2, centerY - circleSize / 2, circleSize, circleSize);

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
    }

    private JButton createCloseSuggestionsButton() {
        JButton closeBtn = new JButton() {
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
                g2d.setColor(isHovered ? new Color(200, 0, 0) : Color.RED);

                int padding = 8;
                int x1 = padding;
                int y1 = padding;
                int x2 = getWidth() - padding;
                int y2 = getHeight() - padding;

                g2d.drawLine(x1, y1, x2, y2);
                g2d.drawLine(x2, y1, x1, y2);
                g2d.dispose();
            }
        };

        closeBtn.addActionListener(e -> {
            suggestionScrollPanel.setVisible(false);
            searchField.setText("");
            currentPage = 0;
            updateQuickKeysPage();
        });

        return closeBtn;
    }

    private void setupFilterComboRenderer() {
        priceFilterCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {

                if (value != null && value.toString().startsWith("───")) {
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

                JLabel label = (JLabel) super.getListCellRendererComponent(list, value,
                    index, isSelected, cellHasFocus);

                Object selectedItem = priceFilterCombo.getSelectedItem();
                if (value != null && value.equals(selectedItem) && !value.equals("No Filter")) {
                    label.setText("✓  " + value.toString());
                    label.setFont(label.getFont().deriveFont(Font.BOLD));
                }

                return label;
            }
        });

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
                            Object item = combo.getItemAt(index0);
                            if (item != null && item.toString().startsWith("───")) {
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

        priceFilterCombo.addActionListener(e -> {
            String selected = (String) priceFilterCombo.getSelectedItem();
            if (selected != null && selected.startsWith("───")) {
                priceFilterCombo.setSelectedItem(currentSortOrder != null ? currentSortOrder : "No Filter");
            }
        });
    }

    private void layoutComponents() {
        JPanel quickKeysContainer = new JPanel(new BorderLayout(5, 5));

        // Search and filter panel
        JPanel searchFilterPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.gridy = 0;

        int comboHeight = priceFilterCombo.getPreferredSize().height;
        searchField.setPreferredSize(new Dimension(300, comboHeight));
        searchField.setMinimumSize(new Dimension(200, comboHeight));
        searchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, comboHeight));

        int buttonSize = comboHeight - 4;
        clearSearchButton.setPreferredSize(new Dimension(buttonSize, buttonSize));
        clearSearchButton.setSize(buttonSize, buttonSize);

        searchFieldPanel.setPreferredSize(new Dimension(300, comboHeight));
        searchFieldPanel.setMinimumSize(new Dimension(200, comboHeight));
        searchFieldPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, comboHeight));

        searchFieldPanel.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int panelWidth = searchFieldPanel.getWidth();
                int panelHeight = searchFieldPanel.getHeight();

                searchField.setBounds(0, 0, panelWidth, panelHeight);

                int btnSize = panelHeight - 8;
                int btnY = (panelHeight - btnSize) / 2;

                int searchIconX = panelWidth - btnSize - 6;
                searchIconButton.setBounds(searchIconX, btnY, btnSize, btnSize);

                int clearBtnX = searchIconX - btnSize - 4;
                clearSearchButton.setBounds(clearBtnX, btnY, btnSize, btnSize);
            }
        });

        searchFieldPanel.add(searchField);
        searchFieldPanel.add(searchIconButton);
        searchFieldPanel.add(clearSearchButton);

        searchFieldPanel.setComponentZOrder(searchIconButton, 0);
        searchFieldPanel.setComponentZOrder(clearSearchButton, 0);

        gbc.gridx = 0;
        gbc.weightx = 0;
        JLabel searchLabel = new JLabel("Search:");
        searchLabel.setFont(new Font("Arial", Font.BOLD, 16));
        searchFilterPanel.add(searchLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.5;
        searchFilterPanel.add(searchFieldPanel, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        JLabel filterLabel = new JLabel("Filter:");
        filterLabel.setFont(new Font("Arial", Font.BOLD, 16));
        searchFilterPanel.add(filterLabel, gbc);

        gbc.gridx = 3;
        gbc.weightx = 0.5;
        searchFilterPanel.add(priceFilterCombo, gbc);

        quickKeysContainer.add(searchFilterPanel, BorderLayout.NORTH);

        // Main content area with grid and suggestions
        JPanel mainContentPanel = new JPanel(new BorderLayout(10, 10));

        // Grid panel wrapper
        JPanel gridWrapper = new JPanel(new BorderLayout());
        gridWrapper.add(quickKeysGridPanel, BorderLayout.CENTER);

        gridWrapper.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int oldItemsPerPage = currentItemsPerPage;
                int[] gridDims = calculateGridDimensions();
                int newItemsPerPage = gridDims[0] * gridDims[1];

                if (newItemsPerPage != oldItemsPerPage) {
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
        add(quickKeysContainer, BorderLayout.CENTER);
    }

    private void attachEventHandlers() {
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

        // Show/hide clear button
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

        searchIconButton.addActionListener(e -> handleSearchSubmit());

        clearSearchButton.addActionListener(e -> {
            searchField.setText("");
            suggestionScrollPanel.setVisible(false);
            filteredProducts = new ArrayList<>(allProducts);
            applyDefaultSort();
            currentPage = 0;
            updateQuickKeysPage();
        });

        searchField.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE) {
                    if (suggestionScrollPanel.isVisible()) {
                        suggestionScrollPanel.setVisible(false);
                        currentPage = 0;
                        updateQuickKeysPage();
                    }
                }
            }
        });

        searchField.addActionListener(e -> handleSearchSubmit());

        priceFilterCombo.addActionListener(e -> handlePriceFilterChange());
    }

    private void loadAllProducts() {
        try {
            allProducts = priceBookService.getAllItems();
            filteredProducts = new ArrayList<>(allProducts);
            applyDefaultSort();
            updateQuickKeysPage();
        } catch (SQLException e) {
            showError("Failed to load products: " + e.getMessage());
        }
    }

    private void applyDefaultSort() {
        filteredProducts.sort((a, b) -> {
            if (a.isFeatured() != b.isFeatured()) {
                return a.isFeatured() ? -1 : 1;
            }

            Integer posA = a.quickKeyPosition();
            Integer posB = b.quickKeyPosition();

            if (posA == null && posB == null) return 0;
            if (posA == null) return 1;
            if (posB == null) return -1;

            return Integer.compare(posA, posB);
        });
    }

    private int[] calculateGridDimensions() {
        Container parent = quickKeysGridPanel.getParent();
        if (parent == null || parent.getWidth() <= 0 || parent.getHeight() <= 0) {
            return new int[]{4, 3};
        }

        int availableWidth = parent.getWidth() - 20;
        int availableHeight = parent.getHeight() - 60;

        if (suggestionScrollPanel.isVisible()) {
            availableHeight = Math.max(availableHeight, 200);
        }

        int maxCols = Math.max(2, (availableWidth + GRID_SPACING) / (BUTTON_WIDTH + GRID_SPACING));
        int maxRows = Math.max(2, (availableHeight + GRID_SPACING) / (BUTTON_HEIGHT + GRID_SPACING));

        maxCols = Math.min(maxCols, 6);
        maxRows = Math.min(maxRows, 6);

        return new int[]{maxRows, maxCols};
    }

    private void updateQuickKeysPage() {
        quickKeysGridPanel.removeAll();

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
            btn.addActionListener(e -> onItemSelected.accept(item));
            quickKeysGridPanel.add(btn);
        }

        int displayed = endIdx - startIdx;
        for (int i = displayed; i < currentItemsPerPage; i++) {
            JButton emptyBtn = new JButton("---");
            emptyBtn.setEnabled(false);
            quickKeysGridPanel.add(emptyBtn);
        }

        int totalPages = (int) Math.ceil((double) filteredProducts.size() / currentItemsPerPage);
        pageLabel.setText(String.format("Page %d of %d (%d items)",
            currentPage + 1, Math.max(1, totalPages), filteredProducts.size()));
        prevPageButton.setEnabled(currentPage > 0);
        nextPageButton.setEnabled(currentPage < totalPages - 1);

        quickKeysGridPanel.revalidate();
        quickKeysGridPanel.repaint();
    }

    private void handleSearchInput() {
        String searchText = searchField.getText().trim().toLowerCase();

        if (searchText.isEmpty()) {
            if (suggestionScrollPanel.isVisible()) {
                suggestionScrollPanel.setVisible(false);
                currentPage = 0;
                updateQuickKeysPage();
            }
            return;
        }

        List<PriceBook> suggestions = allProducts.stream()
            .filter(p -> p.name().toLowerCase().contains(searchText) ||
                        p.upc().toLowerCase().contains(searchText))
            .limit(15)
            .toList();

        if (suggestions.isEmpty() || suggestions.size() < SUGGESTION_THRESHOLD) {
            if (suggestionScrollPanel.isVisible()) {
                suggestionScrollPanel.setVisible(false);
                currentPage = 0;
                updateQuickKeysPage();
            }
            return;
        }

        suggestionContainer.removeAll();

        for (PriceBook product : suggestions) {
            JPanel card = createSuggestionCard(product);
            suggestionContainer.add(card);
        }

        if (!suggestionScrollPanel.isVisible()) {
            suggestionScrollPanel.setVisible(true);
            currentPage = 0;
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

        JPanel infoPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        infoPanel.setOpaque(false);

        String displayName = product.name().length() > 30
            ? product.name().substring(0, 27) + "..."
            : product.name();
        JLabel nameLabel = new JLabel(displayName);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 12));

        JLabel detailsLabel = new JLabel(String.format("UPC: %s | $%.2f",
            product.upc(), product.price()));
        detailsLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        detailsLabel.setForeground(new Color(100, 100, 100));

        infoPanel.add(nameLabel);
        infoPanel.add(detailsLabel);

        card.add(infoPanel, BorderLayout.CENTER);

        JLabel priceLabel = new JLabel(String.format("$%.2f", product.price()));
        priceLabel.setFont(new Font("Arial", Font.BOLD, 14));
        priceLabel.setForeground(new Color(0, 120, 0));
        card.add(priceLabel, BorderLayout.EAST);

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
            public void mousePressed(java.awt.event.MouseEvent e) {
                showProductActionMenu(product, card);
            }
        });

        return card;
    }

    private void showProductActionMenu(PriceBook product, Component sourceComponent) {
        // Get screen dimensions for responsive sizing
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenWidth = screenSize.width;
        int screenHeight = screenSize.height;
        float scaleFactor = screenHeight / 1080.0f;

        // Calculate dynamic font sizes
        int nameFontSize = Math.round(22 * scaleFactor);
        int priceFontSize = Math.round(20 * scaleFactor);
        int buttonFontSize = Math.round(18 * scaleFactor);
        int buttonHeight = Math.round(50 * scaleFactor);

        // Calculate dialog size based on screen (smaller than before but still touch-friendly)
        int dialogWidth = Math.min(350, screenWidth / 4);
        int dialogHeight = 120 + (buttonHeight * 3) + 40;  // Info + 3 buttons + padding

        // Create touch-friendly dialog instead of small popup menu
        JDialog actionDialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Select Action", Dialog.ModalityType.APPLICATION_MODAL);
        actionDialog.setUndecorated(true);
        actionDialog.setResizable(false);
        actionDialog.setSize(dialogWidth, dialogHeight);
        actionDialog.setMinimumSize(new Dimension(320, 300));
        actionDialog.setLocationRelativeTo(null);  // Center on screen

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Product info at top
        JPanel infoPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        JLabel nameLabel = new JLabel(product.name());
        nameLabel.setFont(new Font("Arial", Font.BOLD, nameFontSize));
        JLabel priceLabel = new JLabel(String.format("$%.2f", product.price()));
        priceLabel.setFont(new Font("Arial", Font.PLAIN, priceFontSize));
        priceLabel.setForeground(new Color(0, 120, 0));
        infoPanel.add(nameLabel);
        infoPanel.add(priceLabel);

        // Add mouse drag functionality to info panel
        final java.awt.Point[] mouseDownCompCoords = {null};
        infoPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) {
                mouseDownCompCoords[0] = e.getPoint();
            }
            public void mouseReleased(java.awt.event.MouseEvent e) {
                mouseDownCompCoords[0] = null;
            }
        });
        infoPanel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent e) {
                if (mouseDownCompCoords[0] != null) {
                    java.awt.Point currCoords = e.getLocationOnScreen();
                    actionDialog.setLocation(currCoords.x - mouseDownCompCoords[0].x, currCoords.y - mouseDownCompCoords[0].y);
                }
            }
        });


        // Touch-friendly buttons with dynamic sizing
        JPanel buttonPanel = new JPanel(new GridLayout(3, 1, 8, 8));

        JButton addToCartButton = new JButton("Add to Cart");
        addToCartButton.setFont(new Font("Arial", Font.BOLD, buttonFontSize));
        addToCartButton.setPreferredSize(new Dimension(0, buttonHeight));
        addToCartButton.setBackground(new Color(40, 167, 69));
        addToCartButton.setForeground(Color.WHITE);
        addToCartButton.addActionListener(e -> {
            onItemSelected.accept(product);
            suggestionScrollPanel.setVisible(false);
            searchField.setText("");  // Clear search field after adding item
            actionDialog.dispose();
        });
        applyRoundedStyle(addToCartButton);

        JButton viewDetailsButton = new JButton("View Details");
        viewDetailsButton.setFont(new Font("Arial", Font.BOLD, buttonFontSize));
        viewDetailsButton.setPreferredSize(new Dimension(0, buttonHeight));
        viewDetailsButton.setBackground(new Color(0, 123, 255));
        viewDetailsButton.setForeground(Color.WHITE);
        viewDetailsButton.addActionListener(e -> {
            actionDialog.dispose();
            showProductDetails(product);
        });
        applyRoundedStyle(viewDetailsButton);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(new Font("Arial", Font.BOLD, buttonFontSize));
        cancelButton.setPreferredSize(new Dimension(0, buttonHeight));
        cancelButton.setBackground(new Color(220, 53, 69));  // Red
        cancelButton.setForeground(Color.WHITE);
        cancelButton.addActionListener(e -> actionDialog.dispose());
        applyRoundedStyle(cancelButton);

        buttonPanel.add(addToCartButton);
        buttonPanel.add(viewDetailsButton);
        buttonPanel.add(cancelButton);

        mainPanel.add(infoPanel, BorderLayout.NORTH);
        mainPanel.add(buttonPanel, BorderLayout.CENTER);

        actionDialog.add(mainPanel);
        actionDialog.setVisible(true);
    }

    private void showProductDetails(PriceBook product) {
        // Get screen dimensions for responsive sizing
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenWidth = screenSize.width;
        int screenHeight = screenSize.height;
        float scaleFactor = screenHeight / 1080.0f;

        // Calculate dynamic font sizes
        int headerFontSize = Math.round(28 * scaleFactor);
        int labelFontSize = Math.round(20 * scaleFactor);
        int valueFontSize = Math.round(19 * scaleFactor);
        int buttonFontSize = Math.round(18 * scaleFactor);
        int buttonHeight = Math.round(50 * scaleFactor);

        // Calculate dialog size
        int dialogWidth = Math.min(400, screenWidth / 3);
        int dialogHeight = 350;

        // Create touch-friendly details dialog
        JDialog detailsDialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Product Details", Dialog.ModalityType.APPLICATION_MODAL);
        detailsDialog.setUndecorated(true);
        detailsDialog.setResizable(false);
        detailsDialog.setSize(dialogWidth, dialogHeight);
        detailsDialog.setMinimumSize(new Dimension(350, 400));
        detailsDialog.setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Product name header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(0, 123, 255));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel headerLabel = new JLabel(product.name());
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
                    detailsDialog.setLocation(currCoords.x - mouseDownCompCoords[0].x, currCoords.y - mouseDownCompCoords[0].y);
                }
            }
        });


        // Details panel with labeled fields
        JPanel detailsPanel = new JPanel(new GridLayout(5, 2, 10, 12));
        detailsPanel.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));

        // UPC
        JLabel upcLabel = new JLabel("UPC:");
        upcLabel.setFont(new Font("Arial", Font.BOLD, labelFontSize));
        JLabel upcValue = new JLabel(product.upc());
        upcValue.setFont(new Font("Arial", Font.PLAIN, valueFontSize));

        // Name
        JLabel nameLabel = new JLabel("Product Name:");
        nameLabel.setFont(new Font("Arial", Font.BOLD, labelFontSize));
        JLabel nameValue = new JLabel(product.name());
        nameValue.setFont(new Font("Arial", Font.PLAIN, valueFontSize));

        // Price
        JLabel priceLabel = new JLabel("Price:");
        priceLabel.setFont(new Font("Arial", Font.BOLD, labelFontSize));
        JLabel priceValue = new JLabel(String.format("$%.2f", product.price()));
        priceValue.setFont(new Font("Arial", Font.PLAIN, valueFontSize));
        priceValue.setForeground(new Color(0, 120, 0));

        // Featured
        JLabel featuredLabel = new JLabel("Featured:");
        featuredLabel.setFont(new Font("Arial", Font.BOLD, labelFontSize));
        JLabel featuredValue = new JLabel(product.isFeatured() ? "Yes" : "No");
        featuredValue.setFont(new Font("Arial", Font.PLAIN, valueFontSize));

        // Quick Key Position
        JLabel positionLabel = new JLabel("Quick Key:");
        positionLabel.setFont(new Font("Arial", Font.BOLD, labelFontSize));
        JLabel positionValue = new JLabel(product.quickKeyPosition() != null ?
            "#" + product.quickKeyPosition().toString() : "Not assigned");
        positionValue.setFont(new Font("Arial", Font.PLAIN, valueFontSize));

        detailsPanel.add(upcLabel);
        detailsPanel.add(upcValue);
        detailsPanel.add(nameLabel);
        detailsPanel.add(nameValue);
        detailsPanel.add(priceLabel);
        detailsPanel.add(priceValue);
        detailsPanel.add(featuredLabel);
        detailsPanel.add(featuredValue);
        detailsPanel.add(positionLabel);
        detailsPanel.add(positionValue);

        // Close button
        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        JButton closeButton = new JButton("Close");
        closeButton.setFont(new Font("Arial", Font.BOLD, buttonFontSize));
        closeButton.setPreferredSize(new Dimension(0, buttonHeight));
        closeButton.setBackground(new Color(220, 53, 69));  // Red
        closeButton.setForeground(Color.WHITE);
        closeButton.addActionListener(e -> detailsDialog.dispose());
        applyRoundedStyle(closeButton);

        buttonPanel.add(closeButton, BorderLayout.CENTER);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(detailsPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        detailsDialog.add(mainPanel);
        detailsDialog.setVisible(true);
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

    private void handleSearchSubmit() {
        String searchText = searchField.getText().trim().toLowerCase();

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

        applySortOrder();
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
                applyDefaultSort();
                break;
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(this), message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public void reloadProducts() {
        loadAllProducts();
    }

    public void setProductSelectionEnabled(boolean enabled) {
        // Disable/enable search field
        searchField.setEnabled(enabled);
        searchIconButton.setEnabled(enabled);
        clearSearchButton.setEnabled(enabled);

        // Disable/enable filter combo
        priceFilterCombo.setEnabled(enabled);

        // Disable/enable pagination buttons
        prevPageButton.setEnabled(enabled && currentPage > 0);
        nextPageButton.setEnabled(enabled && (currentPage + 1) * currentItemsPerPage < filteredProducts.size());

        // Disable/enable all product buttons in the grid
        for (Component comp : quickKeysGridPanel.getComponents()) {
            if (comp instanceof JButton) {
                comp.setEnabled(enabled);
            }
        }

        // Repaint to show visual changes
        repaint();
    }

    private void openKeyboardDialog() {
        // Get screen dimensions for responsive sizing
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenHeight = screenSize.height;
        float scaleFactor = screenHeight / 1080.0f;

        int dialogWidth = Math.min(800, screenSize.width - 100);
        int dialogHeight = Math.min(500, screenSize.height - 100);

        JDialog keyboardDialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Search Products", Dialog.ModalityType.MODELESS);
        keyboardDialog.setUndecorated(true);
        keyboardDialog.setResizable(false);
        keyboardDialog.setSize(dialogWidth, dialogHeight);
        keyboardDialog.setMinimumSize(new Dimension(700, 450));

        // Position at lower center of screen
        int x = (screenSize.width - dialogWidth) / 2;
        int y = screenSize.height - dialogHeight - 50; // 50px padding from bottom
        keyboardDialog.setLocation(x, y);
        keyboardDialog.setLayout(new BorderLayout());

        // Header Panel with close button
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(23, 162, 184));  // Info blue
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel headerLabel = new JLabel("Enter Product Name");
        headerLabel.setFont(new Font("Arial", Font.BOLD, 24));
        headerLabel.setForeground(Color.WHITE);
        headerPanel.add(headerLabel, BorderLayout.CENTER);

        // Add custom close button (X) to header
        JButton closeButton = new JButton() {
            private boolean isHovered = false;

            {
                setFocusPainted(false);
                setBorderPainted(false);
                setContentAreaFilled(false);
                setOpaque(false);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                setPreferredSize(new Dimension(40, 40));

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

                addActionListener(e -> keyboardDialog.dispose());
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int size = Math.min(getWidth(), getHeight());
                int padding = size / 3;

                if (isHovered) {
                    g2d.setColor(new Color(220, 53, 69)); // Red on hover
                    g2d.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 4, 4);
                }

                g2d.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2d.setColor(Color.WHITE);

                int x1 = padding;
                int y1 = padding;
                int x2 = size - padding;
                int y2 = size - padding;

                g2d.drawLine(x1, y1, x2, y2);
                g2d.drawLine(x2, y1, x1, y2);

                g2d.dispose();
            }
        };

        headerPanel.add(closeButton, BorderLayout.EAST);

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
                    keyboardDialog.setLocation(currCoords.x - mouseDownCompCoords[0].x, currCoords.y - mouseDownCompCoords[0].y);
                }
            }
        });

        keyboardDialog.add(headerPanel, BorderLayout.NORTH);

        // Center Panel with input field and keyboard
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Input text field (read-only, keyboard only)
        JTextField inputField = new JTextField(searchField.getText());
        inputField.setFont(new Font("Arial", Font.BOLD, 20));
        inputField.setHorizontalAlignment(JTextField.CENTER);
        inputField.setMaximumSize(new Dimension(700, 50));
        inputField.setPreferredSize(new Dimension(700, 50));
        inputField.setAlignmentX(Component.CENTER_ALIGNMENT);
        inputField.setEditable(false);  // Only keyboard can input
        centerPanel.add(inputField);

        centerPanel.add(Box.createVerticalStrut(15));

        // QWERTY Keyboard Panel
        JPanel keyboardPanel = createQWERTYKeyboard(inputField);
        keyboardPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(keyboardPanel);

        keyboardDialog.add(centerPanel, BorderLayout.CENTER);

        // Allow Escape key to close dialog
        keyboardDialog.getRootPane().registerKeyboardAction(
            e -> keyboardDialog.dispose(),
            KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        // Close dialog when clicking outside
        keyboardDialog.addWindowFocusListener(new java.awt.event.WindowFocusListener() {
            @Override
            public void windowGainedFocus(java.awt.event.WindowEvent e) {
                // Do nothing
            }

            @Override
            public void windowLostFocus(java.awt.event.WindowEvent e) {
                keyboardDialog.dispose();
            }
        });

        keyboardDialog.setVisible(true);
    }

    private JPanel createQWERTYKeyboard(JTextField inputField) {
        JPanel keyboardPanel = new JPanel(new GridBagLayout());
        keyboardPanel.setBackground(Color.WHITE);
        keyboardPanel.setMaximumSize(new Dimension(700, 350));
        keyboardPanel.setPreferredSize(new Dimension(700, 350));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(3, 3, 3, 3);
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;

        // Row 0: 1 2 3 4 5 6 7 8 9 0 (Number row)
        String[] row0 = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"};
        gbc.gridy = 0;
        for (int i = 0; i < row0.length; i++) {
            gbc.gridx = i;
            gbc.gridwidth = 1;
            JButton key = createKeyboardKey(row0[i], inputField);
            keyboardPanel.add(key, gbc);
        }

        // Row 1: Q W E R T Y U I O P
        String[] row1 = {"Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"};
        gbc.gridy = 1;
        for (int i = 0; i < row1.length; i++) {
            gbc.gridx = i;
            gbc.gridwidth = 1;
            JButton key = createKeyboardKey(row1[i], inputField);
            keyboardPanel.add(key, gbc);
        }

        // Row 2: A S D F G H J K L
        String[] row2 = {"A", "S", "D", "F", "G", "H", "J", "K", "L"};
        gbc.gridy = 2;
        for (int i = 0; i < row2.length; i++) {
            gbc.gridx = i;
            gbc.gridwidth = 1;
            JButton key = createKeyboardKey(row2[i], inputField);
            keyboardPanel.add(key, gbc);
        }

        // Backspace button (right of row 2)
        gbc.gridx = 9;
        gbc.gridwidth = 1;
        JButton backspaceBtn = createKeyboardKey("←", inputField);
        backspaceBtn.setBackground(new Color(255, 193, 7)); // Amber
        backspaceBtn.setForeground(Color.WHITE);
        applyRoundedStyle(backspaceBtn);
        keyboardPanel.add(backspaceBtn, gbc);

        // Row 3: Z X C V B N M
        String[] row3 = {"Z", "X", "C", "V", "B", "N", "M"};
        gbc.gridy = 3;
        for (int i = 0; i < row3.length; i++) {
            gbc.gridx = i;
            gbc.gridwidth = 1;
            JButton key = createKeyboardKey(row3[i], inputField);
            keyboardPanel.add(key, gbc);
        }

        // Clear button (right of row 3)
        gbc.gridx = 7;
        gbc.gridwidth = 3;
        JButton clearBtn = createKeyboardKey("Clear", inputField);
        clearBtn.setBackground(new Color(220, 53, 69)); // Red
        clearBtn.setForeground(Color.WHITE);
        applyRoundedStyle(clearBtn);
        keyboardPanel.add(clearBtn, gbc);

        // Row 4: Space bar + Enter
        gbc.gridy = 4;
        gbc.gridx = 0;
        gbc.gridwidth = 7;
        JButton spaceBtn = createKeyboardKey("Space", inputField);
        keyboardPanel.add(spaceBtn, gbc);

        // Enter button (right side of row 4)
        gbc.gridx = 7;
        gbc.gridwidth = 3;
        JButton enterBtn = createKeyboardKey("Enter", inputField);
        enterBtn.setBackground(new Color(40, 167, 69)); // Green
        enterBtn.setForeground(Color.WHITE);
        applyRoundedStyle(enterBtn);
        keyboardPanel.add(enterBtn, gbc);

        return keyboardPanel;
    }

    private JButton createKeyboardKey(String key, JTextField inputField) {
        JButton keyButton = new JButton(key);
        keyButton.setFont(new Font("Arial", Font.BOLD, 16));
        keyButton.setFocusPainted(false);
        keyButton.setBackground(new Color(248, 249, 250)); // Light gray
        keyButton.setForeground(Color.BLACK);

        keyButton.addActionListener(e -> {
            String currentText = inputField.getText();
            String newText = currentText;

            if (key.equals("Clear")) {
                newText = "";
            } else if (key.equals("←")) {
                // Backspace - remove last character
                if (currentText.length() > 0) {
                    newText = currentText.substring(0, currentText.length() - 1);
                }
            } else if (key.equals("Space")) {
                newText = currentText + " ";
            } else if (key.equals("Enter")) {
                // Enter key - close the dialog
                Window window = SwingUtilities.getWindowAncestor(keyButton);
                if (window != null) {
                    window.dispose();
                }
                return; // Don't update text fields
            } else {
                // Letter or number key
                newText = currentText + key;
            }

            // Update dialog input field
            inputField.setText(newText);

            // Also update the actual search field in real-time
            searchField.setText(newText);

            // Trigger search/auto-suggest as user types
            handleSearchInput();
        });

        return keyButton;
    }
}
