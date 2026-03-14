package org.possystem;

import org.possystem.database.DatabaseManager;
import org.possystem.ui.PosInterface;

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
            PosInterface posInterface = new PosInterface();
            posInterface.setVisible(true);
        });
    }
}
