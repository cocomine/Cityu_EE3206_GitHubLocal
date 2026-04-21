package ui.controller.issue;

import domain.Issue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import static ui.controller.common.UiText.safe;

/**
 * Custom ListCell factory for issue list rendering.
 * Displays title, metadata, and status in a compact row layout.
 */
public final class IssueListCellFactory {
    private IssueListCellFactory() {}

    public static Callback<ListView<Issue>, ListCell<Issue>> create() {
        return list -> new ListCell<>() {
            @Override
            protected void updateItem(Issue issue, boolean empty) {
                super.updateItem(issue, empty);

                if (empty || issue == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                Label title = new Label(issue.title());

                String metaText = "#" + safe(issue.id().value())
                        + " opened by " + safe(issue.creator())
                        + " • " + safe(issue.createdAt());

                Label meta = new Label(metaText);
                Label status = new Label(safe(issue.status() == null ? null : issue.status().canonical()));

                VBox textBox = new VBox(4, title, meta);
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                HBox row = new HBox(10, textBox, spacer, status);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(10));

                setText(null);
                setGraphic(row);
            }
        };
    }
}
