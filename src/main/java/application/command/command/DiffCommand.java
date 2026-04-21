package application.command.command;

import application.command.ParsedGitCommand;
import application.command.parser.DiffParser;
import domain.GitDiff;
import domain.LocalRepo;
import infrastructure.git.GitClient;

import java.util.List;

/**
 * Command to execute `git diff` and parse the output.
 * This command can show either staged or unstaged changes.
 */
public class DiffCommand extends ParsedGitCommand<GitDiff> {
    private final boolean staged;

    /**
     * Constructs a DiffCommand.
     *
     * @param repo The local repository to run the command in.
     * @param client The Git client to use for executing the command.
     * @param staged If true, shows staged changes (`git diff --staged`).
     *               If false, shows unstaged changes (`git diff`).
     */
    public DiffCommand(LocalRepo repo, GitClient client, boolean staged) {
        super(repo, client, new DiffParser());
        this.staged = staged;
    }

    @Override
    protected List<String> buildArgs() {
        if (staged) {
            return List.of("diff", "--staged", "--no-color", "--no-ext-diff", "--find-renames", "-U3");
        }
        return List.of("diff", "--no-color", "--no-ext-diff", "--find-renames", "-U3");
    }
}
