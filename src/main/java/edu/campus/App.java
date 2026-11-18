package edu.campus;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public final class App {
    private App() {}

    public static void main(String[] arguments) {
        List<String> args = Arrays.asList(arguments);
        boolean demo = args.contains("--demo");
        try {
            String defaultFile = Path.of(System.getProperty("user.home"), ".campus-lost-found", "campus.db").toString();
            Path file = demo ? Files.createTempDirectory("campus-demo-").resolve("demo.db")
                    : Path.of(System.getProperty("campus.db", defaultFile));
            SqliteRepository repository = new SqliteRepository(file);
            repository.initialize();
            if (demo) file.toFile().deleteOnExit();
            CampusService service = new CampusService(repository);
            if (demo) service.seedDemo();
            if (args.contains("--check-db")) {
                System.out.println("DATABASE_OK reports=" + service.all().size());
                return;
            }
            SwingUtilities.invokeLater(() -> new CampusFrame(service, demo).setVisible(true));
        } catch (Exception error) {
            if (args.contains("--check-db")) {
                System.err.println(error.getMessage());
            } else {
                JOptionPane.showMessageDialog(null, error.getMessage(), "Campus Lost and Found", JOptionPane.ERROR_MESSAGE);
            }
            System.exit(1);
        }
    }
}
