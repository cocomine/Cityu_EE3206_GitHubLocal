package domain;

import java.util.List;
import java.util.Objects;

/**
 * Represents a parsed Git diff output.
 *
 * @param files changed files included in this diff
 */
public record GitDiff(List<FileDiff> files) {
    public GitDiff {
        files = List.copyOf(Objects.requireNonNull(files, "files cannot be null"));
    }

    @Override
    public List<FileDiff> files() {
        return List.copyOf(files);
    }

    /**
     * High-level status of a file in a diff.
     */
    public enum FileStatus {
        /**
         * A new file was added.
         */
        NEW,
        /**
         * A file was deleted.
         */
        DELETE,
        /**
         * A file was renamed (path changed).
         */
        RENAME,
        /**
         * A file was modified.
         */
        CHANGE
    }

    /**
     * Status of a single line within a diff hunk.
     */
    public enum LineStatus {
        /**
         * An added line.
         */
        NEW,
        /**
         * A deleted line.
         */
        DELETE,
        /**
         * A line that is part of the context but unchanged.
         */
        NO_CHANGE,
        /**
         * A metadata line, like a hunk header.
         */
        META
    }

    /**
     * Represents one file section in a diff output.
     *
     * @param oldPath old path from diff header or /dev/null
     * @param newPath new path from diff header or /dev/null
     * @param status  high-level file status (new, delete, rename, change)
     * @param binary  whether file is detected as binary
     * @param hunks   parsed hunk blocks
     */
    public record FileDiff(String oldPath, String newPath, FileStatus status, boolean binary, List<Hunk> hunks) {
        public FileDiff {
            oldPath = Objects.requireNonNull(oldPath, "oldPath cannot be null");
            newPath = Objects.requireNonNull(newPath, "newPath cannot be null");
            status = Objects.requireNonNull(status, "status cannot be null");
            hunks = List.copyOf(Objects.requireNonNull(hunks, "hunks cannot be null"));
        }

        @Override
        public List<Hunk> hunks() {
            return List.copyOf(hunks);
        }
    }

    /**
     * Represents one @@ hunk section in a file diff.
     *
     * @param oldStart old file start line
     * @param oldCount number of lines in old file part
     * @param newStart new file start line
     * @param newCount number of lines in new file part
     * @param header   optional hunk trailing header (often function signature)
     * @param lines    line-level changes in this hunk
     */
    public record Hunk(int oldStart, int oldCount, int newStart, int newCount, String header, List<DiffLine> lines) {
        public Hunk {
            if (oldStart < 0 || oldCount < 0 || newStart < 0 || newCount < 0) {
                throw new IllegalArgumentException("hunk ranges cannot be negative");
            }
            header = Objects.requireNonNull(header, "header cannot be null");
            lines = List.copyOf(Objects.requireNonNull(lines, "lines cannot be null"));
        }

        @Override
        public List<DiffLine> lines() {
            return List.copyOf(lines);
        }
    }

    /**
     * Represents one line in a hunk.
     *
     * @param status    line type (new/delete/no-change/meta)
     * @param text      content excluding diff prefix (+/-/space) for data lines
     * @param oldLineNo old file line number, null for pure additions/meta
     * @param newLineNo new file line number, null for pure deletions/meta
     */
    public record DiffLine(LineStatus status, String text, Integer oldLineNo, Integer newLineNo) {
        public DiffLine {
            status = Objects.requireNonNull(status, "status cannot be null");
            text = Objects.requireNonNull(text, "text cannot be null");
        }
    }
}
