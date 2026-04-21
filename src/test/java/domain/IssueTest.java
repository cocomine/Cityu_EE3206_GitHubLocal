package domain;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class IssueTest {
    @Test
    void closeAndReopenShouldUpdateStatus() {
        Issue issue = new Issue(new IssueId("ISS-1"), "Sample", "alice");

        assertEquals(IssueStatus.TODO, issue.status());

        issue.close();
        assertEquals(IssueStatus.DONE, issue.status());
        assertNotNull(issue.closedAt());
        assertEquals("alice", issue.closedBy());

        issue.reopen();
        assertEquals(IssueStatus.TODO, issue.status());
        assertNull(issue.closedAt());
        assertNull(issue.closedBy());
    }

    @Test
    void addCommentShouldAppendToComments() {
        Issue issue = new Issue(new IssueId("ISS-2"), "Sample", "alice");

        issue.addComment(new Comment("bob", "Looks good"));

        assertEquals(1, issue.comments().size());
        assertEquals("bob", issue.comments().getFirst().author());
        assertNotNull(issue.updatedAt());
    }

    @Test
    void metadataShouldDefaultDeterministically() {
        Issue issue = new Issue(new IssueId("ISS-3"), "Sample", "alice");

        assertNull(issue.assignee());
        assertEquals(IssuePriority.MEDIUM, issue.priority());
        assertEquals(List.of(), issue.labels());
    }

    @Test
    void metadataShouldBeSetAndNormalized() {
        Issue issue = new Issue(new IssueId("ISS-4"), "Sample", "alice");
        List<String> rawLabels = new ArrayList<>(List.of("  Bug", "BUG", "enhancement  ", "", "  ", "Feature"));
        rawLabels.add(null);

        issue.assignTo("  bob  ");
        issue.setPriority(IssuePriority.HIGH);
        issue.setLabels(rawLabels);

        assertEquals("bob", issue.assignee());
        assertEquals(IssuePriority.HIGH, issue.priority());
        assertEquals(List.of("bug", "enhancement", "feature"), issue.labels());
    }
}
