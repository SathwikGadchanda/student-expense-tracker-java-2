package edu.campus;

import java.time.LocalDate;
import java.util.Objects;

public record Item(long id, Kind kind, String title, Category category, String location,
                   LocalDate date, String description, Status status) {
    public enum Kind { LOST, FOUND }
    public enum Status { OPEN, CLAIMED, RETURNED }
    public enum Category {
        ELECTRONICS, DOCUMENTS, ACCESSORIES, BOOKS, OTHER;
        @Override public String toString() {
            return name().charAt(0) + name().substring(1).toLowerCase(java.util.Locale.ROOT);
        }
    }

    public record Draft(Kind kind, String title, Category category, String location,
                        LocalDate date, String description, byte[] photo) {
        public Draft {
            Objects.requireNonNull(kind);
            Objects.requireNonNull(category);
            Objects.requireNonNull(date);
            title = required(title, 100, "Item name");
            location = required(location, 100, "Location");
            description = required(description, 500, "Description");
            if (date.isAfter(LocalDate.now())) throw new IllegalArgumentException("Date cannot be in the future.");
            if (photo != null && photo.length > Photo.MAX_BYTES) throw new IllegalArgumentException("Photo must be 4 MB or smaller.");
            photo = photo == null ? null : photo.clone();
        }

        @Override public byte[] photo() { return photo == null ? null : photo.clone(); }
    }

    public record Claim(String claimant, String evidence, String recordedAt, String returnedAt) {
        public Claim {
            claimant = required(claimant, 80, "Claimant name");
            evidence = required(evidence, 300, "Verification notes");
        }
    }

    public record Details(Item item, byte[] photo, Claim claim) {}

    static String required(String value, int max, String field) {
        String cleaned = value == null ? "" : value.strip();
        if (cleaned.isEmpty() || cleaned.length() > max)
            throw new IllegalArgumentException(field + " must contain 1 to " + max + " characters.");
        return cleaned;
    }
}
