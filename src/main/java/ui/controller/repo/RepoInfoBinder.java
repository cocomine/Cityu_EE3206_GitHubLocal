package ui.controller.repo;

import domain.LocalRepo;
import javafx.scene.control.Label;

/**
 * Utility binder for repository summary labels.
 * Shows selected path and whether the folder is a Git repository.
 */
public final class RepoInfoBinder {
    private RepoInfoBinder() {}

    public static void bind(LocalRepo repo, Label repoInfoLabel, Label repoGitLabel) {
        if (repo == null) {
            repoInfoLabel.setText("No repository selected.");
            repoGitLabel.setText("Git initialized: no");
            return;
        }

        repoInfoLabel.setText("Path: " + repo.root());
        repoGitLabel.setText("Git initialized: " + (repo.isGitRepo() ? "yes" : "no"));
    }
}
