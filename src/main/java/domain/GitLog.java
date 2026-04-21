package domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Represents a single Git log entry.
 *
 * @param hash the commit hash
 * @param tag the list of tags associated with the commit
 * @param message the commit message
 * @param author the author of the commit
 * @param date the date and time of the commit
 */
public record GitLog(String hash, List<String> tag, String message, String author, LocalDateTime date) {
    /**
     * Creates a {@code GitLog} record and defensively copies the tag list.
     *
     * @throws NullPointerException if {@code tag} is {@code null}
     */
    public GitLog {
        tag = List.copyOf(Objects.requireNonNull(tag, "files cannot be null"));
    }

    /**
     * Returns a defensive copy of the tags associated with this commit.
     *
     * @return an immutable copy of the tag list
     */
    @Override
    public List<String> tag() {
        return List.copyOf(tag);
    }
}
