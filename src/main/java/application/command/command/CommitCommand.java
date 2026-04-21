package application.command.command;

import application.command.ResultGitCommand;
import domain.LocalRepo;
import infrastructure.git.GitClient;

import java.util.List;

/**
 * Represents a command to record changes to the repository.
 * This command uses 'git commit -m'.
 */
public class CommitCommand extends ResultGitCommand {
    private final String message;

    /**
     * Constructs a CommitCommand.
     *
     * @param repo    The local repository.
     * @param client  The Git client.
     * @param message The commit message.
     * @throws IllegalArgumentException if the message is null or blank.
     */
    public CommitCommand(LocalRepo repo, GitClient client, String message) throws IllegalArgumentException {
        super(repo, client);
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message cannot be blank");
        }
        this.message = message;
    }

    @Override
    protected List<String> buildArgs() {
        return List.of("commit", "-m", message);
    }
}
