package application.command.parser;

import domain.BranchListInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * A parser implementation that processes the raw output of the {@code git branch} command
 * and converts it into a structured {@link BranchListInfo} object.
 */
public class BranchListParser implements GitOutputParser<BranchListInfo> {
    /**
     * Parses the raw output string from the git branch command.
     * Extracts the list of all available branches and identifies the currently active branch
     * (indicated by an asterisk '*' in the git output).
     *
     * @param text the raw multiline string output from the executed git branch command.
     *             Can be null or blank, in which case an empty {@link BranchListInfo} is returned.
     * @return a {@link BranchListInfo} object containing the parsed list of branches and the active branch name.
     */
    @Override
    public BranchListInfo parse(String text) {
        List<String> branches = new ArrayList<>();
        String currentBranch = "";
        if (text == null || text.isBlank()) {
            return new BranchListInfo(branches, currentBranch);
        }

        for (String line : text.split("\\R")) {
            if (line.isBlank()) {
                continue;
            }
            String trimmed = line.trim();
            boolean isCurrentBranch = trimmed.startsWith("*");
            String cleaned = trimmed;
            if (isCurrentBranch) {
                cleaned = cleaned.substring(1).trim();
                currentBranch = cleaned;
            }
            branches.add(cleaned);
        }
        return new BranchListInfo(branches, currentBranch);
    }
}
