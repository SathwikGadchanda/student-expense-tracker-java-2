package edu.campus;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.List;
import java.util.Locale;
import static edu.campus.Item.*;

final class CampusFrame extends JFrame {
    static final Color INK = new Color(23, 41, 53);
    private static final Color TEAL = new Color(0, 99, 95);
    private static final Color MUTED = new Color(73, 99, 110);
    private static final Color BG = new Color(238, 246, 246);
    private static final Color LINE = new Color(203, 221, 223);

    private final CampusService service;
    private final ReportTable model = new ReportTable();
    private final JTable table = new JTable(model);
    private final JTextField search = new JTextField(13);
    private final JComboBox<Object> kinds = filter("All types", Kind.values());
    private final JComboBox<Object> categories = filter("All categories", Category.values());
    private final JComboBox<Object> statuses = filter("All statuses", Status.values());
    private final JLabel counts = label("", 14, TEAL, true);
    private final JLabel footer = label(" ", 13, MUTED, false);
    private final JLabel itemTitle = label("Select a report", 21, INK, true);
    private final JLabel itemMeta = label("", 13, TEAL, true);
    private final JLabel photo = label("", 13, MUTED, false);
    private final JTextArea description = new JTextArea();
    private final JButton edit = button("Edit report", false);
    private final JButton claim = button("Record claim", false);
    private final JButton returned = button("Confirm return", true);
    private List<Item> items = List.of();
    private Details selection;

