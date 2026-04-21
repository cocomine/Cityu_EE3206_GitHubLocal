package application.command.command;

import application.command.ResultGitCommand;
import domain.LocalRepo;
import infrastructure.git.GitClient;

import java.util.List;

/**
 * Represents a Git command to merge a branch into the currently active branch.
 * Extends {@link ResultGitCommand} to execute the {@code git merge <branch>} command
 * and return its execution result.
 */
public class MergeCommand extends ResultGitCommand {
    private final String fromBranch;

    /**
     * Constructs a new {@code MergeCommand}.
     *
     * @param repo       the local repository where the merge will occur
     * @param client     the Git client used to execute the command
     * @param fromBranch the name of the branch to merge into the current branch
     * @throws IllegalArgumentException if the provided branch name is null or blank
     */
    public MergeCommand(LocalRepo repo, GitClient client, String fromBranch) {
        super(repo, client);
        if (fromBranch == null || fromBranch.isBlank()) {
            throw new IllegalArgumentException("fromBranch cannot be blank");
        }
        this.fromBranch = fromBranch;
    }

    /**
     * Builds the command-line arguments necessary for this Git command.
     *
     * @return a list of string arguments ({@code ["merge", <fromBranch>]}) used to perform the merge
     */
    @Override
    protected List<String> buildArgs() {
        return List.of("merge", fromBranch);
    }
}
