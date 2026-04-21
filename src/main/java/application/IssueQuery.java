package application;

import domain.IssuePriority;
import domain.IssueSortMode;
import domain.IssueStatus;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Immutable query object for issue filtering and sorting.
 *
 * <p>Inputs are normalized at construction time so matching logic can assume
 * canonical values (trimmed text, canonical status aliases, lowercase labels).
 */
public record IssueQuery(
        String text,
        IssueStatus status,
        String assignee,
        IssuePriority priority,
        List<String> labels,
        IssueSortMode sortMode
) {
    public IssueQuery() {
        this(null, null, null, null, List.of(), IssueSortMode.UPDATED_DESC);
    }

    /**
     * Canonical constructor that normalizes optional user input into a stable shape.
     */
    public IssueQuery {
        text = normalizeNullable(text);
        status = status == null ? null : status.canonical();
        assignee = normalizeNullable(assignee);
        labels = normalizeLabels(labels);
        sortMode = sortMode == null ? IssueSortMode.UPDATED_DESC : sortMode;
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static List<String> normalizeLabels(List<String> labels) {
        if (labels == null || labels.isEmpty()) {
            return List.of();
        }

        Set<String> normalized = new LinkedHashSet<>();
        for (String label : labels) {
            String value = normalizeNullable(label);
            if (value != null) {
                // Lowercase labels so filtering behaves consistently across UI/user input variants.
                normalized.add(value.toLowerCase(Locale.ROOT));
            }
        }
        return List.copyOf(normalized);
    }
}
