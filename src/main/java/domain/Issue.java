package domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Domain aggregate for an issue and its discussion thread.
 *
 * <p>This class centralizes invariants for status transitions, closure metadata,
 * assignee/label normalization, and comment mutations so callers do not need to
 * coordinate those rules manually.
 */
public class Issue {
    private final IssueId id;
    private final String title;
    private String description;
    private final String creator;
    private IssueStatus status;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant closedAt;
    private String closedBy;
    private String assignee;
    private IssuePriority priority;
    private List<String> labels;
    private final List<Comment> comments;

    public Issue(IssueId id, String title, String creator) {
        this(
                id,
                title,
                title,
                creator,
                IssueStatus.TODO,
                Instant.now(),
                Instant.now(),
                null,
                null,
                null,
                IssuePriority.MEDIUM,
                List.of(),
                new ArrayList<>()
        );
    }

    public Issue(
            IssueId id,
            String title,
            String description,
            String creator,
            IssueStatus status,
            Instant createdAt,
            Instant updatedAt,
            Instant closedAt,
            String closedBy,
            List<Comment> comments
    ) {
        this(
                id,
                title,
                description,
                creator,
                status,
                createdAt,
                updatedAt,
                closedAt,
                closedBy,
                null,
                IssuePriority.MEDIUM,
                List.of(),
                comments
        );
    }

