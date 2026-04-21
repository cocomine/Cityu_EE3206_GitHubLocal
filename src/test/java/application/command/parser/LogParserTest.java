package application.command.parser;

import domain.GitLog;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogParserTest {
    @Test
    void parseShouldReturnStructuredGitLogs() {
        LogParser parser = new LogParser();
        List<GitLog> logs = parser.parse("""
                d4a1856  (HEAD -> oscar, origin/oscar) Refactor CommitCommand and add unit tests for validation and execution | cocomine | Sun Mar 15 19:34:59 2026 +0800
                6945e1a  Refactor InitCommandTest to update import path for InitCommand class | cocomine | Sun Mar 15 05:00:18 2026 +0800
                """);

        assertEquals(2, logs.size());
        assertEquals("d4a1856", logs.get(0).hash());
        assertEquals(List.of("HEAD -> oscar", "origin/oscar"), logs.get(0).tag());
        assertEquals("Refactor CommitCommand and add unit tests for validation and execution", logs.get(0).message());
        assertEquals("cocomine", logs.get(0).author());
        assertEquals(LocalDateTime.of(2026, 3, 15, 19, 34, 59), logs.get(0).date());

        assertEquals("6945e1a", logs.get(1).hash());
        assertTrue(logs.get(1).tag().isEmpty());
        assertEquals("Refactor InitCommandTest to update import path for InitCommand class", logs.get(1).message());
        assertEquals("cocomine", logs.get(1).author());
        assertEquals(LocalDateTime.of(2026, 3, 15, 5, 0, 18), logs.get(1).date());
    }

    @Test
    void parseShouldIgnoreMalformedLines() {
        LogParser parser = new LogParser();
        List<GitLog> logs = parser.parse("""
                malformed-line
                another bad line | only author
                """);

        assertTrue(logs.isEmpty());
    }
}
