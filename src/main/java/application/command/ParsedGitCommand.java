package application.command;

import application.command.parser.GitOutputParser;
import domain.GitResult;
import domain.LocalRepo;
import infrastructure.git.GitClient;

import java.util.Objects;

/**
 * A base class for Git commands that execute and parse its output using a provided `GitOutputParser`.
 * This class extends GitCommand and adds functionality to handle the result of the Git command execution by parsing the output.
 *
 * @param <T> the type of the parsed result produced by the `GitOutputParser` after executing the command
 */
public abstract class ParsedGitCommand<T> extends GitCommand<T> {
    private final GitOutputParser<T> parser;

    /**
     * Creates a new ParsedGitCommand with the specified repository, Git client, and output parser.
     *
     * @param repo   the local repository on which the command will be executed
     * @param client the Git client to use for executing the command
     * @param parser the `GitOutputParser` to use for parsing the output of the Git command execution. Must not be null.
     */
    protected ParsedGitCommand(LocalRepo repo, GitClient client, GitOutputParser<T> parser) {
        super(repo, client);
        this.parser = Objects.requireNonNull(parser, "parser cannot be null");
    }

    @Override
    protected T handleResult(GitResult result) {
        if (!result.isSuccess()) {
            throw new IllegalStateException("Git command failed: " + result.stderr().trim());
        }
        return parser.parse(result.stdout());
    }
}
