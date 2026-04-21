package ui.controller;

import application.AppFacade;
import domain.GitLog;
import domain.LocalRepo;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import ui.controller.common.RepoAwareController;
import ui.controller.history.HistoryDetailsBinder;

import java.time.format.DateTimeFormatter;
import java.util.List;

import static ui.controller.common.UiDialogs.showError;

/**
 * Controller for commit history browsing.
 * Fetches git log entries, displays them in a table, and binds selected commit details.
 */
public class HistoryController extends RepoAwareController {
    private static final DateTimeFormatter LOG_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String ERROR_HEADER = "History Action Error";

    private final TextField maxCountField = new TextField("20");
    private final TableView<GitLog> logTable = new TableView<>();

    private final Label hashValue = new Label("-");
    private final Label tagValue = new Label("-");
    private final Label authorValue = new Label("-");
    private final Label dateValue = new Label("-");
    private final TextArea messageValue = new TextArea();

    private final Parent view;

    public HistoryController(AppFacade facade, ObjectProperty<LocalRepo> repoProperty) {
        super(facade, repoProperty);
        configureControls();
        this.view = buildView();
    }

    @Override
    protected void onGitRepoReady(LocalRepo repo) {
        refreshLog();
    }

    public Parent getView() {
        return view;
    }

    private void configureControls() {
        maxCountField.setPrefWidth(80);

        logTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<GitLog, String> hashCol = new TableColumn<>("Hash");
        hashCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().hash()));
        hashCol.setMaxWidth(70.0);

        TableColumn<GitLog, String> tagCol = new TableColumn<>("Tag");
        tagCol.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(String.join(", ", data.getValue().tag())));

        TableColumn<GitLog, String> messageCol = new TableColumn<>("Message");
        messageCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().message()));
        messageCol.setMinWidth(200.0);

        TableColumn<GitLog, String> authorCol = new TableColumn<>("Author");
        authorCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().author()));

        TableColumn<GitLog, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                data.getValue().date().format(LOG_DATE_FORMATTER)));

        logTable.getColumns().addAll(hashCol, tagCol, messageCol, authorCol, dateCol);

        messageValue.setEditable(false);
        messageValue.setWrapText(true);
        messageValue.setPrefRowCount(3);

        logTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, selected) ->
                HistoryDetailsBinder.bind(
                        selected,
                        hashValue,
                        tagValue,
                        authorValue,
                        dateValue,
                        messageValue,
                        LOG_DATE_FORMATTER
                )
        );
    }

    private Parent buildView() {
        Button refreshLogButton = new Button("Refresh Log");
        refreshLogButton.setOnAction(event -> refreshLog());

        HBox logBar = new HBox(10, new Label("Max commits:"), maxCountField, refreshLogButton);

        GridPane detailGrid = buildDetailGrid();

        VBox box = new VBox(10, logBar, logTable, detailGrid);
        box.setPadding(new Insets(12));
        VBox.setVgrow(logTable, Priority.ALWAYS);
        return box;
    }

    private GridPane buildDetailGrid() {
        GridPane detailGrid = new GridPane();
        detailGrid.setHgap(10);
        detailGrid.setVgap(6);
        detailGrid.addRow(0, new Label("Hash:"), hashValue);
        detailGrid.addRow(1, new Label("Tag:"), tagValue);
        detailGrid.addRow(2, new Label("Author:"), authorValue);
        detailGrid.addRow(3, new Label("Date:"), dateValue);
        detailGrid.addRow(4, new Label("Message:"), messageValue);
        return detailGrid;
    }

    /**
     * Reloads commit history using max-count input, validates input,
     * and updates table + detail panel selection state.
     */
    private void refreshLog() {
        try {
            int maxCount = Integer.parseInt(maxCountField.getText().trim());
            if (maxCount <= 0) {
                throw new IllegalArgumentException("Max commits must be a positive integer.");
            }
            List<GitLog> logs = facade.gitLog(requireRepo(), maxCount);
            logTable.getItems().setAll(logs);

            if (!logs.isEmpty()) {
                logTable.getSelectionModel().selectFirst();
            } else {
                HistoryDetailsBinder.clear(
                        hashValue,
                        tagValue,
                        authorValue,
                        dateValue,
                        messageValue
                );
            }
        } catch (Exception e) {
            showError(ERROR_HEADER, e.getMessage());
        }
    }
}
