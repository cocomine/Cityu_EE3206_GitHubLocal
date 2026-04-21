package application.command.command;

import application.command.ResultGitCommand;
import domain.LocalRepo;
import infrastructure.git.GitClient;

import java.util.List;

/**
 * Represents a Git command to create a new branch in a repository.
 * Extends {@link ResultGitCommand} to specifically execute the
 * {@code git branch <branch>} command and return its execution result.
 */
public class CreateBranchCommand extends ResultGitCommand {
    private final String branch;

    /**
     * Constructs a new {@code CreateBranchCommand}.
     *
     * @param repo   the local repository where the branch creation will be executed
     * @param client the Git client used to execute the command
     * @param branch the name of the new branch to create
     * @throws IllegalArgumentException if the provided branch name is null or blank
     */
    public CreateBranchCommand(LocalRepo repo, GitClient client, String branch) {
        super(repo, client);
        if (branch == null || branch.isBlank()) {
            throw new IllegalArgumentException("branch cannot be blank");
        }
        this.branch = branch;
    }

    /**
     * Builds the command-line arguments necessary for this Git command.
     *
     * @return a list of string arguments ({@code ["branch", <branch>]}) used to create the branch
     */
    @Override
    protected List<String> buildArgs() {
        return List.of("branch", branch);
    }
}
