package domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Strongly-typed identifier for {@link Comment} entities.
 */
public final class CommentId {
    private final String value;

    public CommentId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("CommentId value cannot be blank");
        }
        this.value = value;
    }

    public static CommentId random() {
        return new CommentId(UUID.randomUUID().toString());
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CommentId commentId)) {
            return false;
        }
        return value.equals(commentId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
