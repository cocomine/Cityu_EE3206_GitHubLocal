# README (Ivan Part) — Comment & Issue Backend Handoff for GUI

This document explains what changed in the Comment/Issue backend and what the GUI owner needs to do to cooperate with those changes.

---

## 1) Scope of my changes

I only changed backend-related parts for Comment/Issue:

- `domain`
- `infrastructure/store`
- related backend tests

I did **not** change GUI controllers/views as part of this backend evolution.

---

## 2) What changed in backend

### 2.1 Domain model is richer now

#### `Issue`
- New fields:
  - `description`
  - `updatedAt`
  - `closedAt`
  - `closedBy`
- New behavior:
  - `updateDescription(String)`
  - `close(String actor)` (existing `close()` still exists for compatibility)
  - `editComment(CommentId, String)`
  - `deleteComment(CommentId, String)`
- Invariants are stricter now (constructor validation):
  - open issue cannot carry closed metadata
  - closed issue must carry closed metadata
  - `updatedAt` cannot be before `createdAt`

#### `Comment`
- Added stable identity: `CommentId`
- New fields:
  - `createdAt`
  - `editedAt`
  - `deletedAt`
  - `deletedBy`
  - `replyToCommentId` (optional hook)
  - `commitReference` (optional hook)
  - `filePath` (optional hook)
- New behavior:
  - `editBody(String)`
  - `delete(String actor)`
  - `isDeleted()`
- Compatibility method kept:
  - `time()` still exists and returns `createdAt`.

---

### 2.2 Persistence format evolved (JSON store)

`JsonIssueStore` now supports a **versioned envelope** and safer file handling.

#### New `issues.json` shape (written format)

```json
{
  "schemaVersion": 1,
  "storeRevision": "...",
  "issues": [ ... ]
}
```

#### Migration / compatibility
- Legacy bare-array files are still readable.
- If legacy records are missing new fields (example: comment id/timestamp), backend synthesizes them and persists migrated data.

#### Safer write/load semantics
- Save path: temp file + atomic replace (`ATOMIC_MOVE` with fallback).
- Corrupt/unreadable store file is quarantined to:
  - `.gitgui/issues.corrupt-<timestamp>.json`
  - then load fails with explicit exception.

---

### 2.3 Store contract grew extension points

`IssueStore` now also has default helpers:

- `loadSnapshot(LocalRepo)`
- `saveSnapshot(LocalRepo, IssueStoreSnapshot)`
- `upsertIssue(LocalRepo, Issue)`
- `deleteIssue(LocalRepo, IssueId)`

This is to support future backend evolution while keeping old `load/save` flow usable.

---

## 3) What still works exactly as before

Current GUI calls remain valid:

- `facade.listIssues(...)`
- `facade.createIssue(...)`
- `facade.commentIssue(...)`
- `facade.closeIssue(...)`
- `facade.reopenIssue(...)`

So existing Issue tab should still run, but it is not using the richer backend fields yet.

---

## 4) What GUI owner should do now (required to cooperate)

### 4.1 Update issue details rendering

In `IssueController.showIssueDetails(...)`, include new issue metadata:

- `issue.description()`
- `issue.updatedAt()`
- `issue.closedAt()`
- `issue.closedBy()`

For comments, prefer:

- `comment.createdAt()` instead of old `comment.time()`
- still safe to use `time()` if needed (it maps to `createdAt`)

### 4.2 Show deleted/edited comment state properly

When rendering each comment:

- if `comment.isDeleted()`:
  - display as deleted (do not show as normal editable content)
  - optionally show `deletedBy` and `deletedAt`
- if `comment.editedAt() != null`:
  - show an "edited" indicator

### 4.3 Handle backend load failures clearly

When `listIssues` throws `IllegalStateException`, show user-friendly error text like:

- issue store is corrupted
- file has been quarantined under `.gitgui/issues.corrupt-*.json`
- user can inspect/recover and refresh

This prevents silent confusion when a bad JSON file exists.

---

## 5) What GUI owner should do next (to use new capabilities)

These are not fully wired through `AppFacade` yet, but backend domain supports them.

### 5.1 Add GUI actions for richer lifecycle

Recommended new GUI actions:

- Edit issue description
- Edit comment
- Delete comment (soft delete view behavior)

### 5.2 Coordinate API wiring in application layer

To expose those actions cleanly from GUI, add facade/service methods such as:

- `updateIssueDescription(...)`
- `editIssueComment(...)`
- `deleteIssueComment(...)`
- optional: `closeIssue(..., actor)` to record real closer identity from UI input

Current `closeIssue(...)` path calls `issue.close()` (no actor argument), so `closedBy` defaults to issue creator in that flow.

---

## 6) Quick field map for GUI binding

### Issue
- `id().value()`
- `title()`
- `description()`
- `creator()`
- `status()`
- `createdAt()`
- `updatedAt()`
- `closedAt()`
- `closedBy()`
- `comments()`

### Comment
- `id().value()`
- `author()`
- `body()`
- `createdAt()`
- `editedAt()`
- `deletedAt()`
- `deletedBy()`
- `isDeleted()`
- `replyToCommentId()`
- `commitReference()`
- `filePath()`

---

## 7) Validation status

Backend verification already completed after these changes:

- `./gradlew test --tests "*Issue*" --tests "*Comment*" --tests "*JsonIssueStore*"` ✅
- `./gradlew test` ✅
- `./gradlew build` ✅

So GUI integration work can proceed on top of a passing backend baseline.
