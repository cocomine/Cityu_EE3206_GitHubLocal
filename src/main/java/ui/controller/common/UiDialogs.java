package ui.controller.common;

import javafx.scene.control.*;

/**
 * Shared dialog helpers for info/error alerts and text input prompts.
 */
public final class UiDialogs {
    private UiDialogs() {}

    public static void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void showError(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static String promptSingleLine(String title, String header, String initialValue) {
        TextInputDialog dialog = new TextInputDialog(initialValue == null ? "" : initialValue);
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText("Value:");
        return dialog.showAndWait().orElse(null);
    }

    public static String promptMultiline(String title, String header, String initialValue) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(header);

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        TextArea area = new TextArea(initialValue == null ? "" : initialValue);
        area.setWrapText(true);
        area.setPrefRowCount(10);
        dialog.getDialogPane().setContent(area);

        dialog.setResultConverter(buttonType ->
                buttonType == saveButtonType ? area.getText() : null);

        return dialog.showAndWait().orElse(null);
    }
}
