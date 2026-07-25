# 修正五十音排序功能計畫

本計畫旨在實作符合日本五十音順序（Go-juon order）的單字排序功能，並將排序邏輯抽離至獨立的工具檔案中。

## 建議的變更內容

### 1. 建立排序工具類 (`core/utils`)

#### [NEW] [JapaneseSortUtils.kt](file:///C:/Users/fuala/AndroidStudioProjects/Learn_Japanese_with_Music/app/src/main/java/com/learn_japanese_with_music/core/utils/JapaneseSortUtils.kt)
- 實作 `JapaneseSortUtils` 物件。
- 使用 `java.text.Collator` 並設定 `Locale.JAPANESE` 來處理標準的五十音排序。
- `Collator` 會正確處理：
    - 平假名與片假名的混合排序。
    - 清音、濁音、半清音的順序。
    - 長音符號（ー）的處理。
- 提供 `sortSavedWords` 函式，接受 `List<SavedWord>`, `WordSortMode`, 與 `SortOrder`。

### 2. 重構 Word Card 頁面 (`features/vocabulary/ui`)

#### [MODIFY] [WordCardPage.kt](file:///C:/Users/fuala/AndroidStudioProjects/Learn_Japanese_with_Music/app/src/main/java/com/learn_japanese_with_music/features/vocabulary/ui/WordCardPage.kt)
- 移除內部的 `sortWords` 函式。
- 在重組邏輯中呼叫 `JapaneseSortUtils.sortSavedWords`。
- 確保排序依據優先使用 `reading`，若無則使用 `word`。

## 驗證計畫

### 手動驗證
1. **五十音驗證**：進入 Word Card 頁面並點擊五十音排序標籤。
   - 確認「あ」開頭的字排在「か」前面。
   - 確認平假名與片假名的相同讀音單字能正確排在一起。
2. **正反序切換**：多次點擊標籤，確認 `Ascending` (あ -> ん) 與 `Descending` (ん -> あ) 切換正確。
3. **時間排序驗證**：切換至時間排序，確認仍能依據收藏時間正確排列。