    public Issue(
            IssueId id,
            String title,
            String description,
            String creator,
            IssueStatus status,
            Instant createdAt,
            Instant updatedAt,
            Instant closedAt,
            String closedBy,
            String assignee,
            IssuePriority priority,
            List<String> labels,
            List<Comment> comments
    ) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title cannot be blank");
        }
        if (description == null) {
            throw new IllegalArgumentException("description cannot be null");
        }
        if (creator == null || creator.isBlank()) {
            throw new IllegalArgumentException("creator cannot be blank");
        }
        this.title = title;
        this.description = description;
        this.creator = creator;
        // Accept legacy aliases (OPEN/CLOSED) while keeping a single canonical state model.
        IssueStatus canonicalStatus = Objects.requireNonNull(status, "status cannot be null").canonical();
        this.status = canonicalStatus;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt cannot be null");
        this.closedAt = closedAt;
        this.closedBy = closedBy;
        this.assignee = normalizeAssignee(assignee);
        this.priority = priority == null ? IssuePriority.MEDIUM : priority;
        this.labels = normalizeLabels(labels);
        this.comments = new ArrayList<>(Objects.requireNonNull(comments, "comments cannot be null"));

        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("updatedAt cannot be before createdAt");
        }

        if (closedAt != null && (closedBy == null || closedBy.isBlank())) {
            throw new IllegalArgumentException("closedBy cannot be blank when closedAt is set");
        }
        if (closedAt == null && closedBy != null && !closedBy.isBlank()) {
            throw new IllegalArgumentException("closedAt cannot be null when closedBy is set");
        }

        if (!canonicalStatus.isDoneLike() && (closedAt != null || (closedBy != null && !closedBy.isBlank()))) {
            throw new IllegalArgumentException("non-done issue cannot have closed metadata");
        }
        if (canonicalStatus.isDoneLike() && (closedAt == null || closedBy == null || closedBy.isBlank())) {
            throw new IllegalArgumentException("done issue must include closed metadata");
        }
    }

    public Issue(IssueId id, String title, String creator, IssueStatus status, Instant createdAt, List<Comment> comments) {
        this(id, title, title, creator, status, createdAt, createdAt, null, null, null, IssuePriority.MEDIUM, List.of(), comments);
    }

    public IssueId id() {
        return id;
    }

    public String title() {
        return title;
    }

    public String creator() {
        return creator;
    }

    public String description() {
        return description;
    }

    public IssueStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public Instant closedAt() {
        return closedAt;
    }

    public String closedBy() {
        return closedBy;
    }

    public String assignee() {
        return assignee;
    }

    public IssuePriority priority() {
        return priority;
    }

    public List<String> labels() {
        return List.copyOf(labels);
    }

    public List<Comment> comments() {
        return List.copyOf(comments);
    }

    public void addComment(Comment c) {
        comments.add(Objects.requireNonNull(c, "comment cannot be null"));
        touch();
    }

    public void close() {
        close(creator);
    }

    public void close(String actor) {
        transitionTo(IssueStatus.DONE, actor);
    }

    public void reopen() {
        transitionTo(IssueStatus.TODO, creator);
    }

    public void transitionTo(IssueStatus targetStatus) {
        transitionTo(targetStatus, creator);
    }

    /**
     * Applies a validated status transition and keeps closure metadata consistent.
     *
     * @param targetStatus target workflow state
     * @param actor user performing the transition; required when moving to a done-like state
     * @throws IllegalArgumentException if input data is invalid
     * @throws IllegalStateException if the transition is not allowed by the workflow
     * @see IssueStatus#canTransitionTo(IssueStatus)
     */
    public void transitionTo(IssueStatus targetStatus, String actor) {
        IssueStatus normalizedTarget = Objects.requireNonNull(targetStatus, "targetStatus cannot be null").canonical();
        IssueStatus currentStatus = status.canonical();

        if (!currentStatus.canTransitionTo(normalizedTarget)) {
            throw new IllegalStateException(
                    "Invalid status transition: " + currentStatus + " -> " + normalizedTarget
            );
        }

        if (normalizedTarget.isDoneLike()) {
            // "Done" is the only state that carries closure metadata; stamp all of it atomically.
            if (actor == null || actor.isBlank()) {
                throw new IllegalArgumentException("closedBy cannot be blank");
            }
            Instant now = Instant.now();
            this.status = IssueStatus.DONE;
            this.closedAt = now;
            this.closedBy = actor;
            this.updatedAt = now;
            return;
        }

        this.status = normalizedTarget;
        this.closedAt = null;
        this.closedBy = null;
        touch();
    }

    public void updateDescription(String newDescription) {
        if (newDescription == null || newDescription.isBlank()) {
            throw new IllegalArgumentException("description cannot be blank");
        }
        this.description = newDescription;
        touch();
    }

    public void assignTo(String assignee) {
        this.assignee = normalizeAssignee(assignee);
        touch();
    }

    public void setPriority(IssuePriority priority) {
        this.priority = priority == null ? IssuePriority.MEDIUM : priority;
        touch();
    }

    public void setLabels(List<String> labels) {
        this.labels = normalizeLabels(labels);
        touch();
    }

    public void editComment(CommentId commentId, String newBody) {
        findComment(commentId).editBody(newBody);
        touch();
    }

    public void deleteComment(CommentId commentId, String deletedBy) {
        findComment(commentId).delete(deletedBy);
        touch();
    }

    private Comment findComment(CommentId commentId) {
        Objects.requireNonNull(commentId, "commentId cannot be null");
        return comments.stream()
                .filter(comment -> comment.id().equals(commentId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + commentId.value()));
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    private static String normalizeAssignee(String assignee) {
        if (assignee == null) {
            return null;
        }
        String normalized = assignee.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static List<String> normalizeLabels(List<String> labels) {
        if (labels == null || labels.isEmpty()) {
            return List.of();
        }

        Set<String> normalized = new LinkedHashSet<>();
        for (String label : labels) {
            if (label == null) {
                continue;
            }
            // Lowercase + de-duplicate avoids subtle bugs like treating "Bug" and "bug" as different labels.
            String value = label.trim().toLowerCase(Locale.ROOT);
            if (!value.isEmpty()) {
                normalized.add(value);
            }
        }
        return List.copyOf(normalized);
    }

    @Override
    public String toString() {
        return "[%s] %s (%s)".formatted(status, title, id.value());
    }
}
