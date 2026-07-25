package com.learn_japanese_with_music.core.utils

import com.learn_japanese_with_music.core.data.database.SavedWord
import com.learn_japanese_with_music.features.vocabulary.ui.SortOrder
import com.learn_japanese_with_music.features.vocabulary.ui.WordSortMode
import java.text.Collator
import java.util.Locale

object JapaneseSortUtils {
    private val japaneseCollator = Collator.getInstance(Locale.JAPANESE)

    /**
     * 對已收藏的單字列表進行排序
     */
    fun sortSavedWords(
        words: List<SavedWord>,
        mode: WordSortMode,
        order: SortOrder
    ): List<SavedWord> {
        val sortedList = when (mode) {
            WordSortMode.Alphabetical -> {
                // 優先使用讀音 (reading) 進行五十音排序，若無則使用單字本身
                words.sortedWith { a, b ->
                    val sourceA = a.reading.ifEmpty { a.word }
                    val sourceB = b.reading.ifEmpty { b.word }
                    japaneseCollator.compare(sourceA, sourceB)
                }
            }
            WordSortMode.Time -> {
                // 根據時間戳記排序
                words.sortedBy { it.timestamp }
            }
        }

        if(mode.equals(WordSortMode.Alphabetical)){
            return if (order == SortOrder.Ascending) sortedList.reversed() else sortedList
        }else{
            return if (order == SortOrder.Descending) sortedList.reversed() else sortedList
        }
    }
}
