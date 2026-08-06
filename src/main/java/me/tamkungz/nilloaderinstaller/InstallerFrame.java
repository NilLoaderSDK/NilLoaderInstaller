package me.tamkungz.nilloaderinstaller;

import me.tamkungz.nilloaderinstaller.model.InstallResult;
import me.tamkungz.nilloaderinstaller.model.InstallTarget;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class InstallerFrame extends JFrame {
    private final TargetTableModel model = new TargetTableModel();
    private final JTable table = new JTable(model);
    private final JButton installButton = new JButton("Install NilLoader");
    private final JButton refreshButton = new JButton("Refresh");
    private final JButton browseButton = new JButton("Add launcher / instance folder…");
    private final JProgressBar progress = new JProgressBar();
    private final JLabel status = new JLabel("Detecting Minecraft installations…");

    public InstallerFrame() {
        super(AppInfo.APP_NAME);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(860, 540));
        setSize(960, 620);
        setLocationRelativeTo(null);
        buildUi();
        refreshTargets();
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBorder(new EmptyBorder(22, 24, 20, 24));
        setContentPane(root);

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("NilLoader Installer");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 28f));
        JLabel subtitle = new JLabel("Install NilLoader " + AppInfo.NILLOADER_VERSION + " without manually editing JVM arguments or Prism component JSON.");
        subtitle.setBorder(new EmptyBorder(6, 0, 0, 0));
        header.add(title);
        header.add(subtitle);
        root.add(header, BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(30);
        table.setAutoCreateRowSorter(true);
        table.getColumnModel().getColumn(0).setPreferredWidth(290);
        table.getColumnModel().getColumn(1).setPreferredWidth(150);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        table.getColumnModel().getColumn(3).setPreferredWidth(90);
        table.getSelectionModel().addListSelectionListener(e -> updateInstallButton());
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) installSelected();
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createTitledBorder("Detected installations / instances"));
        root.add(scroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(12, 10));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        refreshButton.addActionListener(e -> refreshTargets());
        browseButton.addActionListener(e -> browse());
        installButton.addActionListener(e -> installSelected());
        installButton.setEnabled(false);
        actions.add(refreshButton);
        actions.add(browseButton);
        actions.add(installButton);
        bottom.add(actions, BorderLayout.NORTH);

        JPanel statusPanel = new JPanel(new BorderLayout(10, 0));
        progress.setIndeterminate(false);
        progress.setVisible(false);
        progress.setPreferredSize(new Dimension(150, 18));
        statusPanel.add(status, BorderLayout.CENTER);
        statusPanel.add(progress, BorderLayout.EAST);
        bottom.add(statusPanel, BorderLayout.SOUTH);
        root.add(bottom, BorderLayout.SOUTH);
    }

    private void refreshTargets() {
        setBusy(true, "Detecting Minecraft installations…");
        new SwingWorker<List<InstallTarget>, Void>() {
            @Override protected List<InstallTarget> doInBackground() {
                return LauncherDetector.detectAll();
            }

            @Override protected void done() {
                try {
                    List<InstallTarget> targets = get();
                    model.setTargets(targets);
                    status.setText(targets.isEmpty()
                            ? "No launcher profiles found. Use “Add launcher / instance folder…” to choose one manually."
                            : "Found " + targets.size() + " install target" + (targets.size() == 1 ? "." : "s."));
                    if (!targets.isEmpty()) table.setRowSelectionInterval(0, 0);
                } catch (Exception e) {
                    showError("Detection failed", e);
                } finally {
                    setBusy(false, null);
                    updateInstallButton();
                }
            }
        }.execute();
    }

    private void browse() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Choose Minecraft launcher root or Prism/PolyMC instance");
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path chosen = chooser.getSelectedFile().toPath();
        List<InstallTarget> found = LauncherDetector.detectPath(chosen);
        if (found.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "That folder does not look like a supported Minecraft Launcher root or Prism/PolyMC/MultiMC instance.\n\n"
                            + "Official launcher: choose the folder containing launcher_profiles.json (or launcher_profiles_microsoft_store.json).\n"
                            + "Prism/PolyMC: choose either the launcher root containing instances/ or a single instance folder.",
                    "No supported installation found", JOptionPane.WARNING_MESSAGE);
            return;
        }
        model.addTargets(found);
        int first = Math.max(0, model.getRowCount() - found.size());
        table.setRowSelectionInterval(first, first);
        status.setText("Added " + found.size() + " target" + (found.size() == 1 ? "." : "s."));
    }

    private void installSelected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return;
        int row = table.convertRowIndexToModel(viewRow);
        InstallTarget target = model.get(row);

        boolean repair = target.installed();
        String note;
        if (target.launcherType().name().equals("OFFICIAL")) {
            note = repair
                    ? "Close the Minecraft Launcher before continuing. This repairs the selected NilLoader installation in place and keeps a backup of the launcher profile database."
                    : "Close the Minecraft Launcher before continuing. The selected installation will be cloned; the original will not be modified.";
        } else {
            note = "Close Prism/PolyMC/MultiMC before continuing so it reloads the component files cleanly.";
        }
        String action = repair ? "Repair" : "Install";
        int confirm = JOptionPane.showConfirmDialog(this,
                action + " NilLoader " + AppInfo.NILLOADER_VERSION + " in:\n\n"
                        + target.displayName() + "\nMinecraft " + target.minecraftVersion() + "\n"
                        + target.gameDir() + "\n\n" + note,
                action + " NilLoader", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.OK_OPTION) return;

        setBusy(true, (target.installed() ? "Repairing" : "Installing") + " NilLoader in " + target.displayName() + "…");
        new SwingWorker<InstallResult, Void>() {
            @Override protected InstallResult doInBackground() throws Exception {
                return InstallerService.install(target);
            }

            @Override protected void done() {
                try {
                    InstallResult result = get();
                    status.setText(target.installed() ? "Repaired successfully." : "Installed successfully.");
                    Object[] options = {"Done", "Open nilmods folder"};
                    int selected = JOptionPane.showOptionDialog(InstallerFrame.this,
                            result.message() + "\n\nPut NilLoader mods in:\n" + result.nilmodsDirectory(),
                            target.installed() ? "NilLoader repaired" : "NilLoader installed", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
                            null, options, options[0]);
                    if (selected == 1) openFolder(result.nilmodsDirectory());
                    refreshTargets();
                } catch (Exception e) {
                    showError("Installation failed", unwrap(e));
                    setBusy(false, null);
                }
            }
        }.execute();
    }

    private void openFolder(Path dir) {
        try {
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(dir.toFile());
        } catch (Exception e) {
            showError("Could not open folder", e);
        }
    }

    private void setBusy(boolean busy, String text) {
        progress.setVisible(busy);
        progress.setIndeterminate(busy);
        refreshButton.setEnabled(!busy);
        browseButton.setEnabled(!busy);
        installButton.setEnabled(!busy && table.getSelectedRow() >= 0);
        if (text != null) status.setText(text);
    }

    private void updateInstallButton() {
        int viewRow = table.getSelectedRow();
        boolean selected = viewRow >= 0;
        installButton.setEnabled(!progress.isVisible() && selected);
        if (selected) {
            int row = table.convertRowIndexToModel(viewRow);
            installButton.setText(model.get(row).installed() ? "Repair NilLoader" : "Install NilLoader");
        } else {
            installButton.setText("Install NilLoader");
        }
    }

    private void showError(String title, Throwable e) {
        status.setText(title + ": " + e.getMessage());
        JTextArea area = new JTextArea(e.toString());
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBackground(UIManager.getColor("Panel.background"));
        JOptionPane.showMessageDialog(this, area, title, JOptionPane.ERROR_MESSAGE);
    }

    private static Throwable unwrap(Throwable e) {
        Throwable cur = e;
        while (cur.getCause() != null && (cur instanceof java.util.concurrent.ExecutionException)) cur = cur.getCause();
        return cur;
    }

    private static final class TargetTableModel extends AbstractTableModel {
        private final String[] columns = {"Name", "Launcher", "Minecraft", "Status", "Game directory"};
        private final List<InstallTarget> targets = new ArrayList<>();

        void setTargets(List<InstallTarget> list) {
            targets.clear();
            targets.addAll(list);
            fireTableDataChanged();
        }

        void addTargets(List<InstallTarget> list) {
            for (InstallTarget t : list) {
                boolean exists = targets.stream().anyMatch(existing -> same(existing, t));
                if (!exists) targets.add(t);
            }
            fireTableDataChanged();
        }

        InstallTarget get(int row) { return targets.get(row); }

        @Override public int getRowCount() { return targets.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int column) { return columns[column]; }

        @Override public Object getValueAt(int rowIndex, int columnIndex) {
            InstallTarget t = targets.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> t.displayName();
                case 1 -> t.launcherType().displayName();
                case 2 -> t.minecraftVersion();
                case 3 -> t.installed() ? "Installed" : "Not installed";
                case 4 -> t.gameDir();
                default -> "";
            };
        }

        private static boolean same(InstallTarget a, InstallTarget b) {
            return java.util.Objects.equals(a.profileId(), b.profileId())
                    && java.util.Objects.equals(a.instanceRoot(), b.instanceRoot())
                    && java.util.Objects.equals(a.launcherRoot(), b.launcherRoot());
        }
    }
}
