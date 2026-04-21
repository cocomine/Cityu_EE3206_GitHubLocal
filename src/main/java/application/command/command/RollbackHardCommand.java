package application.command.command;

import application.command.ResultGitCommand;
import domain.LocalRepo;
import infrastructure.git.GitClient;

import java.util.List;

/**
 * Represents a Git command to perform a hard reset to a specific commit.
 * Extends {@link ResultGitCommand} to specifically execute the
 * {@code git reset --hard <commit>} command and return its execution result.
 */
public class RollbackHardCommand extends ResultGitCommand {
    private final String commit;

    /**
     * Constructs a new {@code RollbackHardCommand}.
     *
     * @param repo   the local repository where the rollback will be executed
     * @param client the Git client used to execute the command
     * @param commit the target commit hash or reference to reset to
     * @throws IllegalArgumentException if the provided commit is null or blank
     */
    public RollbackHardCommand(LocalRepo repo, GitClient client, String commit) {
        super(repo, client);
        if (commit == null || commit.isBlank()) {
            throw new IllegalArgumentException("commit cannot be blank");
        }
        this.commit = commit;
    }

    /**
     * Builds the command-line arguments necessary for this Git command.
     *
     * @return a list of string arguments ({@code ["reset", "--hard", <commit>]}) used to perform the reset
     */
    @Override
    protected List<String> buildArgs() {
        return List.of("reset", "--hard", commit);
    }
}
