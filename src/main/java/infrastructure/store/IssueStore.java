package infrastructure.store;

import domain.Issue;
import domain.IssueId;
import domain.LocalRepo;

import java.util.ArrayList;
import java.util.List;

/**
 * Persistence boundary for issue data.
 *
 * <p>Implementations may store data in any format, but callers interact with
 * domain-level {@link Issue} objects and optional snapshot metadata.
 *
 * @see JsonIssueStore
 */
public interface IssueStore {
    /**
     * Loads all issues for a repository.
     *
     * @param repo target repository
     * @return issue list, never {@code null}
     */
    List<Issue> load(LocalRepo repo);

    /**
     * Persists a full issue list for a repository.
     *
     * @param repo target repository
     * @param issues issues to persist
     */
    void save(LocalRepo repo, List<Issue> issues);

    /**
     * Loads issues together with store metadata such as schema/revision.
     *
     * @param repo target repository
     * @return snapshot view of store content
     */
    default IssueStoreSnapshot loadSnapshot(LocalRepo repo) {
        return new IssueStoreSnapshot(1, null, load(repo));
    }

    /**
     * Persists a metadata-aware snapshot.
     *
     * @param repo target repository
     * @param snapshot snapshot to persist
     */
    default void saveSnapshot(LocalRepo repo, IssueStoreSnapshot snapshot) {
        save(repo, snapshot.issues());
    }

    default void upsertIssue(LocalRepo repo, Issue issue) {
        List<Issue> issues = new ArrayList<>(load(repo));
        boolean replaced = false;
        for (int i = 0; i < issues.size(); i++) {
            if (issues.get(i).id().equals(issue.id())) {
                issues.set(i, issue);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            issues.add(issue);
        }
        save(repo, issues);
    }

    default void deleteIssue(LocalRepo repo, IssueId issueId) {
        List<Issue> issues = new ArrayList<>(load(repo));
        issues.removeIf(issue -> issue.id().equals(issueId));
        save(repo, issues);
    }
}
