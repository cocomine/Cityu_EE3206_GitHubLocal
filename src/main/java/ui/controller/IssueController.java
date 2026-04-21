package ui.controller;

import application.AppFacade;
import application.IssueQuery;
import domain.Comment;
import domain.Issue;
import domain.IssueStatus;
import domain.LocalRepo;
import javafx.beans.property.ObjectProperty;
import javafx.collections.FXCollections;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import ui.controller.common.RepoAwareController;
import ui.controller.issue.*;
import ui.controller.issue.actions.IssueActions;

/**
 * Controller for issue management UI.
 * Handles filtering, selection, creation, comments, description edits, and status transitions.
 */
public class IssueController extends RepoAwareController {
    private final IssueActions actions;
    private final Parent view;

    // Left side
    private final ListView<Issue> issueList = new ListView<>();

    // Filters
    private final TextField searchField = new TextField();
    private final ComboBox<String> statusFilter = new ComboBox<>();
    private final TextField assigneeFilter = new TextField();
    private final ComboBox<String> priorityFilter = new ComboBox<>();
    private final TextField labelsFilter = new TextField();
    private final ComboBox<String> sortFilter = new ComboBox<>();

    // Right side detail
    private final VBox detailPane = new VBox(12);
    private final ScrollPane detailScroll = new ScrollPane(detailPane);

    private final Label issueTitleLabel = new Label("Select an issue");
    private final Label statusBadge = new Label();
    private final Label metaLabel = new Label();
    private final Label assigneeLabel = new Label();
    private final Label priorityLabel = new Label();
    private final Label labelsLabel = new Label();
    private final Label closedInfoLabel = new Label();

    private final TextArea descriptionArea = new TextArea();
    private final VBox commentsBox = new VBox(12);

    private final Button editDescriptionButton = new Button("Edit Description");
    private final ComboBox<IssueStatus> statusActionBox = new ComboBox<>();
    private final Button updateStatusButton = new Button("Update Status");

    public IssueController(AppFacade facade, ObjectProperty<LocalRepo> repoProperty) {
        super(facade, repoProperty);
        this.actions = new IssueActions(facade);

        configureFilterControls();
        configureIssueList();
        configureDetailControls();

        IssueFormPaneFactory.IssueForms forms = IssueFormPaneFactory.create();
        bindFormActions(forms);

        IssueViewBuilder.FilterControls filterControls = new IssueViewBuilder.FilterControls(
                searchField,
                statusFilter,
                assigneeFilter,
                priorityFilter,
                labelsFilter,
                sortFilter
        );

        IssueViewBuilder.DetailControls detailControls = new IssueViewBuilder.DetailControls(
                detailPane,
                detailScroll,
                issueTitleLabel,
                statusBadge,
                metaLabel,
                assigneeLabel,
                priorityLabel,
                labelsLabel,
                closedInfoLabel,
                descriptionArea,
                commentsBox,
                editDescriptionButton,
                statusActionBox,
                updateStatusButton
        );

        IssueViewBuilder.ActionHandlers actionHandlers = new IssueViewBuilder.ActionHandlers(
                this::refreshIssues,
                this::resetFilters,
                () -> updateIssueState(true),
                () -> updateIssueState(false)
        );

        this.view = IssueViewBuilder.build(
                issueList,
                filterControls,
                detailControls,
                forms,
                actionHandlers
        );
    }

    @Override
    protected void onGitRepoReady(LocalRepo repo) {
        refreshIssues();
    }

    public Parent getView() {
        return view;
    }

    private void configureFilterControls() {
        searchField.setPromptText("Search title / description");
        assigneeFilter.setPromptText("Assignee");
        labelsFilter.setPromptText("bug, ui, urgent");

        statusFilter.setItems(FXCollections.observableArrayList(
                "ALL", "TODO", "IN_PROGRESS", "REVIEW", "BLOCKED", "DONE"
        ));
        statusFilter.setValue("ALL");

        priorityFilter.setItems(FXCollections.observableArrayList(
                "ALL", "LOW", "MEDIUM", "HIGH"
        ));
        priorityFilter.setValue("ALL");

        sortFilter.setItems(FXCollections.observableArrayList(
                "UPDATED_DESC", "CREATED_DESC"
        ));
        sortFilter.setValue("UPDATED_DESC");
    }

