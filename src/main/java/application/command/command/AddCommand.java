package application.command.command;

import application.command.ResultGitCommand;
import domain.LocalRepo;
import infrastructure.git.GitClient;

import java.util.List;

/**
 * Represents a command to add file contents to the index.
 */
public class AddCommand extends ResultGitCommand {
    private final String path;

    /**
     * Constructs an AddCommand.
     *
     * @param repo   The local repository.
     * @param client The Git client.
     * @param path   The path of the file to add.
     * @throws IllegalArgumentException if the path is null or blank.
     */
    public AddCommand(LocalRepo repo, GitClient client, String path) throws IllegalArgumentException {
        super(repo, client);
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("path cannot be blank");
        }
        this.path = path;
    }

    @Override
    protected List<String> buildArgs() {
        return List.of("add", path);
    }
}
