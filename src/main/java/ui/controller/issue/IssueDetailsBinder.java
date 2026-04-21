package ui.controller.issue;

import domain.Issue;
import domain.IssueStatus;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

import static ui.controller.common.UiText.safe;

/**
 * Utility binder for mapping Issue fields into detail controls.
 * Handles both populated and empty-selection states.
 */
public final class IssueDetailsBinder {
    private IssueDetailsBinder() {}

    /**
     * Binds scalar issue fields (title/meta/status/description/etc.) into detail controls.
     * Handles null issue by resetting controls to empty/default state.
     */
    public static void bind(
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
            Runnable onNoComments
    ) {
        if (issue == null) {
            issueTitleLabel.setText("Select an issue");
            statusBadge.setText("");
            metaLabel.setText("");
            assigneeLabel.setText("");
            priorityLabel.setText("");
            labelsLabel.setText("");
            closedInfoLabel.setText("");
            descriptionArea.clear();
            commentsBox.getChildren().clear();
            statusActionBox.setValue(null);
            return;
        }

        issueTitleLabel.setText(issue.title() + " #" + issue.id().value());
        statusBadge.setText(safe(issue.status() == null ? null : issue.status().canonical()));
        metaLabel.setText("Opened by " + safe(issue.creator())
                + " • Created: " + safe(issue.createdAt())
                + " • Updated: " + safe(issue.updatedAt()));

        assigneeLabel.setText("Assignee: " + safe(issue.assignee()));
        priorityLabel.setText("Priority: " + safe(issue.priority()));
        labelsLabel.setText("Labels: " + formatLabels(issue.labels()));

        if (issue.closedAt() != null || issue.closedBy() != null) {
            closedInfoLabel.setText("Closed At: " + safe(issue.closedAt()) + " • Closed By: " + safe(issue.closedBy()));
        } else {
            closedInfoLabel.setText("");
        }

        descriptionArea.setText(safe(issue.description()));
        statusActionBox.setValue(issue.status());

        if (issue.comments() == null || issue.comments().isEmpty()) {
            onNoComments.run();
        }
    }

    private static String formatLabels(java.util.List<String> labels) {
        if (labels == null || labels.isEmpty()) return "(none)";
        return String.join(", ", labels);
    }
}
