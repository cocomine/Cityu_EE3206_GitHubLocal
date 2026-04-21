package application;

import application.command.GitCommand;
import application.command.command.BranchListCommand;
import application.command.command.CheckoutCommand;
import application.command.command.CreateBranchCommand;
import application.command.command.LogCommand;
import application.command.command.MergeCommand;
import application.command.command.RollbackHardCommand;
import application.command.command.AddCommand;
import application.command.command.CommitCommand;
import application.command.command.DiffCommand;
import application.command.command.InitCommand;
import application.command.command.StatusCommand;
import application.command.command.UnstageCommand;
import domain.*;
import infrastructure.git.GitClient;
import infrastructure.git.SystemGitClient;
import infrastructure.store.JsonIssueStore;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Application-level facade that exposes use cases for Git and Issue operations.
 * Controllers call this class instead of talking directly to domain services or infrastructure.
 */
public class AppFacade {
    private final GitService gitService;
    private final IssueService issueService;
    private final GitClient gitClient;

    public AppFacade() {
        this(new GitService(), new IssueService(new JsonIssueStore()), new SystemGitClient());
    }

    public AppFacade(GitService gitService, IssueService issueService, GitClient gitClient) {
        this.gitService = Objects.requireNonNull(gitService, "gitService cannot be null");
        this.issueService = Objects.requireNonNull(issueService, "issueService cannot be null");
        this.gitClient = Objects.requireNonNull(gitClient, "gitClient cannot be null");
    }

    /**
     * Creates and returns a LocalRepo instance representing a local repository at the specified file system path.
     *
     * @param path the file system path to the local repository. Must not be null.
     * @return a LocalRepo instance representing the local repository at the specified path.
     */
    public LocalRepo openRepo(Path path) {
        return new LocalRepo(path);
    }

    /**
     * Initializes a new Git repository at the specified local repository path.
     * This method executes the {@link InitCommand} using the GitService and returns the result of the initialization process.
     *
     * @param repo the local repository to initialize as a Git repository. Must not be null.
     * @return a {@link GitResult} object containing the output and status of the git initialization command execution.
     */
    public GitResult gitInit(LocalRepo repo) throws IllegalArgumentException, IllegalStateException, NullPointerException {
        return executeGit(new InitCommand(repo, gitClient));
    }

    /**
     * Retrieves the status of the specified local repository by executing the {@link StatusCommand} using the GitService.
     * The method returns a {@link RepoStatus} object that contains lists of staged and unstaged changes in the repository.
     *
     * @param repo the local repository for which to retrieve the status. Must not be null.
     * @return a {@link RepoStatus} object representing the current status of the repository, including staged and unstaged changes.
     */
    public RepoStatus gitStatus(LocalRepo repo) throws IllegalArgumentException, IllegalStateException, NullPointerException {
        return executeGit(new StatusCommand(repo, gitClient));
    }

    /**
     * Stages the specified file path in the given local repository by executing the {@link AddCommand} using the GitService.
     *
     * @param repo the local repository for which to retrieve the status. Must not be null.
     * @param path the file path to stage in the repository. Must not be null.
     * @return a {@link GitResult} object containing the output and status of the git add command execution.
     */
    public GitResult gitStage(LocalRepo repo, String path) throws IllegalArgumentException, IllegalStateException, NullPointerException {
        return executeGit(new AddCommand(repo, gitClient, path));
    }

    /**
     * Unstages the specified file path in the given local repository by executing the {@link UnstageCommand}.
     *
     * @param repo The local repository. Must not be null.
     * @param path The file path to unstage. Must not be null.
     * @return A {@link GitResult} object containing the output and status of the git reset HEAD -- command execution.
     */
    public GitResult gitUnstage(LocalRepo repo, String path) throws IllegalArgumentException, IllegalStateException, NullPointerException {
        return executeGit(new UnstageCommand(repo, gitClient, path));
    }

    /**
     * Commits the staged changes in the given local repository with the specified message.
     * This method executes the {@link CommitCommand}.
     *
     * @param repo    The local repository. Must not be null.
     * @param message The commit message. Must not be null or blank.
     * @return A {@link GitResult} object containing the output and status of the git commit command execution.
     */
    public GitResult gitCommit(LocalRepo repo, String message) throws IllegalArgumentException, IllegalStateException, NullPointerException {
        return executeGit(new CommitCommand(repo, gitClient, message));
    }

    /**
     * Retrieves the commit history for the given local repository.
     * This method executes the {@link LogCommand}.
     *
     * @param repo     The local repository. Must not be null.
     * @param maxCount The maximum number of log entries to retrieve. Must be positive.
     * @return A list of {@link GitLog} objects representing the commit history.
     */
    public List<GitLog> gitLog(LocalRepo repo, int maxCount) throws IllegalArgumentException, IllegalStateException, NullPointerException {
        return executeGit(new LogCommand(repo, gitClient, maxCount));
    }

    /**
     * Retrieves the diff of the repository, showing changes between the index and the working tree,
     * or between the HEAD and the index.
     * This method executes the {@link DiffCommand}.
     *
     * @param repo   The local repository. Must not be null.
     * @param staged If true, shows the diff between HEAD and the index (staged changes).
     *               If false, shows the diff between the index and the working tree (unstaged changes).
     * @return A {@link GitDiff} object representing the parsed diff output.
     */
    public GitDiff gitDiff(LocalRepo repo, boolean staged) throws IllegalArgumentException, IllegalStateException, NullPointerException {
        return executeGit(new DiffCommand(repo, gitClient, staged));
    }

