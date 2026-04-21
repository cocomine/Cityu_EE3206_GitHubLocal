package application.command.parser;

import domain.ChangeType;
import domain.FileChange;
import domain.RepoStatus;

/**
 * A parser for the output of the `git status --porcelain` command.
 * This parser converts the plain text output into a {@link RepoStatus} object.
 */
public class RepoStatusParser implements GitOutputParser<RepoStatus> {
    /**
     * Parses the given text output from a `git status --porcelain` command.
     *
     * @param text the raw string output from the git command.
     * @return a {@link RepoStatus} object representing the parsed status.
     */
    @Override
    public RepoStatus parse(String text) {
        RepoStatus status = new RepoStatus();
        // Handle empty or null output
        if (text == null || text.isBlank()) {
            return status;
        }

        String[] lines = text.split("\\R"); // split by any line terminator (handles both \n and \r\n)
        for (String line : lines) {
            // Skip empty line
            if (line == null || line.isBlank()) {
                continue;
            }

            // Handle untracked files (lines starting with "??")
            if (line.startsWith("??")) {
                String path = extractPath(line);
                status.addUnstaged(new FileChange(path, ChangeType.UNTRACKED));
                continue;
            }

            // Lines should be at least 3 characters long (two status codes and a space) to be valid
            if (line.length() < 3) {
                continue;
            }

            // Extract the staged and unstaged status codes and the file path
            char stagedCode = line.charAt(0);
            char unstagedCode = line.charAt(1);
            String path = extractPath(line);

            // If the staged code is not a space, add it to the staged changes
            if (stagedCode != ' ') {
                status.addStaged(new FileChange(path, mapType(stagedCode)));
            }
            // If the unstaged code is not a space, add it to the unstaged changes
            if (unstagedCode != ' ') {
                status.addUnstaged(new FileChange(path, mapType(unstagedCode)));
            }
        }
        return status;
    }

    /**
     * Extracts the file path from a line of `git status --porcelain` output.
     *
     * @param line the line of output.
     * @return the extracted file path.
     */
    private String extractPath(String line) {
        return line.length() > 3 ? line.substring(3).trim() : ""; // get path without first 3 characters (status codes and space)
    }

    /**
     * Maps a character code from `git status --porcelain` to a {@link ChangeType}.
     *
     * @param code the character code representing the status of a file.
     * @return the corresponding {@link ChangeType}.
     */
    private ChangeType mapType(char code) {
        return switch (code) {
            case 'A' -> ChangeType.ADDED;
            case 'D' -> ChangeType.DELETED;
            case 'R' -> ChangeType.RENAMED;
            case '?' -> ChangeType.UNTRACKED;
            default -> ChangeType.MODIFIED; // code = M
        };
    }
}
