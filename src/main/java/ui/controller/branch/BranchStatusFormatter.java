package ui.controller.branch;

/**
 * Formatter for user-facing branch status labels (current branch, last merge summary).
 */
public final class BranchStatusFormatter {
    private BranchStatusFormatter() {}

    public static String formatCurrentBranch(String branchName) {
        if (branchName == null || branchName.isBlank()) {
            return "Current branch: (unknown)";
        }

        return "Current branch: " + branchName;
    }

    public static String formatLastMerge(String targetBranch, String sourceBranch) {
        if (targetBranch == null || targetBranch.isBlank()
                || sourceBranch == null || sourceBranch.isBlank()) {
            return "Last merge: (none)";
        }

        return "Last merge: merged " + sourceBranch + " into " + targetBranch;
    }
}
