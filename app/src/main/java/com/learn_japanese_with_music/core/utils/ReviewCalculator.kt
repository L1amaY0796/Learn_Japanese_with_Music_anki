package com.learn_japanese_with_music.core.utils

import com.learn_japanese_with_music.core.data.database.SavedWord
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

object ReviewCalculator {
    fun calculateR(word: SavedWord): Double {
        if (word.stability == 0.0) return 0.0
        val now = System.currentTimeMillis()
        val daysElapsed = (now - word.lastReviewed) / (1000.0 * 60 * 60 * 24)
        return FSRSEngine.calculateRetrievability(daysElapsed, word.stability)
    }

    fun formatDueTime(due: Long): String {
        val now = System.currentTimeMillis()
        val diff = due - now
        if (diff <= 0) return "Already Due"

        val mins = TimeUnit.MILLISECONDS.toMinutes(diff)
        if (mins < 60) return "$mins mins"

        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        if (hours < 24) return "$hours hours"

        val days = TimeUnit.MILLISECONDS.toDays(diff)
        return "$days days"
    }

    fun formatStability(stability: Double): String {
        return if (stability < 1.0) {
            val hours = (stability * 24).toInt()
            if (hours < 1) "${(stability * 24 * 60).toInt()}m" else "${hours}h"
        } else if (stability < 30.0) {
            "${stability.roundToInt()}d"
        } else if (stability < 365.0) {
            "${(stability / 30.0).roundToInt()}mo"
        } else {
            "${(stability / 365.0).roundToInt()}y"
        }
    }
}