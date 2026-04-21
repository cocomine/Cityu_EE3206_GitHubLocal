package ui.controller;

import application.AppFacade;
import domain.BranchListInfo;
import domain.GitLog;
import domain.GitResult;
import domain.LocalRepo;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.collections.FXCollections;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import ui.controller.branch.BranchCommandLogFormatter;
import ui.controller.branch.BranchSelectionFormatter;
import ui.controller.branch.BranchStatusFormatter;
import ui.controller.branch.BranchViewBuilder;
import ui.controller.common.RepoAwareController;

import java.util.List;

import static ui.controller.common.UiDialogs.showError;

/**
 * Controller for branch-related workflows (list, create, checkout, merge, rollback).
 * Coordinates UI state with repository operations and keeps branch/rollback views synchronized.
 */
public class BranchController extends RepoAwareController {
    private static final String ERROR_HEADER = "Branch Action Error";
    private static final int ROLLBACK_COMMIT_LIMIT = 20;
    private static final double INPUT_BOX_WIDTH = 260;

    private final ListView<String> branchList = new ListView<>();
    private final TextArea resultArea = new TextArea();

    private final Label currentBranchLabel = new Label();
    private final Label lastMergeLabel = new Label();

    private String currentBranchName;

    // Create still uses text input because it needs a new branch name
    private final TextField createField = new TextField();

    // Existing values are better represented as dropdowns
    private final ComboBox<String> checkoutBranchBox = new ComboBox<>();
    private final ComboBox<String> mergeBranchBox = new ComboBox<>();
    private final ComboBox<GitLog> rollbackCommitBox = new ComboBox<>();

    private final Parent view;

    public BranchController(AppFacade facade, ObjectProperty<LocalRepo> repoProperty) {
        super(facade, repoProperty);
        configureControls();

        BranchViewBuilder.BranchControls branchControls = new BranchViewBuilder.BranchControls(
                createField,
                checkoutBranchBox,
                mergeBranchBox,
                rollbackCommitBox,
                branchList,
                resultArea
        );

        BranchViewBuilder.BranchStatusControls statusControls = new BranchViewBuilder.BranchStatusControls(
                currentBranchLabel,
                lastMergeLabel
        );

        BranchViewBuilder.BranchActionHandlers actionHandlers = new BranchViewBuilder.BranchActionHandlers(
                this::refreshBranchData,
                this::handleCreate,
                this::handleCheckout,
                this::handleMerge,
                this::handleRollback
        );

        this.view = BranchViewBuilder.build(branchControls, statusControls, actionHandlers);
    }

    @Override
    protected void onGitRepoReady(LocalRepo repo) {
        refreshBranchData();
    }

    public Parent getView() {
        return view;
    }

    private void configureControls() {
        int minWidth = 280;     // 263

        createField.setPromptText("new branch name");
        createField.setPrefWidth(INPUT_BOX_WIDTH);
        createField.setMaxWidth(Region.USE_PREF_SIZE);
        createField.setMinWidth(minWidth + 3);

        checkoutBranchBox.setPromptText("select branch to be current");
        checkoutBranchBox.setPrefWidth(INPUT_BOX_WIDTH);
        checkoutBranchBox.setMaxWidth(Region.USE_PREF_SIZE);
        checkoutBranchBox.setMinWidth(minWidth);

        mergeBranchBox.setPromptText("select branch merge to current");
        mergeBranchBox.setPrefWidth(INPUT_BOX_WIDTH);
        mergeBranchBox.setMaxWidth(Region.USE_PREF_SIZE);
        mergeBranchBox.setMinWidth(minWidth);

        rollbackCommitBox.setPromptText("select commit for hard reset");
        rollbackCommitBox.setPrefWidth(INPUT_BOX_WIDTH);
        rollbackCommitBox.setMaxWidth(Region.USE_PREF_SIZE);
        rollbackCommitBox.setMinWidth(minWidth);
        configureRollbackCommitBox();

        branchList.setPrefHeight(220);

        resultArea.setEditable(false);
        resultArea.setWrapText(true);
        resultArea.setPromptText("Branch command output appears here.");

        currentBranchLabel.setText(BranchStatusFormatter.formatCurrentBranch(null));
        lastMergeLabel.setText(BranchStatusFormatter.formatLastMerge(null, null));
    }

