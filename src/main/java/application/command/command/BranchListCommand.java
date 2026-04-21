package application.command.command;

import application.command.ParsedGitCommand;
import application.command.parser.BranchListParser;
import domain.BranchListInfo;
import domain.LocalRepo;
import infrastructure.git.GitClient;

import java.util.List;

/**
 * Represents a Git command to retrieve a list of branches in a repository.
 * Iterates upon the base {@link ParsedGitCommand} to specifically execute
 * the {@code git branch --list} command and parse its output.
 */
public class BranchListCommand extends ParsedGitCommand<BranchListInfo> {

    /**
     * Constructs a new {@code BranchListCommand}.
     *
     * @param repo   the local repository where the Git command will be executed
     * @param client the Git client responsible for executing the underlying Git processes
     */
    public BranchListCommand(LocalRepo repo, GitClient client) {
        super(repo, client, new BranchListParser());
    }

    /**
     * Builds the command-line arguments necessary for this Git command.
     *
     * @return a list of string arguments ({@code ["branch", "--list"]}) used to list the branches
     */
    @Override
    protected List<String> buildArgs() {
        return List.of("branch", "--list");
    }
}
