package infrastructure.git;

import domain.GitResult;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class SystemGitClient implements GitClient {
    private final String gitExe;

    public SystemGitClient() {
        this("git");
    }

    public SystemGitClient(String gitExe) {
        if (gitExe == null || gitExe.isBlank()) {
            throw new IllegalArgumentException("git executable cannot be blank");
        }
        this.gitExe = gitExe;
    }

    @Override
    public GitResult run(Path repoRoot, List<String> args) {
        Objects.requireNonNull(repoRoot, "repoRoot cannot be null");
        Objects.requireNonNull(args, "args cannot be null");

        List<String> command = new ArrayList<>();
        command.add(gitExe);
        command.addAll(args);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(repoRoot.toFile());

        try {
            Process process = processBuilder.start();
            try (ExecutorService executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
                Future<String> stdoutFuture = executor.submit(() -> readAll(process.getInputStream()));
                Future<String> stderrFuture = executor.submit(() -> readAll(process.getErrorStream()));

                int exitCode = process.waitFor();
                String stdout = stdoutFuture.get();
                String stderr = stderrFuture.get();
                return new GitResult(exitCode, stdout, stderr);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new GitResult(-1, "", "Interrupted while running git command");
        } catch (IOException | ExecutionException e) {
            return new GitResult(-1, "", "Failed to run git command: " + e.getMessage());
        }
    }

    private String readAll(InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }
}
