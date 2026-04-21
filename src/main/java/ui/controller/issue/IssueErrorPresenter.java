package ui.controller.issue;

import javafx.scene.control.Alert;

import static ui.controller.common.UiText.safe;

/**
 * Centralized presenter for issue-related errors.
 * Translates common error patterns into user-friendly alert messages.
 */
public final class IssueErrorPresenter {
    private IssueErrorPresenter() {}

    /**
     * Maps technical/validation errors to user-oriented alert messaging,
     * with special handling for issue-store corruption and invalid transitions.
     */
    public static void show(String message) {
        String normalized = message == null ? "" : message.toLowerCase();

        Alert alert = new Alert(Alert.AlertType.ERROR);

        if (normalized.contains("corrupt") || normalized.contains("quarantin")) {
            alert.setHeaderText("Issue Store Error");
            alert.setContentText(
                    "The issue store appears to be corrupted.\n" +
                            "A copy may have been quarantined under .gitgui/issues.corrupt-*.json.\n\n" +
                            safe(message)
            );
        } else if (normalized.contains("cannot move issue") || normalized.contains("transition")) {
            alert.setHeaderText("Invalid Issue Transition");
            alert.setContentText(safe(message) + "\nPlease choose a valid next state.");
        } else {
            alert.setHeaderText("Issue Action Error");
            alert.setContentText(safe(message));
        }

        alert.showAndWait();
    }
}
