# README (Ivan Part 2) — Backend Delta + GUI Cooperation Guide

This file is the follow-up to `README_Ivan_Part.md` and focuses on the **new backend delta work** that was added after Part 1 (mainly issue workflow metadata + query capabilities).

---

## 1) What I modified in backend (Part 2 scope)

I added backend support for richer issue workflow and querying.

### 1.1 Issue status lifecycle is now expanded and validated

In `domain/IssueStatus.java` and `domain/Issue.java`:

- Status set now includes:
  - `TODO`, `IN_PROGRESS`, `REVIEW`, `BLOCKED`, `DONE`
  - legacy aliases `OPEN` and `CLOSED` are still accepted and canonicalized
- Transition validation is enforced (invalid transitions throw an exception).
- Close/reopen behavior now maps to canonical states:
  - close -> `DONE`
  - reopen -> `TODO`

### 1.2 Issue assignment / priority / labels are now first-class fields

In `domain/Issue.java` (+ JSON persistence):

- Added issue metadata and behavior:
  - `assignee`
  - `priority` (`LOW`, `MEDIUM`, `HIGH`)
  - `labels` (normalized to lowercase, deduplicated)
- Domain mutators are available:
  - `assignTo(...)`
  - `setPriority(...)`
  - `setLabels(...)`

### 1.3 Search / filter / sort support is now available in application layer

In `application/IssueQuery.java`, `application/IssueService.java`, `application/AppFacade.java`:

- New query DTO: `IssueQuery(text, status, assignee, priority, labels, sortMode)`
- New facade entry point:
  - `facade.queryIssues(LocalRepo repo, IssueQuery query)`
- Query behavior:
  - text search on `title` + `description` (case-insensitive)
  - filter by canonical `status`
  - filter by `assignee` (case-insensitive)
  - filter by `priority`
  - filter by `labels` (issue must contain all requested labels)
  - sort mode support:
    - `UPDATED_DESC` (default)
    - `CREATED_DESC`
  - deterministic tie-breaker by issue id

---

## 2) What GUI owner must do to cooperate with these changes

Current `ui/controller/IssueController.java` still uses only `facade.listIssues(...)` and does not expose the new query/filter/sort features yet.

### 2.1 Replace simple refresh flow with query-aware refresh

In `IssueController.refreshIssues()`:

- Build an `IssueQuery` from UI control values
- Call `facade.queryIssues(requireRepo(), query)` instead of `facade.listIssues(requireRepo())`
- Use `new IssueQuery()` when no filters are set (or pass null and keep default behavior)

### 2.2 Add filter/sort controls in Issue tab

Add these controls and map them to `IssueQuery` fields:

- Search text input -> `text`
- Status dropdown (`ALL`, `TODO`, `IN_PROGRESS`, `REVIEW`, `BLOCKED`, `DONE`) -> `status` (null for ALL)
- Assignee input -> `assignee`
- Priority dropdown (`ALL`, `LOW`, `MEDIUM`, `HIGH`) -> `priority` (null for ALL)
- Labels input (comma-separated) -> `labels` list
- Sort dropdown (`Updated ↓`, `Created ↓`) -> `sortMode`

Important:
- labels are normalized in backend, but GUI should still trim input before sending
- keep filter controls optional; blank/ALL should map to null or empty list

### 2.3 Update issue detail rendering to show new metadata

In `showIssueDetails(...)`, include at least:

- `description`
- `assignee`
- `priority`
- `labels`
- expanded status values (`TODO/IN_PROGRESS/REVIEW/BLOCKED/DONE`)

This avoids confusion where backend state changed but GUI hides it.

### 2.4 Add safe error display for invalid status transitions

When backend throws transition errors (for invalid lifecycle jumps), show user-friendly alerts instead of generic failure text.

Suggested message style:
- "Cannot move issue from REVIEW to TODO directly."
- "Please choose a valid next state."

---

## 3) Recommended GUI implementation checklist

1. Add query controls to top bar (search/status/assignee/priority/labels/sort).
2. Implement `buildIssueQueryFromUI()` helper in `IssueController`.
3. Switch refresh from `listIssues` to `queryIssues`.
4. Render new issue metadata in details pane.
5. Keep existing close/reopen buttons for now (maps to DONE/TODO).
6. Optionally add explicit status transition UI later (for REVIEW/BLOCKED flows).

---

## 4) Quick API reference for GUI owner

### Facade method

```java
List<Issue> queryIssues(LocalRepo repo, IssueQuery query)
```

### Query object

```java
new IssueQuery(
    text,       // String or null
    status,     // IssueStatus or null
    assignee,   // String or null
    priority,   // IssuePriority or null
    labels,     // List<String> (can be empty)
    sortMode    // IssueSortMode or null (defaults to UPDATED_DESC)
)
```

### Enums

- `IssueStatus`: `TODO`, `IN_PROGRESS`, `REVIEW`, `BLOCKED`, `DONE` (legacy `OPEN/CLOSED` still accepted)
- `IssuePriority`: `LOW`, `MEDIUM`, `HIGH`
- `IssueSortMode`: `UPDATED_DESC`, `CREATED_DESC`

---

## 5) Notes for integration sequencing

- Part 1 introduced richer issue/comment lifecycle fields.
- Part 2 adds query/filter/sort + workflow metadata usage.
- Best integration order for GUI:
  1) show metadata in details panel,
  2) wire query controls,
  3) refine status transition UX.

This order gives immediate user-visible value while minimizing risk.
