package org.possystem.database;

import java.sql.*;

/**
 * DataSeeder class to pre-populate the price_book table
 * with 36 products on first run.
 * Checks if data already exists before inserting
 * to avoid duplicates on restart.
 */
public class DataSeeder {

    public static void seed() throws SQLException {
        Connection conn = DatabaseManager.getConnection();

        Statement checkStmt = conn.createStatement();
        ResultSet rs = checkStmt.executeQuery("SELECT COUNT(*) FROM price_book");
        rs.next();
        int count = rs.getInt(1);

        if (count > 0) {
            System.out.println("Price book already populated, skipping seed.");
            return;
        }

        String sql = "INSERT INTO price_book (upc, name, price, stock, is_age_restricted) VALUES (?, ?, ?, ?, ?)";
        PreparedStatement stmt = conn.prepareStatement(sql);

        Object[][] products = {
                // Beverages
                {"012345678901", "Coca Cola 350ml",          1.99,  100, false},
                {"012345678902", "Pepsi 350ml",               1.99,  100, false},
                {"012345678903", "Sprite 350ml",              1.99,  100, false},
                {"012345678904", "Mountain Dew 350ml",        1.99,  100, false},
                {"012345678905", "Orange Juice 1L",           3.49,   80, false},
                {"012345678906", "Apple Juice 1L",            3.49,   80, false},
                {"012345678907", "Water 500ml",               0.99,  200, false},
                {"012345678908", "Iced Tea 350ml",            1.49,  150, false},
                // Snacks
                {"012345678909", "Lays Classic Chips",        2.49,   75, false},
                {"012345678910", "Doritos Nacho Cheese",      2.49,   75, false},
                {"012345678911", "Pringles Original",         3.29,   60, false},
                {"012345678912", "Oreo Cookies",              2.99,   90, false},
                {"012345678913", "Cheetos Puffs",             2.49,   70, false},
                {"012345678914", "Ritz Crackers",             3.49,   55, false},
                {"012345678915", "Granola Bar",               1.79,  120, false},
                {"012345678916", "Chocolate Bar",             1.49,  130, false},
                // Dairy
                {"012345678917", "Whole Milk 1L",             2.99,   60, false},
                {"012345678918", "Cheese Slice 200g",         4.99,   45, false},
                {"012345678919", "Butter 250g",               3.99,   50, false},
                {"012345678920", "Yogurt 150g",               1.49,   80, false},
                {"012345678921", "Eggs 12pcs",                3.99,   70, false},
                // Bread & Bakery
                {"012345678922", "White Bread",               2.49,   55, false},
                {"012345678923", "Wheat Bread",               2.99,   50, false},
                {"012345678924", "Croissant",                 1.29,   40, false},
                {"012345678925", "Hotdog Bun 6pcs",           1.99,   60, false},
                // Canned Goods
                {"012345678926", "Canned Tuna 180g",          1.99,   90, false},
                {"012345678927", "Canned Corn 400g",          1.49,   85, false},
                {"012345678928", "Canned Beans 400g",         1.49,   85, false},
                {"012345678929", "Tomato Sauce 250g",         1.29,   95, false},
                {"012345678930", "Sardines 155g",             1.19,  100, false},
                // Household
                {"012345678931", "Dish Soap 500ml",           3.49,   40, false},
                {"012345678932", "Laundry Detergent 1kg",     7.99,   30, false},
                {"012345678933", "Tissue Paper 10pcs",        2.99,   65, false},
                {"012345678934", "Trash Bag 10pcs",           2.49,   55, false},
                // Personal Care
                {"012345678935", "Shampoo 200ml",             4.99,   45, false},
                {"012345678936", "Toothpaste 100g",           2.99,   50, false},
                // Age Restricted Items (randomly placed in new UPCs)
                {"012345678937", "Beer Corona 330ml",         3.99,   80, true},
                {"012345678938", "Red Wine 750ml",            12.99,  40, true},
                {"012345678939", "Vodka 500ml",               18.99,  30, true},
                {"012345678940", "Cigarettes Marlboro",       9.99,   50, true},
                {"012345678941", "White Wine 750ml",          11.99,  35, true},
                {"012345678942", "Whiskey 700ml",             24.99,  25, true},
        };

        for (Object[] product : products) {
            stmt.setString(1, (String) product[0]);
            stmt.setString(2, (String) product[1]);
            stmt.setDouble(3, (Double) product[2]);
            stmt.setInt(4, (Integer) product[3]);
            stmt.setBoolean(5, (Boolean) product[4]);
            stmt.addBatch();
        }

        stmt.executeBatch();
        System.out.println("Price book seeded with 42 products successfully!");
    }
}