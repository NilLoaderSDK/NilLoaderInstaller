package me.tamkungz.nilloaderinstaller;

import javax.swing.*;
import java.awt.*;

public final class Main {
    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("NilLoader Installer requires a graphical desktop environment.");
            System.exit(2);
        }
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new InstallerFrame().setVisible(true));
    }
}
