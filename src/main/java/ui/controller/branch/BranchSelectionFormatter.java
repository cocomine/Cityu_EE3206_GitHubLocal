package ui.controller.branch;

import domain.GitLog;

/**
 * Formatter for branch/rollback selection labels.
 * Produces compact, readable commit options from GitLog entries.
 */
public final class BranchSelectionFormatter {
    private static final int SHORT_HASH_LENGTH = 7;

    private BranchSelectionFormatter() {}

    public static String formatRollbackOption(GitLog log) {
        if (log == null) {
            return "";
        }

        String shortHash = shortenHash(log.hash());
        String message = sanitize(log.message());

        if (message.isBlank()) {
            return shortHash;
        }

        return shortHash + " - " + message;
    }

    private static String shortenHash(String hash) {
        if (hash == null || hash.isBlank()) {
            return "(no-hash)";
        }

        return hash.length() <= SHORT_HASH_LENGTH
                ? hash
                : hash.substring(0, SHORT_HASH_LENGTH);
    }

    private static String sanitize(String text) {
        if (text == null) {
            return "";
        }

        return text.replace('\n', ' ').trim();
    }
}
