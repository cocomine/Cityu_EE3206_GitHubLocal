package infrastructure.store;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import domain.Comment;
import domain.CommentId;
import domain.Issue;
import domain.IssueId;
import domain.IssuePriority;
import domain.IssueStatus;
import domain.LocalRepo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * JSON-backed {@link IssueStore} implementation.
 *
 * <p>The store supports lightweight schema evolution and legacy migration while
 * preserving data durability through write-to-temp then move semantics.
 */
public class JsonIssueStore implements IssueStore {
    private static final String FILE_NAME = "issues.json";
    private static final int CURRENT_SCHEMA_VERSION = 1;

    private final ObjectMapper mapper;

    public JsonIssueStore() {
        mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public List<Issue> load(LocalRepo repo) {
        return loadSnapshot(repo).issues();
    }

    /**
     * Loads a metadata-aware snapshot from disk.
     *
     * <p>Supports both the current object format and the legacy array format.
     * Legacy records are converted to canonical domain objects, and missing
     * historical fields are synthesized when required for compatibility.
     *
     * @param repo target repository
     * @return loaded snapshot
     * @throws IllegalStateException when data is invalid or unreadable
     */
    @Override
    public IssueStoreSnapshot loadSnapshot(LocalRepo repo) {
        Objects.requireNonNull(repo, "repo cannot be null");
        Path path = issueFile(repo);
        if (!Files.exists(path)) {
            return new IssueStoreSnapshot(CURRENT_SCHEMA_VERSION, null, List.of());
        }

        try (InputStream inputStream = Files.newInputStream(path)) {
            JsonNode root = mapper.readTree(inputStream);
            if (root == null || root.isNull()) {
                throw new IllegalStateException("Issue store root cannot be null in " + path);
            }
            if (root.isArray()) {
                // Old format: plain issue list without schema metadata.
                List<IssueRecord> legacyRecords = mapper.convertValue(root, new TypeReference<>() {});
                DomainConversionResult conversion = toDomainIssues(legacyRecords);
                IssueStoreSnapshot migratedSnapshot = new IssueStoreSnapshot(
                        CURRENT_SCHEMA_VERSION,
                        Instant.now().toString(),
                        conversion.issues()
                );
                if (conversion.synthesizedLegacyFields()) {
                    // Persist healed fields immediately so migration is one-time, not every startup.
                    saveSnapshot(repo, migratedSnapshot);
                }
                return migratedSnapshot;
            }

            if (root.isObject()) {
                StoreDocument document = mapper.treeToValue(root, StoreDocument.class);
                if (document.schemaVersion() <= 0) {
                    throw new IllegalStateException("Invalid issue schema version in " + path + ": " + document.schemaVersion());
                }
                if (document.schemaVersion() > CURRENT_SCHEMA_VERSION) {
                    throw new IllegalStateException(
                            "Unsupported issue schema version in " + path + ": " + document.schemaVersion()
                    );
                }
                return new IssueStoreSnapshot(
                        document.schemaVersion(),
                        document.storeRevision(),
                        toDomainIssues(document.issues() == null ? List.of() : document.issues()).issues()
                );
            }

            throw new IllegalStateException("Unsupported issue store root format in " + path);
        } catch (IOException | RuntimeException e) {
            // Keep the broken file for forensics and let the app continue with a clean state next run.
            quarantineCorruptFile(path);
            throw new IllegalStateException("Failed to load issues from " + path, e);
        }
    }

    @Override
    public void save(LocalRepo repo, List<Issue> issues) {
        saveSnapshot(repo, new IssueStoreSnapshot(CURRENT_SCHEMA_VERSION, Instant.now().toString(), issues));
    }

    /**
     * Persists a snapshot using atomic replacement when supported by the filesystem.
     *
     * @param repo target repository
     * @param snapshot snapshot to persist
     * @throws IllegalStateException when writing fails
     */
    @Override
    public void saveSnapshot(LocalRepo repo, IssueStoreSnapshot snapshot) {
        Objects.requireNonNull(repo, "repo cannot be null");
        Objects.requireNonNull(snapshot, "snapshot cannot be null");

        Path metaDir = repo.metaDir();
        Path path = issueFile(repo);
        Path tempFile = null;
        try {
            Files.createDirectories(metaDir);
            List<IssueRecord> records = new ArrayList<>();
            for (Issue issue : snapshot.issues()) {
                records.add(toRecord(issue));
            }
            StoreDocument document = new StoreDocument(CURRENT_SCHEMA_VERSION, snapshot.storeRevision(), records);

            tempFile = Files.createTempFile(metaDir, "issues-", ".tmp");
            try (OutputStream outputStream = Files.newOutputStream(tempFile)) {
                mapper.writerWithDefaultPrettyPrinter().writeValue(outputStream, document);
            }

            try {
                Files.move(tempFile, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                // Some filesystems cannot do atomic moves; fallback still guarantees replace-on-success.
                Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save issues to " + path, e);
        } finally {
            cleanupTempFile(tempFile);
        }
    }

    private Path issueFile(LocalRepo repo) {
        return repo.metaDir().resolve(FILE_NAME);
    }

    private void cleanupTempFile(Path tempFile) {
        if (tempFile == null) {
            return;
        }
        try {
            Files.deleteIfExists(tempFile);
        } catch (IOException ignored) {
            // Best effort cleanup: stale temp files are not ideal, but data integrity is already decided.
        }
    }

    private void quarantineCorruptFile(Path path) {
        if (!Files.exists(path)) {
            return;
        }

        try {
            String backupName = "issues.corrupt-" + Instant.now().toEpochMilli() + ".json";
            Path backupPath = path.resolveSibling(backupName);
            Files.move(path, backupPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
            try {
                String backupName = "issues.corrupt-" + Instant.now().toEpochMilli() + ".json";
                Path backupPath = path.resolveSibling(backupName);
                Files.copy(path, backupPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignoredAgain) {
            }
        }
    }

    private DomainConversionResult toDomainIssues(List<IssueRecord> records) {
        List<Issue> issues = new ArrayList<>();
        boolean synthesizedLegacyFields = false;
        for (IssueRecord record : records) {
            DomainIssueConversionResult conversionResult = toDomain(record);
            issues.add(conversionResult.issue());
            synthesizedLegacyFields = synthesizedLegacyFields || conversionResult.synthesizedLegacyFields();
        }
        return new DomainConversionResult(issues, synthesizedLegacyFields);
    }

    private DomainIssueConversionResult toDomain(IssueRecord record) {
        List<Comment> comments = new ArrayList<>();
        boolean synthesizedLegacyFields = false;
        List<CommentRecord> commentRecords = record.comments() == null ? List.of() : record.comments();
        for (CommentRecord commentRecord : commentRecords) {
            boolean missingId = commentRecord.id() == null || commentRecord.id().isBlank();
            String commentIdValue = missingId ? CommentId.random().value() : commentRecord.id();
            Instant createdAt = commentRecord.createdAt() != null
                    ? commentRecord.createdAt()
                    : commentRecord.time();
            boolean missingCreatedAt = createdAt == null;
            if (createdAt == null) {
                // If time metadata was never stored, "now" is the safest fallback for domain validation.
                createdAt = Instant.now();
            }
            synthesizedLegacyFields = synthesizedLegacyFields || missingId || missingCreatedAt;
            comments.add(new Comment(
                    new CommentId(commentIdValue),
                    commentRecord.author(),
                    commentRecord.body(),
                    createdAt,
                    commentRecord.editedAt(),
                    commentRecord.deletedAt(),
                    commentRecord.deletedBy(),
                    commentRecord.replyToCommentId(),
                    commentRecord.commitReference(),
                    commentRecord.filePath()
            ));
        }

        // Older files used title as description; preserve that behavior during migration.
        String description = record.description() == null ? record.title() : record.description();
        Instant updatedAt = record.updatedAt() == null ? record.createdAt() : record.updatedAt();
        Issue issue = new Issue(
                new IssueId(record.id()),
                record.title(),
                description,
                record.creator(),
                record.status(),
                record.createdAt(),
                updatedAt,
                record.closedAt(),
                record.closedBy(),
                record.assignee(),
                record.priority() == null ? IssuePriority.MEDIUM : record.priority(),
                record.labels() == null ? List.of() : record.labels(),
                comments
        );
        return new DomainIssueConversionResult(issue, synthesizedLegacyFields);
    }

    private IssueRecord toRecord(Issue issue) {
        List<CommentRecord> comments = new ArrayList<>();
        for (Comment comment : issue.comments()) {
            comments.add(new CommentRecord(
                    comment.id().value(),
                    comment.author(),
                    comment.body(),
                    comment.createdAt(),
                    comment.editedAt(),
                    comment.deletedAt(),
                    comment.deletedBy(),
                    comment.replyToCommentId(),
                    comment.commitReference(),
                    comment.filePath(),
                    comment.time()
            ));
        }
        return new IssueRecord(
                issue.id().value(),
                issue.title(),
                issue.description(),
                issue.creator(),
                issue.status(),
                issue.createdAt(),
                issue.updatedAt(),
                issue.closedAt(),
                issue.closedBy(),
                issue.assignee(),
                issue.priority(),
                issue.labels(),
                comments
        );
    }

    private record StoreDocument(
            int schemaVersion,
            String storeRevision,
            List<IssueRecord> issues
    ) {
    }

    private record IssueRecord(
            String id,
            String title,
            String description,
            String creator,
            IssueStatus status,
            Instant createdAt,
            Instant updatedAt,
            Instant closedAt,
            String closedBy,
            String assignee,
            IssuePriority priority,
            List<String> labels,
            List<CommentRecord> comments
    ) {
    }

    private record CommentRecord(
            String id,
            String author,
            String body,
            Instant createdAt,
            Instant editedAt,
            Instant deletedAt,
            String deletedBy,
            String replyToCommentId,
            String commitReference,
            String filePath,
            Instant time
    ) {
    }

    private record DomainConversionResult(List<Issue> issues, boolean synthesizedLegacyFields) {
    }

    private record DomainIssueConversionResult(Issue issue, boolean synthesizedLegacyFields) {
    }
}
