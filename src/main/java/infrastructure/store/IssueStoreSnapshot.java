package infrastructure.store;

import domain.Issue;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Immutable snapshot of issue-store data and metadata at a point in time.
 */
public record IssueStoreSnapshot(int schemaVersion, String storeRevision, List<Issue> issues) {
    public IssueStoreSnapshot {
        if (schemaVersion <= 0) {
            throw new IllegalArgumentException("schemaVersion must be positive");
        }
        Objects.requireNonNull(issues, "issues cannot be null");
        issues = List.copyOf(new ArrayList<>(issues));
    }
}
