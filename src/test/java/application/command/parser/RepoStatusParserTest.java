package application.command.parser;

import domain.ChangeType;
import domain.RepoStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Unit tests for the {@link RepoStatusParser} class.
 */
class RepoStatusParserTest {
    /**
     * Tests that the parser correctly splits staged and unstaged changes
     * from `git status --porcelain` output.
     */
    @Test
    void parseShouldSplitStagedAndUnstagedChanges() {
        String porcelain = "A  src/Main.java\n M README.md\n?? notes.txt\nR  old.txt -> new.txt\n";

        RepoStatusParser parser = new RepoStatusParser();
        RepoStatus status = parser.parse(porcelain);

        assertEquals(2, status.staged().size());
        assertEquals(2, status.unstaged().size());
        assertFalse(status.changes().isEmpty());
        assertEquals(ChangeType.ADDED, status.staged().getFirst().type());
        assertEquals("old.txt -> new.txt", status.staged().get(1).path());
    }
}
