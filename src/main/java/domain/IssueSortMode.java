package domain;

import java.util.Comparator;

/**
 * Supported ordering strategies for issue lists.
 *
 * <p>Each mode appends a stable tie-breaker by issue id to keep sorting deterministic
 * across repeated renders.
 */
public enum IssueSortMode {
    UPDATED_DESC(Comparator.comparing(Issue::updatedAt, Comparator.reverseOrder())),
    CREATED_DESC(Comparator.comparing(Issue::createdAt, Comparator.reverseOrder()));

    private final Comparator<Issue> comparator;

    IssueSortMode(Comparator<Issue> primaryComparator) {
        this.comparator = primaryComparator.thenComparing(issue -> issue.id().value());
    }

    /**
     * @return comparator implementing this sort mode
     */
    public Comparator<Issue> comparator() {
        return comparator;
    }
}
