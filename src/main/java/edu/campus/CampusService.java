package edu.campus;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import static edu.campus.Item.*;

public final class CampusService {
    private final ItemRepository repository;

    public CampusService(ItemRepository repository) { this.repository = repository; }

    public long add(Draft draft) throws IOException, SQLException {
        validatePhoto(draft);
        return repository.add(draft);
    }

    public void edit(long id, Draft draft) throws IOException, SQLException {
        validatePhoto(draft);
        repository.edit(id, draft);
    }

    private static void validatePhoto(Draft draft) throws IOException {
        if (draft.photo() != null) Photo.decode(draft.photo());
    }

    public List<Item> all() throws SQLException { return repository.list(); }
    public Details details(long id) throws SQLException { return repository.details(id); }

    public List<Item> matching(List<Item> items, String search, Kind kind, Category category, Status status) {
        String query = search.strip().toLowerCase(Locale.ROOT);
        return items.stream()
                .filter(item -> kind == null || kind == item.kind())
                .filter(item -> category == null || category == item.category())
                .filter(item -> status == null || status == item.status())
                .filter(item -> query.isEmpty() || (item.title() + " " + item.location() + " " + item.description())
                        .toLowerCase(Locale.ROOT).contains(query))
                .toList();
    }

    public void recordClaim(long id, String claimant, String evidence) throws SQLException {
        repository.claim(id, new Claim(claimant, evidence, Instant.now().toString(), null));
    }

    public void markReturned(long id) throws SQLException {
        repository.returnItem(id, Instant.now().toString());
    }

    public void seedDemo() throws IOException, SQLException {
        LocalDate today = LocalDate.now();
        add(new Draft(Kind.LOST, "Navy umbrella", Category.ACCESSORIES, "Main gate", today.minusDays(2),
                "Folding umbrella with a wooden handle.", null));
        add(new Draft(Kind.LOST, "Engineering notebook", Category.BOOKS, "Lecture hall 3", today.minusDays(1),
                "Blue notebook with handwritten circuit diagrams.", null));
        add(new Draft(Kind.FOUND, "ID card holder", Category.DOCUMENTS, "Library counter", today.minusDays(1),
                "Clear plastic sleeve. Held at the library counter.", null));
        add(new Draft(Kind.FOUND, "Black headphones", Category.ELECTRONICS, "Computer lab", today,
                "Wireless over-ear headphones in a soft grey case.", null));
        add(new Draft(Kind.FOUND, "Green water bottle", Category.ACCESSORIES, "Sports block", today,
                "Stainless-steel bottle with a green lid.", null));
    }
}
