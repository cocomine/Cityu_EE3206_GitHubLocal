package ui.controller.issue;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Factory for issue and comment input forms.
 * Creates reusable form controls and returns them as a typed record bundle.
 */
public final class IssueFormPaneFactory {
    private IssueFormPaneFactory() {}

    public static IssueForms create() {
        TextField titleField = new TextField();
        titleField.setPromptText("Issue title");

        TextField creatorField = new TextField();
        creatorField.setPromptText("Creator");

        Button createButton = new Button("Create Issue");

        VBox createBox = new VBox(
                10,
                new Label("New Issue"),
                creatorField,
                titleField,
                createButton
        );
        createBox.setMinWidth(250);

        TextField commentAuthorField = new TextField();
        commentAuthorField.setPromptText("Comment author");

        TextArea commentBodyArea = new TextArea();
        commentBodyArea.setPromptText("Write a comment...");
        commentBodyArea.setPrefRowCount(4);
        commentBodyArea.setWrapText(true);

        Button addCommentButton = new Button("Add Comment");

        VBox commentBox = new VBox(
                10,
                new Label("New Comment"),
                commentAuthorField,
                commentBodyArea,
                addCommentButton
        );
        commentBox.setMinWidth(250);

        HBox issueCommentBox = new HBox(12, createBox, new Separator(), commentBox);

        return new IssueForms(
                issueCommentBox,
                titleField,
                creatorField,
                createButton,
                commentAuthorField,
                commentBodyArea,
                addCommentButton
        );
    }

    public record IssueForms(
            HBox issueCommentBox,
            TextField titleField,
            TextField creatorField,
            Button createButton,
            TextField commentAuthorField,
            TextArea commentBodyArea,
            Button addCommentButton
    ) {}
}
