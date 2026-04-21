package application.command.parser;

import domain.GitDiff;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiffParserTest {
    /**
     * Tests that the parser correctly handles a complex diff with a mix of
     * file modifications (CHANGE), new files (NEW), and deleted files (DELETE).
     * It verifies that the file paths, statuses, and line-by-line changes
     * within each hunk are parsed correctly.
     */
    @Test
    void parseShouldReturnStructuredDiffForChangeNewAndDelete() {
        String diff = String.join("\n",
                "diff --git a/src/A.java b/src/A.java",
                "index 1111111..2222222 100644",
                "--- a/src/A.java",
                "+++ b/src/A.java",
                "@@ -9,3 +9,4 @@ public class A {",
                " context-line",
                "-removed",
                "+added",
                "+added2",
                "diff --git a/src/NewFile.java b/src/NewFile.java",
                "new file mode 100644",
                "index 0000000..3333333",
                "--- /dev/null",
                "+++ b/src/NewFile.java",
                "@@ -0,0 +1,2 @@",
                "+new-1",
                "+new-2",
                "diff --git a/src/OldFile.java b/src/OldFile.java",
                "deleted file mode 100644",
                "index 4444444..0000000",
                "--- a/src/OldFile.java",
                "+++ /dev/null",
                "@@ -1,2 +0,0 @@",
                "-old-1",
                "-old-2"
        );

        DiffParser parser = new DiffParser();
        GitDiff parsed = parser.parse(diff);

        assertEquals(3, parsed.files().size());

        // 1 CHANGE
        GitDiff.FileDiff changed = parsed.files().get(0);
        assertEquals("src/A.java", changed.oldPath());
        assertEquals("src/A.java", changed.newPath());
        assertEquals(GitDiff.FileStatus.CHANGE, changed.status());
        assertEquals(1, changed.hunks().size());
        assertEquals(4, changed.hunks().getFirst().lines().size());
        assertEquals(GitDiff.LineStatus.NO_CHANGE, changed.hunks().getFirst().lines().get(0).status());
        assertEquals(Integer.valueOf(9), changed.hunks().getFirst().lines().get(0).oldLineNo());
        assertEquals(Integer.valueOf(9), changed.hunks().getFirst().lines().get(0).newLineNo());
        assertEquals(GitDiff.LineStatus.DELETE, changed.hunks().getFirst().lines().get(1).status());
        assertEquals(Integer.valueOf(10), changed.hunks().getFirst().lines().get(1).oldLineNo());
        assertNull(changed.hunks().getFirst().lines().get(1).newLineNo());
        assertEquals(GitDiff.LineStatus.NEW, changed.hunks().getFirst().lines().get(2).status());
        assertNull(changed.hunks().getFirst().lines().get(2).oldLineNo());
        assertEquals(Integer.valueOf(10), changed.hunks().getFirst().lines().get(2).newLineNo());
        assertEquals(GitDiff.LineStatus.NEW, changed.hunks().getFirst().lines().get(3).status());
        assertEquals(Integer.valueOf(11), changed.hunks().getFirst().lines().get(3).newLineNo());

        // 2 NEW
        GitDiff.FileDiff added = parsed.files().get(1);
        assertEquals("/dev/null", added.oldPath());
        assertEquals("src/NewFile.java", added.newPath());
        assertEquals(GitDiff.FileStatus.NEW, added.status());
        assertEquals(1, added.hunks().size());
        assertEquals(GitDiff.LineStatus.NEW, added.hunks().getFirst().lines().getFirst().status());
        assertEquals(Integer.valueOf(1), added.hunks().getFirst().lines().getFirst().newLineNo());

        // 3 DELETE
        GitDiff.FileDiff deleted = parsed.files().get(2);
        assertEquals("src/OldFile.java", deleted.oldPath());
        assertEquals("/dev/null", deleted.newPath());
        assertEquals(GitDiff.FileStatus.DELETE, deleted.status());
        assertEquals(1, deleted.hunks().size());
        assertEquals(GitDiff.LineStatus.DELETE, deleted.hunks().getFirst().lines().getFirst().status());
        assertEquals(Integer.valueOf(1), deleted.hunks().getFirst().lines().getFirst().oldLineNo());
    }

    /**
     * Tests that the parser correctly handles empty or null diff output.
     * It should produce a {@link GitDiff} object with an empty list of files.
     */
    @Test
    void parseShouldHandleEmptyOutput() {
        DiffParser parser = new DiffParser();
        GitDiff parsed = parser.parse("");

        assertTrue(parsed.files().isEmpty());
    }

    /**
     * Tests that the parser correctly identifies a file rename when the diff
     * output contains explicit "rename from" and "rename to" metadata lines.
     * It verifies that the file status is set to RENAME and the old/new paths
     * are correctly extracted.
     */
    @Test
    void parseShouldMarkRenameStatusWhenRenameMetadataExists() {
        String diff = String.join("\n",
                "diff --git a/src/OldName.java b/src/NewName.java",
                "similarity index 100%",
                "rename from src/OldName.java",
                "rename to src/NewName.java"
        );

        DiffParser parser = new DiffParser();
        GitDiff parsed = parser.parse(diff);

        assertEquals(1, parsed.files().size());
        GitDiff.FileDiff renamed = parsed.files().getFirst();
        assertEquals("src/OldName.java", renamed.oldPath());
        assertEquals("src/NewName.java", renamed.newPath());
        assertEquals(GitDiff.FileStatus.RENAME, renamed.status());
        assertTrue(renamed.hunks().isEmpty());
    }

    @Test
    void parseShouldMarkRenameWhenOnlyDiffHeaderPathChanged() {
        String diff = String.join("\n",
                "diff --git a/src/OldName.java b/src/NewName.java",
                "index 1111111..2222222 100644",
                "--- a/src/OldName.java",
                "+++ b/src/NewName.java",
                "@@ -1 +1 @@",
                "-old",
                "+new"
        );

        DiffParser parser = new DiffParser();
        GitDiff parsed = parser.parse(diff);

        assertEquals(1, parsed.files().size());
        GitDiff.FileDiff renamed = parsed.files().getFirst();
        assertEquals("src/OldName.java", renamed.oldPath());
        assertEquals("src/NewName.java", renamed.newPath());
        assertEquals(GitDiff.FileStatus.RENAME, renamed.status());
        assertEquals(1, renamed.hunks().size());
    }
}
