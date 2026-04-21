package ui.controller.issue.actions;

import application.AppFacade;
import domain.Issue;
import domain.IssueStatus;
import domain.LocalRepo;
import ui.controller.common.UiDialogs;
import ui.controller.issue.IssueErrorPresenter;
import ui.controller.issue.IssueStatusTransitionHandler;

/**
 * Action service for issue state changes (close, reopen, status update).
 * Handles dialog prompts and routes transitions through facade/domain rules.
 */
public class IssueStateActions {
    private final AppFacade facade;
    private final IssueStatusTransitionHandler transitionHandler;

    public IssueStateActions(AppFacade facade) {
        this.facade = facade;
        this.transitionHandler = new IssueStatusTransitionHandler(facade);
    }

    public void closeOrReopen(LocalRepo repo, Issue issue, boolean close, Runnable onSuccess) {
        try {
            if (close) {
                String actor = UiDialogs.promptSingleLine(
                        "Close Issue",
                        "Closed by",
                        issue.creator()
                );

                if (actor == null) {
                    return;
                }

                actor = actor.trim();
                if (actor.isEmpty()) {
                    throw new IllegalStateException("Closed by cannot be blank.");
                }

                facade.closeIssue(repo, issue.id(), actor);
            } else {
                facade.reopenIssue(repo, issue.id());
            }

            onSuccess.run();
        } catch (Exception e) {
            IssueErrorPresenter.show(e.getMessage());
        }
    }

    public void updateStatus(LocalRepo repo, Issue issue, IssueStatus target, Runnable onSuccess) {
        try {
            transitionHandler.apply(
                    repo,
                    issue,
                    target,
                    initial -> UiDialogs.promptSingleLine("Update Issue Status", "Changed by", initial)
            );
            onSuccess.run();
        } catch (Exception e) {
            IssueErrorPresenter.show(e.getMessage());
        }
    }
}
