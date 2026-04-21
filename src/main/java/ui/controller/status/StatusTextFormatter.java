package ui.controller.status;

import domain.FileChange;

import java.util.List;

/**
 * Formatter for rendering file change lists into multiline status text.
 */
public final class StatusTextFormatter {
    private StatusTextFormatter() {}

    public static String format(List<FileChange> changes) {
        if (changes == null || changes.isEmpty()) {
            return "  (none)\n";
        }

        StringBuilder sb = new StringBuilder();
        for (FileChange change : changes) {
            sb.append("  - ")
                    .append(change.path())
                    .append(" [")
                    .append(change.type())
                    .append("]\n");
        }
        return sb.toString();
    }
}
