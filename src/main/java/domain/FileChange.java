package domain;

import java.util.Objects;

/**
 * Represents a single file change in a commit.
 * This class is immutable.
 */
public record FileChange(String path, ChangeType type) {
    /**
     * Constructs a new FileChange.
     *
     * @param path the path of the file that was changed. Cannot be null or blank.
     * @param type the type of change. Cannot be null.
     * @throws IllegalArgumentException if the path is null or blank.
     * @throws NullPointerException     if the type is null.
     */
    public FileChange(String path, ChangeType type) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("path cannot be blank");
        }
        this.path = path;
        this.type = Objects.requireNonNull(type, "type cannot be null");
    }

    /**
     * Returns the path of the file that was changed.
     *
     * @return the file path.
     */
    @Override
    public String path() {
        return path;
    }

    /**
     * Returns the type of change.
     *
     * @return the change type.
     */
    @Override
    public ChangeType type() {
        return type;
    }

    @Override
    public String toString() {
        return "%s [%s]".formatted(path, type);
    }
}
