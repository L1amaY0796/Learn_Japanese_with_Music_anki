package com.learn_japanese_with_music.features.vocabulary.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.learn_japanese_with_music.core.data.SettingsManager
import com.learn_japanese_with_music.core.data.database.AppDatabase
import com.learn_japanese_with_music.core.data.database.SavedWord
import com.learn_japanese_with_music.core.utils.FSRSEngine
import com.learn_japanese_with_music.features.lyrics.model.LyricSegment
import com.learn_japanese_with_music.core.theme.appTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewSessionScreen(
    settingsManager: SettingsManager,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()
    
    var dueWords by remember { mutableStateOf<List<SavedWord>>(emptyList()) }
    var currentIndex by remember { mutableStateOf(0) }
    var isRevealed by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    
    val currentWord = dueWords.getOrNull(currentIndex)
    
    LaunchedEffect(Unit) {
        dueWords = withContext(Dispatchers.IO) {
            database.savedWordDao().getDueWords(System.currentTimeMillis())
        }
        isLoading = false
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    if (!isLoading) {
                        Text("${currentIndex + 1} / ${dueWords.size}", style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Monospace)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (dueWords.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No words due for review!", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onClose) {
                        Text("Go Back")
                    }
                }
            } else if (currentWord != null) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Progress Indicator
                    LinearProgressIndicator(
                        progress = (currentIndex + 1).toFloat() / dueWords.size,
                        modifier = Modifier.fillMaxWidth().height(5.dp),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                    )

                    // Flashcard
                    Flashcard(
                        word = currentWord,
                        isRevealed = isRevealed,
                        settingsManager = settingsManager,
                        onReveal = { isRevealed = true },
                        modifier = Modifier.weight(1f)
                    )

                    // Buttons (only when revealed)
                    if (isRevealed) {
                        ReviewActionButtons(
                            onAction = { grade ->
                                scope.launch(Dispatchers.IO) {
                                    processReview(database, currentWord, grade)
                                    withContext(Dispatchers.Main) {
                                        if (currentIndex < dueWords.size - 1) {
                                            currentIndex++
                                            isRevealed = false
                                        } else {
                                            onClose()
                                        }
                                    }
                                }
                            }
                        )
                    } else {
                        Button(
                            onClick = { isRevealed = true },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ){
                                Icon(Icons.Default.Cached, contentDescription = null)
                                Text("Reveal Answer", fontFamily = FontFamily.Monospace,fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Flashcard(
    word: SavedWord,
    isRevealed: Boolean,
    settingsManager: SettingsManager,
    onReveal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isRevealed) 180f else 0f,
        animationSpec = tween(durationMillis = 500)
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clickable(enabled = !isRevealed) { onReveal() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (rotation <= 90f) {
                // Front Side
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = word.word,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 64.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Back Side
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationY = 180f }
                ) {
                    val posList: List<String> = try {
                        com.google.gson.Gson().fromJson(word.partOfSpeech, object : com.google.gson.reflect.TypeToken<List<String>>() {}.type)
                    } catch (e: Exception) { emptyList() }

                    VocabularyCardContent(
                        segment = LyricSegment(word.word, word.reading, word.dictionaryForm, posList),
                        contextLine = word.contextLine,
                        settingsManager = settingsManager,
                        splitMode = word.splitMode,
                        songTitle = word.songTitle,
                        songArtist = word.songArtist,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun ReviewActionButtons(
    onAction: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ReviewButton(
            text = "Again",
            color = Color(0xFFEF5350),
            onClick = { onAction(1) },
            modifier = Modifier.weight(1f)
        )
        ReviewButton(
            text = "Hard",
            color = Color(0xFFFF9800),
            onClick = { onAction(2) },
            modifier = Modifier.weight(1f)
        )
        ReviewButton(
            text = "Good",
            color = Color(0xFF66BB6A),
            onClick = { onAction(3) },
            modifier = Modifier.weight(1f)
        )
        ReviewButton(
            text = "Easy",
            color = Color(0xFF42A5F5),
            onClick = { onAction(4) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun ReviewButton(
    text: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(text, color = Color.White,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold)
    }
}

private suspend fun processReview(database: AppDatabase, word: SavedWord, grade: Int) {
    val now = System.currentTimeMillis()
    
    var nextDifficulty = word.difficulty
    var nextStability = word.stability
    var nextState = word.state
    var nextInterval = 0

    if (word.reps == 0) {
        // Initial review (New -> Learning)
        nextDifficulty = FSRSEngine.initDifficulty(grade)
        nextStability = FSRSEngine.initStability(grade)
        nextState = if (grade == 4) 2 else 1 // Easy graduates immediately
    } else {
        // Long term review
        val daysElapsed = (now - word.lastReviewed) / (1000.0 * 60 * 60 * 24)
        val currentR = FSRSEngine.calculateRetrievability(daysElapsed, word.stability)
        
        nextDifficulty = FSRSEngine.nextDifficulty(word.difficulty, grade)
        
        if (grade == 1) {
            nextStability = FSRSEngine.nextStabilityFailure(word.difficulty, word.stability, currentR)
            nextState = 3 // Relearning
        } else {
            nextStability = FSRSEngine.nextStabilitySuccess(word.difficulty, word.stability, currentR, grade)
            nextState = 2 // Review
        }
    }
    
    nextInterval = FSRSEngine.calculateInterval(nextStability)
    val nextLapses = if (grade == 1) word.lapses + 1 else word.lapses
    
    // Leech Detection (Threshold = 6)
    if (nextLapses >= 6 && grade == 1) {
        nextState = 4 // Suspended
    }

    val nextDue = if (nextState == 1 || nextState == 3) {
        // Learning/Relearning steps
        if (grade == 1) {
            // Again: 30s for new words, 10m for mature words
            if (word.state <= 1) now + 30 * 1000 else now + 10 * 60 * 1000
        } else {
            // Good: 15m
            now + 15 * 60 * 1000
        }
    } else if (nextState == 4) {
        // Suspended: Set due to infinity or far future
        Long.MAX_VALUE
    } else {
        now + nextInterval.toLong() * 24 * 60 * 60 * 1000
    }
    
    database.savedWordDao().updateFSRSStatus(
        word = word.word,
        contextLine = word.contextLine,
        splitMode = word.splitMode,
        difficulty = nextDifficulty,
        stability = nextStability,
        reps = word.reps + 1,
        lapses = nextLapses,
        lastReviewed = now,
        state = nextState,
        due = nextDue
    )
}
