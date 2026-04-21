package domain;

import java.util.List;
import java.util.Objects;

/**
 * Represents parsed output from `git branch --list`.
 *
 * @param branches      all local branch names
 * @param currentBranch currently checked out branch name, or empty string when unavailable
 */
public record BranchListInfo(List<String> branches, String currentBranch) {
    public BranchListInfo {
        branches = List.copyOf(Objects.requireNonNull(branches, "branches cannot be null"));
        currentBranch = currentBranch == null ? "" : currentBranch;
    }

    @Override
    public List<String> branches() {
        return List.copyOf(branches);
    }

    public boolean hasCurrentBranch() {
        return !currentBranch.isBlank();
    }
}
