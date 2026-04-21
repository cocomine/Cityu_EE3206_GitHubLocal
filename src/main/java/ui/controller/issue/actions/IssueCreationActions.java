package ui.controller.issue.actions;

import application.AppFacade;
import domain.Issue;
import domain.LocalRepo;
import ui.controller.issue.IssueErrorPresenter;

/**
 * Action service for creating issues and adding comments.
 * Wraps facade calls and centralizes issue error presentation.
 */
public class IssueCreationActions {
    private final AppFacade facade;

    public IssueCreationActions(AppFacade facade) {
        this.facade = facade;
    }

    public void createIssue(LocalRepo repo, String title, String creator, Runnable onSuccess) {
        try {
            facade.createIssue(repo, title, creator);
            onSuccess.run();
        } catch (Exception e) {
            IssueErrorPresenter.show(e.getMessage());
        }
    }

    public void addComment(LocalRepo repo, Issue issue, String author, String body, Runnable onSuccess) {
        try {
            facade.commentIssue(repo, issue.id(), author, body);
            onSuccess.run();
        } catch (Exception e) {
            IssueErrorPresenter.show(e.getMessage());
        }
    }
}
