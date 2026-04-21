package ui.controller.diff;

import domain.GitDiff;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapper that converts domain diff models into UI-friendly display rows and styles.
 * Centralizes diff text/status/path formatting and list cell styling.
 */
public final class DiffDisplayMapper {
    private DiffDisplayMapper() {}

    public static String mapFileStatus(GitDiff.FileStatus status) {
        return switch (status) {
            case NEW -> "New";
            case DELETE -> "Delete";
            case RENAME -> "Rename";
            case CHANGE -> "Change";
        };
    }

    /**
     * Produces display path for a file diff, including rename visualization and /dev/null handling.
     */
    public static String renderPath(GitDiff.FileDiff file) {
        if (file.status() == GitDiff.FileStatus.RENAME) {
            return file.oldPath() + " → " + file.newPath();
        }
        return "/dev/null".equals(file.oldPath()) ? file.newPath() : file.oldPath();
    }

    /**
     * Converts a domain FileDiff into UI display rows (headers, hunks, and line-level entries).
     * Keeps rendering concerns out of controller logic.
     */
    public static List<DiffDisplayLine> toDisplayLines(GitDiff.FileDiff file) {
        List<DiffDisplayLine> rows = new ArrayList<>();
        rows.add(new DiffDisplayLine(
                DiffLineKind.FILE_HEADER,
                null,
                null,
                "%s [%s]%s".formatted(
                        renderPath(file),
                        mapFileStatus(file.status()),
                        file.binary() ? " (binary)" : ""
                )
        ));

        if (file.binary()) {
            rows.add(new DiffDisplayLine(
                    DiffLineKind.META_LINE,
                    null,
                    null,
                    "(binary file content not shown)"
            ));
            return rows;
        }

        for (GitDiff.Hunk h : file.hunks()) {
            String header = "@@ -%d,%d +%d,%d @@%s".formatted(
                    h.oldStart(), h.oldCount(), h.newStart(), h.newCount(),
                    h.header().isBlank() ? "" : " " + h.header()
            );
            rows.add(new DiffDisplayLine(DiffLineKind.HUNK_HEADER, null, null, header));

            for (GitDiff.DiffLine line : h.lines()) {
                switch (line.status()) {
                    case NEW -> rows.add(new DiffDisplayLine(
                            DiffLineKind.NEW_LINE, line.oldLineNo(), line.newLineNo(), "+" + line.text()));
                    case DELETE -> rows.add(new DiffDisplayLine(
                            DiffLineKind.DELETE_LINE, line.oldLineNo(), line.newLineNo(), "-" + line.text()));
                    case NO_CHANGE -> rows.add(new DiffDisplayLine(
                            DiffLineKind.CONTEXT_LINE, line.oldLineNo(), line.newLineNo(), " " + line.text()));
                    case META -> rows.add(new DiffDisplayLine(
                            DiffLineKind.META_LINE, line.oldLineNo(), line.newLineNo(), line.text()));
                }
            }
        }

        return rows;
    }

    public static DiffDisplayLine noChangesLine() {
        return new DiffDisplayLine(DiffLineKind.META_LINE, null, null, "(no changes)");
    }

    /**
     * Creates styled cells for diff rows so each line type (added/removed/header/meta)
     * has consistent visual formatting.
     */
    public static Callback<ListView<DiffDisplayLine>, ListCell<DiffDisplayLine>> createCellFactory() {
        return lv -> new ListCell<>() {
            @Override
            protected void updateItem(DiffDisplayLine item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }

                String oldNo = item.oldLineNo() == null ? "" : item.oldLineNo().toString();
                String newNo = item.newLineNo() == null ? "" : item.newLineNo().toString();

                String line = String.format("%6s %6s  %s", oldNo, newNo, item.text());
                setText(line);

                setStyle(switch (item.kind()) {
                    case HUNK_HEADER ->
                            "-fx-font-family: 'Consolas'; -fx-background-color: #004080; -fx-text-fill: #80c0ff;";
                    case NEW_LINE ->
                            "-fx-font-family: 'Consolas'; -fx-background-color: #008040; -fx-text-fill: #80ffc0;";
                    case DELETE_LINE ->
                            "-fx-font-family: 'Consolas'; -fx-background-color: #800000; -fx-text-fill: #ff8080;";
                    case CONTEXT_LINE ->
                            "-fx-font-family: 'Consolas'; -fx-text-fill: #80c0ff;";
                    case META_LINE ->
                            "-fx-font-family: 'Consolas'; -fx-text-fill: #607080; -fx-font-style: italic;";
                    case FILE_HEADER ->
                            "-fx-font-family: 'Consolas'; -fx-background-color: #004080; -fx-font-weight: bold;";
                });
            }
        };
    }

    public record DiffDisplayLine(
            DiffLineKind kind,
            Integer oldLineNo,
            Integer newLineNo,
            String text
    ) {}

    public enum DiffLineKind {
        FILE_HEADER,
        HUNK_HEADER,
        NEW_LINE,
        DELETE_LINE,
        CONTEXT_LINE,
        META_LINE
    }
}
