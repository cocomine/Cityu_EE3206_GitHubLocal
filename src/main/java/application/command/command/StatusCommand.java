package application.command.command;

import application.command.ParsedGitCommand;
import application.command.parser.RepoStatusParser;
import domain.LocalRepo;
import domain.RepoStatus;
import infrastructure.git.GitClient;

import java.util.List;

/**
 * A command to retrieve the status of a Git repository. It executes the `git status --porcelain` command
 * and parses the output to produce a `RepoStatus` object that contains lists of staged and unstaged changes.
 */
public class StatusCommand extends ParsedGitCommand<RepoStatus> {
    /**
     * Creates a new StatusCommand for the given repository and Git client.
     * @param repo the local repository for which to retrieve the status
     * @param client the Git client to use for executing the command
     */
    public StatusCommand(LocalRepo repo, GitClient client) {
        super(repo, client, new RepoStatusParser());
    }

    @Override
    protected List<String> buildArgs() {
        return List.of("status", "--porcelain");
    }
}
