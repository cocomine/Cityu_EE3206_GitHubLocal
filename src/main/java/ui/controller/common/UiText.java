package ui.controller.common;

/**
 * Shared UI text helper utilities.
 */
public final class UiText {
    private UiText() {}

    public static String safe(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }
}
