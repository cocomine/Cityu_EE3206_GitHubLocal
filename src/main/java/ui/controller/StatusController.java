package ui.controller;

import application.AppFacade;
import domain.LocalRepo;
import domain.RepoStatus;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import ui.controller.common.RepoAwareController;
import ui.controller.status.StatusTextFormatter;

import static ui.controller.common.UiDialogs.showError;

/**
 * Controller for working tree/index status actions.
 * Supports stage/unstage/commit flows and displays staged/unstaged file changes.
 */
public class StatusController extends RepoAwareController {
    private static final String ERROR_HEADER = "Status Action Error";

    private final TextArea statusAreaStage = new TextArea();
    private final TextArea statusAreaUnstage = new TextArea();
    private final Parent view;

    public StatusController(AppFacade facade, ObjectProperty<LocalRepo> repoProperty) {
        super(facade, repoProperty);
        this.view = buildView();
    }

    @Override
    protected void onGitRepoReady(LocalRepo repo) {
        refreshStatus();
    }

    public Parent getView() {
        return view;
    }

    private Parent buildView() {
        TextField filePathField = new TextField();
        filePathField.setPromptText("file path (e.g. src/main/java/App.java or .)");
        filePathField.setMinWidth(350);

        TextField commitMessageField = new TextField();
        commitMessageField.setPromptText("commit message (need un-empty staged)");
        commitMessageField.setMinWidth(350);

        statusAreaStage.setEditable(false);
        statusAreaStage.setPromptText("Staged status output appears here.");

        statusAreaUnstage.setEditable(false);
        statusAreaUnstage.setPromptText("Unstaged status output appears here.");

        HBox stageBar = buildStageBar(filePathField);
        HBox commitBar = buildCommitBar(commitMessageField);
        VBox statusPane = buildStatusPane();

        VBox box = new VBox(10, stageBar, commitBar, statusPane);
        box.setPadding(new Insets(12));
        return box;
    }

    private HBox buildStageBar(TextField filePathField) {
        Button stageButton = new Button("Stage");
        stageButton.setOnAction(event -> {
            if (executeGitCommand(() -> facade.gitStage(requireRepo(), filePathField.getText()), ERROR_HEADER)) {
                refreshStatus();
            }
        });

        Button unstageButton = new Button("Unstage");
        unstageButton.setOnAction(event -> {
            if (executeGitCommand(() -> facade.gitUnstage(requireRepo(), filePathField.getText()), ERROR_HEADER)) {
                refreshStatus();
            }
        });

        return new HBox(10, new Label("Path:       "), filePathField, stageButton, unstageButton);
    }

    private HBox buildCommitBar(TextField commitMessageField) {
        Button commitButton = new Button("Commit");
        commitButton.setOnAction(event -> {
            if (executeGitCommand(() -> facade.gitCommit(requireRepo(), commitMessageField.getText()), ERROR_HEADER)) {
                refreshStatus();
                commitMessageField.clear();
            }
        });

        Button refreshButton = new Button("Refresh Status");
        refreshButton.setOnAction(event -> refreshStatus());

        return new HBox(10, new Label("Message:"), commitMessageField, commitButton, refreshButton);
    }

    private VBox buildStatusPane() {
        return new VBox(
                10,
                new Label("Staged:"), statusAreaStage,
                new Label("Unstaged:"), statusAreaUnstage
        );
    }

    private void refreshStatus() {
        try {
            RepoStatus status = facade.gitStatus(requireRepo());
            statusAreaStage.setText(StatusTextFormatter.format(status.staged()));
            statusAreaUnstage.setText(StatusTextFormatter.format(status.unstaged()));
        } catch (Exception e) {
            showError(ERROR_HEADER, e.getMessage());
        }
    }
}
