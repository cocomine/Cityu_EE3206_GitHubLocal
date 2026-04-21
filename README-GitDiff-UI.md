# GitDiff UI 使用說明

這份文件給 UI 負責同學，說明如何正確使用 `domain.GitDiff`。

## 目標類別
- `domain.GitDiff`
- 取得方式：`AppFacade.gitDiff(LocalRepo repo, boolean staged)`

## `GitDiff` 代表什麼
`GitDiff` 是 `git diff` 或 `git diff --staged` 解析後的一次快照（snapshot）。

最上層只有一個欄位：
- `files()`：本次 diff 的檔案清單

重要行為：
- 回傳的是不可變副本（`List.copyOf(...)`）。
- 不是即時資料，執行 Git 操作後要重新抓一次。
- 沒有變更時，`files()` 會是空清單。

## 檔案層級：`GitDiff.FileDiff`
每個檔案包含：
- `oldPath()`：舊路徑（新增檔可能是 `/dev/null`）
- `newPath()`：新路徑（刪除檔可能是 `/dev/null`）
- `status()`：`NEW`、`DELETE`、`RENAME`、`CHANGE`
- `binary()`：是否為 binary 檔案
- `hunks()`：`@@ ... @@` 區塊清單

`status()` 與 UI 對照建議：
- `NEW` -> New
- `DELETE` -> Delete
- `RENAME` -> Rename
- `CHANGE` -> Change

## 區塊層級：`GitDiff.Hunk`
每個 hunk 對應一段 `@@ -oldStart,oldCount +newStart,newCount @@`：
- `oldStart()` / `oldCount()`
- `newStart()` / `newCount()`
- `header()`：`@@ ... @@` 後面的附加資訊（通常是函式名稱）
- `lines()`：區塊內每一行的差異

## 行層級：`GitDiff.DiffLine`
每行包含：
- `status()`：`NEW`、`DELETE`、`NO_CHANGE`、`META`
- `text()`：行內容（不含 `+` / `-` / 空白前綴）
- `oldLineNo()`：舊檔行號（純新增時為 `null`）
- `newLineNo()`：新檔行號（純刪除時為 `null`）

`status()` 與 diff 前綴對照：
- `NEW` -> `+`
- `DELETE` -> `-`
- `NO_CHANGE` -> ` `
- `META` -> 例如 `\ No newline at end of file`

## UI 使用建議
- File list：先顯示 `newPath`，若 rename/比較需求再補 `oldPath -> newPath`
- Hunk header：直接用 `oldStart/oldCount/newStart/newCount` 組字串顯示
- Line table：左右欄用 `oldLineNo/newLineNo`，中間欄用 `status`
- replace 顯示：連續 `DELETE` + 緊接 `NEW` 可在視覺上合併成「修改」
- binary 檔：若 `binary() == true`，不要渲染 line diff，改顯示 `Binary file changed`

## 最小整合範例（UI 側）
```java
LocalRepo repo = requireRepo();
GitDiff diff = facade.gitDiff(repo, stagedToggle.isSelected());

for (GitDiff.FileDiff file : diff.files()) {
    renderFileHeader(file.newPath(), file.status().name());

    if (file.binary()) {
        renderBinaryHint();
        continue;
    }

    for (GitDiff.Hunk hunk : file.hunks()) {
        renderHunkHeader(hunk.oldStart(), hunk.oldCount(), hunk.newStart(), hunk.newCount(), hunk.header());
        for (GitDiff.DiffLine line : hunk.lines()) {
            renderLine(line.oldLineNo(), line.newLineNo(), line.status(), line.text());
        }
    }
}
```

## 何時刷新 Diff
建議在以下操作後重新呼叫 `gitDiff(...)`：
- `gitStage`
- `gitUnstage`
- `gitCommit`
- `gitCheckout`
- `gitMerge`
- `gitRollbackHard`
- 使用者切換 staged/unstaged toggle
- 使用者按下手動刷新按鈕

這樣可確保畫面與目前工作樹/暫存區狀態一致。
