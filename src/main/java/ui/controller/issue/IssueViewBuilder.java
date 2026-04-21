package ui.controller.issue;

import domain.Issue;
import domain.IssueStatus;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Builder for composing the Issue tab layout.
 * Assembles filter pane, issue list, forms, detail pane, and action bar wiring.
 */
public final class IssueViewBuilder {
    private static final double FILTER_MIN_WIDTH = 175;
    private static final double LEFT_PANE_WIDTH = 420;

    private IssueViewBuilder() {}

    public static Parent build(
            ListView<Issue> issueList,
            FilterControls filters,
            DetailControls details,
            IssueFormPaneFactory.IssueForms forms,
            ActionHandlers handlers
    ) {
        applyLayoutDefaults(filters, details);
        configureDetailLayout(details);

        VBox leftPane = buildLeftPane(issueList, filters, forms, handlers);

        SplitPane splitPane = new SplitPane(leftPane, details.detailScroll());
        splitPane.setDividerPositions(0.6);
        return splitPane;
    }

    private static void applyLayoutDefaults(FilterControls filters, DetailControls details) {
        filters.statusFilter().setMinWidth(FILTER_MIN_WIDTH);
        filters.priorityFilter().setMinWidth(FILTER_MIN_WIDTH);
        filters.sortFilter().setMinWidth(FILTER_MIN_WIDTH);

        details.detailScroll().setFitToWidth(true);
        details.detailScroll().setPannable(true);
        details.detailPane().setPadding(new Insets(16));
    }

    private static VBox buildLeftPane(
            ListView<Issue> issueList,
            FilterControls filters,
            IssueFormPaneFactory.IssueForms forms,
            ActionHandlers handlers
    ) {
        VBox leftPane = new VBox(
                12,
                buildActionBar(handlers),
                buildFilterRow("Search   ", filters.searchField(), "Status  ", filters.statusFilter()),
                buildFilterRow("Assignee", filters.assigneeFilter(), "Priority", filters.priorityFilter()),
                buildFilterRow("Labels    ", filters.labelsFilter(), "Sort    ", filters.sortFilter()),
                issueList,
                new Separator(),
                forms.issueCommentBox()
        );

        leftPane.setPadding(new Insets(12));
        leftPane.setPrefWidth(LEFT_PANE_WIDTH);
        VBox.setVgrow(issueList, Priority.ALWAYS);
        return leftPane;
    }

    private static HBox buildActionBar(ActionHandlers handlers) {
        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(event -> handlers.refreshAction().run());

        Button clearFiltersButton = new Button("Clear Filters");
        clearFiltersButton.setOnAction(event -> handlers.clearFiltersAction().run());

        Button closeButton = new Button("Close");
        closeButton.setOnAction(event -> handlers.closeAction().run());

        Button reopenButton = new Button("Reopen");
        reopenButton.setOnAction(event -> handlers.reopenAction().run());

        HBox actionBar = new HBox(10, refreshButton, clearFiltersButton, closeButton, reopenButton);
        actionBar.setAlignment(Pos.CENTER_LEFT);
        return actionBar;
    }

    private static HBox buildFilterRow(
            String leftLabelText,
            Region leftControl,
            String rightLabelText,
            Region rightControl
    ) {
        HBox row = new HBox(
                10,
                new Label(leftLabelText),
                leftControl,
                new Label(rightLabelText),
                rightControl
        );
        HBox.setHgrow(leftControl, Priority.ALWAYS);
        return row;
    }

    private static void configureDetailLayout(DetailControls details) {
        Label descriptionHeader = new Label("Description");
        HBox descriptionBar = new HBox(10, descriptionHeader, details.editDescriptionButton());

        Label commentsHeader = new Label("Comments");
        HBox statusActionBar = new HBox(10, details.statusActionBox(), details.updateStatusButton());

        details.detailPane().getChildren().setAll(
                details.issueTitleLabel(),
                details.metaLabel(),
                details.statusBadge(),
                details.assigneeLabel(),
                details.priorityLabel(),
                details.labelsLabel(),
                details.closedInfoLabel(),
                statusActionBar,
                new Separator(),
                descriptionBar,
                details.descriptionArea(),
                new Separator(),
                commentsHeader,
                details.commentsBox()
        );
    }

    public record FilterControls(
            TextField searchField,
            ComboBox<String> statusFilter,
            TextField assigneeFilter,
            ComboBox<String> priorityFilter,
            TextField labelsFilter,
            ComboBox<String> sortFilter
    ) {}

    public record DetailControls(
            VBox detailPane,
            ScrollPane detailScroll,
            Label issueTitleLabel,
            Label statusBadge,
            Label metaLabel,
            Label assigneeLabel,
            Label priorityLabel,
            Label labelsLabel,
            Label closedInfoLabel,
            TextArea descriptionArea,
            VBox commentsBox,
            Button editDescriptionButton,
            ComboBox<IssueStatus> statusActionBox,
            Button updateStatusButton
    ) {}

    public record ActionHandlers(
            Runnable refreshAction,
            Runnable clearFiltersAction,
            Runnable closeAction,
            Runnable reopenAction
    ) {}
}
