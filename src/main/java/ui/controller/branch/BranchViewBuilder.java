package ui.controller.branch;

import domain.GitLog;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Builder for Branch tab layout.
 * Assembles branch controls, status labels, and action button wiring.
 */
public final class BranchViewBuilder {
    private BranchViewBuilder() {}

    public static Parent build(
            BranchControls controls,
            BranchStatusControls statusControls,
            BranchActionHandlers handlers
    ) {
        Button refreshButton = new Button("Refresh Branches");
        refreshButton.setOnAction(event -> handlers.refreshAction().run());

        Button createButton = new Button("Create Branch");
        createButton.setOnAction(event -> handlers.createAction().run());

        Button checkoutButton = new Button("Checkout to be Current");
        checkoutButton.setOnAction(event -> handlers.checkoutAction().run());

        Button mergeButton = new Button("Merge to Current");
        mergeButton.setOnAction(event -> handlers.mergeAction().run());

        Button rollbackButton = new Button("Rollback Hard");
        rollbackButton.setOnAction(event -> handlers.rollbackAction().run());

        HBox row1 = new HBox(10, refreshButton);
        HBox row2 = buildActionRow("Create:    ", controls.createField(), createButton);
        HBox row3 = buildActionRow("Checkout:", controls.checkoutBranchBox(), checkoutButton);
        HBox row4 = buildActionRow("Merge:    ", controls.mergeBranchBox(), mergeButton);
        HBox row5 = buildActionRow("Rollback: ", controls.rollbackCommitBox(), rollbackButton);

        VBox statusBox = new VBox(6, statusControls.currentBranchLabel(), statusControls.lastMergeLabel());

        VBox box = new VBox(
                10,
                row1,
                statusBox,
                controls.branchList(),
                row2,
                row3,
                row4,
                row5,
                controls.resultArea()
        );
        box.setPadding(new Insets(12));
        VBox.setVgrow(controls.branchList(), Priority.ALWAYS);
        return box;
    }

    private static HBox buildActionRow(String labelText, Region inputControl, Button actionButton) {
        return new HBox(10, new Label(labelText), inputControl, actionButton);
    }

    public record BranchControls(
            TextField createField,
            ComboBox<String> checkoutBranchBox,
            ComboBox<String> mergeBranchBox,
            ComboBox<GitLog> rollbackCommitBox,
            ListView<String> branchList,
            TextArea resultArea
    ) {}

    public record BranchStatusControls(
            Label currentBranchLabel,
            Label lastMergeLabel
    ) {}

    public record BranchActionHandlers(
            Runnable refreshAction,
            Runnable createAction,
            Runnable checkoutAction,
            Runnable mergeAction,
            Runnable rollbackAction
    ) {}
}
