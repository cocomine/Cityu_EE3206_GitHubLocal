package domain;

/**
 * Workflow states for issues.
 *
 * <p>{@code OPEN}/{@code CLOSED} are legacy aliases kept for backward compatibility
 * with older persisted data and are normalized through {@link #canonical()}.
 */
public enum IssueStatus {
    TODO,
    IN_PROGRESS,
    REVIEW,
    BLOCKED,
    DONE,
    OPEN,
    CLOSED;

    /**
     * Normalizes legacy aliases to the canonical workflow states.
     *
     * @return canonical status value used internally by transition logic
     */
    public IssueStatus canonical() {
        return switch (this) {
            case OPEN -> TODO;
            case CLOSED -> DONE;
            default -> this;
        };
    }

    /**
     * Determines whether a transition is legal under the issue workflow rules.
     *
     * @param target requested next status
     * @return {@code true} when transition is allowed
     */
    public boolean canTransitionTo(IssueStatus target) {
        IssueStatus from = canonical();
        IssueStatus to = target.canonical();

        if (from == to) {
            return true;
        }

        return switch (from) {
            case TODO -> to == IN_PROGRESS || to == BLOCKED || to == DONE;
            case IN_PROGRESS -> to == TODO || to == REVIEW || to == BLOCKED || to == DONE;
            case REVIEW -> to == IN_PROGRESS || to == BLOCKED || to == DONE;
            case BLOCKED -> to == TODO || to == IN_PROGRESS;
            case DONE -> to == TODO;
            // Guardrail: this branch should be unreachable once canonicalization is applied.
            case OPEN, CLOSED -> throw new IllegalStateException("Legacy aliases must be canonicalized before transition checks");
        };
    }

    public boolean isDoneLike() {
        return canonical() == DONE;
    }
}
