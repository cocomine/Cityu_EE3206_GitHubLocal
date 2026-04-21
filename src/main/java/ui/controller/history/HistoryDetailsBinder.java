package ui.controller.history;

import domain.GitLog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

import java.time.format.DateTimeFormatter;

/**
 * Utility binder for mapping selected GitLog fields into history detail controls.
 */
public final class HistoryDetailsBinder {
    private HistoryDetailsBinder() {}

    public static void bind(
            GitLog log,
            Label hashValue,
            Label tagValue,
            Label authorValue,
            Label dateValue,
            TextArea messageValue,
            DateTimeFormatter formatter
    ) {
        if (log == null) {
            clear(hashValue, tagValue, authorValue, dateValue, messageValue);
            return;
        }

        hashValue.setText(log.hash());
        tagValue.setText(String.join(", ", log.tag()));
        authorValue.setText(log.author());
        dateValue.setText(log.date().format(formatter));
        messageValue.setText(log.message());
    }

    public static void clear(
            Label hashValue,
            Label tagValue,
            Label authorValue,
            Label dateValue,
            TextArea messageValue
    ) {
        hashValue.setText("-");
        tagValue.setText("-");
        authorValue.setText("-");
        dateValue.setText("-");
        messageValue.setText("");
    }
}
