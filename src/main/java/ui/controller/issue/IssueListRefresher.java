package ui.controller.issue;

import application.AppFacade;
import application.IssueQuery;
import domain.Issue;
import domain.LocalRepo;
import javafx.collections.FXCollections;
import javafx.scene.control.ListView;

import java.util.List;
import java.util.function.Consumer;

/**
 * Utility for refreshing issue list contents from query results.
 * Preserves selection when possible and falls back to first item selection.
 */
public final class IssueListRefresher {
    private IssueListRefresher() {}

    /**
     * Refreshes issue list from query results while preserving previously selected issue id when possible.
     * Falls back to selecting the first issue for stable detail-pane behavior.
     */
    public static void refresh(
            AppFacade facade,
            LocalRepo repo,
            IssueQuery query,
            ListView<Issue> issueList,
            Consumer<Issue> onIssueSelected
    ) {
        String selectedIssueId = getSelectedIssueId(issueList);
        List<Issue> issues = facade.queryIssues(repo, query);

        issueList.setItems(FXCollections.observableArrayList(issues));

        if (issues.isEmpty()) {
            onIssueSelected.accept(null);
            return;
        }

        if (selectedIssueId != null) {
            for (Issue issue : issues) {
                if (issue.id().value().equals(selectedIssueId)) {
                    issueList.getSelectionModel().select(issue);
                    onIssueSelected.accept(issue);
                    return;
                }
            }
        }

        issueList.getSelectionModel().selectFirst();
        onIssueSelected.accept(issueList.getSelectionModel().getSelectedItem());
    }

    private static String getSelectedIssueId(ListView<Issue> issueList) {
        Issue selected = issueList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return null;
        }
        return selected.id().value();
    }
}
