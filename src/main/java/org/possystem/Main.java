package org.possystem;

import org.possystem.database.DatabaseManager;
import org.possystem.ui.PosInterfacePolished;

import javax.swing.*;

/**
 * Main entry point for POS System
 */
public class Main {
    public static void main(String[] args) {
        // Initialize database first
        DatabaseManager.initialize();

        // Launch POS Interface on Swing thread
        SwingUtilities.invokeLater(() -> {
            PosInterfacePolished posInterface = new PosInterfacePolished();
            posInterface.setVisible(true);
        });
    }
}
