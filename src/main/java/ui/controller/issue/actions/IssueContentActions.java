package ui.controller.issue.actions;

import application.AppFacade;
import domain.Comment;
import domain.Issue;
import domain.LocalRepo;
import ui.controller.common.UiDialogs;
import ui.controller.issue.IssueErrorPresenter;

/**
 * Action service for editing issue content (description/comments) and deleting comments.
 * Handles user prompts, validation, and error presentation.
 */
public class IssueContentActions {
    private final AppFacade facade;

    public IssueContentActions(AppFacade facade) {
        this.facade = facade;
    }

    public void editDescription(LocalRepo repo, Issue issue, Runnable onSuccess) {
        try {
            String updated = UiDialogs.promptMultiline(
                    "Edit Issue Description",
                    "Description",
                    issue.description()
            );

            if (updated == null) {
                return;
            }

            facade.updateIssueDescription(repo, issue.id(), updated);
            onSuccess.run();
        } catch (Exception e) {
            IssueErrorPresenter.show(e.getMessage());
        }
    }

    public void editComment(LocalRepo repo, Issue issue, Comment comment, Runnable onSuccess) {
        try {
            String updated = UiDialogs.promptMultiline(
                    "Edit Comment",
                    "Comment body",
                    comment.body()
            );

            if (updated == null) {
                return;
            }

            facade.editIssueComment(repo, issue.id(), comment.id(), updated);
            onSuccess.run();
        } catch (Exception e) {
            IssueErrorPresenter.show(e.getMessage());
        }
    }

    public void deleteComment(LocalRepo repo, Issue issue, Comment comment, Runnable onSuccess) {
        try {
            String actor = UiDialogs.promptSingleLine(
                    "Delete Comment",
                    "Deleted by",
                    comment.author()
            );

            if (actor == null) {
                return;
            }

            actor = actor.trim();
            if (actor.isEmpty()) {
                throw new IllegalStateException("Deleted by cannot be blank.");
            }

            facade.deleteIssueComment(repo, issue.id(), comment.id(), actor);
            onSuccess.run();
        } catch (Exception e) {
            IssueErrorPresenter.show(e.getMessage());
        }
    }
}
