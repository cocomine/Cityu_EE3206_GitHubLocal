package application.command.parser;

import domain.GitDiff;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the output of a `git diff` command into a {@link GitDiff} object.
 */
public class DiffParser implements GitOutputParser<GitDiff> {
    private static final String FILE_HEADER_PREFIX = "diff --git ";
    private static final String RENAME_FROM_PREFIX = "rename from ";
    private static final String RENAME_TO_PREFIX = "rename to ";
    private static final Pattern DIFF_HEADER_PATH_PATTERN =
            Pattern.compile("^diff --git (\"(?:\\\\.|[^\"])+\"|\\S+) (\"(?:\\\\.|[^\"])+\"|\\S+)$");
    private static final Pattern HUNK_HEADER_PATTERN =
            Pattern.compile("^@@ -(\\d+)(?:,(\\d+))? \\+(\\d+)(?:,(\\d+))? @@ ?(.*)$");

    /**
     * Parses the given text output from a `git diff` command.
     *
     * @param text The raw string output from `git diff`.
     * @return A {@link GitDiff} object representing the parsed diff.
     */
    @Override
    public GitDiff parse(String text) {
        // Check for null or empty input and return an empty GitDiff if so
        List<GitDiff.FileDiff> files = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return new GitDiff(files);
        }

