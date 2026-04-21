package application.command;

import domain.GitResult;
import domain.LocalRepo;
import infrastructure.git.GitClient;

/**
 * A base class for Git commands that return a `GitResult`.
 * This class provides a common implementation for handling the GitResult returned by the `GitClient`,
 * allowing subclasses to focus on building the command arguments and executing the command.
 *
 * Subclasses should override the buildArgs() method to specify the specific git command and its arguments,
 * and can override handleResult() if they need to perform additional processing on the `GitResult` before returning it.
 */
public abstract class ResultGitCommand extends GitCommand<GitResult> {

    /**
     * Constructs a new ResultGitCommand with the specified repository and Git client.
     *
     * @param repo the local repository on which the command will operate. Must not be null.
     * @param client the Git client used to execute commands against the repository. Must not be null.
     */
    protected ResultGitCommand(LocalRepo repo, GitClient client) {
        super(repo, client);
    }

    @Override
    protected GitResult handleResult(GitResult result) {
        return result;
    }
}
