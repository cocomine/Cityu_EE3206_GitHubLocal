package application.command.command;

import application.command.ResultGitCommand;
import domain.LocalRepo;
import infrastructure.git.GitClient;

import java.util.List;

/**
 * Represents a command to unstage a file from the Git index.
 * This command uses 'git restore --staged'.
 */
public class UnstageCommand extends ResultGitCommand {
    private final String path;

    /**
     * Constructs an UnstageCommand.
     *
     * @param repo   The local repository.
     * @param client The Git client.
     * @param path   The path of the file to unstage.
     * @throws IllegalArgumentException if the path is null or blank.
     */
    public UnstageCommand(LocalRepo repo, GitClient client, String path) throws IllegalArgumentException {
        super(repo, client);
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("path cannot be blank");
        }
        this.path = path;
    }

    @Override
    protected List<String> buildArgs() {
        return List.of("restore", "--staged", path);
    }
}