    CampusFrame(CampusService service, boolean demo) {
        super("Campus Lost and Found System");
        this.service = service;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1150, 755);
        setMinimumSize(new Dimension(1010, 675));
        setLocationRelativeTo(null);
        JPanel root = new JPanel(new BorderLayout(0, 18));
        root.setBackground(BG);
        root.setBorder(new EmptyBorder(22, 24, 15, 24));
        root.add(header(demo), BorderLayout.NORTH);
        JPanel center = new JPanel(new BorderLayout(16, 0));
        center.setOpaque(false);
        center.add(board(), BorderLayout.CENTER);
        center.add(detailsPanel(), BorderLayout.EAST);
        root.add(center, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);
        setContentPane(root);
        edit.addActionListener(e -> showForm(selection));
        claim.addActionListener(e -> showClaim());
        returned.addActionListener(e -> confirmReturn());
        run(() -> reload(-1, "Ready"));
    }

    private JPanel header(boolean demo) {
        JPanel outer = new JPanel(new BorderLayout(0, 13));
        outer.setOpaque(false);
        JPanel bar = new JPanel(new BorderLayout(12, 5));
        bar.setBackground(INK);
        bar.setBorder(new EmptyBorder(17, 19, 17, 15));
        JPanel words = new JPanel(new GridLayout(2, 1, 0, 3));
        words.setOpaque(false);
        words.add(label("Campus Lost and Found", 27, Color.WHITE, true));
        words.add(label(demo ? "Demo workspace / Sample records" : "Campus desk / Local records",
                13, new Color(170, 213, 215), false));
        bar.add(words, BorderLayout.CENTER);
        JButton add = button("+ New report", true);
        add.addActionListener(e -> showForm(null));
        bar.add(add, BorderLayout.EAST);
        outer.add(bar, BorderLayout.NORTH);
        outer.add(counts, BorderLayout.SOUTH);
        return outer;
    }

    private JPanel board() {
        JPanel panel = surface(new BorderLayout(0, 12));
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        filters.setOpaque(false);
        JLabel find = label("Search", 13, INK, true);
        find.setLabelFor(search);
        filters.add(find);
        filters.add(search);
        for (JComboBox<Object> filter : List.of(kinds, categories, statuses)) {
            filter.setPreferredSize(new Dimension(110, 30));
            filter.addActionListener(e -> applyFilter());
            filters.add(filter);
        }
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) { applyFilter(); }
            public void removeUpdate(DocumentEvent event) { applyFilter(); }
            public void changedUpdate(DocumentEvent event) { applyFilter(); }
        });
        panel.add(filters, BorderLayout.NORTH);
        table.setRowHeight(39);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setForeground(INK);
        table.setSelectionBackground(new Color(216, 242, 236));
        table.setSelectionForeground(INK);
        table.setShowVerticalLines(false);
        table.setGridColor(LINE);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setPreferredSize(new Dimension(0, 34));
        table.getColumnModel().getColumn(0).setPreferredWidth(175);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showSelection();
        });
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(LINE));
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel detailsPanel() {
        JPanel panel = surface(new BorderLayout(0, 10));
        panel.setPreferredSize(new Dimension(325, 0));
        JPanel meta = new JPanel(new GridLayout(2, 1, 0, 4));
        meta.setOpaque(false);
        itemTitle.putClientProperty("html.disable", true);
        meta.add(itemTitle);
        meta.add(itemMeta);
        panel.add(meta, BorderLayout.NORTH);
        JPanel main = new JPanel(new BorderLayout(0, 8));
        photo.setHorizontalAlignment(SwingConstants.CENTER);
        photo.setPreferredSize(new Dimension(270, 135));
        photo.setOpaque(true);
        photo.setBackground(BG);
        main.add(photo, BorderLayout.NORTH);
        description.setEditable(false);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        main.add(new JScrollPane(description), BorderLayout.CENTER);
        panel.add(main, BorderLayout.CENTER);
        JPanel actions = new JPanel(new GridLayout(0, 1, 0, 6));
        actions.add(edit);
        actions.add(claim);
        actions.add(returned);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private void reload(long select, String message) throws Exception {
        items = service.all();
        long lost = items.stream().filter(i -> i.kind() == Kind.LOST).count();
        long found = items.stream().filter(i -> i.kind() == Kind.FOUND && i.status() == Status.OPEN).count();
        long completed = items.stream().filter(i -> i.status() == Status.RETURNED).count();
        counts.setText(items.size() + " reports    /    " + lost + " lost    /    " + found
                + " found awaiting claim    /    " + completed + " returned");
        applyFilter();
        for (int i = 0; i < model.items.size(); i++) {
            if (model.items.get(i).id() == select) table.setRowSelectionInterval(i, i);
        }
        footer.setText(message + "   /   " + model.items.size() + " visible");
    }

    private void applyFilter() {
        table.clearSelection();
        model.items = service.matching(items, search.getText(), value(kinds, Kind.class),
                value(categories, Category.class), value(statuses, Status.class));
        model.fireTableDataChanged();
        showSelection();
        footer.setText(model.items.size() + " visible");
    }

    private void showSelection() {
        selection = null;
        int row = table.getSelectedRow();
        if (row >= 0 && row < model.items.size()) run(() -> selection = service.details(model.items.get(row).id()));
        photo.setIcon(null);
        photo.setText("No photo attached");
        edit.setEnabled(false);
        claim.setEnabled(false);
        returned.setEnabled(false);
        itemTitle.setText("Select a report");
        itemMeta.setText(" ");
        description.setText("Select a row to view the report, its location and claim details.");
        if (selection == null) return;

        Item item = selection.item();
        itemTitle.setText(item.title());
        itemMeta.setText("#" + item.id() + "   /   " + display(item.kind()) + "   /   " + display(item.status()));
        String full = "Location: " + item.location() + "\nDate: " + item.date() + "\n\n" + item.description();
        Claim record = selection.claim();
        if (record != null) {
            full += "\n\nClaimant: " + record.claimant() + "\nVerification: " + record.evidence();
            full += "\nClaim recorded (UTC): " + record.recordedAt();
            if (record.returnedAt() != null) full += "\nReturned (UTC): " + record.returnedAt();
        }
        description.setText(full);
        description.setCaretPosition(0);
        if (selection.photo() != null) run(() -> {
            var picture = Photo.decode(selection.photo());
            double scale = Math.min(270.0 / picture.getWidth(), 132.0 / picture.getHeight());
            photo.setText("");
            photo.setIcon(new ImageIcon(picture.getScaledInstance(Math.max(1, (int) (picture.getWidth() * scale)),
                    Math.max(1, (int) (picture.getHeight() * scale)), Image.SCALE_SMOOTH)));
        });
        edit.setEnabled(item.status() == Status.OPEN);
        claim.setEnabled(item.kind() == Kind.FOUND && item.status() == Status.OPEN);
        returned.setEnabled(item.status() == Status.CLAIMED);
    }

    private void showForm(Details current) {
        ReportDialog dialog = new ReportDialog(this, current, draft -> {
            long id = current == null ? service.add(draft) : current.item().id();
            if (current != null) service.edit(id, draft);
            search.setText("");
            kinds.setSelectedIndex(0);
            categories.setSelectedIndex(0);
            statuses.setSelectedIndex(0);
            reload(id, current == null ? "Report saved" : "Report updated");
        });
        dialog.setVisible(true);
    }

    private void showClaim() {
        if (selection == null) return;
        long id = selection.item().id();
        JTextField name = new JTextField(26);
        JTextField evidence = new JTextField(26);
        JPanel form = new JPanel(new GridLayout(0, 1, 0, 6));
        form.add(new JLabel("Claimant name"));
        form.add(name);
        form.add(new JLabel("Ownership verification notes"));
        form.add(evidence);
        while (JOptionPane.showConfirmDialog(this, form, "Record claim", JOptionPane.OK_CANCEL_OPTION)
                == JOptionPane.OK_OPTION) {
            if (run(() -> {
                service.recordClaim(id, name.getText(), evidence.getText());
                reload(id, "Claim recorded");
            })) return;
        }
    }

    private void confirmReturn() {
        if (selection == null) return;
        long id = selection.item().id();
        if (JOptionPane.showConfirmDialog(this, "Confirm that the item was handed to the verified claimant?",
                "Confirm return", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            run(() -> {
                service.markReturned(id);
                reload(id, "Return logged");
            });
        }
    }

    @FunctionalInterface private interface Job { void run() throws Exception; }

    private boolean run(Job job) {
        try {
            job.run();
            return true;
        } catch (Exception error) {
            JOptionPane.showMessageDialog(this, error.getMessage(), "Campus Lost and Found", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private static JPanel surface(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE),
                new EmptyBorder(13, 13, 13, 13)));
        return panel;
    }

    static JLabel label(String text, int size, Color color, boolean bold) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", bold ? Font.BOLD : Font.PLAIN, size));
        label.setForeground(color);
        return label;
    }

    static JButton button(String text, boolean primary) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setBackground(primary ? TEAL : BG);
        button.setForeground(primary ? Color.WHITE : TEAL);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary ? TEAL : LINE), new EmptyBorder(8, 13, 8, 13)));
        return button;
    }

    private static JComboBox<Object> filter(String first, Object[] values) {
        JComboBox<Object> combo = new JComboBox<>();
        combo.addItem(first);
        for (Object value : values) combo.addItem(value);
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean selected, boolean focused) {
                return super.getListCellRendererComponent(list, value == null ? "" : display(value),
                        index, selected, focused);
            }
        });
        combo.getAccessibleContext().setAccessibleName(first);
        return combo;
    }

    private static <T> T value(JComboBox<Object> combo, Class<T> type) {
        Object selected = combo.getSelectedItem();
        return type.isInstance(selected) ? type.cast(selected) : null;
    }

    private static String display(Object value) {
        String text = String.valueOf(value);
        return value instanceof Enum<?> ? text.charAt(0) + text.substring(1).toLowerCase(Locale.ROOT) : text;
    }

    private static final class ReportTable extends AbstractTableModel {
        private final String[] columns = {"Item", "Type", "Category", "Status", "Date"};
        private List<Item> items = List.of();

        public int getRowCount() { return items.size(); }
        public int getColumnCount() { return columns.length; }
        public String getColumnName(int index) { return columns[index]; }
        public Object getValueAt(int row, int column) {
            Item item = items.get(row);
            return switch (column) {
                case 0 -> item.title();
                case 1 -> display(item.kind());
                case 2 -> display(item.category());
                case 3 -> display(item.status());
                case 4 -> item.date().toString();
                default -> throw new IndexOutOfBoundsException(column);
            };
        }
    }
}
