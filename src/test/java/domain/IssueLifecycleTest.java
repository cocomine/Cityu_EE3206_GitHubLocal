package domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IssueLifecycleTest {
    @Test
    void closeShouldRecordLifecycleMetadata() {
        Issue issue = new Issue(new IssueId("ISS-L1"), "Lifecycle", "alice");
        Instant beforeClose = issue.updatedAt();

        issue.close("bob");

        assertEquals(IssueStatus.DONE, issue.status());
        assertNotNull(issue.closedAt());
        assertEquals("bob", issue.closedBy());
        assertTrue(!issue.updatedAt().isBefore(beforeClose));
    }

    @Test
    void blankDescriptionUpdateShouldBeRejected() {
        Issue issue = new Issue(new IssueId("ISS-L2"), "Lifecycle", "alice");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> issue.updateDescription(" ")
        );

        assertEquals("description cannot be blank", ex.getMessage());
    }

    @Test
    void nonDoneIssueWithClosedMetadataShouldBeRejected() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new Issue(
                        new IssueId("ISS-L3"),
                        "Lifecycle",
                        "desc",
                        "alice",
                        IssueStatus.TODO,
                        Instant.parse("2026-01-01T00:00:00Z"),
                        Instant.parse("2026-01-01T00:00:01Z"),
                        Instant.parse("2026-01-01T00:00:02Z"),
                        "alice",
                        List.of()
                )
        );

        assertEquals("non-done issue cannot have closed metadata", ex.getMessage());
    }
}
