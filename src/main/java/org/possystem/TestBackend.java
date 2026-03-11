package org.possystem;

import org.possystem.database.DatabaseManager;
import org.possystem.entity.PriceBook;
import org.possystem.entity.TransactionItem;
import org.possystem.service.PriceBookService;
import org.possystem.service.TransactionService;

import java.sql.SQLException;
import java.util.List;

/**
 * Test class to verify all backend functionality before UI development.
 */
public class TestBackend {

    public static void main(String[] args) {
        try {
            System.out.println("=".repeat(60));
            System.out.println("TESTING POS SYSTEM BACKEND");
            System.out.println("=".repeat(60));

            // Initialize database
            DatabaseManager.initialize();
            System.out.println();

            // Test 1: Featured Items (Quick Keys)
            System.out.println("TEST 1: Quick Keys (Featured Items)");
            System.out.println("-".repeat(60));
            PriceBookService priceBookService = new PriceBookService();
            List<PriceBook> featuredItems = priceBookService.getFeaturedItems();
            System.out.println("Found " + featuredItems.size() + " featured items for quick keys:");
            for (PriceBook item : featuredItems) {
                System.out.printf("  [%2d] %-40s $%6.2f%n",
                        item.quickKeyPosition(), item.name(), item.price());
            }
            System.out.println();

            // Test 2: Barcode Scanner (UPC Lookup)
            System.out.println("TEST 2: Barcode Scanner (UPC Lookup)");
            System.out.println("-".repeat(60));
            String testUpc = featuredItems.get(0).upc();
            System.out.println("Scanning UPC: " + testUpc);
            var result = priceBookService.getItemByUpc(testUpc);
            if (result.isPresent()) {
                PriceBook item = result.get();
                System.out.printf("Found: %s - $%.2f%n", item.name(), item.price());
            }
            System.out.println();

            // Test 3: Create Transaction
            System.out.println("TEST 3: Create New Transaction");
            System.out.println("-".repeat(60));
            TransactionService txService = new TransactionService();
            txService.createTransaction();
            System.out.println("Transaction created with ID: " + txService.getCurrentTransactionId());
            System.out.println();

            // Test 4: Add Items (Auto-increment duplicate)
            System.out.println("TEST 4: Add Items (Testing Auto-Increment)");
            System.out.println("-".repeat(60));
            PriceBook item1 = featuredItems.get(0);
            PriceBook item2 = featuredItems.get(1);

            System.out.println("Adding: " + item1.name());
            txService.addItem(item1.upc(), item1.name(), item1.price());

            System.out.println("Adding: " + item2.name());
            txService.addItem(item2.upc(), item2.name(), item2.price());

            System.out.println("Adding: " + item1.name() + " again (should auto-increment quantity)");
            txService.addItem(item1.upc(), item1.name(), item1.price());
            System.out.println();

            // Test 5: View Current Sale
            System.out.println("TEST 5: Current Sale Items");
            System.out.println("-".repeat(60));
            List<TransactionItem> saleItems = txService.getCurrentSaleItems();
            System.out.printf("%-10s %-35s %5s %8s %10s%n", "ID", "Name", "Qty", "Price", "Line Total");
            System.out.println("-".repeat(70));
            for (TransactionItem item : saleItems) {
                if (item.status().equals("ACTIVE")) {
                    System.out.printf("%-10d %-35s %5d $%7.2f $%9.2f%n",
                            item.id(),
                            item.name().substring(0, Math.min(35, item.name().length())),
                            item.quantity(),
                            item.unitPrice(),
                            item.subtotal());
                }
            }
            System.out.println();

            // Test 6: Calculate Totals
            System.out.println("TEST 6: Calculate Transaction Totals");
            System.out.println("-".repeat(60));
            double subtotal = txService.getTransactionSubtotal();
            double total = txService.getTransactionTotal();
            double tax = total - subtotal;
            System.out.printf("Subtotal: $%.2f%n", subtotal);
            System.out.printf("Tax (7%%): $%.2f%n", tax);
            System.out.printf("Total:    $%.2f%n", total);
            System.out.println();

            // Test 7: Process Cash Payment
            System.out.println("TEST 7: Process Cash Payment");
            System.out.println("-".repeat(60));
            double amountTendered = 20.00;
            System.out.printf("Amount Tendered: $%.2f%n", amountTendered);
            txService.processCash(amountTendered);
            double change = amountTendered - total;
            System.out.printf("Change: $%.2f%n", change);
            System.out.println();

            System.out.println("=".repeat(60));
            System.out.println("ALL BACKEND TESTS COMPLETED SUCCESSFULLY!");
            System.out.println("=".repeat(60));

        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
