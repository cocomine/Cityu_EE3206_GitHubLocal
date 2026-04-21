package infrastructure.store;

import domain.Issue;
import domain.IssuePriority;
import domain.IssueStatus;
import domain.LocalRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonIssueStoreMigrationTest {
    @TempDir
    Path tempDir;

    @Test
    void legacyFileShouldAutoMigrateOnNextSave() throws Exception {
        JsonIssueStore store = new JsonIssueStore();
        LocalRepo repo = new LocalRepo(tempDir);
        Path metaDir = tempDir.resolve(".gitgui");
        Files.createDirectories(metaDir);
        Path issueFile = metaDir.resolve("issues.json");

        String legacyJson = """
                [
                  {
                    "id": "ISS-1",
                    "title": "Legacy issue",
                    "creator": "alice",
                    "status": "OPEN",
                    "createdAt": "2026-01-01T00:00:00Z",
                    "comments": [
                      {
                        "author": "bob",
                        "body": "legacy comment",
                        "time": "2026-01-01T01:00:00Z"
                      }
                    ]
                  }
                ]
                """;
        Files.writeString(issueFile, legacyJson);

        List<Issue> loaded = store.load(repo);
        assertEquals(1, loaded.size());
        Issue issue = loaded.getFirst();
        assertEquals(IssueStatus.TODO, issue.status());
        assertEquals(IssuePriority.MEDIUM, issue.priority());
        assertEquals(List.of(), issue.labels());
        assertNull(issue.assignee());

        store.save(repo, loaded);
        String rewritten = Files.readString(issueFile);
        assertTrue(rewritten.contains("\"schemaVersion\""));
        assertTrue(rewritten.contains("\"issues\""));
        assertTrue(!rewritten.trim().startsWith("["));
    }

    @Test
    void malformedRootShouldFailClearly() throws Exception {
        JsonIssueStore store = new JsonIssueStore();
        LocalRepo repo = new LocalRepo(tempDir);
        Path metaDir = tempDir.resolve(".gitgui");
        Files.createDirectories(metaDir);
        Path issueFile = metaDir.resolve("issues.json");

        Files.writeString(issueFile, "123");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> store.load(repo));
        assertTrue(ex.getMessage().contains("Failed to load issues"));
    }

    @Test
    void futureSchemaVersionShouldFailClearly() throws Exception {
        JsonIssueStore store = new JsonIssueStore();
        LocalRepo repo = new LocalRepo(tempDir);
        Path metaDir = tempDir.resolve(".gitgui");
        Files.createDirectories(metaDir);
        Path issueFile = metaDir.resolve("issues.json");

        String unsupportedSchemaJson = """
                {
                  "schemaVersion": 99,
                  "storeRevision": "rev-1",
                  "issues": []
                }
                """;
        Files.writeString(issueFile, unsupportedSchemaJson);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> store.load(repo));
        assertTrue(ex.getMessage().contains("Failed to load issues"));
    }

    @Test
    void legacyClosedStatusShouldNormalizeToDone() throws Exception {
        JsonIssueStore store = new JsonIssueStore();
        LocalRepo repo = new LocalRepo(tempDir);
        Path metaDir = tempDir.resolve(".gitgui");
        Files.createDirectories(metaDir);
        Path issueFile = metaDir.resolve("issues.json");

        String legacyClosedJson = """
                [
                  {
                    "id": "ISS-2",
                    "title": "Legacy closed",
                    "creator": "alice",
                    "status": "CLOSED",
                    "createdAt": "2026-01-01T00:00:00Z",
                    "updatedAt": "2026-01-01T00:00:01Z",
                    "closedAt": "2026-01-01T00:00:02Z",
                    "closedBy": "alice",
                    "comments": []
                  }
                ]
                """;
        Files.writeString(issueFile, legacyClosedJson);

        List<Issue> loaded = store.load(repo);

        assertEquals(1, loaded.size());
        Issue issue = loaded.getFirst();
        assertEquals(IssueStatus.DONE, issue.status());
        assertNotNull(issue.closedAt());
        assertEquals("alice", issue.closedBy());
    }
}
