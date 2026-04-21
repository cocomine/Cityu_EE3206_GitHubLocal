package application.command.command;

import domain.GitLog;
import domain.GitResult;
import domain.LocalRepo;
import infrastructure.git.GitClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.time.LocalDateTime;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LogCommandTest {
    @TempDir
    Path tempDir;

    @Test
    void executeShouldRequestDecoratedFormattedLog() throws IOException {
        CapturingGitClient client = new CapturingGitClient();
        Files.createDirectories(tempDir.resolve(".git"));
        LocalRepo repo = new LocalRepo(tempDir);

        LogCommand command = new LogCommand(repo, client, 3);
        List<GitLog> logs = command.execute();

        assertEquals(
                List.of("log", "--decorate=short", "-n", "3", "--pretty=format:%h %d %s | %an | %ad"),
                client.lastArgs
        );
        assertEquals(1, logs.size());
        assertEquals("a1b2c3", logs.getFirst().hash());
        assertEquals(List.of("HEAD -> main", "abc"), logs.getFirst().tag());
        assertEquals("Test commit", logs.getFirst().message());
        assertEquals("alice", logs.getFirst().author());
        assertEquals(LocalDateTime.of(2026, 3, 15, 19, 34, 59), logs.getFirst().date());
    }

    private static class CapturingGitClient implements GitClient {
        private List<String> lastArgs = List.of();

        @Override
        public GitResult run(Path repoRoot, List<String> args) {
            lastArgs = List.copyOf(args);
            return new GitResult(0, "a1b2c3 (HEAD -> main, abc) Test commit | alice | Sun Mar 15 19:34:59 2026 +0800\n", "");
        }
    }
}
