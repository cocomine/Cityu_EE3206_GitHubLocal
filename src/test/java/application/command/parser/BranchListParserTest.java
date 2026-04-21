package application.command.parser;

import domain.BranchListInfo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BranchListParserTest {
    @Test
    void parseShouldReturnBranchesAndCurrentBranch() {
        BranchListParser parser = new BranchListParser();
        BranchListInfo info = parser.parse("""
                  Ivan
                  main
                * oscar
                """);

        assertEquals(List.of("Ivan", "main", "oscar"), info.branches());
        assertEquals("oscar", info.currentBranch());
        assertTrue(info.hasCurrentBranch());
    }

    @Test
    void parseShouldReturnEmptyInfoForBlankOutput() {
        BranchListParser parser = new BranchListParser();
        BranchListInfo info = parser.parse("   \n");

        assertTrue(info.branches().isEmpty());
        assertEquals("", info.currentBranch());
        assertFalse(info.hasCurrentBranch());
    }
}
