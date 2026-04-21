package application;

import domain.Comment;
import domain.CommentId;
import domain.Issue;
import domain.IssueId;
import domain.IssuePriority;
import domain.IssueStatus;
import domain.LocalRepo;
import infrastructure.store.IssueStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Application service for issue and comment operations.
 *
 * <p>The service follows a straightforward load-modify-save pattern against
 * {@link IssueStore}, which keeps behavior explicit and easy to reason about
 * for this local, single-user project scope.
 */
public class IssueService {
    private final IssueStore store;

    public IssueService(IssueStore store) {
        this.store = Objects.requireNonNull(store, "store cannot be null");
    }

    public List<Issue> list(LocalRepo repo) {
        return new ArrayList<>(store.load(repo));
    }

    /**
     * Returns issues matching the supplied criteria.
     *
     * @param repo target repository
     * @param query filter and sort criteria; {@code null} means default query
     * @return filtered, sorted issues
     * @see IssueQuery
     */
    public List<Issue> query(LocalRepo repo, IssueQuery query) {
        IssueQuery criteria = query == null ? new IssueQuery() : query;
        return store.load(repo).stream()
                .filter(issue -> matches(issue, criteria))
                .sorted(criteria.sortMode().comparator())
                .toList();
    }

    /**
     * Creates a new issue with default lifecycle values.
     *
     * @param repo target repository
     * @param title issue title
     * @param creator issue creator
     * @return generated issue identifier
     */
    public IssueId create(LocalRepo repo, String title, String creator) {
        List<Issue> issues = new ArrayList<>(store.load(repo));
        IssueId id = IssueId.random();
        issues.add(new Issue(id, title, creator));
        store.save(repo, issues);
        return id;
    }

    /**
     * Adds a top-level comment to an existing issue.
     *
     * @param repo target repository
     * @param id issue id
     * @param author comment author
     * @param body comment content
     */
    public void comment(LocalRepo repo, IssueId id, String author, String body) {
        List<Issue> issues = new ArrayList<>(store.load(repo));
        Issue issue = findById(issues, id);
        issue.addComment(new Comment(author, body));
        store.save(repo, issues);
    }

    public void updateDescription(LocalRepo repo, IssueId id, String description) {
        List<Issue> issues = new ArrayList<>(store.load(repo));
        Issue issue = findById(issues, id);
        issue.updateDescription(description);
        store.save(repo, issues);
    }

    public void editComment(LocalRepo repo, IssueId issueId, CommentId commentId, String body) {
        List<Issue> issues = new ArrayList<>(store.load(repo));
        Issue issue = findById(issues, issueId);
        issue.editComment(commentId, body);
        store.save(repo, issues);
    }

    public void deleteComment(LocalRepo repo, IssueId issueId, CommentId commentId, String actor) {
        List<Issue> issues = new ArrayList<>(store.load(repo));
        Issue issue = findById(issues, issueId);
        issue.deleteComment(commentId, actor);
        store.save(repo, issues);
    }

    public void close(LocalRepo repo, IssueId id) {
        List<Issue> issues = new ArrayList<>(store.load(repo));
        Issue issue = findById(issues, id);
        issue.close();
        store.save(repo, issues);
    }

    public void close(LocalRepo repo, IssueId id, String actor) {
        List<Issue> issues = new ArrayList<>(store.load(repo));
        Issue issue = findById(issues, id);
        issue.close(actor);
        store.save(repo, issues);
    }

    public void reopen(LocalRepo repo, IssueId id) {
        List<Issue> issues = new ArrayList<>(store.load(repo));
        Issue issue = findById(issues, id);
        issue.reopen();
        store.save(repo, issues);
    }

    public void assign(LocalRepo repo, IssueId id, String assignee) {
        List<Issue> issues = new ArrayList<>(store.load(repo));
        Issue issue = findById(issues, id);
        issue.assignTo(assignee);
        store.save(repo, issues);
    }

    public void setPriority(LocalRepo repo, IssueId id, IssuePriority priority) {
        List<Issue> issues = new ArrayList<>(store.load(repo));
        Issue issue = findById(issues, id);
        issue.setPriority(priority);
        store.save(repo, issues);
    }

    public void setLabels(LocalRepo repo, IssueId id, List<String> labels) {
        List<Issue> issues = new ArrayList<>(store.load(repo));
        Issue issue = findById(issues, id);
        issue.setLabels(labels);
        store.save(repo, issues);
    }

    private Issue findById(List<Issue> issues, IssueId id) {
        Objects.requireNonNull(id, "id cannot be null");
        // Linear scan is intentional here: collections are small and this keeps the flow simple.
        return issues.stream()
                .filter(issue -> issue.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Issue not found: " + id.value()));
    }

    private boolean matches(Issue issue, IssueQuery query) {
        return matchesText(issue, query.text())
                && matchesStatus(issue, query.status())
                && matchesAssignee(issue, query.assignee())
                && matchesPriority(issue, query.priority())
                && matchesLabels(issue, query.labels());
    }

    private boolean matchesText(Issue issue, String text) {
        if (text == null) {
            return true;
        }

        String normalizedText = text.toLowerCase(Locale.ROOT);
        String title = issue.title() == null ? "" : issue.title().toLowerCase(Locale.ROOT);
        String description = issue.description() == null ? "" : issue.description().toLowerCase(Locale.ROOT);

        return title.contains(normalizedText) || description.contains(normalizedText);
    }

    private boolean matchesStatus(Issue issue, IssueStatus status) {
        if (status == null) {
            return true;
        }
        IssueStatus issueStatus = issue.status() == null ? null : issue.status().canonical();
        return issueStatus == status.canonical();
    }

    private boolean matchesAssignee(Issue issue, String assignee) {
        return assignee == null
                || (issue.assignee() != null && issue.assignee().equalsIgnoreCase(assignee));
    }

    private boolean matchesPriority(Issue issue, IssuePriority priority) {
        return priority == null || issue.priority() == priority;
    }

    private boolean matchesLabels(Issue issue, List<String> labels) {
        List<String> issueLabels = issue.labels() == null ? List.of() : issue.labels();
        return labels.isEmpty() || issueLabels.containsAll(labels);
    }

    /**
     * Transitions an issue to a target workflow status.
     *
     * @param repo target repository
     * @param id issue id
     * @param target desired status
     * @throws IllegalStateException when the transition violates workflow rules
     * @see Issue#transitionTo(IssueStatus)
     */
    public void updateStatus(LocalRepo repo, IssueId id, IssueStatus target) {
        List<Issue> issues = new ArrayList<>(store.load(repo));
        Issue issue = findById(issues, id);

        issue.transitionTo(target);

        store.save(repo, issues);
    }
}
