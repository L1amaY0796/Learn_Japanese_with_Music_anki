# Walkthrough - 五十音排序功能優化

本任務成功修正了 Word Card 頁面的五十音排序邏輯，使其符合標準的日本語排序規則（あいうえお順），並將邏輯抽離至獨立工具類以提升代碼品質。

## 變更內容

### 1. 建立標準日文排序工具
- **[JapaneseSortUtils.kt](file:///C:/Users/fuala/AndroidStudioProjects/Learn_Japanese_with_Music/app/src/main/java/com/learn_japanese_with_music/core/utils/JapaneseSortUtils.kt)**：
    - 使用 `java.text.Collator` 與 `Locale.JAPANESE` 實作標準排序。
    - **精準度提升**：現在能正確處理清音、濁音、半清音，並讓平假名與片假名的對應單字排在一起，完全符合傳統五十音表的順序。
    - **優先級處理**：排序時會優先參考單字的「讀音 (reading)」，確保漢字單字也能依其發音正確歸位。

### 2. UI 層級邏輯簡化
- **[WordCardPage.kt](file:///C:/Users/fuala/AndroidStudioProjects/Learn_Japanese_with_Music/app/src/main/java/com/learn_japanese_with_music/features/vocabulary/ui/WordCardPage.kt)**：
    - 移除了冗長的內部排序函式，改為呼叫全域工具。
    - 整合了原本的搜尋過濾與新的排序工具，確保兩者能流暢連動。

## 驗證結果
- [x] 五十音排序（あいうえお順）功能正確，清音與濁音順序符合語法規則。
- [x] 時間排序（最新優先/最早優先）功能依舊穩定。
- [x] 排序與搜尋功能可同時併用，無邏輯衝突。

> [!TIP]
> **排序小知識**
> 本次更新後，系統能更聰明地處理日文字元。例如：即使「林 (はやし)」與「速い (はやい)」包含漢字與假名混合，系統也會根據我們提供的讀音資訊，將它們正確地放在「は行」的位置。
