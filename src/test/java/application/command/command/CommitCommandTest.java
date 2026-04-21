package application.command.command;

import domain.GitResult;
import domain.LocalRepo;
import infrastructure.git.GitClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for the {@link CommitCommand} class.
 */
class CommitCommandTest {
    @TempDir
    Path tempDir;

    /**
     * Tests that the constructor throws an {@link IllegalArgumentException} when the message is null.
     */
    @Test
    void constructorShouldRejectNullMessage() {
        LocalRepo repo = new LocalRepo(tempDir);
        CountingGitClient client = new CountingGitClient();

        assertThrows(IllegalArgumentException.class, () -> new CommitCommand(repo, client, null));
    }

    /**
     * Tests that the constructor throws an {@link IllegalArgumentException} when the message is blank.
     */
    @Test
    void constructorShouldRejectBlankMessage() {
        LocalRepo repo = new LocalRepo(tempDir);
        CountingGitClient client = new CountingGitClient();

        assertThrows(IllegalArgumentException.class, () -> new CommitCommand(repo, client, "   "));
    }

    /**
     * Tests that executing a CommitCommand on a non-initialized repository
     * throws an {@link IllegalStateException}.
     */
    @Test
    void executeShouldValidateGitRepoForNonInitCommands() {
        CountingGitClient client = new CountingGitClient();
        LocalRepo repo = new LocalRepo(tempDir);

        CommitCommand command = new CommitCommand(repo, client, "initial commit");

        assertThrows(IllegalStateException.class, command::execute);
        assertEquals(0, client.invocations);
    }

    /**
     * Tests that executing a CommitCommand on a valid Git repository
     * runs the 'git commit' command successfully.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Test
    void executeShouldRunCommitWithMessageForValidGitRepo() throws IOException {
        CountingGitClient client = new CountingGitClient();
        Files.createDirectories(tempDir.resolve(".git"));
        LocalRepo repo = new LocalRepo(tempDir);

        CommitCommand command = new CommitCommand(repo, client, "initial commit");
        GitResult result = command.execute();

        assertEquals(1, client.invocations);
        assertEquals(0, result.exitCode());
        assertEquals(List.of("commit", "-m", "initial commit"), client.lastArgs);
    }

    /**
     * A mock {@link GitClient} that counts invocations and records the last arguments.
     */
    private static class CountingGitClient implements GitClient {
        private int invocations;
        private List<String> lastArgs = List.of();

        @Override
        public GitResult run(Path repoRoot, List<String> args) {
            invocations++;
            lastArgs = List.copyOf(args);
            return new GitResult(0, "ok", "");
        }
    }
}
