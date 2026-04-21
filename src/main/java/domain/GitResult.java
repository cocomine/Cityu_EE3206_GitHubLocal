package domain;

/**
 * Represents the result of a Git command execution.
 *
 * @param exitCode The exit code of the Git command.
 * @param stdout   The standard output of the Git command.
 * @param stderr   The standard error of the Git command.
 */
public record GitResult(int exitCode, String stdout, String stderr) {
    /**
     * Compact constructor for GitResult.
     * Ensures that stdout and stderr are never null.
     */
    public GitResult {
        stdout = stdout == null ? "" : stdout;
        stderr = stderr == null ? "" : stderr;
    }

    /**
     * Checks if the Git command was successful.
     *
     * @return true if the exit code is 0, false otherwise.
     */
    public boolean isSuccess() {
        return exitCode == 0;
    }
}
