package domain;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class LocalRepo {
    private final Path root; // The root directory of the local repository

    /**
     * Creates a new LocalRepo instance with the specified root path.
     * @param root the root directory of the local repository; must not be null
     */
    public LocalRepo(Path root) {
        Objects.requireNonNull(root, "root cannot be null");
        this.root = root.toAbsolutePath().normalize();
    }

    /**
     * Returns the root directory of the local repository.
     * @return the root path
     */
    public Path root() {
        return root;
    }

    /**
     * Returns the path to the .git directory within the repository.
     * @return the .git directory path
     */
    public Path gitDir() {
        return root.resolve(".git");
    }

    /**
     * Returns the path to the .gitgui directory within the repository, used for storing GUI-related metadata.
     * @return the .gitgui directory path
     */
    public Path metaDir() {
        return root.resolve(".gitgui");
    }

    /**
     * Checks if the repository is a valid Git repository by verifying the existence of the .git directory.
     * @return true if the .git directory exists and is a directory; false otherwise
     */
    public boolean isGitRepo() {
        return Files.isDirectory(gitDir());
    }

    @Override
    public String toString() {
        return root.toString();
    }
}
