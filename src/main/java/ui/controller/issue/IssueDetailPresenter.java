package ui.controller.issue;

import domain.Comment;
import domain.Issue;
import domain.IssueStatus;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * Presenter that renders selected issue details into the detail pane.
 * Delegates scalar field binding and creates per-comment UI cards with edit/delete callbacks.
 */
public final class IssueDetailPresenter {
    private IssueDetailPresenter() {}

    /**
     * Renders selected issue details and comment cards.
     * Uses binder for core fields and appends interactive comment cards for each comment.
     */
    public static void present(
            Issue issue,
            Label issueTitleLabel,
            Label statusBadge,
            Label metaLabel,
            Label assigneeLabel,
            Label priorityLabel,
            Label labelsLabel,
            Label closedInfoLabel,
            TextArea descriptionArea,
            VBox commentsBox,
            ComboBox<IssueStatus> statusActionBox,
            Consumer<Comment> onEditComment,
            Consumer<Comment> onDeleteComment
    ) {
        commentsBox.getChildren().clear();

        IssueDetailsBinder.bind(
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
                () -> commentsBox.getChildren().add(new Label("No comments yet."))
        );

        if (issue == null || issue.comments() == null || issue.comments().isEmpty()) {
            return;
        }

        issue.comments().forEach(comment ->
                commentsBox.getChildren().add(
                        IssueCommentCardFactory.build(comment, onEditComment, onDeleteComment)
                )
        );
    }
}
