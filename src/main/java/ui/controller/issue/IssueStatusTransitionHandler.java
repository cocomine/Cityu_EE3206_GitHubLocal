package ui.controller.issue;

import application.AppFacade;
import domain.Issue;
import domain.IssueStatus;
import domain.LocalRepo;

import java.util.function.Function;

/**
 * Encapsulates issue status transition rules.
 * Applies domain-specific behavior for close/reopen transitions and actor requirements.
 */
public class IssueStatusTransitionHandler {
    private final AppFacade facade;

    public IssueStatusTransitionHandler(AppFacade facade) {
        this.facade = facade;
    }

    /**
     * Applies issue status transition rules.
     * DONE requires actor confirmation and maps to closeIssue;
     * DONE -> TODO maps to reopenIssue;
     * all other transitions use updateIssueStatus.
     */
    public void apply(LocalRepo repo, Issue issue, IssueStatus target,
                      Function<String, String> actorPrompt) {
        if (target == null) {
            throw new IllegalStateException("Please choose a target status.");
        }

        if (target.canonical() == IssueStatus.DONE) {
            String actor = actorPrompt.apply(issue.creator());
            if (actor == null) return;
            actor = actor.trim();
            if (actor.isEmpty()) {
                throw new IllegalStateException("Actor cannot be blank.");
            }
            facade.closeIssue(repo, issue.id(), actor);
            return;
        }

        if (target.canonical() == IssueStatus.TODO
                && issue.status() != null
                && issue.status().canonical() == IssueStatus.DONE) {
            facade.reopenIssue(repo, issue.id());
            return;
        }

        facade.updateIssueStatus(repo, issue.id(), target);
    }
}
