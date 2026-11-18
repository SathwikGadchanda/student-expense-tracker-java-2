package edu.campus;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import static edu.campus.Item.*;

final class ReportDialog extends JDialog {
    @FunctionalInterface interface Saver { void save(Draft draft) throws Exception; }

    private final JTextField title = new JTextField(26);
    private final JTextField location = new JTextField(26);
    private final JTextField date = new JTextField(LocalDate.now().toString());
    private final JTextArea description = new JTextArea(4, 26);
    private final JComboBox<Kind> kind = new JComboBox<>(Kind.values());
    private final JComboBox<Category> category = new JComboBox<>(Category.values());
    private final JLabel photoStatus = new JLabel("No photo selected");
    private byte[] photo;

    ReportDialog(JFrame owner, Details current, Saver saver) {
        super(owner, current == null ? "New report" : "Edit report", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBorder(new EmptyBorder(16, 18, 16, 18));
        root.add(CampusFrame.label(current == null ? "Report a lost or found item" : "Edit item report",
                21, CampusFrame.INK, true), BorderLayout.NORTH);
        JPanel fields = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 4, 5, 4);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        row(fields, c, 0, "Type", kind);
        row(fields, c, 1, "Item name", title);
        row(fields, c, 2, "Category", category);
        row(fields, c, 3, "Location", location);
        row(fields, c, 4, "Date (YYYY-MM-DD)", date);
        row(fields, c, 5, "Description", new JScrollPane(description));
        JPanel attachment = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JButton choose = new JButton("Choose photo");
        JButton remove = new JButton("Remove");
        choose.addActionListener(e -> choosePhoto());
        remove.addActionListener(e -> {
            photo = null;
            photoStatus.setText("No photo selected");
        });
        attachment.add(choose);
        attachment.add(remove);
        row(fields, c, 6, "Optional JPEG / PNG", attachment);
        row(fields, c, 7, "", photoStatus);
        root.add(fields, BorderLayout.CENTER);
        if (current != null) load(current);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancel = CampusFrame.button("Cancel", false);
        JButton save = CampusFrame.button("Save report", true);
        cancel.addActionListener(e -> dispose());
        save.addActionListener(e -> {
            try {
                Draft draft = new Draft((Kind) kind.getSelectedItem(), title.getText(),
                        (Category) category.getSelectedItem(), location.getText(),
                        LocalDate.parse(date.getText().strip()), description.getText(), photo);
                saver.save(draft);
                dispose();
            } catch (DateTimeParseException error) {
                showError("Enter a valid date using YYYY-MM-DD.");
            } catch (Exception error) {
                showError(error.getMessage());
            }
        });
        actions.add(cancel);
        actions.add(save);
        root.add(actions, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(save);
        setContentPane(root);
        pack();
        setMinimumSize(new Dimension(545, getHeight()));
        setSize(Math.max(565, getWidth()), getHeight());
        setLocationRelativeTo(owner);
    }

    private static void row(JPanel panel, GridBagConstraints c, int y, String label, JComponent field) {
        c.gridy = y;
        c.gridx = 0;
        c.weightx = 0;
        JLabel caption = new JLabel(label);
        caption.setLabelFor(field);
        panel.add(caption, c);
        c.gridx = 1;
        c.weightx = 1;
        panel.add(field, c);
    }

    private void choosePhoto() {
        JFileChooser picker = new JFileChooser();
        picker.setFileFilter(new FileNameExtensionFilter("JPEG or PNG photos", "jpg", "jpeg", "png"));
        if (picker.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            photo = Photo.load(picker.getSelectedFile().toPath());
            photoStatus.setText(picker.getSelectedFile().getName());
        } catch (Exception error) {
            showError(error.getMessage());
        }
    }

    private void load(Details current) {
        Item item = current.item();
        kind.setSelectedItem(item.kind());
        category.setSelectedItem(item.category());
        title.setText(item.title());
        location.setText(item.location());
        date.setText(item.date().toString());
        description.setText(item.description());
        photo = current.photo();
        if (photo != null) photoStatus.setText("Existing photo attached");
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Check report details", JOptionPane.ERROR_MESSAGE);
    }
}
