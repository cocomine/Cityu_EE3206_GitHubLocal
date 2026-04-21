package domain;

/**
 * Represents the type of change made to a file.
 */
public enum ChangeType {
    /**
     * A new file was added.
     */
    ADDED,
    /**
     * An existing file was modified.
     */
    MODIFIED,
    /**
     * A file was deleted.
     */
    DELETED,
    /**
     * A file was renamed.
     */
    RENAMED,
    /**
     * A file is not tracked by Git.
     */
    UNTRACKED
}
