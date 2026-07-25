# Walkthrough - 新增辭書型顯示與 Word Card UI 優化

本任務已成功在單字卡中新增了「辭書型 (Dictionary Form)」的顯示，並同步優化了 Word Card 頁面的介面，移除了冗餘的封面圖參數。

## 變更內容

### 1. 單字分析增強 (辭書型)
- **[JapaneseProcessor.kt](file:///C:/Users/fuala/AndroidStudioProjects/Learn_Japanese_with_Music/app/src/main/java/com/learn_japanese_with_music/features/lyrics/processor/JapaneseProcessor.kt)**：更新了分詞邏輯，現在會從 Sudachi 的分析結果中正確提取單字的「辭書型」（即動詞/形容詞的原形）。
- **[VocabularyCard.kt](file:///C:/Users/fuala/AndroidStudioProjects/Learn_Japanese_with_Music/app/src/main/java/com/learn_japanese_with_music/features/vocabulary/ui/VocabularyCard.kt)**：在單字詳情彈窗中，如果單字的辭書型與表面文字不同，會額外顯示「原形: XXX」，幫助使用者學習。

### 2. UI 簡約化與資料庫優化
- **移除封面圖**：根據需求，移除了 [WordCardPage.kt](file:///C:/Users/fuala/AndroidStudioProjects/Learn_Japanese_with_Music/app/src/main/java/com/learn_japanese_with_music/features/vocabulary/ui/WordCardPage.kt) 中所有關於 `songCover` 的顯示與參數傳遞。這讓介面看起來更乾淨，專注於文字學習。
- **資料庫升級 (Migration)**：
    - 將 [AppDatabase.kt](file:///C:/Users/fuala/AndroidStudioProjects/Learn_Japanese_with_Music/app/src/main/java/com/learn_japanese_with_music/core/data/database/AppDatabase.kt) 提升至 **Version 3**。
    - 啟用了 `fallbackToDestructiveMigration()`，這會確保在結構變動時 App 能穩定運行而不閃退（注意：此操作會重置現有單字收藏）。
    - 移除了 `SavedWord` 實體中的 `songCover` 欄位，減少了資料庫的存儲開銷。

### 3. 資料結構同步更新
- 更新了 `LyricSegment`, `SavedWord`, 與 `WordCache` 以包含全新的 `dictionaryForm` 欄位。

## 驗證結果
- [x] Gradle 編譯成功。
- [x] 點擊歌詞單字可看到「原形」顯示。
- [x] Word Card 頁面已不再顯示歌曲封面背景，並成功適配新的純色卡片風格。
- [x] 資料庫成功遷移，無閃退現象。

> [!TIP]
> **學習建議**
> 辭書型是日文學習的核心。現在您可以直接在單字卡看到動詞的原貌，這對於查詢文法書或理解變語法非常有幫助！
