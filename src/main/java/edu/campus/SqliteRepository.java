package edu.campus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import static edu.campus.Item.*;

public final class SqliteRepository implements ItemRepository {
    private final Path file;

    public SqliteRepository(Path file) { this.file = file.toAbsolutePath().normalize(); }

    public void initialize() throws IOException, SQLException {
        Files.createDirectories(file.getParent());
        try (Connection c = connect(); Statement s = c.createStatement()) {
            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    kind TEXT NOT NULL CHECK (kind IN ('LOST','FOUND')),
                    title TEXT NOT NULL, category TEXT NOT NULL, location TEXT NOT NULL,
                    item_date TEXT NOT NULL, description TEXT NOT NULL, photo BLOB,
                    status TEXT NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN','CLAIMED','RETURNED'))
                )
                """);
            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS claims (
                    item_id INTEGER PRIMARY KEY REFERENCES items(id),
                    claimant TEXT NOT NULL, evidence TEXT NOT NULL,
                    recorded_at TEXT NOT NULL, returned_at TEXT
                )
                """);
        }
    }

    private Connection connect() throws SQLException {
        Connection c = DriverManager.getConnection("jdbc:sqlite:" + file);
        try (Statement s = c.createStatement()) {
            s.execute("PRAGMA foreign_keys=ON");
            s.execute("PRAGMA busy_timeout=3000");
            return c;
        } catch (SQLException e) {
            c.close();
            throw e;
        }
    }

    @Override public long add(Draft draft) throws SQLException {
        try (Connection c = connect(); PreparedStatement s = c.prepareStatement("""
            INSERT INTO items(kind,title,category,location,item_date,description,photo)
            VALUES(?,?,?,?,?,?,?) RETURNING id
            """)) {
            bind(s, draft);
            try (ResultSet rows = s.executeQuery()) {
                if (!rows.next()) throw new SQLException("No item ID returned.");
                return rows.getLong(1);
            }
        }
    }

    @Override public void edit(long id, Draft draft) throws SQLException {
        try (Connection c = connect(); PreparedStatement s = c.prepareStatement("""
            UPDATE items SET kind=?,title=?,category=?,location=?,item_date=?,description=?,photo=?
            WHERE id=? AND status='OPEN'
            """)) {
            bind(s, draft);
            s.setLong(8, id);
            requireChange(s, "Only open reports can be edited.");
        }
    }

    private static void bind(PreparedStatement s, Draft draft) throws SQLException {
        s.setString(1, draft.kind().name());
        s.setString(2, draft.title());
        s.setString(3, draft.category().name());
        s.setString(4, draft.location());
        s.setString(5, draft.date().toString());
        s.setString(6, draft.description());
        s.setBytes(7, draft.photo());
    }

    private static Item map(ResultSet r) throws SQLException {
        return new Item(r.getLong("id"), Kind.valueOf(r.getString("kind")), r.getString("title"),
                Category.valueOf(r.getString("category")), r.getString("location"),
                LocalDate.parse(r.getString("item_date")), r.getString("description"),
                Status.valueOf(r.getString("status")));
    }

    @Override public List<Item> list() throws SQLException {
        try (Connection c = connect(); PreparedStatement s = c.prepareStatement("""
            SELECT id,kind,title,category,location,item_date,description,status FROM items
            ORDER BY item_date DESC,id DESC
            """); ResultSet r = s.executeQuery()) {
            List<Item> items = new ArrayList<>();
            while (r.next()) items.add(map(r));
            return List.copyOf(items);
        }
    }

    @Override public Details details(long id) throws SQLException {
        try (Connection c = connect(); PreparedStatement s = c.prepareStatement("""
            SELECT i.*,c.claimant,c.evidence,c.recorded_at,c.returned_at
            FROM items i LEFT JOIN claims c ON i.id=c.item_id WHERE i.id=?
            """)) {
            s.setLong(1, id);
            try (ResultSet r = s.executeQuery()) {
                if (!r.next()) throw new IllegalArgumentException("Item report no longer exists.");
                String claimant = r.getString("claimant");
                Claim claim = claimant == null ? null : new Claim(claimant, r.getString("evidence"),
                        r.getString("recorded_at"), r.getString("returned_at"));
                return new Details(map(r), r.getBytes("photo"), claim);
            }
        }
    }

    @Override public void claim(long id, Claim claim) throws SQLException {
        transaction(c -> {
            try (PreparedStatement s = c.prepareStatement(
                    "UPDATE items SET status='CLAIMED' WHERE id=? AND kind='FOUND' AND status='OPEN'")) {
                s.setLong(1, id);
                requireChange(s, "Only open found reports can be claimed.");
            }
            try (PreparedStatement s = c.prepareStatement(
                    "INSERT INTO claims(item_id,claimant,evidence,recorded_at) VALUES(?,?,?,?)")) {
                s.setLong(1, id);
                s.setString(2, claim.claimant());
                s.setString(3, claim.evidence());
                s.setString(4, claim.recordedAt());
                s.executeUpdate();
            }
        });
    }

    @Override public void returnItem(long id, String returnedAt) throws SQLException {
        transaction(c -> {
            try (PreparedStatement s = c.prepareStatement(
                    "UPDATE items SET status='RETURNED' WHERE id=? AND status='CLAIMED'")) {
                s.setLong(1, id);
                requireChange(s, "Record a claim before confirming a return.");
            }
            try (PreparedStatement s = c.prepareStatement(
                    "UPDATE claims SET returned_at=? WHERE item_id=? AND returned_at IS NULL")) {
                s.setString(1, returnedAt);
                s.setLong(2, id);
                requireChange(s, "No active claim exists for this item.");
            }
        });
    }

    private static void requireChange(PreparedStatement statement, String message) throws SQLException {
        if (statement.executeUpdate() != 1) throw new IllegalArgumentException(message);
    }

    @FunctionalInterface private interface Work { void run(Connection connection) throws SQLException; }

    private void transaction(Work work) throws SQLException {
        try (Connection c = connect()) {
            c.setAutoCommit(false);
            try {
                work.run(c);
                c.commit();
            } catch (SQLException | RuntimeException e) {
                try { c.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                throw e;
            }
        }
    }
}
