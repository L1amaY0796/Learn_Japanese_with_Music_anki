package com.learn_japanese_with_music.features.review_monitor.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.learn_japanese_with_music.core.components.MenuButton
import com.learn_japanese_with_music.core.components.SearchBar
import com.learn_japanese_with_music.core.data.database.AppDatabase
import com.learn_japanese_with_music.core.data.database.SavedWord
import com.learn_japanese_with_music.core.utils.ReviewCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ReviewMonitorPage(
    onMenuClick: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()
    
    var words by remember { mutableStateOf<List<SavedWord>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var sortDescending by remember { mutableStateOf(false) } // Default Ascending (soonest due first)
    
    // Sorting modes: 0: Due Date, 1: Familiarity, 2: Stability
    var sortMode by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        words = withContext(Dispatchers.IO) {
            database.savedWordDao().getAllSavedWords()
        }
    }

    val filteredWords = words.filter {
        it.word.contains(searchQuery, ignoreCase = true)
    }.sortedWith { a, b ->
        when (sortMode) {
            1 -> { // Familiarity (R)
                val rA = ReviewCalculator.calculateR(a)
                val rB = ReviewCalculator.calculateR(b)
                if (sortDescending) rB.compareTo(rA) else rA.compareTo(rB)
            }
            2 -> { // Stability (S)
                if (sortDescending) b.stability.compareTo(a.stability) else a.stability.compareTo(b.stability)
            }
            else -> { // Due Date
                if (sortDescending) b.due.compareTo(a.due) else a.due.compareTo(b.due)
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp, start = 16.dp, end = 16.dp)
        ) {
            // Header: Menu + Search
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MenuButton(onClick = onMenuClick)
                Spacer(modifier = Modifier.width(12.dp))
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    performSearch = { /* Reactive search */ }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Controls: Sort
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Words: ${filteredWords.size}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { 
                        sortMode = (sortMode + 1) % 3 
                    }) {
                        Text(
                            text = when(sortMode) {
                                1 -> "By Familiarity"
                                2 -> "By Stability"
                                else -> "By Due Date"
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    IconButton(onClick = { sortDescending = !sortDescending }) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort",
                            modifier = Modifier.graphicsLayer {
                                rotationX = if (sortDescending) 180f else 0f
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(filteredWords) { word ->
                    MonitorItem(
                        word = word,
                        onUnsuspend = {
                            scope.launch(Dispatchers.IO) {
                                database.savedWordDao().updateFSRSStatus(
                                    word.word, word.contextLine, word.splitMode,
                                    word.difficulty, word.stability, word.reps,
                                    0, // Reset lapses
                                    System.currentTimeMillis(),
                                    1, // Back to Learning
                                    System.currentTimeMillis() // Due now
                                )
                                // Refresh list
                                val updatedWords = database.savedWordDao().getAllSavedWords()
                                withContext(Dispatchers.Main) {
                                    words = updatedWords
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MonitorItem(
    word: SavedWord,
    onUnsuspend: () -> Unit
) {
    val r = ReviewCalculator.calculateR(word)
    val rPercent = (r * 100).toInt()
    val dueText = ReviewCalculator.formatDueTime(word.due)
    val stabilityText = ReviewCalculator.formatStability(word.stability)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = word.word,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (word.reading.isNotEmpty()) {
                        Text(
                            text = word.reading,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "S: $stabilityText",
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (word.state == 4 || word.lapses >= 6) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (word.state == 4) "SUSPENDED" else "LEECH",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                        if (word.state == 4) {
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(
                                onClick = onUnsuspend,
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Text(
                                    "Unsuspend",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                    }
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$rPercent%",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = if (rPercent < 80) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                Text(
                    text = dueText,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (word.due <= System.currentTimeMillis()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
