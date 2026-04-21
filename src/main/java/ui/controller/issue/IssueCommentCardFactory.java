package ui.controller.issue;

import domain.Comment;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

import static ui.controller.common.UiText.safe;

/**
 * Factory for rendering a single issue comment card in the UI.
 * Builds author/meta/body/action sections and disables actions for deleted comments.
 */
public final class IssueCommentCardFactory {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private IssueCommentCardFactory() {}

    public static VBox build(
            Comment comment,
            Consumer<Comment> onEdit,
            Consumer<Comment> onDelete
    ) {
        String createTime = FORMATTER.format(comment.createdAt());
        Label authorLabel = new Label(safe(comment.author()) + " • " + createTime);

        StringBuilder metaText = new StringBuilder();
        //metaText.append("Comment #").append(safe(comment.id().value()));
        //metaText.append(" • ").append(safe(comment.createdAt()));

        if (comment.editedAt() != null) {
            String editTime = FORMATTER.format(comment.editedAt());
            metaText.append("(edited • ").append(editTime).append(")");
        }

        if (comment.isDeleted()) {
            metaText.append("(deleted");
            if (comment.deletedBy() != null) {
                metaText.append(" by ").append(comment.deletedBy());
            }
            if (comment.deletedAt() != null) {
                String deleteTime = FORMATTER.format(comment.deletedAt());
                metaText.append(" • ").append(deleteTime);
            }
            metaText.append(")");
        }

        VBox headerBox = new VBox(2, authorLabel);
        if (!metaText.isEmpty()) {
            headerBox.getChildren().add(new Label(metaText.toString()));
        }

        Label bodyLabel = new Label(comment.isDeleted() ? "(deleted comment)" : safe(comment.body()));
        bodyLabel.setWrapText(true);

        Button editButton = new Button("Edit");
        editButton.setDisable(comment.isDeleted());
        editButton.setOnAction(event -> onEdit.accept(comment));

        Button deleteButton = new Button("Delete");
        deleteButton.setDisable(comment.isDeleted());
        deleteButton.setOnAction(event -> onDelete.accept(comment));

        HBox actionBar = new HBox(8, editButton, deleteButton);

        VBox extraBox = new VBox(4);
        if (comment.replyToCommentId() != null) {
            extraBox.getChildren().add(buildSmallMeta("Reply To: " + comment.replyToCommentId()));
        }
        if (comment.commitReference() != null) {
            extraBox.getChildren().add(buildSmallMeta("Commit: " + comment.commitReference()));
        }
        if (comment.filePath() != null) {
            extraBox.getChildren().add(buildSmallMeta("File: " + comment.filePath()));
        }

        VBox card = new VBox(10, headerBox, bodyLabel, actionBar);
        if (!extraBox.getChildren().isEmpty()) {
            card.getChildren().add(extraBox);
        }

        card.setPadding(new Insets(12));
        return card;
    }

    private static Label buildSmallMeta(String text) {
        return new Label(text);
    }
}
