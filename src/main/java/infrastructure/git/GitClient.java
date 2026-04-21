package infrastructure.git;

import domain.GitResult;

import java.nio.file.Path;
import java.util.List;

public interface GitClient {
    GitResult run(Path repoRoot, List<String> args);
}