        // Split the diff output into lines and iterate through them to identify file diffs and hunks
        String[] lines = text.split("\\R");
        int index = 0;
        while (index < lines.length) {
            String line = lines[index];
            
            // Skip lines until we find a file header line that starts with "diff --git "
            if (!line.startsWith(FILE_HEADER_PREFIX)) {
                index++;
                continue;
            }
            
            // When we find a file header, we call parseFile to extract the file diff and move the index to the next file header
            ParsedFile parsedFile = parseFile(lines, index);
            files.add(parsedFile.file()); // parse
            index = parsedFile.nextIndex();
        }
        return new GitDiff(files);
    }

    
    /**
     * Parses a single file's diff from the provided lines of `git diff` output.
     * It starts parsing from the given `startIndex`, which should be the line
     * containing the "diff --git" header.
     *
     * @param lines The array of all lines from the `git diff` output.
     * @param startIndex The index in the `lines` array where this file's diff starts.
     * @return A {@link ParsedFile} record containing the parsed {@link GitDiff.FileDiff}
     *         and the index of the line where the next file's diff starts.
     */
    private ParsedFile parseFile(String[] lines, int startIndex) {
        PathPair pathPair = extractPathPair(lines[startIndex]); //parse
        String oldPath = pathPair.oldPath(); //old path
        String newPath = pathPair.newPath(); //new path
        GitDiff.FileStatus status = GitDiff.FileStatus.CHANGE; // default to CHANGE, may be updated based on file header lines
        boolean binary = false; // default to false, may be updated if binary file indicators are found
        List<GitDiff.Hunk> hunks = new ArrayList<>(); // list to hold parsed hunks for this file

        // We continue parsing lines until we reach the next file header (which starts with "diff --git ") or the end of the lines array.
        int index = startIndex + 1;
        while (index < lines.length && !lines[index].startsWith(FILE_HEADER_PREFIX)) {
            String line = lines[index];

            // "new file mode " indicates the file was added, set the status to NEW and continue parsing for hunks
            // (which may still be present in the diff output).
            if (line.startsWith("new file mode ")) {
                status = GitDiff.FileStatus.NEW;
                index++;
                continue;
            }
            
            // "deleted file mode " indicates the file was deleted, set the status to DELETE and continue parsing for hunks
            // (which may still be present in the diff output).
            if (line.startsWith("deleted file mode ")) {
                status = GitDiff.FileStatus.DELETE;
                index++;
                continue;
            }

            // "rename from " and "rename to " lines indicate the old and new paths of a renamed file.
            // extract and normalize these paths, set the status to RENAME, and continue parsing for hunks.
            if (line.startsWith(RENAME_FROM_PREFIX)) {
                oldPath = normalizePathToken(line.substring(RENAME_FROM_PREFIX.length()).trim());
                status = GitDiff.FileStatus.RENAME;
                index++;
                continue;
            }
            if (line.startsWith(RENAME_TO_PREFIX)) {
                newPath = normalizePathToken(line.substring(RENAME_TO_PREFIX.length()).trim());
                status = GitDiff.FileStatus.RENAME;
                index++;
                continue;
            }
            
            // "Binary files a/path and b/path differ" or "GIT binary patch" indicate the file is binary, 
            // so set the binary flag and skip to the next lines for further parsing.
            if (line.startsWith("Binary files ") || line.equals("GIT binary patch")) {
                binary = true;
                index++;
                continue;
            }
            
            // The "---" line indicates the old file path in the diff header. Extract and normalize it, then continue parsing for hunks.
            if (line.startsWith("--- ")) {
                oldPath = normalizePathToken(line.substring(4).trim());
                index++;
                continue;
            }
            
            // The "+++" line indicates the new file path in the diff header. Extract and normalize it, then continue parsing for hunks.
            if (line.startsWith("+++ ")) {
                newPath = normalizePathToken(line.substring(4).trim());
                index++;
                continue;
            }
            
            // Hunks start with lines like "@@ -1,5 +1,6 @@ optional header"
            if (line.startsWith("@@ ")) {
                ParsedHunk parsedHunk = parseHunk(lines, index); // parser
                hunks.add(parsedHunk.hunk());
                index = parsedHunk.nextIndex();
                continue;
            }
            index++;
        }

        // If the old path is "/dev/null", it means the file was added, set the status to NEW.
        // If the new path is "/dev/null", it means the file was deleted, set the status to DELETE.
        if ("/dev/null".equals(oldPath)) {
            status = GitDiff.FileStatus.NEW;
        } else if ("/dev/null".equals(newPath)) {
            status = GitDiff.FileStatus.DELETE;
        } else if (status == GitDiff.FileStatus.CHANGE && !oldPath.equals(newPath)) {
            // In some outputs rename metadata lines may be omitted, but path change still indicates rename.
            status = GitDiff.FileStatus.RENAME;
        }

        return new ParsedFile(new GitDiff.FileDiff(oldPath, newPath, status, binary, hunks), index);
    }

    /**
     * Parses a single hunk from the diff output. A hunk starts with a line like
     * "@@ -1,5 +1,6 @@ optional header" and contains the line-by-line changes.
     *
     * @param lines The array of all lines from the `git diff` output.
     * @param startIndex The index in the `lines` array where this hunk starts.
     * @return A {@link ParsedHunk} record containing the parsed {@link GitDiff.Hunk}
     *         and the index of the line where the next hunk or file diff starts.
     */
    private ParsedHunk parseHunk(String[] lines, int startIndex) {
        // The hunk header line is expected to match the pattern defined by HUNK_HEADER_PATTERN, which captures the old and new line ranges and an optional header.
        Matcher headerMatcher = HUNK_HEADER_PATTERN.matcher(lines[startIndex]);
        if (!headerMatcher.matches()) {
            return new ParsedHunk(new GitDiff.Hunk(0, 0, 0, 0, "", List.of()), startIndex + 1);
        }

        // Extract the old and new line numbers and counts from the hunk header using the capturing groups from the regex match.
        int oldStart = Integer.parseInt(headerMatcher.group(1));
        int oldCount = parseCount(headerMatcher.group(2));
        int newStart = Integer.parseInt(headerMatcher.group(3));
        int newCount = parseCount(headerMatcher.group(4));
        String header = headerMatcher.group(5) == null ? "" : headerMatcher.group(5).trim();

        // Initialize line number trackers for the old and new files.
        int oldLineNo = oldStart;
        int newLineNo = newStart;
        List<GitDiff.DiffLine> diffLines = new ArrayList<>();

        // continue parsing lines until we reach the next file header (which starts with "diff --git "),
        // the next hunk header (which starts with "@@ "),
        // or the end of the lines array.
        int index = startIndex + 1;
        while (index < lines.length) {
            String line = lines[index];
            // stop parsing lines for this hunk when we encounter the next file header (which starts with "diff --git ") or the next hunk header (which starts with "@@ ").
            if (line.startsWith(FILE_HEADER_PREFIX) || line.startsWith("@@ ")) {
                break;
            }

            // Lines starting with "\ No newline at end of file" are metadata lines indicating that the last line of the file.
            // add them as META lines and continue parsing.
            if (line.startsWith("\\ No newline at end of file")) {
                diffLines.add(new GitDiff.DiffLine(GitDiff.LineStatus.META, line, null, null));
                index++;
                continue;
            }

            // Empty lines can occur in the diff output and should be treated as META lines to preserve the structure of the diff.
            // add them as META lines and continue parsing.
            if (line.isEmpty()) {
                diffLines.add(new GitDiff.DiffLine(GitDiff.LineStatus.META, "", null, null));
                index++;
                continue;
            }

            // Lines starting with " " indicate context lines that are unchanged,
            // "+" indicate added lines,
            // "-" indicate deleted lines.
            char prefix = line.charAt(0);
            String content = line.substring(1);
            switch (prefix) {
                case ' ' -> { // unchange
                    diffLines.add(new GitDiff.DiffLine(GitDiff.LineStatus.NO_CHANGE, content, oldLineNo, newLineNo));
                    oldLineNo++;
                    newLineNo++;
                }
                case '+' -> { // new
                    diffLines.add(new GitDiff.DiffLine(GitDiff.LineStatus.NEW, content, null, newLineNo));
                    newLineNo++;
                }
                case '-' -> { // old
                    diffLines.add(new GitDiff.DiffLine(GitDiff.LineStatus.DELETE, content, oldLineNo, null));
                    oldLineNo++;
                }
                // meta
                default -> diffLines.add(new GitDiff.DiffLine(GitDiff.LineStatus.META, line, null, null));
            }
            index++;
        }

        GitDiff.Hunk hunk = new GitDiff.Hunk(oldStart, oldCount, newStart, newCount, header, diffLines);
        return new ParsedHunk(hunk, index);
    }

    /**
     * Extracts the old and new file paths from a "diff --git" header line.
     * It uses a regular expression to capture the path portions of the line.
     *
     * @param diffHeaderLine The "diff --git" line from the diff output.
     * @return A {@link PathPair} containing the extracted old and new paths.
     *         Returns a pair of empty strings if the line doesn't match the expected format.
     */
    private PathPair extractPathPair(String diffHeaderLine) {
        Matcher matcher = DIFF_HEADER_PATH_PATTERN.matcher(diffHeaderLine); // e.g., diff --git a/src/A.java b/src/A.java
        
        // If the line doesn't match the expected format, return a pair of empty strings to avoid parsing errors later on
        if (!matcher.matches()) {
            return new PathPair("", "");
        }
        
        // If the line matches, we extract the old and new paths using the capturing groups from the regex and normalize them.
        String oldPath = normalizePathToken(matcher.group(1));
        String newPath = normalizePathToken(matcher.group(2));
        return new PathPair(oldPath, newPath);
    }

    /**
     * Normalizes a path token extracted from a diff line. This involves:
     * - Trimming whitespace.
     * - Removing surrounding quotes if present.
     * - Un-escaping characters like quotes and backslashes.
     * - Removing the "a/" or "b/" prefix used by git.
     *
     * @param token The raw path token from the diff output.
     * @return The normalized path string.
     */
    private String normalizePathToken(String token) {
        String value = token.trim();
        
        // Git diff outputs may quote paths that contain spaces or special characters. We remove the surrounding quotes if they exist.
        if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
            value = value.substring(1, value.length() - 1);
        }
        
        // Un-escape backslashes and quotes in the path (e.g., "a/src/Some\\ File.java" -> "a/src/Some File.java")
        value = value.replace("\\\"", "\"").replace("\\\\", "\\");
        
        // Git diff paths often have "a/" or "b/" prefixes to indicate old vs new paths. We remove these for normalization.
        if (value.startsWith("a/") || value.startsWith("b/")) {
            return value.substring(2);
        }
        return value;
    }

    private int parseCount(String value) {
        return value == null || value.isBlank() ? 1 : Integer.parseInt(value);
    }

    /**
     * A pair of paths extracted from a diff header line.
     * @param oldPath The old path of the file.
     * @param newPath The new path of the file.
     */
    private record PathPair(String oldPath, String newPath) {
    }

    /**
     * A parsed file diff and the index of the next line to process.
     * @param file The parsed {@link GitDiff.FileDiff}.
     * @param nextIndex The index of the next line to continue parsing from.
     */
    private record ParsedFile(GitDiff.FileDiff file, int nextIndex) {
    }

    /**
     * A parsed hunk and the index of the next line to process.
     * @param hunk The parsed {@link GitDiff.Hunk}.
     * @param nextIndex The index of the next line to continue parsing from.
     */
    private record ParsedHunk(GitDiff.Hunk hunk, int nextIndex) {
    }
}
