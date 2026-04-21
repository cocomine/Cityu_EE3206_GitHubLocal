package ui.controller.issue;

import application.IssueQuery;
import domain.IssuePriority;
import domain.IssueSortMode;
import domain.IssueStatus;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Maps filter input values from UI controls into an IssueQuery object.
 * Parses enums, normalizes text input, and converts label text into a list.
 */
public final class IssueFilterMapper {
    private IssueFilterMapper() {}

    public static IssueQuery toQuery(
            String searchText,
            String statusValue,
            String assigneeText,
            String priorityValue,
            String labelsRaw,
            String sortValue
    ) {
        String text = normalize(searchText);
        String assignee = normalize(assigneeText);
        IssueStatus status = parseIssueStatus(statusValue);
        IssuePriority priority = parseIssuePriority(priorityValue);
        IssueSortMode sortMode = parseSortMode(sortValue);
        List<String> labels = parseLabels(labelsRaw);

        return new IssueQuery(text, status, assignee, priority, labels, sortMode);
    }

    private static IssueStatus parseIssueStatus(String value) {
        if (value == null || value.equals("ALL")) return null;
        return IssueStatus.valueOf(value);
    }

    private static IssuePriority parseIssuePriority(String value) {
        if (value == null || value.equals("ALL")) return null;
        return IssuePriority.valueOf(value);
    }

    private static IssueSortMode parseSortMode(String value) {
        if (value == null || value.isBlank()) return IssueSortMode.UPDATED_DESC;
        return IssueSortMode.valueOf(value);
    }

    private static List<String> parseLabels(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private static String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
