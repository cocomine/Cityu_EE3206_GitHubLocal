package application.command.command;

import domain.GitResult;
import domain.LocalRepo;
import infrastructure.git.GitClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InitCommandTest {
    @TempDir
    Path tempDir;

    /**
     * Tests that executing the InitCommand calls the GitClient with the correct arguments and repository root.
     */
    @Test
    void executeShouldCallGitClientWithInitArgs() {
        CapturingGitClient client = new CapturingGitClient();
        LocalRepo repo = new LocalRepo(tempDir);

        InitCommand command = new InitCommand(repo, client);
        GitResult result = command.execute();

        assertInstanceOf(GitResult.class, result);
        assertEquals(List.of("init"), client.args);
        assertEquals(tempDir.toAbsolutePath().normalize(), client.repoRoot.toAbsolutePath().normalize());
    }

    /**
     * Tests that executing the InitCommand creates a .gitignore file with an entry for .gitgui if it does not already exist.
     */
    @Test
    void executeShouldCreateGitignoreWithGitguiEntry() throws IOException {
        CapturingGitClient client = new CapturingGitClient();
        LocalRepo repo = new LocalRepo(tempDir);
        Path gitignore = tempDir.resolve(".gitignore");

        assertFalse(Files.exists(gitignore));

        new InitCommand(repo, client).execute();

        assertTrue(Files.exists(gitignore));
        assertEquals(".gitgui", Files.readString(gitignore, StandardCharsets.UTF_8).trim());
    }

    /**
     * Tests that executing the InitCommand appends an entry for .gitgui to the .gitignore file if it already exists but does not contain the entry.
     */
    @Test
    void executeShouldAppendGitguiEntryWhenMissing() throws IOException {
        CapturingGitClient client = new CapturingGitClient();
        LocalRepo repo = new LocalRepo(tempDir);
        Path gitignore = tempDir.resolve(".gitignore");

        Files.writeString(gitignore, "target/" + System.lineSeparator(), StandardCharsets.UTF_8);

        new InitCommand(repo, client).execute();

        String content = Files.readString(gitignore, StandardCharsets.UTF_8);
        assertTrue(content.contains("target/"));
        assertEquals(1, countLine(content));
    }

    /**
     * Tests that executing the InitCommand does not duplicate the .gitgui entry in the .gitignore file if it already exists.
     */
    @Test
    void executeShouldNotDuplicateGitguiEntry() throws IOException {
        CapturingGitClient client = new CapturingGitClient();
        LocalRepo repo = new LocalRepo(tempDir);
        Path gitignore = tempDir.resolve(".gitignore");

        Files.writeString(gitignore, ".gitgui" + System.lineSeparator(), StandardCharsets.UTF_8);

        new InitCommand(repo, client).execute();

        String content = Files.readString(gitignore, StandardCharsets.UTF_8);
        assertEquals(1, countLine(content));
    }

    /**
     * Tests that executing the InitCommand does not create or modify the .gitignore file if the Git initialization fails.
     */
    @Test
    void executeShouldNotCreateGitignoreWhenInitFails() {
        CapturingGitClient client = new CapturingGitClient(new GitResult(1, "", "init failed"));
        LocalRepo repo = new LocalRepo(tempDir);
        Path gitignore = tempDir.resolve(".gitignore");

        new InitCommand(repo, client).execute();

        assertFalse(Files.exists(gitignore));
    }

    /**
     * Counts the number of lines in the given content
     *
     * @param content the content to count lines in
     * @return the number of lines that match the expected content
     */
    private static long countLine(String content) {
        return content.lines()
                .map(String::trim)
                .filter(".gitgui"::equals)
                .count();
    }

    /**
     * A simple GitClient implementation that captures the arguments and repository root passed to its run method for testing purposes.
     */
    private static class CapturingGitClient implements GitClient {
        private final GitResult resultToReturn;
        private Path repoRoot;
        private List<String> args = new ArrayList<>();

        private CapturingGitClient() {
            this(new GitResult(0, "initialized", ""));
        }

        private CapturingGitClient(GitResult resultToReturn) {
            this.resultToReturn = resultToReturn;
        }

        @Override
        public GitResult run(Path repoRoot, List<String> args) {
            this.repoRoot = repoRoot;
            this.args = new ArrayList<>(args);
            return resultToReturn;
        }
    }
}
