package infrastructure.store;

import domain.Issue;
import domain.IssueId;
import domain.LocalRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonIssueStoreAtomicWriteTest {
    @TempDir
    Path tempDir;

    @Test
    void saveShouldWriteValidEnvelopeAndLeaveNoTempFiles() throws Exception {
        JsonIssueStore store = new JsonIssueStore();
        LocalRepo repo = new LocalRepo(tempDir);

        Issue issue = new Issue(new IssueId("ISS-A1"), "Atomic", "alice");
        store.save(repo, List.of(issue));

        Path metaDir = tempDir.resolve(".gitgui");
        Path issueFile = metaDir.resolve("issues.json");
        String content = Files.readString(issueFile);

        assertTrue(content.contains("\"schemaVersion\""));
        assertTrue(content.contains("\"issues\""));
        try (Stream<Path> pathStream = Files.list(metaDir)) {
            assertEquals(1L, pathStream.filter(path -> path.getFileName().toString().equals("issues.json")).count());
        }
    }

    @Test
    void corruptFileShouldRaiseExceptionAndPreserveBackup() throws Exception {
        JsonIssueStore store = new JsonIssueStore();
        LocalRepo repo = new LocalRepo(tempDir);
        Path metaDir = tempDir.resolve(".gitgui");
        Files.createDirectories(metaDir);
        Path issueFile = metaDir.resolve("issues.json");

        Files.writeString(issueFile, "{ bad json");

        assertThrows(IllegalStateException.class, () -> store.load(repo));
        assertTrue(!Files.exists(issueFile));
        long backupCount;
        try (Stream<Path> pathStream = Files.list(metaDir)) {
            backupCount = pathStream
                    .filter(path -> path.getFileName().toString().startsWith("issues.corrupt-"))
                    .count();
        }
        assertTrue(backupCount >= 1);
    }

    @Test
    void semanticallyInvalidDocumentShouldBeQuarantined() throws Exception {
        JsonIssueStore store = new JsonIssueStore();
        LocalRepo repo = new LocalRepo(tempDir);
        Path metaDir = tempDir.resolve(".gitgui");
        Files.createDirectories(metaDir);
        Path issueFile = metaDir.resolve("issues.json");

        String invalidLifecycleJson = """
                {
                  "schemaVersion": 1,
                  "storeRevision": "rev-1",
                  "issues": [
                    {
                      "id": "ISS-BAD",
                      "title": "bad",
                      "description": "bad",
                      "creator": "alice",
                      "status": "OPEN",
                      "createdAt": "2026-01-01T00:00:00Z",
                      "updatedAt": "2026-01-01T00:00:01Z",
                      "closedAt": "2026-01-01T00:00:02Z",
                      "closedBy": "alice",
                      "comments": []
                    }
                  ]
                }
                """;
        Files.writeString(issueFile, invalidLifecycleJson);

        assertThrows(IllegalStateException.class, () -> store.load(repo));
        assertTrue(!Files.exists(issueFile));
        try (Stream<Path> pathStream = Files.list(metaDir)) {
            assertTrue(pathStream.anyMatch(path -> path.getFileName().toString().startsWith("issues.corrupt-")));
        }
    }
}
