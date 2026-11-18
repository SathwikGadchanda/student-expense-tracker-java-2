package edu.campus;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import static edu.campus.Item.*;

public final class DemoWalkthrough {
    private static final String ITEM = "Blue calculator";
    private static final String DESCRIPTION = "Scientific calculator in a blue slide cover.";
    private static Path output;
    private static CampusFrame frame;
    private static Throwable failure;

    public static void main(String[] args) throws Exception {
        output = Files.createDirectories(Path.of(args[0]));
        Path file = Files.createTempDirectory("campus-pbl-demo").resolve("demo.db");
        SqliteRepository repository = new SqliteRepository(file);
        repository.initialize();
        CampusService service = new CampusService(repository);
        service.seedDemo();
        SwingUtilities.invokeAndWait(() -> {
            try {
                frame = new CampusFrame(service, true);
                frame.setVisible(true);
                snapshot("01-board", null);
                modal("New report", () -> button(frame, "+ New report").doClick(), dialog -> {
                    entry(dialog, "Item name").setText(ITEM);
                    entry(dialog, "Location").setText("Lecture hall 2");
                    descendants(dialog, JTextArea.class).get(0).setText(DESCRIPTION);
                    JComboBox<?> type = descendants(dialog, JComboBox.class).stream()
                            .filter(box -> box.getItemCount() > 0 && box.getItemAt(0) instanceof Kind)
                            .findFirst().orElseThrow();
                    type.setSelectedItem(Kind.FOUND);
                    snapshot("02-report-form", dialog);
                    button(dialog, "Save report").doClick();
                });
                snapshot("03-saved", null);
                descendants(frame, JTextField.class).get(0).setText("calculator");
                JTable table = descendants(frame, JTable.class).get(0);
                if (table.getRowCount() != 1 || !ITEM.equals(table.getValueAt(0, 0)))
                    throw new AssertionError("Search did not isolate the new report");
                table.setRowSelectionInterval(0, 0);
                snapshot("04-filtered", null);
                modal("Record claim", () -> button(frame, "Record claim").doClick(), dialog -> {
                    List<JTextField> fields = descendants(dialog, JTextField.class);
                    fields.get(0).setText("Demo Student");
                    fields.get(1).setText("Described the scratched slide cover.");
                    snapshot("05-claim-form", dialog);
                    button(dialog, "OK").doClick();
                });
                snapshot("06-claimed", null);
                modal("Confirm return", () -> button(frame, "Confirm return").doClick(),
                        dialog -> button(dialog, "Yes").doClick());
                snapshot("07-returned", null);
            } catch (Throwable error) {
                failure = error;
            } finally {
                if (frame != null) frame.dispose();
            }
        });
        if (failure != null) throw new AssertionError(failure);

        SqliteRepository reopened = new SqliteRepository(file);
        reopened.initialize();
        Item saved = reopened.list().stream().filter(i -> i.title().equals(ITEM)).findFirst().orElseThrow();
        if (saved.status() != Status.RETURNED || reopened.details(saved.id()).claim().returnedAt() == null)
            throw new AssertionError("Return did not persist");
        System.out.println("DEMO_OK created searched claimed returned reopened");
        Files.deleteIfExists(file);
        Files.deleteIfExists(file.getParent());
    }

    private static JTextComponent entry(Container dialog, String title) {
        return descendants(dialog, JLabel.class).stream().filter(l -> title.equals(l.getText()))
                .map(l -> (JTextComponent) l.getLabelFor()).findFirst().orElseThrow();
    }

    @FunctionalInterface private interface DialogAction { void run(JDialog dialog) throws Exception; }

    private static void modal(String title, Runnable open, DialogAction action) throws Exception {
        Timer timer = new Timer(100, event -> {
            for (Window window : Window.getWindows()) {
                if (window instanceof JDialog dialog && dialog.isShowing() && dialog.getTitle().equals(title)) {
                    ((Timer) event.getSource()).stop();
                    try {
                        action.run(dialog);
                    } catch (Exception | AssertionError error) {
                        failure = error;
                        dialog.dispose();
                    }
                    return;
                }
            }
        });
        timer.start();
        try { open.run(); } finally { timer.stop(); }
        if (failure != null) throw new AssertionError(failure);
    }

    private static void snapshot(String name, JDialog dialog) throws Exception {
        Container content = frame.getContentPane();
        BufferedImage image = new BufferedImage(content.getWidth(), content.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        content.printAll(g);
        if (dialog != null) {
            Container overlay = dialog.getContentPane();
            g.setColor(new Color(10, 32, 34, 80));
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
            g.translate((image.getWidth() - overlay.getWidth()) / 2, (image.getHeight() - overlay.getHeight()) / 2);
            overlay.printAll(g);
        }
        g.dispose();
        ImageIO.write(image, "png", output.resolve(name + ".png").toFile());
    }

    private static JButton button(Container root, String text) {
        return descendants(root, JButton.class).stream().filter(b -> text.equals(b.getText())).findFirst().orElseThrow();
    }

    private static <T extends Component> List<T> descendants(Container root, Class<T> type) {
        List<T> components = new ArrayList<>();
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) components.add(type.cast(child));
            if (child instanceof Container container) components.addAll(descendants(container, type));
        }
        return components;
    }
}
