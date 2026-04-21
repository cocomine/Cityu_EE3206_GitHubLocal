package application;

import domain.Issue;
import domain.IssueId;
import domain.IssuePriority;
import domain.IssueStatus;
import domain.LocalRepo;
import org.junit.jupiter.api.Test;

import infrastructure.store.IssueStore;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IssueServiceQueryTest {
    private final LocalRepo repo = new LocalRepo(Path.of("repo"));

    @Test
    void queryShouldApplyCombinedFilters() {
        Issue matching = issue(
                "ISS-2",
                "Search API",
                "Need additive backend filter support",
                IssueStatus.TODO,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T04:00:00Z"),
                "Bob",
                IssuePriority.HIGH,
                List.of("Backend", "Bug")
        );
        Issue wrongStatus = issue(
                "ISS-1",
                "Search API",
                "Need additive backend filter support",
                IssueStatus.IN_PROGRESS,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T05:00:00Z"),
                "Bob",
                IssuePriority.HIGH,
                List.of("backend", "bug")
        );
        Issue wrongLabel = issue(
                "ISS-3",
                "Search API",
                "Need additive backend filter support",
                IssueStatus.TODO,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T03:00:00Z"),
                "Bob",
                IssuePriority.HIGH,
                List.of("backend")
        );

        IssueService service = new IssueService(new InMemoryIssueStore(List.of(matching, wrongStatus, wrongLabel)));

        List<Issue> result = service.query(
                repo,
                new IssueQuery(" search ", IssueStatus.OPEN, "  bob ", IssuePriority.HIGH, List.of("BUG", "backend"), null)
        );

        assertEquals(List.of("ISS-2"), result.stream().map(issue -> issue.id().value()).toList());
    }

    @Test
    void queryShouldMatchTextAndAssigneeCaseInsensitive() {
        Issue matching = issue(
                "ISS-1",
                "Release Prep",
                "Triage blocker before shipping",
                IssueStatus.IN_PROGRESS,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T02:00:00Z"),
                "SaM",
                IssuePriority.MEDIUM,
                List.of("ops")
        );
        Issue nonMatching = issue(
                "ISS-2",
                "Release Prep",
                "Draft changelog",
                IssueStatus.IN_PROGRESS,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T01:00:00Z"),
                "alex",
                IssuePriority.MEDIUM,
                List.of("ops")
        );

        IssueService service = new IssueService(new InMemoryIssueStore(List.of(nonMatching, matching)));

        List<Issue> result = service.query(repo, new IssueQuery("TRIAGE", null, "sam", null, List.of(), null));

        assertEquals(List.of("ISS-1"), result.stream().map(issue -> issue.id().value()).toList());
    }

    @Test
    void queryShouldUseIssueIdAsTieBreakerForEqualSortKeys() {
        Instant sameUpdatedAt = Instant.parse("2026-01-01T05:00:00Z");
        Issue second = issue("ISS-20", "Second", "same time", IssueStatus.TODO, Instant.parse("2026-01-01T00:00:00Z"), sameUpdatedAt, null, IssuePriority.MEDIUM, List.of());
        Issue first = issue("ISS-10", "First", "same time", IssueStatus.TODO, Instant.parse("2026-01-01T00:00:00Z"), sameUpdatedAt, null, IssuePriority.MEDIUM, List.of());
        Issue third = issue("ISS-30", "Third", "same time", IssueStatus.TODO, Instant.parse("2026-01-01T00:00:00Z"), sameUpdatedAt, null, IssuePriority.MEDIUM, List.of());

        IssueService service = new IssueService(new InMemoryIssueStore(List.of(second, third, first)));

        List<Issue> result = service.query(repo, new IssueQuery());

        assertEquals(List.of("ISS-10", "ISS-20", "ISS-30"), result.stream().map(issue -> issue.id().value()).toList());
    }

    @Test
    void queryShouldDefaultToUpdatedDesc() {
        Issue oldest = issue("ISS-1", "Oldest", "older", IssueStatus.TODO, Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T01:00:00Z"), null, IssuePriority.MEDIUM, List.of());
        Issue newest = issue("ISS-2", "Newest", "newer", IssueStatus.TODO, Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T03:00:00Z"), null, IssuePriority.MEDIUM, List.of());
        Issue middle = issue("ISS-3", "Middle", "middle", IssueStatus.TODO, Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T02:00:00Z"), null, IssuePriority.MEDIUM, List.of());

        IssueService service = new IssueService(new InMemoryIssueStore(List.of(oldest, newest, middle)));

        List<Issue> result = service.query(repo, null);

        assertEquals(List.of("ISS-2", "ISS-3", "ISS-1"), result.stream().map(issue -> issue.id().value()).toList());
    }

    private static Issue issue(
            String id,
            String title,
            String description,
            IssueStatus status,
            Instant createdAt,
            Instant updatedAt,
            String assignee,
            IssuePriority priority,
            List<String> labels
    ) {
        Instant closedAt = status.canonical().isDoneLike() ? updatedAt : null;
        String closedBy = status.canonical().isDoneLike() ? "alice" : null;
        return new Issue(
                new IssueId(id),
                title,
                description,
                "alice",
                status,
                createdAt,
                updatedAt,
                closedAt,
                closedBy,
                assignee,
                priority,
                labels,
                List.of()
        );
    }

    private static final class InMemoryIssueStore implements IssueStore {
        private List<Issue> issues;

        private InMemoryIssueStore(List<Issue> issues) {
            this.issues = new ArrayList<>(issues);
        }

        @Override
        public List<Issue> load(LocalRepo repo) {
            return List.copyOf(issues);
        }

        @Override
        public void save(LocalRepo repo, List<Issue> issues) {
            this.issues = new ArrayList<>(issues);
        }
    }
}
