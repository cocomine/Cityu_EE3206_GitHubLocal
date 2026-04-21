package domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents the status of a Git repository, including staged, unstaged, and all changes.
 */
public class RepoStatus {
    private final List<FileChange> changes;
    private final List<FileChange> staged;
    private final List<FileChange> unstaged;

    /**
     * Constructs an empty RepoStatus.
     */
    public RepoStatus() {
        this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    /**
     * Constructs a RepoStatus with the given lists of changes.
     * The provided lists are defensively copied.
     *
     * @param changes the list of all file changes.
     * @param staged the list of staged file changes.
     * @param unstaged the list of unstaged file changes.
     */
    public RepoStatus(List<FileChange> changes, List<FileChange> staged, List<FileChange> unstaged) {
        this.changes = new ArrayList<>(Objects.requireNonNull(changes));
        this.staged = new ArrayList<>(Objects.requireNonNull(staged));
        this.unstaged = new ArrayList<>(Objects.requireNonNull(unstaged));
    }

    /**
     * Adds a file change to the list of all changes.
     *
     * @param change the file change to add.
     */
    public void addChange(FileChange change) {
        changes.add(Objects.requireNonNull(change));
    }

    /**
     * Adds a file change to the list of staged changes and the list of all changes.
     *
     * @param change the file change to add.
     */
    public void addStaged(FileChange change) {
        staged.add(Objects.requireNonNull(change));
        changes.add(change);
    }

    /**
     * Adds a file change to the list of unstaged changes and the list of all changes.
     *
     * @param change the file change to add.
     */
    public void addUnstaged(FileChange change) {
        unstaged.add(Objects.requireNonNull(change));
        changes.add(change);
    }

    /**
     * Returns an immutable list of all file changes.
     *
     * @return the list of all changes.
     */
    public List<FileChange> changes() {
        return List.copyOf(changes);
    }

    /**
     * Returns an immutable list of staged file changes.
     *
     * @return the list of staged changes.
     */
    public List<FileChange> staged() {
        return List.copyOf(staged);
    }

    /**
     * Returns an immutable list of unstaged file changes.
     *
     * @return the list of unstaged changes.
     */
    public List<FileChange> unstaged() {
        return List.copyOf(unstaged);
    }

    @Override
    public String toString() {
        return "RepoStatus{staged=%d, unstaged=%d}".formatted(staged.size(), unstaged.size());
    }
}
