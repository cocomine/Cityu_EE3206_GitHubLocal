# GitLog UI 使用說明

這份文件給 UI 負責同學，說明如何正確使用 `List<GitLog>`。

## 目標類別
- `domain.GitLog`
- 取得方式：`AppFacade.gitLog(LocalRepo repo, int maxCount)`

## `GitLog` 欄位定義
`GitLog` 目前是 Java record：

```java
public record GitLog(
    String hash,
    List<String> tag,
    String message,
    String author,
    LocalDateTime date
) {}
```

UI 端使用 accessor：
- `hash()`：短 commit hash（例：`d4a1856`）
- `tag()`：來自 `git log %d` 的 decoration 清單
- `message()`：commit message（不含 decoration）
- `author()`：作者名稱
- `date()`：提交時間（`LocalDateTime`）

## `tag()` 解析規則（重點）
後端 parser 會把 `%d` 內容拆成 `List<String>`：

- 輸入：`(HEAD -> oscar, origin/oscar)`  
  `tag()`：`["HEAD -> oscar", "origin/oscar"]`
- 輸入：`(tag:v1.2.0)`  
  `tag()`：`["tag:v1.2.0"]`
- 沒有 decoration 時：`tag()` 會是空清單

注意：
- `tag()` 回傳的是不可變副本（不能直接修改）。
- `HEAD -> xxx` 會保留原字串，UI 可自行拆解或直接顯示。

## 最小整合範例（UI 側）
```java
LocalRepo repo = requireRepo();
List<GitLog> logs = facade.gitLog(repo, 50);

for (GitLog log : logs) {
    String refs = log.tag().isEmpty() ? "" : " [" + String.join(", ", log.tag()) + "]";
    String line = "%s%s %s | %s | %s".formatted(
            log.hash(),
            refs,
            log.message(),
            log.author(),
            log.date()
    );
    appendToHistoryArea(line);
}
```

## UI 顯示建議
- 主列表：`hash + message`
- 次資訊：`author + date`
- 標籤區：顯示 `tag()`（可做 badge/chip）

建議排序：
- 直接使用回傳順序（git 已是由新到舊）

## 何時刷新 Log
建議在以下操作後重新呼叫 `gitLog(...)`：
- `gitCommit`
- `gitCheckout`
- `gitMerge`
- `gitRollbackHard`
- 使用者按下手動刷新按鈕

這樣可確保畫面與 Git 歷史一致。
