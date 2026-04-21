package domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommentLifecycleTest {
    @Test
    void creationShouldAssignStableIdentityAndTimestamps() {
        Comment comment = new Comment("alice", "hello");

        assertNotNull(comment.id());
        assertNotNull(comment.createdAt());
        assertEquals(comment.createdAt(), comment.time());
        assertTrue(!comment.isDeleted());
    }

    @Test
    void invalidMutationShouldBeRejected() {
        Comment comment = new Comment("alice", "hello");

        issueWithComment(comment).deleteComment(comment.id(), "bob");

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> issueWithComment(comment).editComment(comment.id(), "new text")
        );

        assertEquals("cannot edit a deleted comment", ex.getMessage());
    }

    @Test
    void invalidChronologyShouldBeRejected() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new Comment(
                        new CommentId("COM-C2"),
                        "alice",
                        "hello",
                        Instant.parse("2026-01-01T02:00:00Z"),
                        Instant.parse("2026-01-01T01:00:00Z"),
                        null,
                        null,
                        null,
                        null,
                        null
                )
        );

        assertEquals("editedAt cannot be before createdAt", ex.getMessage());
    }

    private Issue issueWithComment(Comment comment) {
        Issue issue = new Issue(new IssueId("ISS-C1"), "Comments", "alice");
        issue.addComment(comment);
        return issue;
    }
}
