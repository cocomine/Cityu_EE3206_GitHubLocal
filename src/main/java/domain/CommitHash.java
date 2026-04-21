package domain;

import java.util.Objects;

public final class CommitHash {
    private final String value;

    public CommitHash(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("CommitHash value cannot be blank");
        }
        this.value = value;
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CommitHash that)) {
            return false;
        }
        return value.equals(that.value);
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
