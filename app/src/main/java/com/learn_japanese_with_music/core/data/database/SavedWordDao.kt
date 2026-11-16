package com.learn_japanese_with_music.core.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SavedWordDao {
    @Query("SELECT * FROM SavedWord WHERE word = :word AND contextLine = :contextLine AND splitMode = :splitMode")
    suspend fun getSavedWord(word: String, contextLine: String, splitMode: String): SavedWord?

    @Query("SELECT * FROM SavedWord ORDER BY timestamp DESC")
    suspend fun getAllSavedWords(): List<SavedWord>

    @Query("SELECT * FROM SavedWord WHERE due <= :currentTime AND state < 4 ORDER BY due ASC")
    suspend fun getDueWords(currentTime: Long): List<SavedWord>

    @Query("UPDATE SavedWord SET difficulty = :difficulty, stability = :stability, reps = :reps, lapses = :lapses, lastReviewed = :lastReviewed, state = :state, due = :due WHERE word = :word AND contextLine = :contextLine AND splitMode = :splitMode")
    suspend fun updateFSRSStatus(word: String, contextLine: String, splitMode: String, difficulty: Double, stability: Double, reps: Int, lapses: Int, lastReviewed: Long, state: Int, due: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedWord(savedWord: SavedWord)

    /** Bulk insert (one transaction); rows whose key already exists are left untouched. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSavedWordsIfAbsent(savedWords: List<SavedWord>)

    @Query("UPDATE SavedWord SET notes = :notes WHERE word = :word AND contextLine = :contextLine AND splitMode = :splitMode")
    suspend fun updateNotes(word: String, contextLine: String, splitMode: String, notes: String)

    @Delete
    suspend fun deleteSavedWord(savedWord: SavedWord)
    
    @Query("DELETE FROM SavedWord WHERE word = :word AND contextLine = :contextLine AND splitMode = :splitMode")
    suspend fun deleteSavedWordById(word: String, contextLine: String, splitMode: String)
}
