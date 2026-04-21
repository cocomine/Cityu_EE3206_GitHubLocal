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
 * Unit tests for the {@link AddCommand} class.
 */
class AddCommandTest {
    @TempDir
    Path tempDir;

    /**
     * Tests that executing an AddCommand on a non-initialized repository
     * throws an {@link IllegalStateException}.
     */
    @Test
    void executeShouldValidateGitRepoForNonInitCommands() {
        CountingGitClient client = new CountingGitClient();
        LocalRepo repo = new LocalRepo(tempDir);

        AddCommand command = new AddCommand(repo, client, "README.md");

        assertThrows(IllegalStateException.class, command::execute);
        assertEquals(0, client.invocations);
    }

    /**
     * Tests that executing an AddCommand on a valid Git repository
     * runs the 'git add' command successfully.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Test
    void executeShouldRunForValidGitRepo() throws IOException {
        CountingGitClient client = new CountingGitClient();
        Files.createDirectories(tempDir.resolve(".git"));
        LocalRepo repo = new LocalRepo(tempDir);

        AddCommand command = new AddCommand(repo, client, "README.md");
        GitResult result = command.execute();

        assertEquals(1, client.invocations);
        assertEquals(0, result.exitCode());
        assertEquals(List.of("add", "README.md"), client.lastArgs);
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
