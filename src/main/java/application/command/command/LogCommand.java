package application.command.command;

import application.command.ParsedGitCommand;
import application.command.parser.LogParser;
import domain.GitLog;
import domain.LocalRepo;
import infrastructure.git.GitClient;

import java.util.List;

/**
 * Represents a command to show commit logs.
 * This command uses "git log --decorate=short -n --pretty=format:%h %d %s | %an | %ad".
 */
public class LogCommand extends ParsedGitCommand<List<GitLog>> {
    private final int maxCount;

    /**
     * Constructs a LogCommand.
     *
     * @param repo     The local repository.
     * @param client   The Git client.
     * @param maxCount The maximum number of log entries to show.
     * @throws IllegalArgumentException if maxCount is not positive.
     */
    public LogCommand(LocalRepo repo, GitClient client, int maxCount) {
        super(repo, client, new LogParser());
        if (maxCount <= 0) {
            throw new IllegalArgumentException("maxCount must be positive");
        }
        this.maxCount = maxCount;
    }

    @Override
    protected List<String> buildArgs() {
        return List.of("log", "--decorate=short", "-n", String.valueOf(maxCount), "--pretty=format:%h %d %s | %an | %ad");
    }
}
