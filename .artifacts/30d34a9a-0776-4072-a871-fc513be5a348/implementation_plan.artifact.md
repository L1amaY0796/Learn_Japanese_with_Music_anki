# Word Card 功能擴充與 UI 優化計畫 (排除排序功能)

本計畫旨在移除冗餘的封面圖參數，並在單字卡中新增「辭書型」顯示。

## 建議的變更內容

### 1. 資料模型與資料庫更新

#### [MODIFY] [LyricsModels.kt](file:///C:/Users/fuala/AndroidStudioProjects/Learn_Japanese_with_Music/app/src/main/java/com/learn_japanese_with_music/features/lyrics/model/LyricsModels.kt)
- `LyricSegment`: 新增 `dictionaryForm: String` 欄位。

#### [MODIFY] [SavedWord.kt](file:///C:/Users/fuala/AndroidStudioProjects/Learn_Japanese_with_Music/app/src/main/java/com/learn_japanese_with_music/core/data/database/SavedWord.kt)
- 新增 `dictionaryForm` 欄位。
- 移除 `songCover` 欄位。

#### [MODIFY] [WordCache.kt](file:///C:/Users/fuala/AndroidStudioProjects/Learn_Japanese_with_Music/app/src/main/java/com/learn_japanese_with_music/core/data/database/WordCache.kt)
- 新增 `dictionaryForm` 欄位。

#### [MODIFY] [AppDatabase.kt](file:///C:/Users/fuala/AndroidStudioProjects/Learn_Japanese_with_Music/app/src/main/java/com/learn_japanese_with_music/core/data/database/AppDatabase.kt)
- 提升資料庫版本（Version 3），並確保使用毀滅性遷移或處理 Schema 變更。

### 2. 邏輯層更新

#### [MODIFY] [JapaneseProcessor.kt](file:///C:/Users/fuala/AndroidStudioProjects/Learn_Japanese_with_Music/app/src/main/java/com/learn_japanese_with_music/features/lyrics/processor/JapaneseProcessor.kt)
- 更新 `processLine`: 從 Sudachi `Morpheme` 中提取 `dictionaryForm()` 並存入 `LyricSegment`。

### 3. UI 介面優化

#### [MODIFY] [VocabularyCard.kt](file:///C:/Users/fuala/AndroidStudioProjects/Learn_Japanese_with_Music/app/src/main/java/com/learn_japanese_with_music/features/vocabulary/ui/VocabularyCard.kt)
- **新增顯示**：在單字標題下方新增「辭書型 (Dictionary Form)」區塊。
- **移除封面**：移除 `songCover` 相關參數與儲存邏輯。

#### [MODIFY] [LyricPage.kt](file:///C:/Users/fuala/AndroidStudioProjects/Learn_Japanese_with_Music/app/src/main/java/com/learn_japanese_with_music/features/lyrics/ui/LyricPage.kt)
- 移除傳遞給 `VocabularyCardContent` 的 `songCover` 參數。

#### [MODIFY] [WordCardPage.kt](file:///C:/Users/fuala/AndroidStudioProjects/Learn_Japanese_with_Music/app/src/main/java/com/learn_japanese_with_music/features/vocabulary/ui/WordCardPage.kt)
- **移除封面顯示**：
    - 單字卡片背景改為純色或簡單樣式。
    - 類別卡片（歌曲）不再顯示封面背景。

## 驗證計畫

### 手動驗證
1. **單字卡檢查**：點擊歌詞單字，確認彈窗中出現「辭書型」資訊。
2. **介面檢查**：確認 Word Card 頁面已完全移除歌曲封面圖的顯示，版面整潔一致。
3. **持久化驗證**：收藏新單字後，確認「辭書型」能正確儲存並在重新開啟時顯示。
