package com.learn_japanese_with_music.core.data.database

import androidx.room.Entity

@Entity(primaryKeys = ["word", "contextLine", "splitMode"])
data class SavedWord(
    val word: String,
    val contextLine: String,
    val splitMode: String,
    val reading: String,
    val dictionaryForm: String,
    val partOfSpeech: String, // Store as JSON string or concatenated
    val jlptLevel: String,
    val generalMeaning: String,
    val contextualMeaning: String,
    val grammarExplanation: String = "",
    val commonUsages: String, // Store as JSON string (List of UsageExample)
    val notes: String = "",
    val songTitle: String,
    val songArtist: String,
    val timestamp: Long = System.currentTimeMillis(),
    
    // FSRS Fields
    val difficulty: Double = 0.0,
    val stability: Double = 0.0,
    val reps: Int = 0,
    val lapses: Int = 0,
    val lastReviewed: Long = 0,
    val state: Int = 0, // 0: New, 1: Learning, 2: Review, 3: Relearning
    val due: Long = 0 // Unix timestamp in milliseconds
)
