package application.command;

/**
 * Represents a command that can be executed within the application.
 * This interface defines a contract for executing Git-related commands,
 * where each command may return a result of type T.
 * Implementations of this interface will
 *
 * @param <T> The type of the result returned by executing the command.
 *           This allows for flexibility in handling different types of results based on the specific command being performed.
 */
public interface AppCommand<T> {
    /**
     * Executes the Git command. This method validates the repository, builds the command arguments,
     * runs the command using the Git client, and handles the result.
     *
     * @return The result of executing the Git command, which may be processed by the handleResult method.
     * @throws IllegalArgumentException if the repository root does not exist.
     * @throws IllegalStateException if the repository is required to be a Git repository but is not.
     */
    T execute() throws IllegalArgumentException, IllegalStateException;
}