    private void configureRollbackCommitBox() {
        rollbackCommitBox.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(GitLog item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : BranchSelectionFormatter.formatRollbackOption(item));
            }
        });

        rollbackCommitBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(GitLog item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : BranchSelectionFormatter.formatRollbackOption(item));
            }
        });
    }

    private void handleCheckout() {
        try {
            String targetBranch = resolveCheckoutBranch();
            executeResultAction(
                    "Checkout Branch",
                    () -> facade.gitCheckout(requireRepo(), targetBranch),
                    true
            );
        } catch (IllegalStateException e) {
            showError(ERROR_HEADER, e.getMessage());
        }
    }

    private void handleCreate() {
        executeResultAction(
                "Create Branch",
                () -> facade.gitCreateBranch(requireRepo(), createField.getText()),
                true
        );
        createField.clear();
    }

    private void handleRollback() {
        executeResultAction(
                "Rollback Hard",
                () -> facade.gitRollbackHard(requireRepo(), resolveRollbackCommit()),
                true
        );
    }

    private void handleMerge() {
        try {
            String sourceBranch = resolveMergeBranch();
            String targetBranch = resolveCurrentBranchForMerge();

            boolean success = executeResultAction(
                    "Merge Branch",
                    () -> facade.gitMerge(requireRepo(), sourceBranch),
                    true
            );

            if (success) {
                lastMergeLabel.setText(
                        BranchStatusFormatter.formatLastMerge(targetBranch, sourceBranch)
                );
            }
        } catch (IllegalStateException e) {
            showError(ERROR_HEADER, e.getMessage());
        }
    }

    /**
     * Reloads branch and recent commit data from Git, then updates all branch-related controls.
     * Called after successful branch commands to keep UI state consistent with repository state.
     */
    private void refreshBranchData() {
        try {
            BranchListInfo info = facade.gitBranchList(requireRepo());
            List<GitLog> logs = facade.gitLog(requireRepo(), ROLLBACK_COMMIT_LIMIT);

            applyBranchListInfo(info);
            updateRollbackChoices(logs);
        } catch (Exception e) {
            showError(ERROR_HEADER, e.getMessage());
        }
    }

    /**
     * Applies branch list data to list/combobox controls and refreshes current branch label.
     * Preserves the user's selection when possible.
     */
    private void applyBranchListInfo(BranchListInfo info) {
        List<String> branches = info == null ? List.of() : info.branches();

        currentBranchName = (info != null && info.hasCurrentBranch())
                ? info.currentBranch()
                : "";

        currentBranchLabel.setText(BranchStatusFormatter.formatCurrentBranch(currentBranchName));

        branchList.setItems(FXCollections.observableArrayList(branches));
        setStringComboItemsPreservingSelection(checkoutBranchBox, branches);
        setStringComboItemsPreservingSelection(mergeBranchBox, branches);

        if (info != null && info.hasCurrentBranch()) {
            branchList.getSelectionModel().select(info.currentBranch());
        } else {
            branchList.getSelectionModel().clearSelection();
        }
    }

    /**
     * Replaces rollback commit options while preserving currently selected commit by hash
     * if that commit is still present after refresh.
     */
    private void updateRollbackChoices(List<GitLog> logs) {
        GitLog currentValue = rollbackCommitBox.getValue();
        rollbackCommitBox.setItems(FXCollections.observableArrayList(logs));

        if (currentValue != null && containsCommit(logs, currentValue.hash())) {
            rollbackCommitBox.setValue(findCommitByHash(logs, currentValue.hash()));
        } else {
            rollbackCommitBox.setValue(null);
        }
    }

    private void setStringComboItemsPreservingSelection(ComboBox<String> comboBox, List<String> values) {
        String currentValue = comboBox.getValue();
        comboBox.setItems(FXCollections.observableArrayList(values));

        if (currentValue != null && values.contains(currentValue)) {
            comboBox.setValue(currentValue);
        } else {
            comboBox.setValue(null);
        }
    }

    private boolean containsCommit(List<GitLog> logs, String hash) {
        return logs.stream().anyMatch(log -> log.hash().equals(hash));
    }

    private GitLog findCommitByHash(List<GitLog> logs, String hash) {
        return logs.stream()
                .filter(log -> log.hash().equals(hash))
                .findFirst()
                .orElse(null);
    }

    private String resolveCheckoutBranch() {
        return resolveBranchSelection(
                checkoutBranchBox,
                "Please select a branch to checkout."
        );
    }

    private String resolveMergeBranch() {
        return resolveBranchSelection(
                mergeBranchBox,
                "Please select a branch to merge."
        );
    }

    /**
     * Resolves selected branch with fallback priority:
     * 1) explicit ComboBox selection, 2) selected row in branch list.
     * Throws a validation error if neither is available.
     */
    private String resolveBranchSelection(ComboBox<String> comboBox, String emptyMessage) {
        String selectedValue = comboBox.getValue();
        if (selectedValue != null && !selectedValue.isBlank()) {
            return selectedValue;
        }

        String selectedBranch = branchList.getSelectionModel().getSelectedItem();
        if (selectedBranch != null && !selectedBranch.isBlank()) {
            return selectedBranch;
        }

        throw new IllegalStateException(emptyMessage);
    }

    private String resolveCurrentBranchForMerge() {
        if (currentBranchName != null && !currentBranchName.isBlank()) {
            return currentBranchName;
        }

        throw new IllegalStateException("Cannot determine the current checked-out branch. Please refresh branches first.");
    }

    private String resolveRollbackCommit() {
        GitLog selectedCommit = rollbackCommitBox.getValue();
        if (selectedCommit != null && selectedCommit.hash() != null && !selectedCommit.hash().isBlank()) {
            return selectedCommit.hash();
        }

        throw new IllegalStateException("Please select a commit to roll back to.");
    }

    /**
     * Executes a branch command, appends formatted output to command log,
     * and optionally refreshes branch/commit data on success.
     */
    private boolean executeResultAction(String actionName,
                                        java.util.function.Supplier<GitResult> supplier,
                                        boolean refreshAfter) {
        GitResult result = runGitCommandRaw(supplier, ERROR_HEADER);
        if (result == null || !result.isSuccess()) {
            return false;
        }

        appendCommandLog(actionName, result);

        if (refreshAfter) {
            refreshBranchData();
        }
        return true;
    }

    private void appendCommandLog(String actionName, GitResult result) {
        resultArea.setText(
                BranchCommandLogFormatter.append(
                        resultArea.getText(),
                        actionName,
                        result
                )
        );
        scrollCommandLogToBottom();
    }

    /**
     * Scrolls command log to bottom after UI update so newest command output is visible.
     */
    private void scrollCommandLogToBottom() {
        Platform.runLater(() -> {
            resultArea.positionCaret(resultArea.getText().length());
            resultArea.setScrollTop(Double.MAX_VALUE);
        });
    }
}
