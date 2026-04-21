package infrastructure.store;

import domain.Comment;
import domain.Issue;
import domain.IssueId;
import domain.LocalRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonIssueStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void saveAndLoadShouldRoundTripIssues() throws Exception {
        JsonIssueStore store = new JsonIssueStore();
        LocalRepo repo = new LocalRepo(tempDir);

        Issue issue = new Issue(new IssueId("ISS-100"), "Implement parser", "alice");
        issue.addComment(new Comment("bob", "Please add tests"));

        store.save(repo, List.of(issue));
        List<Issue> loaded = store.load(repo);
        String rawJson = Files.readString(tempDir.resolve(".gitgui/issues.json"));

        assertEquals(1, loaded.size());
        assertEquals("ISS-100", loaded.getFirst().id().value());
        assertEquals(1, loaded.getFirst().comments().size());
        assertTrue(rawJson.contains("\"schemaVersion\""));
        assertTrue(rawJson.contains("\"issues\""));
    }
}