    /**
     * Retrieves a list of branches for the specified local repository.
     * This method executes the {@link BranchListCommand} to fetch branch information.
     *
     * @param repo The local repository from which to list branches. Must not be null.
     * @return A {@link BranchListInfo} object containing the list of available branches and identifying the currently active branch.
     */
    public BranchListInfo gitBranchList(LocalRepo repo) {
        return executeGit(new BranchListCommand(repo, gitClient));
    }

    /**
     * Checks out the specified branch in the given local repository.
     * This method executes the {@link CheckoutCommand} to switch branches.
     *
     * @param repo   The local repository where the checkout operation will be performed. Must not be null.
     * @param branch The name of the branch to check out. Must not be null or empty.
     * @return A {@link GitResult} object containing the output and status of the git checkout command execution.
     */
    public GitResult gitCheckout(LocalRepo repo, String branch) {
        return executeGit(new CheckoutCommand(repo, gitClient, branch));
    }

    /**
     * Creates a new branch in the specified local repository.
     * This method executes the {@link CreateBranchCommand} to create the branch.
     *
     * @param repo   The local repository where the new branch will be created. Must not be null.
     * @param branch The name of the new branch to create. Must not be null or blank.
     * @return A {@link GitResult} object containing the output and status of the git branch creation command execution.
     */
    public GitResult gitCreateBranch(LocalRepo repo, String branch) {
        return executeGit(new CreateBranchCommand(repo, gitClient, branch));
    }

    /**
     * Merges the specified branch into the currently active branch in the given local repository.
     * This method executes the {@link MergeCommand} to perform the merge operation.
     *
     * @param repo       The local repository where the merge will occur. Must not be null.
     * @param fromBranch The name of the branch to be merged into the current branch. Must not be null or blank.
     * @return A {@link GitResult} object containing the output and status of the git merge command execution.
     */
    public GitResult gitMerge(LocalRepo repo, String fromBranch) {
        return executeGit(new MergeCommand(repo, gitClient, fromBranch));
    }

    /**
     * Performs a hard rollback (reset) to the specified commit in the given local repository.
     * This method executes the {@link RollbackHardCommand} which resets the index and working tree.
     *
     * @param repo   The local repository where the rollback will be performed. Must not be null.
     * @param commit The commit hash or reference to roll back to. Must not be null or blank.
     * @return A {@link GitResult} object containing the output and status of the git reset command execution.
     */
    public GitResult gitRollbackHard(LocalRepo repo, String commit) {
        return executeGit(new RollbackHardCommand(repo, gitClient, commit));
    }

    public List<Issue> listIssues(LocalRepo repo) {
        return issueService.list(repo);
    }

    public List<Issue> queryIssues(LocalRepo repo, IssueQuery query) {
        return issueService.query(repo, query);
    }

    public IssueId createIssue(LocalRepo repo, String title, String creator) {
        return issueService.create(repo, title, creator);
    }

    public void commentIssue(LocalRepo repo, IssueId id, String author, String body) {
        issueService.comment(repo, id, author, body);
    }

    public void closeIssue(LocalRepo repo, IssueId id) {
        issueService.close(repo, id);
    }

    public void reopenIssue(LocalRepo repo, IssueId id) {
        issueService.reopen(repo, id);
    }

    /**
     * Executes the given GitCommand using the GitService and returns the result.
     * This method abstracts away the details of command execution
     *
     * @param command the GitCommand to execute. Must not be null.
     * @param <T>     the type of the result produced by the GitCommand execution.
     *                This is determined by the specific command implementation.
     * @return the result of executing the GitCommand,
     * which may be of any type depending on the specific command implementation.
     */
    private <T> T executeGit(GitCommand<T> command) throws IllegalArgumentException, IllegalStateException, NullPointerException {
        return gitService.execute(command);
    }

    // issue +
    public void updateIssueDescription(LocalRepo repo, IssueId id, String description) {
        issueService.updateDescription(repo, id, description);
    }

    public void editIssueComment(LocalRepo repo, IssueId issueId, CommentId commentId, String body) {
        issueService.editComment(repo, issueId, commentId, body);
    }

    public void deleteIssueComment(LocalRepo repo, IssueId issueId, CommentId commentId, String actor) {
        issueService.deleteComment(repo, issueId, commentId, actor);
    }

    public void closeIssue(LocalRepo repo, IssueId id, String actor) {
        issueService.close(repo, id, actor);
    }

    public void assignIssue(LocalRepo repo, IssueId id, String assignee) {
        issueService.assign(repo, id, assignee);
    }

    public void setIssuePriority(LocalRepo repo, IssueId id, IssuePriority priority) {
        issueService.setPriority(repo, id, priority);
    }

    public void setIssueLabels(LocalRepo repo, IssueId id, List<String> labels) {
        issueService.setLabels(repo, id, labels);
    }

    public void updateIssueStatus(LocalRepo repo, IssueId id, IssueStatus target) {
        issueService.updateStatus(repo, id, target);
    }
}
