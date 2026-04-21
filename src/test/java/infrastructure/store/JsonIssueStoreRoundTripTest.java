package infrastructure.store;

import domain.Comment;
import domain.CommentId;
import domain.Issue;
import domain.IssueId;
import domain.IssuePriority;
import domain.IssueStatus;
import domain.LocalRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JsonIssueStoreRoundTripTest {
    @TempDir
    Path tempDir;

    @Test
    void enrichedIssueAndCommentShouldRoundTrip() {
        JsonIssueStore store = new JsonIssueStore();
        LocalRepo repo = new LocalRepo(tempDir);

        Comment comment = new Comment(
                new CommentId("COM-1"),
                "bob",
                "Please add tests",
                Instant.parse("2026-01-01T01:00:00Z"),
                Instant.parse("2026-01-01T02:00:00Z"),
                Instant.parse("2026-01-01T03:00:00Z"),
                "alice",
                "COM-0",
                "a1b2c3d4",
                "src/Main.java"
        );

        Issue issue = new Issue(
                new IssueId("ISS-RT1"),
                "RoundTrip",
                "Detailed description",
                "alice",
                IssueStatus.DONE,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T04:00:00Z"),
                Instant.parse("2026-01-01T05:00:00Z"),
                "alice",
                "bob",
                IssuePriority.HIGH,
                List.of("  Bug", "feature", "BUG"),
                List.of(comment)
        );

        store.save(repo, List.of(issue));

        Issue loaded = store.load(repo).getFirst();
        Comment loadedComment = loaded.comments().getFirst();

        assertEquals("Detailed description", loaded.description());
        assertNotNull(loaded.updatedAt());
        assertEquals("alice", loaded.closedBy());
        assertEquals("bob", loaded.assignee());
        assertEquals(IssuePriority.HIGH, loaded.priority());
        assertEquals(List.of("bug", "feature"), loaded.labels());
        assertEquals("COM-1", loadedComment.id().value());
        assertEquals("COM-0", loadedComment.replyToCommentId());
        assertEquals("a1b2c3d4", loadedComment.commitReference());
        assertEquals("src/Main.java", loadedComment.filePath());
        assertEquals("alice", loadedComment.deletedBy());
    }

    @Test
    void snapshotRevisionShouldRoundTrip() {
        JsonIssueStore store = new JsonIssueStore();
        LocalRepo repo = new LocalRepo(tempDir);

        Issue issue = new Issue(new IssueId("ISS-RT2"), "RoundTrip", "alice");
        IssueStoreSnapshot snapshot = new IssueStoreSnapshot(1, "revision-123", List.of(issue));

        store.saveSnapshot(repo, snapshot);
        IssueStoreSnapshot loadedSnapshot = store.loadSnapshot(repo);

        assertEquals(1, loadedSnapshot.schemaVersion());
        assertEquals("revision-123", loadedSnapshot.storeRevision());
        assertEquals(1, loadedSnapshot.issues().size());
    }
}
