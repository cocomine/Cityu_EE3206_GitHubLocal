package domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain model for an issue comment.
 *
 * <p>Comments support in-place edits and soft deletion so history can be preserved
 * without physically removing entries from the discussion timeline.
 */
public class Comment {
    private final CommentId id;
    private final String author;
    private String body;
    private final Instant createdAt;
    private Instant editedAt;
    private Instant deletedAt;
    private String deletedBy;
    private final String replyToCommentId;
    private final String commitReference;
    private final String filePath;

    public Comment(String author, String body) {
        this(CommentId.random(), author, body, Instant.now(), null, null, null, null, null, null);
    }

    public Comment(String author, String body, Instant time) {
        this(CommentId.random(), author, body, time, null, null, null, null, null, null);
    }

    public Comment(
            CommentId id,
            String author,
            String body,
            Instant createdAt,
            Instant editedAt,
            Instant deletedAt,
            String deletedBy,
            String replyToCommentId,
            String commitReference,
            String filePath
    ) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        if (author == null || author.isBlank()) {
            throw new IllegalArgumentException("author cannot be blank");
        }
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("body cannot be blank");
        }
        this.author = author;
        this.body = body;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
        this.editedAt = editedAt;
        this.deletedAt = deletedAt;
        this.deletedBy = deletedBy;
        this.replyToCommentId = replyToCommentId;
        this.commitReference = commitReference;
        this.filePath = filePath;

        if (deletedAt != null && (deletedBy == null || deletedBy.isBlank())) {
            throw new IllegalArgumentException("deletedBy cannot be blank when deletedAt is set");
        }
        if (deletedAt == null && deletedBy != null && !deletedBy.isBlank()) {
            throw new IllegalArgumentException("deletedAt cannot be null when deletedBy is set");
        }
        if (editedAt != null && editedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("editedAt cannot be before createdAt");
        }
        if (deletedAt != null && deletedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("deletedAt cannot be before createdAt");
        }
    }

    public CommentId id() {
        return id;
    }

    public String author() {
        return author;
    }

    public String body() {
        return body;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant editedAt() {
        return editedAt;
    }

    public Instant deletedAt() {
        return deletedAt;
    }

    public String deletedBy() {
        return deletedBy;
    }

    public String replyToCommentId() {
        return replyToCommentId;
    }

    public String commitReference() {
        return commitReference;
    }

    public String filePath() {
        return filePath;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Updates the comment body and records edit time.
     *
     * @param newBody replacement text
     * @throws IllegalStateException if the comment has already been deleted
     * @throws IllegalArgumentException if the new body is blank
     */
    public void editBody(String newBody) {
        if (isDeleted()) {
            throw new IllegalStateException("cannot edit a deleted comment");
        }
        if (newBody == null || newBody.isBlank()) {
            throw new IllegalArgumentException("body cannot be blank");
        }
        this.body = newBody;
        this.editedAt = Instant.now();
    }

    /**
     * Soft-deletes this comment by stamping deletion metadata.
     *
     * @param actor user performing deletion
     * @throws IllegalArgumentException if actor is blank
     * @throws IllegalStateException if the comment is already deleted
     */
    public void delete(String actor) {
        if (actor == null || actor.isBlank()) {
            throw new IllegalArgumentException("deletedBy cannot be blank");
        }
        if (isDeleted()) {
            throw new IllegalStateException("comment is already deleted");
        }
        this.deletedAt = Instant.now();
        this.deletedBy = actor;
    }

    public Instant time() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "%s (%s): %s".formatted(author, createdAt, body);
    }
}
