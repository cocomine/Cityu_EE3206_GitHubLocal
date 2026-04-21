package ui.controller;

import application.AppFacade;
import domain.GitDiff;
import domain.LocalRepo;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import ui.controller.common.RepoAwareController;
import ui.controller.diff.DiffDisplayMapper;

import static ui.controller.common.UiDialogs.showError;

/**
 * Controller for viewing repository diffs.
 * Loads staged/unstaged diffs, shows changed files, and renders selected patch content.
 */
public class DifferenceController extends RepoAwareController {
    private static final String ERROR_HEADER = "Difference Action Error";

    private final CheckBox stagedDiffCheck = new CheckBox("Staged Diff");
    private final TableView<GitDiff.FileDiff> fileTable = new TableView<>();
    private final ListView<DiffDisplayMapper.DiffDisplayLine> patchList = new ListView<>();

    private final Parent view;

    public DifferenceController(AppFacade facade, ObjectProperty<LocalRepo> repoProperty) {
        super(facade, repoProperty);
        configureControls();
        this.view = buildView();
    }

    @Override
    protected void onGitRepoReady(LocalRepo repo) {
        loadDiff();
    }

    public Parent getView() {
        return view;
    }

    private void configureControls() {
        configureFileTable();
        configurePatchList();

        fileTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, selected) -> {
            if (selected == null) {
                patchList.getItems().clear();
            } else {
                patchList.getItems().setAll(DiffDisplayMapper.toDisplayLines(selected));
            }
        });
    }

    private void configureFileTable() {
        fileTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<GitDiff.FileDiff, String> statusCol = new TableColumn<>("Status");
        statusCol.setMaxWidth(90);
        statusCol.setCellValueFactory(d ->
                new ReadOnlyStringWrapper(DiffDisplayMapper.mapFileStatus(d.getValue().status())));

        TableColumn<GitDiff.FileDiff, String> pathCol = new TableColumn<>("File");
        pathCol.setCellValueFactory(d ->
                new ReadOnlyStringWrapper(DiffDisplayMapper.renderPath(d.getValue())));

        fileTable.getColumns().addAll(statusCol, pathCol);
    }

    private void configurePatchList() {
        patchList.setCellFactory(DiffDisplayMapper.createCellFactory());
    }

    private Parent buildView() {
        Button showDiffButton = new Button("Show Diff");
        showDiffButton.setOnAction(event -> loadDiff());

        HBox diffBar = new HBox(10, stagedDiffCheck, showDiffButton);

        SplitPane splitPane = new SplitPane(fileTable, patchList);
        splitPane.setDividerPositions(0.25);

        VBox box = new VBox(10, diffBar, splitPane);
        box.setPadding(new Insets(12));
        box.setPrefHeight(700);
        VBox.setVgrow(splitPane, Priority.ALWAYS);
        return box;
    }

    /**
     * Loads staged or unstaged diff based on checkbox state
     * and updates file table and patch preview.
     */
    private void loadDiff() {
        try {
            GitDiff diff = facade.gitDiff(requireRepo(), stagedDiffCheck.isSelected());
            fileTable.getItems().setAll(diff.files());

            if (!diff.files().isEmpty()) {
                fileTable.getSelectionModel().selectFirst();
            } else {
                patchList.getItems().setAll(DiffDisplayMapper.noChangesLine());
            }
        } catch (Exception e) {
            showError(ERROR_HEADER, e.getMessage());
        }
    }
}
