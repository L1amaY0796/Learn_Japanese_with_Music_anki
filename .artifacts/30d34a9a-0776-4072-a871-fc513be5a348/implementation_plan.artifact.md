# Word Card 搜尋功能改善計畫 (更新版：按下搜尋鈕觸發)

本計畫旨在優化 `WordCardPage` 的搜尋機制，使其根據當前介面狀態自動切換搜尋目標，且僅在使用者明確按下「搜尋」按鈕後才套用過濾條件。

## 建議的變更內容

### [MODIFY] [WordCardPage.kt](file:///C:/Users/fuala/AndroidStudioProjects/Learn_Japanese_with_Music/app/src/main/java/com/learn_japanese_with_music/features/vocabulary/ui/WordCardPage.kt)

1.  **狀態管理優化**：
    - 區分 `searchQuery`（使用者正在輸入的文字）與 `appliedSearchQuery`（實際被套用至過濾邏輯的文字）。
    - 移除 `isSearching` 狀態，改以 `appliedSearchQuery` 是否為空來判斷。

2.  **搜尋函式 `performSearch` 實作**：
    - 呼叫時將 `appliedSearchQuery` 設為當前的 `searchQuery`。
    - 呼叫 `focusManager.clearFocus()` 收起鍵盤。

3.  **情境感知過濾邏輯**：
    - **單字列表 (Leaf View / "全部" 模式)**：
        - 根據 `appliedSearchQuery` 過濾 `savedWords`（比對日文、一般意思、上下文）。
    - **類別列表 (Category View)**：
        - 根據 `appliedSearchQuery` 過濾「歌曲標題」或「詞性名稱」。

4.  **UI 互動優化**：
    - 更新 `SearchBar` 的 `onQueryChange`，僅更新輸入狀態。
    - 在搜尋框被手動清空時，同步重設 `appliedSearchQuery` 為空，以即時回復完整清單。

## 驗證計畫

### 手動驗證
1.  **延遲套用驗證**：在搜尋框輸入文字，確認下方的列表**不會**立即變動。
2.  **觸發驗證**：按下鍵盤搜尋鍵或螢幕按鈕，確認列表根據輸入內容進行過濾。
3.  **類別搜尋驗證**：在「歌曲」模式下搜尋，確認顯示符合名稱的歌曲卡片。
4.  **單字搜尋驗證**：在單一歌曲內搜尋，確認顯示該歌曲中符合條件的單字。
5.  **重設驗證**：刪除搜尋框文字後，確認顯示狀態回復到原始完整清單。
