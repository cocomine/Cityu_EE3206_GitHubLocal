# RepoStatus UI 使用說明

這份文件給 UI 負責同學，說明如何正確使用 `domain.RepoStatus`。

## 目標類別
- `domain.RepoStatus`
- 相關型別：`domain.FileChange`、`domain.ChangeType`
- 取得方式：`AppFacade.gitStatus(LocalRepo repo)`

## `RepoStatus` 代表什麼
`RepoStatus` 是 `git status --porcelain` 解析後的一次快照（snapshot）。

它包含三個清單：
- `staged()`：已進 index、可 commit 的檔案
- `unstaged()`：工作目錄有變更但尚未 stage 的檔案
- `changes()`：`staged` + `unstaged` 的合併結果

重要行為：
- 回傳的是不可變副本（`List.copyOf(...)`）。
- 不是即時資料，執行 Git 操作後要重新抓一次。
- 同一路徑可同時出現在 `staged()` 與 `unstaged()`。
- `changes()` 可能出現重複路徑（同檔案同時 staged+unstaged）。

## UI 使用約定
建議 UI 直接這樣對應：
- Staged 區塊只吃 `status.staged()`
- Unstaged 區塊只吃 `status.unstaged()`
- `changes()` 只用在總數統計或可接受重複的整合視圖

UI 不建議做的事：
- 在 UI 流程中呼叫 `addStaged` / `addUnstaged` 去改狀態物件
- 假設 `changes()` 內每個 path 一定唯一

## 與 IntelliJ IDEA 的差異（重要）
和 IDEA 不一樣，這是預期行為。

說明：
- Git 本質流程仍是：`修改 -> unstaged -> stage(add) -> commit`
- IDEA 在預設設定下，常把 stage 流程包在 Commit UI 裡，看起來像「不用先 add」
- 本專案目前採用「明確分 staged / unstaged」設計，對齊 `RepoStatus` 資料模型

UI 設計注意：
- Commit 只提交 `staged()` 清單內容
- 若 `staged()` 為空，建議禁用 Commit 按鈕或顯示提示
- 在畫面上明確提供 `Stage` / `Unstage` 操作，避免使用者誤解
- 可加上一句提示文案：`IDEA 可能自動處理 stage；本工具採顯式 stage 流程`

或者你可以直接修改為與idea行為一樣的ui, 同時處理 staged 與 unstaged 的檔案

## 顯示規則
每個 `FileChange` 具備：
- `path()`：要顯示的檔案路徑（rename 已正規化）
- `type()`：`ADDED`、`MODIFIED`、`DELETED`、`RENAMED`、`UNTRACKED`

建議文案：
- `ADDED` -> Added
- `MODIFIED` -> Modified
- `DELETED` -> Deleted
- `RENAMED` -> Renamed
- `UNTRACKED` -> Untracked

## Porcelain 與 UI 對照範例
下面示範一行 `git status --porcelain` 會如何進入 UI：

- ` M src/A.java`
  - `staged()`：無
  - `unstaged()`：`src/A.java [MODIFIED]`

- `M  src/A.java`
  - `staged()`：`src/A.java [MODIFIED]`
  - `unstaged()`：無

- `MM src/A.java`
  - `staged()`：`src/A.java [MODIFIED]`
  - `unstaged()`：`src/A.java [MODIFIED]`
  - UI 應同時在兩區塊顯示此檔案。

- `?? src/NewFile.java`
  - `staged()`：無
  - `unstaged()`：`src/NewFile.java [UNTRACKED]`

- `R  old.txt -> new.txt`
  - `staged()`：`new.txt [RENAMED]`
  - `unstaged()`：無
  - 解析器保留新路徑（`new.txt`）供 UI 顯示。

## 最小整合範例（UI 側）
```java
RepoStatus status = facade.gitStatus(currentRepo);

List<FileChange> staged = status.staged();
List<FileChange> unstaged = status.unstaged();

renderStagedList(staged);
renderUnstagedList(unstaged);

boolean isClean = staged.isEmpty() && unstaged.isEmpty();
cleanBadge.setVisible(isClean);
```

## 何時要刷新狀態
建議在以下操作後呼叫 `gitStatus` 並重畫 UI：
- `gitStage`
- `gitUnstage`
- `gitCommit`
- 使用者按下手動刷新按鈕

這樣可以確保畫面與實際 Git 狀態一致。
