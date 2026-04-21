package application;

import application.command.GitCommand;

import java.util.Objects;

public class GitService {
    /**
     * Executes the given Git command and returns its result. The command must not be null.
     *
     * @param command the Git command to execute. Must not be null.
     * @return the result of executing the given Git command,
     *  which is of type T as defined by the command's implementation.
     * @param <T> the type of the result produced by the Git command execution.
     *           This is determined by the specific command implementation.
     * @throws NullPointerException if the command is null.
     */
    public <T> T execute(GitCommand<T> command) throws IllegalArgumentException, IllegalStateException, NullPointerException  {
        return Objects.requireNonNull(command, "command cannot be null").execute();
    }
}
