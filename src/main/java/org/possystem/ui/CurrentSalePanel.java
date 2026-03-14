package org.possystem.ui;

import org.possystem.entity.TransactionItem;
import org.possystem.service.TransactionService;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Current Sale Panel - Shopping cart with table, totals, and item management
 */
public class CurrentSalePanel extends JPanel {

    private final TransactionService transactionService;
    private final Runnable onSelectionChanged;

    private boolean editingEnabled = true; // Track if editing is allowed
    private JTable saleTable;
    private SaleTableModel saleTableModel;
    private JLabel subtotalLabel;
    private JLabel taxLabel;
    private JLabel totalLabel;

    public static final int MIN_WIDTH = 400;
    public static final int MAX_WIDTH = 800;
    public static final int DEFAULT_WIDTH = 620;

    public CurrentSalePanel(TransactionService transactionService, Runnable onSelectionChanged) {
        this.transactionService = transactionService;
        this.onSelectionChanged = onSelectionChanged;

        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createTitledBorder("Current Sale"));
        setMinimumSize(new Dimension(MIN_WIDTH, 0));

        initializeComponents();
        layoutComponents();
    }

    private void initializeComponents() {
        // Create custom table model
        saleTableModel = new SaleTableModel();
        saleTable = new JTable(saleTableModel);
        saleTable.setRowHeight(45);
        saleTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        saleTable.getTableHeader().setReorderingAllowed(false);

        // Enable row selection
        saleTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        saleTable.setRowSelectionAllowed(true);

        // Add selection listener for row selection
        saleTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && onSelectionChanged != null) {
                onSelectionChanged.run();
            }
        });

        // Set column widths
        saleTable.getColumnModel().getColumn(0).setPreferredWidth(40);  // Checkbox
        saleTable.getColumnModel().getColumn(1).setPreferredWidth(250); // Name
        saleTable.getColumnModel().getColumn(2).setPreferredWidth(150); // Qty
        saleTable.getColumnModel().getColumn(2).setMinWidth(150);
        saleTable.getColumnModel().getColumn(3).setPreferredWidth(80);  // Price
        saleTable.getColumnModel().getColumn(4).setPreferredWidth(90);  // Line Total
        saleTable.getColumnModel().getColumn(5).setPreferredWidth(50);  // Delete
        saleTable.getColumnModel().getColumn(5).setMaxWidth(50);

        // Set custom header renderer for select all checkbox
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

                // Only allow delete if editing is enabled
                if (column == 5 && row >= 0 && editingEnabled) {
                    TransactionItem item = saleTableModel.getItemAt(row);
                    handleSingleClickDelete(item);
                }
            }
        });

        // Add mouse motion listener to change cursor over delete column
        saleTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                int column = saleTable.columnAtPoint(e.getPoint());
                if (column == 5 && editingEnabled) {
                    saleTable.setCursor(new Cursor(Cursor.HAND_CURSOR));
                } else {
                    saleTable.setCursor(Cursor.getDefaultCursor());
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
    }

    private void layoutComponents() {
        // Table with scroll
        JScrollPane scrollPane = new JScrollPane(saleTable);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);

        // Totals panel at bottom
        JPanel totalsPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        totalsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        totalsPanel.add(subtotalLabel);
        totalsPanel.add(taxLabel);
        totalsPanel.add(totalLabel);
        add(totalsPanel, BorderLayout.SOUTH);
    }

    public void refreshDisplay() {
        try {
            List<TransactionItem> items = transactionService.getCurrentSaleItems();
            saleTableModel.setItems(items);

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

    public List<Integer> getSelectedItemIds() {
        return saleTableModel.getSelectedItemIds();
    }

    public boolean hasSelection() {
        return saleTableModel.hasSelection();
    }

    public Integer getRowSelectedItemId() {
        int selectedRow = saleTable.getSelectedRow();
        if (selectedRow >= 0 && selectedRow < saleTableModel.getRowCount()) {
            return saleTableModel.getItemAt(selectedRow).id();
        }
        return null;
    }

    public boolean hasRowSelection() {
        return saleTable.getSelectedRow() >= 0;
    }

    public void setEditingEnabled(boolean enabled) {
        this.editingEnabled = enabled;

        // Disable/enable the table editing (quantity controls and delete buttons still render, but won't be editable)
        // We'll prevent editing by making cells non-editable in the table model
        saleTable.setEnabled(enabled);

        // Also disable checkbox selection
        saleTable.getColumnModel().getColumn(0).setCellEditor(enabled ? new CheckBoxEditor() : null);

        // Reset cursor to default when disabled
        if (!enabled) {
            saleTable.setCursor(Cursor.getDefaultCursor());
        }

        // Repaint to show visual changes (especially for delete icons)
        saleTable.repaint();
    }

    private void toggleSelectAll() {
        boolean newState = !saleTableModel.isAllSelected();
        saleTableModel.selectAll(newState);
        saleTable.repaint();
        saleTable.getTableHeader().repaint();
        if (onSelectionChanged != null) {
            onSelectionChanged.run();
        }
    }

    private void handleSingleClickDelete(TransactionItem item) {
        int confirm = JOptionPane.showConfirmDialog(
            SwingUtilities.getWindowAncestor(this),
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
                refreshDisplay();
            } catch (SQLException e) {
                showError("Failed to delete item: " + e.getMessage());
            }
        }
    }

    private void showError(String message) {
        showErrorDialog("Error", "System Error", message);
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

        // Apply rounded style (simple version without custom UI for now)
        okButton.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        okButton.setFocusPainted(false);

        okButton.addActionListener(e -> errorDialog.dispose());

        buttonPanel.add(okButton);

        errorDialog.add(buttonPanel, BorderLayout.SOUTH);

        errorDialog.setVisible(true);
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
    private class SaleTableModel extends AbstractTableModel {
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
                case 2 -> item;
                case 3 -> String.format("$%.2f", item.unitPrice());
                case 4 -> String.format("$%.2f", item.subtotal());
                case 5 -> "🗑";
                default -> null;
            };
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column == 0 || column == 2;
        }

        @Override
        public void setValueAt(Object value, int row, int column) {
            if (column == 0) {
                selections.set(row, (Boolean) value);
                fireTableCellUpdated(row, column);
                saleTable.getTableHeader().repaint();
                if (onSelectionChanged != null) {
                    onSelectionChanged.run();
                }
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
        private final Color trashGray = new Color(160, 160, 160);

        public DeleteButtonRenderer() {
            setOpaque(false);
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

            // Use gray if editing is disabled, red if enabled
            g2d.setColor(editingEnabled ? trashRed : trashGray);
            g2d.setStroke(new BasicStroke(2));

            g2d.drawRect(x + 3, y + 6, 14, 12);
            g2d.drawLine(x + 2, y + 5, x + 18, y + 5);
            g2d.drawRect(x + 7, y + 2, 6, 3);
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
    private class CheckBoxEditor extends javax.swing.DefaultCellEditor {
        public CheckBoxEditor() {
            super(new JCheckBox());
            ((JCheckBox) getComponent()).setHorizontalAlignment(JLabel.CENTER);
        }
    }

    private class QuantityControlEditor extends javax.swing.AbstractCellEditor implements TableCellEditor {
        private final JPanel panel;
        private final JButton minusBtn;
        private final JTextField qtyField;
        private final JButton plusBtn;
        private TransactionItem currentItem;

        public QuantityControlEditor() {
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 2));

            minusBtn = new JButton("-");
            minusBtn.setPreferredSize(new Dimension(40, 30));
            minusBtn.addActionListener(e -> adjustQuantity(-1));

            qtyField = new JTextField(3);
            qtyField.setHorizontalAlignment(JTextField.CENTER);
            qtyField.setPreferredSize(new Dimension(40, 30));
            qtyField.addActionListener(e -> updateQuantityFromField());

            plusBtn = new JButton("+");
            plusBtn.setPreferredSize(new Dimension(40, 30));
            plusBtn.addActionListener(e -> adjustQuantity(1));

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
                refreshDisplay();
            } catch (SQLException e) {
                showError("Failed to update quantity: " + e.getMessage());
            }
        }
    }
}
