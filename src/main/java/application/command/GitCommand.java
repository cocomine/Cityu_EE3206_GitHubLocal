package application.command;

import domain.GitResult;
import domain.LocalRepo;
import infrastructure.git.GitClient;

import java.nio.file.Files;
import java.util.List;
import java.util.Objects;

/**
 * The GitCommand class serves as a base for all Git-related commands in the application.
 * It provides common functionality for validating the repository, executing Git commands, and handling results.
 *
 * Subclasses must implement the buildArgs method to specify the arguments for their respective Git operations
 * and the handleResult method to process the output from the Git command execution.
 *
 * @param <T> The type of the result returned by executing the Git command.
 *           This allows for flexibility in handling different types of results based on the specific Git operation being performed.
 */
public abstract class GitCommand<T> implements AppCommand<T> {
    private final LocalRepo repo; // The local repository on which the command will operate
    private final GitClient client; // The Git client used to execute commands against the repository

    /**
     * Constructs a new GitCommand with the specified repository and Git client.
     *
     * @param repo   The local repository to operate on. Must not be null.
     * @param client The Git client to use for executing commands. Must not be null.
     * @throws NullPointerException if either repo or client is null.
     */
    protected GitCommand(LocalRepo repo, GitClient client) throws NullPointerException {
        this.repo = Objects.requireNonNull(repo, "repo cannot be null");
        this.client = Objects.requireNonNull(client, "client cannot be null");
    }

    /**
     * Executes the Git command. This method validates the repository, builds the command arguments,
     * runs the command using the Git client, and handles the result.
     *
     * @return The result of executing the Git command, which may be processed by the handleResult method.
     * @throws IllegalArgumentException if the repository root does not exist.
     * @throws IllegalStateException if the repository is required to be a Git repository but is not.
     */
    @Override
    public final T execute() throws IllegalArgumentException, IllegalStateException {
        validateRepo();
        List<String> args = buildArgs();
        GitResult result = client.run(repo.root(), args);
        return handleResult(result);
    }

    /**
     * Validates the repository before executing the command. This method checks if the repository root exists
     * and, if required, whether it is a valid Git repository. If any validation fails, an appropriate exception is thrown.
     *
     * @throws IllegalArgumentException if the repository root does not exist.
     * @throws IllegalStateException if the repository is required to be a Git repository but is not.
     */
    protected void validateRepo() throws IllegalArgumentException, IllegalStateException {
        if (!Files.isDirectory(repo.root())) {
            throw new IllegalArgumentException("Repository root does not exist: " + repo.root());
        }
        if (requiresGitRepo() && !repo.isGitRepo()) {
            throw new IllegalStateException("Not a git repository: " + repo.root());
        }
    }

    /**
     * Determines whether this command requires the repository to be a valid Git repository. By default, this method returns true.
     * @return true if the command requires a Git repository; false otherwise.
     */
    protected boolean requiresGitRepo() {
        return true;
    }

    /**
     * Builds the list of arguments to be passed to the Git client for executing the command.
     * Subclasses must implement this method to provide the specific arguments needed for their respective Git operations.
     *
     * @return A list of strings representing the arguments for the Git command.
     */
    protected abstract List<String> buildArgs();

    /**
     * Handles the result returned by the Git client after executing the command.
     *
     * @param result The GitResult object containing the output and status of the executed command.
     * @return A typed result representing the processed command output.
     * Implementations convert raw Git output into the command's declared return type.
     */
    protected abstract T handleResult(GitResult result);

    /**
     * Provides access to the LocalRepo instance associated with this command.
     * Subclasses can use this method to retrieve information about the repository.
     *
     * @return The LocalRepo instance representing the local repository on which this command operates.
     */
    protected LocalRepo repo() {
        return repo;
    }

    /**
     * Provides access to the GitClient instance associated with this command.
     * Subclasses can use this method to execute additional Git commands.
     *
     * @return The GitClient instance used to execute Git commands against the repository.
     */
    protected GitClient client() {
        return client;
    }
}
