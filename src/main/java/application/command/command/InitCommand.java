package application.command.command;

import application.command.ResultGitCommand;
import domain.GitResult;
import domain.LocalRepo;
import infrastructure.git.GitClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * The InitCommand class is a concrete implementation of the ResultGitCommand abstract class that represents the `git init` command.
 * This command is used to initialize a new Git repository in a specified local repository.
 * The InitCommand does not require an existing Git repository to execute, as it is responsible for creating one.
 * <p>
 * When executed, it returns a `GitResult` containing the output and status of the initialization process.
 */
public class InitCommand extends ResultGitCommand {
    private static final String GITGUI_IGNORE_ENTRY = ".gitgui";

    /**
     * Constructs a new InitCommand with the specified repository and Git client.
     * This command initializes a new Git repository
     *
     * @param repo   the local repository to initialize as a Git repository. Must not be null.
     * @param client the Git client to use for executing the initialization command. Must not be null.
     */
    public InitCommand(LocalRepo repo, GitClient client) {
        super(repo, client);
    }

    @Override
    protected GitResult handleResult(GitResult result) {
        if (result.isSuccess()) {
            ensureGitignoreContainsGitgui();
        }
        return result;
    }

    /**
     * Ensures that the .gitignore file in the repository contains an entry to ignore the .gitgui directory.
     */
    private void ensureGitignoreContainsGitgui() {
        Path gitignorePath = repo().root().resolve(".gitignore"); // resolve .gitignore path
        try {
            // if not exists, create it
            if (Files.notExists(gitignorePath)) {
                Files.writeString(
                        gitignorePath,
                        GITGUI_IGNORE_ENTRY + System.lineSeparator(),
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE_NEW,
                        StandardOpenOption.WRITE
                );
                return;
            }

            // read .gitignore content and check if it already contains the .gitgui entry
            String content = Files.readString(gitignorePath, StandardCharsets.UTF_8);
            if (containsGitguiEntry(content)) {
                return;
            }

            // get end of file separator
            String separator = content.isEmpty() || content.endsWith("\n") || content.endsWith("\r")
                    ? ""
                    : System.lineSeparator();
            // append the .gitgui entry
            Files.writeString(
                    gitignorePath,
                    separator + GITGUI_IGNORE_ENTRY + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to update .gitignore: " + gitignorePath, e);
        }
    }

    /**
     * Checks if the provided content contains an entry for .gitgui in various formats.
     *
     * @param content the content of the .gitignore file to check for the .gitgui entry. Must not be null.
     * @return true if the content contains an entry for .gitgui, false otherwise.
     */
    private boolean containsGitguiEntry(String content) {
        for (String line : content.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.equals(".gitgui")
                    || trimmed.equals("/.gitgui")
                    || trimmed.equals(".gitgui/")
                    || trimmed.equals("/.gitgui/")) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean requiresGitRepo() {
        return false;
    }

    @Override
    protected List<String> buildArgs() {
        return List.of("init");
    }
}
