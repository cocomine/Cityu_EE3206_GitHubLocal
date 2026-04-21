# BranchListInfo UI 使用說明

這份文件給 UI 負責同學，說明如何正確使用 `domain.BranchListInfo`。

## 目標類別
- `domain.BranchListInfo`
- 取得方式：`AppFacade.gitBranchList(LocalRepo repo)`

## `BranchListInfo` 代表什麼
`BranchListInfo` 是 `git branch --list` 解析後的一次快照（snapshot）。

它包含兩個核心欄位：
- `branches()`：本地 branch 名稱清單（不含 `*`）
- `currentBranch()`：目前 checkout branch 名稱；如果未能判斷會是空字串

輔助方法：
- `hasCurrentBranch()`：是否成功判斷到目前 branch

重要行為：
- `branches()` 回傳的是不可變副本（`List.copyOf(...)`）。
- 不是即時資料，執行 Git 操作後要重新抓一次。
- 沒有 branch 輸出時，`branches()` 會是空清單。

## 解析規則（重點）
Parser 會按每行處理 `git branch --list`：

- 一般行：當作普通 branch 名稱（例如 `main`）
- `*` 開頭行：代表目前 checkout branch，會同時：
  - 去掉 `*` 後加入 `branches()`
  - 設為 `currentBranch()`

範例輸入：
```text
  Ivan
  main
* oscar
```

解析結果：
- `branches()` -> `["Ivan", "main", "oscar"]`
- `currentBranch()` -> `"oscar"`
- `hasCurrentBranch()` -> `true`

## 最小整合範例（UI 側）
```java
LocalRepo repo = requireRepo();
BranchListInfo info = facade.gitBranchList(repo);

branchList.setItems(FXCollections.observableArrayList(info.branches()));
if (info.hasCurrentBranch()) {
    branchList.getSelectionModel().select(info.currentBranch());
}
```

## UI 顯示建議
- Branch list：直接顯示 `branches()`
- Current indicator：用 `hasCurrentBranch()` + `currentBranch()` 做預設選中/高亮
- 手動輸入 checkout 欄位：可讓使用者輸入，亦可配合 list selection

## 何時刷新 Branch List
建議在以下操作後重新呼叫 `gitBranchList(...)`：
- `gitCheckout`
- `gitCreateBranch`
- `gitMerge`
- `gitRollbackHard`
- 使用者按下手動刷新按鈕

這樣可確保 branch 清單與目前 checkout 狀態一致。
