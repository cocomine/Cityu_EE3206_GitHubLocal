package application.command.command;

import domain.GitDiff;
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

class DiffCommandTest {
    @TempDir
    Path tempDir;

    @Test
    void executeShouldRunUnstagedDiffArguments() throws IOException {
        CapturingGitClient client = new CapturingGitClient();
        Files.createDirectories(tempDir.resolve(".git"));
        LocalRepo repo = new LocalRepo(tempDir);

        DiffCommand command = new DiffCommand(repo, client, false);
        GitDiff parsed = command.execute();

        assertEquals(
                List.of("diff", "--no-color", "--no-ext-diff", "--find-renames", "-U3"),
                client.lastArgs
        );
        assertEquals(1, parsed.files().size());
        assertEquals(GitDiff.FileStatus.CHANGE, parsed.files().getFirst().status());
    }

    @Test
    void executeShouldRunStagedDiffArguments() throws IOException {
        CapturingGitClient client = new CapturingGitClient();
        Files.createDirectories(tempDir.resolve(".git"));
        LocalRepo repo = new LocalRepo(tempDir);

        DiffCommand command = new DiffCommand(repo, client, true);
        command.execute();

        assertEquals(
                List.of("diff", "--staged", "--no-color", "--no-ext-diff", "--find-renames", "-U3"),
                client.lastArgs
        );
    }

    private static class CapturingGitClient implements GitClient {
        private List<String> lastArgs = List.of();

        @Override
        public GitResult run(Path repoRoot, List<String> args) {
            lastArgs = List.copyOf(args);
            String stdout = String.join("\n",
                    "diff --git a/src/A.java b/src/A.java",
                    "index 1111111..2222222 100644",
                    "--- a/src/A.java",
                    "+++ b/src/A.java",
                    "@@ -1 +1 @@",
                    "-old",
                    "+new"
            );
            return new GitResult(0, stdout, "");
        }
    }
}
