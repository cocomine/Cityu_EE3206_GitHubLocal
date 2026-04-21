package ui.controller.branch;

import domain.GitResult;

/**
 * Formatter for branch command output log entries.
 * Appends normalized single-line summaries of command results.
 */
public final class BranchCommandLogFormatter {
    private BranchCommandLogFormatter() {}

    public static String append(String existingLog, String actionName, GitResult result) {
        String newEntry = formatEntry(actionName, format(result));

        if (existingLog == null || existingLog.isBlank()) {
            return newEntry;
        }
        return existingLog + "\n" + newEntry;
    }

    public static String format(GitResult result) {
        if (result == null) {
            return "";
        }

        String stdout = result.stdout();
        return stdout == null || stdout.isBlank()
                ? "Command succeeded."
                : stdout;
    }

    private static String formatEntry(String actionName, String message) {
        String safeActionName = actionName == null || actionName.isBlank()
                ? "Branch Command"
                : actionName;

        String singleLineMessage = toSingleLine(message);

        return "[" + safeActionName + "] " + singleLineMessage;
    }

    private static String toSingleLine(String message) {
        if (message == null || message.isBlank()) {
            return "Command succeeded.";
        }

        return message
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }
}
