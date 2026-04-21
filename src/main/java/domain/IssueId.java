package domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Strongly-typed identifier for {@link Issue} entities.
 */
public final class IssueId {
    private final String value;

    public IssueId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("IssueId value cannot be blank");
        }
        this.value = value;
    }

    public static IssueId random() {
        return new IssueId(UUID.randomUUID().toString());
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IssueId issueId)) {
            return false;
        }
        return value.equals(issueId.value);
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
