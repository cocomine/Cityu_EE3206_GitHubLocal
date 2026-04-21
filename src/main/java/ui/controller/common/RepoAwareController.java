package ui.controller.common;

import application.AppFacade;
import domain.GitResult;
import domain.LocalRepo;
import javafx.beans.property.ObjectProperty;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Base controller for tabs that require a selected repository.
 * Provides repo validation, git command execution helpers, and git-ready lifecycle hook.
 */
public abstract class RepoAwareController {
    protected final AppFacade facade;
    protected final ObjectProperty<LocalRepo> repoProperty;

    protected RepoAwareController(AppFacade facade, ObjectProperty<LocalRepo> repoProperty) {
        this.facade = Objects.requireNonNull(facade, "facade cannot be null");
        this.repoProperty = Objects.requireNonNull(repoProperty, "repoProperty cannot be null");

        this.repoProperty.addListener((obs, oldRepo, newRepo) -> {
            if (newRepo != null && newRepo.isGitRepo()) {
                onGitRepoReady(newRepo);
            }
        });
    }

    protected void onGitRepoReady(LocalRepo repo) {
        // default no-op
    }

    /**
     * Returns the currently selected repository or throws a user-facing error
     * when no repository has been selected yet.
     */
    protected LocalRepo requireRepo() {
        LocalRepo repo = repoProperty.get();
        if (repo == null) {
            throw new IllegalStateException("Please choose a repository in the Repository tab first.");
        }
        return repo;
    }

    /**
     * Executes a Git command and returns true only when the command succeeded.
     * Use this in simple button handlers that only need success/failure behavior.
     */
    protected boolean executeGitCommand(Supplier<GitResult> command, String errorHeader) {
        GitResult result = executeGitCommandInternal(command, errorHeader);
        return result != null && result.isSuccess();
    }

    /**
     * Executes a Git command and returns the raw GitResult for advanced flows
     * (e.g., custom logging or post-processing of stdout/stderr).
     */
    protected GitResult runGitCommandRaw(Supplier<GitResult> command, String errorHeader) {
        return executeGitCommandInternal(command, errorHeader);
    }

    /**
     * Shared command execution path with consistent error dialog behavior.
     * Converts exceptions and failed GitResult states into user-visible messages.
     */
    private GitResult executeGitCommandInternal(Supplier<GitResult> command, String errorHeader) {
        try {
            GitResult result = command.get();
            if (result == null) {
                UiDialogs.showError(errorHeader, "Command failed.");
                return null;
            }

            if (!result.isSuccess()) {
                UiDialogs.showError(errorHeader, extractErrorMessage(result));
            }

            return result;
        } catch (Exception e) {
            UiDialogs.showError(errorHeader, e.getMessage());
            return null;
        }
    }

    private String extractErrorMessage(GitResult result) {
        return result.stderr() == null || result.stderr().isBlank()
                ? "Command failed."
                : result.stderr();
    }
}
