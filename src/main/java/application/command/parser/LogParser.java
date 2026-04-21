package application.command.parser;

import domain.GitLog;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A parser for the output of a custom 'git log' command.
 * This parser is designed to parse a log format where fields are separated by " | ".
 * The expected format for each line is:
 * {@code <hash> (<decoration>) <message> | <author> | <date>}
 */
public class LogParser implements GitOutputParser<List<GitLog>> {
    private static final String FIELD_SEPARATOR = " | ";
    private static final DateTimeFormatter DATE_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("EEE MMM d HH:mm:ss yyyy Z")
            .toFormatter(Locale.ENGLISH);

    /**
     * Parses the given text output from a 'git log' command into a list of {@link GitLog} objects.
     *
     * @param text The raw string output from the git command.
     * @return A list of {@link GitLog} objects. Returns an empty list if the text is null, blank, or cannot be parsed.
     */
    @Override
    public List<GitLog> parse(String text) {
        List<GitLog> logs = new ArrayList<>();

        if (text == null || text.isBlank()) {
            return logs;
        }

        for (String line : text.split("\\R")) {
            if (!line.isBlank()) {
                GitLog log = parseLine(line);
                if (log != null) {
                    logs.add(log);
                }
            }
        }
        return logs;
    }

    /**
     * Parses a single line of 'git log' output.
     *
     * @param line The line to parse.
     * @return A {@link GitLog} object, or null if the line is malformed.
     */
    private GitLog parseLine(String line) {
        // The line is expected to be in the format: "<hash> (<decoration>) <message> | <author> | <date>"
        int dateSeparatorIndex = line.lastIndexOf(FIELD_SEPARATOR);
        if (dateSeparatorIndex < 0) {
            return null;
        }
        String dateText = line.substring(dateSeparatorIndex + FIELD_SEPARATOR.length()).trim();
        String beforeDate = line.substring(0, dateSeparatorIndex);

        // Now, beforeDate should be in the format: "<hash> (<decoration>) <message> | <author>"
        int authorSeparatorIndex = beforeDate.lastIndexOf(FIELD_SEPARATOR);
        if (authorSeparatorIndex < 0) {
            return null;
        }
        String author = beforeDate.substring(authorSeparatorIndex + FIELD_SEPARATOR.length()).trim();
        String hashWithMessage = beforeDate.substring(0, authorSeparatorIndex);

        // Extract the hash, which is the first part of hashWithMessage before the first space.
        // hashWithMessage format: "<hash> (<decoration>) <message>"
        int firstSpaceIndex = hashWithMessage.indexOf(' ');
        if (firstSpaceIndex <= 0) {
            return null;
        }
        String hash = hashWithMessage.substring(0, firstSpaceIndex).trim();
        if (hash.isEmpty()) {
            return null;
        }

        // Extract decorations and the commit message from the remaining part of hashWithMessage.
        // format: "(<decoration>) <message>"
        DecorationAndMessage decorationAndMessage = extractDecorationAndMessage(hashWithMessage.substring(firstSpaceIndex + 1));
        String message = decorationAndMessage.message();
        if (message.isEmpty()) {
            return null;
        }

        // Parse the date string into a LocalDateTime object.
        LocalDateTime date = parseDate(dateText);
        if (date == null) {
            return null;
        }

        return new GitLog(hash, decorationAndMessage.tags(), message, author, date);
    }

    /**
     * Extracts branch/tag decorations and the commit message from the log entry.
     * e.g., "(HEAD -> main, origin/main) feat: Add new feature"
     *
     * @param decoratedMessage The part of the log line containing decorations and the message.
     * @return A {@link DecorationAndMessage} record containing the list of tags and the commit message.
     */
    private DecorationAndMessage extractDecorationAndMessage(String decoratedMessage) {
        // The decoratedMessage is expected to be in the format: "(<decoration>) <message>" or just "<message>" if there are no decorations.
        // First, check if it starts with a parenthesis to determine if there are decorations.
        String content = decoratedMessage.stripLeading();
        if (!content.startsWith("(")) {
            return new DecorationAndMessage(List.of(), content.trim());
        }

        // Find the closing parenthesis that ends the decoration part.
        int decorationEndIndex = content.indexOf(") ");
        if (decorationEndIndex < 0) {
            return new DecorationAndMessage(List.of(), content.trim());
        }

        // Extract the decoration string and split it into tags.
        // Note: New thing Java Stream API
        String decoration = content.substring(1, decorationEndIndex).trim();
        List<String> tags = decoration.isEmpty()
                ? List.of()
                : Stream.of(decoration.split(","))
                        .map(String::trim)
                        .filter(tag -> !tag.isEmpty())
                        .collect(Collectors.toList());
        String message = content.substring(decorationEndIndex + 2).trim();
        return new DecorationAndMessage(tags, message);
    }

    /**
     * Parses a date string from the 'git log' output.
     *
     * @param dateText The date string (e.g., "Sun Mar 15 14:30:00 2026 +0800").
     * @return A {@link LocalDateTime} object, or null if parsing fails.
     */
    private LocalDateTime parseDate(String dateText) {
        try {
            return OffsetDateTime.parse(dateText, DATE_FORMATTER).toLocalDateTime();
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    /**
     * A record to hold the extracted decoration tags and the commit message.
     *
     * @param tags    A list of tags (e.g., "HEAD -> main", "origin/main").
     * @param message The commit message.
     */
    private record DecorationAndMessage(List<String> tags, String message) {
    }
}
