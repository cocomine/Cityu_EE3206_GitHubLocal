package ui.controller;

import application.AppFacade;
import domain.GitResult;
import domain.LocalRepo;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import ui.controller.repo.RepoInfoBinder;

import java.io.File;
import java.util.Objects;

import static ui.controller.common.UiDialogs.showError;
import static ui.controller.common.UiDialogs.showInfo;

/**
 * Controller for repository selection and initialization.
 * Lets users choose a local folder, open it as LocalRepo, and run git init when needed.
 */
public class RepoController {
    private static final String ERROR_HEADER = "Repository Action Error";

    private final AppFacade facade;
    private final ObjectProperty<LocalRepo> repoProperty;

    private final Label repoInfoLabel = new Label();
    private final Label repoGitLabel = new Label();

    private final Parent view;

    public RepoController(AppFacade facade, ObjectProperty<LocalRepo> repoProperty) {
        this.facade = Objects.requireNonNull(facade, "facade cannot be null");
        this.repoProperty = Objects.requireNonNull(repoProperty, "repoProperty cannot be null");

        RepoInfoBinder.bind(null, repoInfoLabel, repoGitLabel);
        this.view = buildView();

        repoProperty.addListener((obs, oldRepo, newRepo) ->
                RepoInfoBinder.bind(newRepo, repoInfoLabel, repoGitLabel)
        );
    }

    public Parent getView() {
        return view;
    }

    private Parent buildView() {
        Button chooseRepoButton = new Button("Choose Folder");
        chooseRepoButton.setOnAction(event ->
                openRepoDialog(chooseRepoButton.getScene().getWindow())
        );

        Button initRepoButton = new Button("Initialize Git (git init)");
        initRepoButton.setOnAction(event -> initializeGit());

        HBox actions = new HBox(10, chooseRepoButton, initRepoButton);

        VBox box = new VBox(
                10,
                actions,
                repoInfoLabel,
                repoGitLabel,
                new Label("Tip: choose any local folder, then initialize Git if needed.")
        );
        box.setPadding(new Insets(12));
        return box;
    }

    private void openRepoDialog(Window owner) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose Repository Folder");
        File selected = chooser.showDialog(owner);
        if (selected == null) {
            return;
        }

        try {
            LocalRepo repo = facade.openRepo(selected.toPath());
            repoProperty.set(repo);
        } catch (Exception e) {
            showError(ERROR_HEADER, e.getMessage());
        }
    }

    private void initializeGit() {
        LocalRepo repo = repoProperty.get();
        if (repo == null) {
            showError(ERROR_HEADER, "Please choose a folder first.");
            return;
        }

        try {
            GitResult result = facade.gitInit(repo);
            if (result.isSuccess()) {
                showInfo("Repository initialized successfully.");
            } else {
                showError(ERROR_HEADER, "git init failed:\n" + result.stderr());
            }

            LocalRepo refreshedRepo = facade.openRepo(repo.root());
            repoProperty.set(refreshedRepo);
            RepoInfoBinder.bind(refreshedRepo, repoInfoLabel, repoGitLabel);
        } catch (Exception e) {
            showError(ERROR_HEADER, e.getMessage());
        }
    }
}