    private void configureIssueList() {
        issueList.setPlaceholder(new Label("No issues found."));
        issueList.setCellFactory(IssueListCellFactory.create());

        issueList.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldIssue, newIssue) -> presentIssueDetails(newIssue));
    }

    private void configureDetailControls() {
        descriptionArea.setEditable(false);
        descriptionArea.setWrapText(true);
        descriptionArea.setPrefRowCount(6);

        editDescriptionButton.setOnAction(event -> editSelectedIssueDescription());

        statusActionBox.setItems(FXCollections.observableArrayList(
                IssueStatus.TODO,
                IssueStatus.IN_PROGRESS,
                IssueStatus.REVIEW,
                IssueStatus.BLOCKED,
                IssueStatus.DONE
        ));

        updateStatusButton.setOnAction(event -> updateSelectedIssueStatus());
    }

    private void bindFormActions(IssueFormPaneFactory.IssueForms forms) {
        forms.createButton().setOnAction(event -> runGuarded(() ->
                actions.createIssue(
                        requireRepo(),
                        forms.titleField().getText(),
                        forms.creatorField().getText(),
                        () -> {
                            forms.titleField().clear();
                            refreshIssues();
                        }
                )
        ));

        forms.addCommentButton().setOnAction(event -> runGuarded(() -> {
            Issue selected = requireSelectedIssue();
            actions.addComment(
                    requireRepo(),
                    selected,
                    forms.commentAuthorField().getText(),
                    forms.commentBodyArea().getText(),
                    () -> {
                        forms.commentBodyArea().clear();
                        refreshIssues();
                    }
            );
        }));
    }

    private void resetFilters() {
        searchField.clear();
        statusFilter.setValue("ALL");
        assigneeFilter.clear();
        priorityFilter.setValue("ALL");
        labelsFilter.clear();
        sortFilter.setValue("UPDATED_DESC");
        refreshIssues();
    }

    /**
     * Builds an IssueQuery from current UI filters and sort controls.
     */
    private IssueQuery buildIssueQueryFromUI() {
        return IssueFilterMapper.toQuery(
                searchField.getText(),
                statusFilter.getValue(),
                assigneeFilter.getText(),
                priorityFilter.getValue(),
                labelsFilter.getText(),
                sortFilter.getValue()
        );
    }

    /**
     * Reloads issue list using active filters and re-renders details for selected issue.
     */
    private void refreshIssues() {
        try {
            IssueListRefresher.refresh(
                    facade,
                    requireRepo(),
                    buildIssueQueryFromUI(),
                    issueList,
                    this::presentIssueDetails
            );
        } catch (Exception e) {
            IssueErrorPresenter.show(e.getMessage());
        }
    }

    private void updateIssueState(boolean close) {
        runGuarded(() -> {
            Issue issue = requireSelectedIssue();
            actions.closeOrReopen(requireRepo(), issue, close, this::refreshIssues);
        });
    }

    private void editSelectedIssueDescription() {
        runGuarded(() -> {
            Issue issue = requireSelectedIssue();
            actions.editDescription(requireRepo(), issue, this::refreshIssues);
        });
    }

    private void presentIssueDetails(Issue issue) {
        IssueDetailPresenter.present(
                issue,
                issueTitleLabel,
                statusBadge,
                metaLabel,
                assigneeLabel,
                priorityLabel,
                labelsLabel,
                closedInfoLabel,
                descriptionArea,
                commentsBox,
                statusActionBox,
                this::editComment,
                this::deleteComment
        );
    }

    private void editComment(Comment comment) {
        runGuarded(() -> {
            Issue issue = requireSelectedIssue();
            actions.editComment(requireRepo(), issue, comment, this::refreshIssues);
        });
    }

    private void deleteComment(Comment comment) {
        runGuarded(() -> {
            Issue issue = requireSelectedIssue();
            actions.deleteComment(requireRepo(), issue, comment, this::refreshIssues);
        });
    }

    private void updateSelectedIssueStatus() {
        runGuarded(() -> {
            Issue issue = requireSelectedIssue();
            actions.updateStatus(requireRepo(), issue, statusActionBox.getValue(), this::refreshIssues);
        });
    }

    /**
     * Central guarded runner for UI actions that should surface validation failures
     * as user-friendly issue error dialogs.
     */
    private void runGuarded(Runnable action) {
        try {
            action.run();
        } catch (IllegalStateException e) {
            IssueErrorPresenter.show(e.getMessage());
        }
    }

    /**
     * Returns selected issue or throws a user-facing validation error
     * when no issue is selected.
     */
    private Issue requireSelectedIssue() {
        Issue selected = issueList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            throw new IllegalStateException("Please select an issue first.");
        }
        return selected;
    }
}
