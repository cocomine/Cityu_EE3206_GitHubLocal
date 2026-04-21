package application.command.command;

import application.command.ResultGitCommand;
import domain.LocalRepo;
import infrastructure.git.GitClient;

import java.util.List;

/**
 * Represents a Git command to switch branches in a repository.
 * Extends {@link ResultGitCommand} to specifically execute the
 * {@code git checkout <branch>} command and return its execution result.
 */
public class CheckoutCommand extends ResultGitCommand {
    private final String branch;

    /**
     * Constructs a new {@code CheckoutCommand}.
     *
     * @param repo   the local repository where the checkout command will be executed
     * @param client the Git client used to execute the command
     * @param branch the target branch name to check out
     * @throws IllegalArgumentException if the provided branch name is null or blank
     */
    public CheckoutCommand(LocalRepo repo, GitClient client, String branch) {
        super(repo, client);
        if (branch == null || branch.isBlank()) {
            throw new IllegalArgumentException("branch cannot be blank");
        }
        this.branch = branch;
    }

    /**
     * Builds the command-line arguments necessary for this Git command.
     *
     * @return a list of string arguments ({@code ["checkout", <branch>]}) used to perform the checkout
     */
    @Override
    protected List<String> buildArgs() {
        return List.of("checkout", branch);
    }
}
