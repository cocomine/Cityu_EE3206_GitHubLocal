package ui.controller.issue.actions;

import application.AppFacade;
import domain.Comment;
import domain.Issue;
import domain.IssueStatus;
import domain.LocalRepo;

/**
 * Facade-like coordinator for issue UI actions.
 * Delegates creation, content, and state operations to specialized action services.
 */
public class IssueActions {
    private final IssueCreationActions creationActions;
    private final IssueContentActions contentActions;
    private final IssueStateActions stateActions;

    public IssueActions(AppFacade facade) {
        this.creationActions = new IssueCreationActions(facade);
        this.contentActions = new IssueContentActions(facade);
        this.stateActions = new IssueStateActions(facade);
    }

    public void createIssue(LocalRepo repo, String title, String creator, Runnable onSuccess) {
        creationActions.createIssue(repo, title, creator, onSuccess);
    }

    public void addComment(LocalRepo repo, Issue issue, String author, String body, Runnable onSuccess) {
        creationActions.addComment(repo, issue, author, body, onSuccess);
    }

    public void editDescription(LocalRepo repo, Issue issue, Runnable onSuccess) {
        contentActions.editDescription(repo, issue, onSuccess);
    }

    public void editComment(LocalRepo repo, Issue issue, Comment comment, Runnable onSuccess) {
        contentActions.editComment(repo, issue, comment, onSuccess);
    }

    public void deleteComment(LocalRepo repo, Issue issue, Comment comment, Runnable onSuccess) {
        contentActions.deleteComment(repo, issue, comment, onSuccess);
    }

    public void closeOrReopen(LocalRepo repo, Issue issue, boolean close, Runnable onSuccess) {
        stateActions.closeOrReopen(repo, issue, close, onSuccess);
    }

    public void updateStatus(LocalRepo repo, Issue issue, IssueStatus target, Runnable onSuccess) {
        stateActions.updateStatus(repo, issue, target, onSuccess);
    }
}
