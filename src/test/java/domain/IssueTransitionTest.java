package domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IssueTransitionTest {
    @Test
    void validTransitionsShouldSucceed() {
        Issue issue = new Issue(new IssueId("ISS-T1"), "Transition", "alice");

        issue.transitionTo(IssueStatus.IN_PROGRESS);
        assertEquals(IssueStatus.IN_PROGRESS, issue.status());

        issue.transitionTo(IssueStatus.REVIEW);
        assertEquals(IssueStatus.REVIEW, issue.status());

        issue.transitionTo(IssueStatus.DONE, "bob");
        assertEquals(IssueStatus.DONE, issue.status());
        assertNotNull(issue.closedAt());
        assertEquals("bob", issue.closedBy());

        issue.transitionTo(IssueStatus.TODO);
        assertEquals(IssueStatus.TODO, issue.status());
    }

    @Test
    void invalidTransitionShouldFail() {
        Issue issue = new Issue(new IssueId("ISS-T2"), "Transition", "alice");

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> issue.transitionTo(IssueStatus.REVIEW)
        );

        assertTrue(ex.getMessage().contains("Invalid status transition"));
        assertEquals(IssueStatus.TODO, issue.status());
    }

    @Test
    void legacyAliasStatusShouldCanonicalizeOnCreation() {
        Issue openLegacy = new Issue(
                new IssueId("ISS-T3"),
                "Legacy Open",
                "alice",
                IssueStatus.OPEN,
                Instant.parse("2026-01-01T00:00:00Z"),
                List.of()
        );
        assertEquals(IssueStatus.TODO, openLegacy.status());
    }
}
