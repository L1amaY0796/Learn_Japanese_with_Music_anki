# Word Card 排序功能實作計畫

本計畫旨在為 Word Card 頁面實作排序功能。使用者可以根據「五十音」或「加入時間」對收藏的單字進行排序，並支援切換正反序。

## 建議的變更內容

### [MODIFY] [WordCardPage.kt](file:///C:/Users/fuala/AndroidStudioProjects/Learn_Japanese_with_Music/app/src/main/java/com/learn_japanese_with_music/features/vocabulary/ui/WordCardPage.kt)

1.  **新增排序狀態**：
    - `enum class WordSortMode`: `Alphabetical` (五十音), `Time` (加入時間)。
    - `enum class SortOrder`: `Ascending`, `Descending`。
    - 狀態變數：`currentSortMode`, `currentSortOrder`。

2.  **實作排序邏輯**：
    - 建立一個通用函式對 `List<SavedWord>` 進行排序。
    - **五十音序**：根據 `word` (或 `reading`) 進行排序。
    - **加入時間**：根據 `timestamp` 進行排序。
    - 根據 `currentSortOrder` 決定 `reverse` 與否。

3.  **UI 組件新增**：
    - 在類別選擇器（Category Selector）右側新增一個 Row。
    - 包含兩個可點擊的標籤（或是帶有圖示的按鈕）：「五十音」與「時間」。
    - **互動邏輯**：
        - 點擊不同標籤：切換模式並預設為正序。
        - 點擊相同標籤：切換正反序（Asc <-> Desc）。
        - 顯示狀態：使用顏色或圖示（如上下箭頭）標示目前的排序狀態。

4.  **顯示時機控制**：
    - 該排序元件僅在顯示「單字清單」時出現（即 `categoryMode == All` 或 `selectedCategoryValue != null`）。

## 驗證計畫

### 手動驗證
1.  **切換模式**：點擊「五十音」，確認列表依日文發音/字形排序。點擊「時間」，確認依收藏時間排序。
2.  **切換正反序**：連續點擊「時間」，確認列表在「最新在前」與「最早在前」之間切換。
3.  **搜尋整合**：確認在有搜尋關鍵字的情況下，排序依然有效。
4.  **UI 連動**：確認在「類別選擇頁面」排序按鈕會自動隱藏，進入子頁面後才出現。
