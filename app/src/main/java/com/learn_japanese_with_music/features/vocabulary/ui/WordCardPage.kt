package com.learn_japanese_with_music.features.vocabulary.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.learn_japanese_with_music.core.components.MenuButton
import com.learn_japanese_with_music.core.components.SearchBar
import com.learn_japanese_with_music.core.data.SettingsManager
import com.learn_japanese_with_music.core.data.database.AppDatabase
import com.learn_japanese_with_music.core.data.database.SavedWord
import com.learn_japanese_with_music.features.lyrics.model.LyricSegment
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.learn_japanese_with_music.core.utils.JapaneseSortUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class WordCategoryMode { All, POS, Song }
enum class WordSortMode { Alphabetical, Time }
enum class SortOrder { Ascending, Descending }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WordCardPage(
    settingsManager: SettingsManager,
    onMenuClick: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var appliedSearchQuery by remember { mutableStateOf("") }
    var categoryMode by remember { mutableStateOf(WordCategoryMode.All) }
    var savedWords by remember { mutableStateOf<List<SavedWord>>(emptyList()) }
    
    // For nested navigation (e.g., viewing words of a specific song)
    var selectedCategoryValue by remember { mutableStateOf<String?>(null) }
    var isMenuExpanded by remember { mutableStateOf(false) }
    var showAnkiExport by remember { mutableStateOf(false) }
    
    // Word detail dialog state
    var selectedWordForDetail by remember { mutableStateOf<SavedWord?>(null) }
    val sheetState = rememberModalBottomSheetState()
    
    // 排序狀態
    var currentSortMode by remember { mutableStateOf(WordSortMode.Time) }
    var currentSortOrder by remember { mutableStateOf(SortOrder.Descending) }
    
    // FSRS Review state
    var isReviewing by remember { mutableStateOf(false) }
    var isCramming by remember { mutableStateOf(false) }
    var dueCount by remember { mutableIntStateOf(0) }
    
    val focusManager = LocalFocusManager.current
    val isImeVisible = WindowInsets.isImeVisible

    // 搜尋函式
    fun performSearch() {
        focusManager.clearFocus()
        appliedSearchQuery = searchQuery
    }

    // Fetch words on launch or when database changes
    LaunchedEffect(Unit) {
        savedWords = withContext(Dispatchers.IO) {
            database.savedWordDao().getAllSavedWords()
        }
    }

    // 當鍵盤收合時，主動清除焦點以隱藏游標
    LaunchedEffect(isImeVisible) {
        if (!isImeVisible) {
            focusManager.clearFocus()
        }
    }

    LaunchedEffect(savedWords) {
        dueCount = withContext(Dispatchers.IO) {
            database.savedWordDao().getDueWords(System.currentTimeMillis()).size
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 48.dp, start = 16.dp, end = 16.dp)
            ) {
                // Top Bar: Home + Search
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MenuButton(
                        onClick = {
                            onMenuClick()
                            focusManager.clearFocus()
                        }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = { 
                            searchQuery = it
                            if (it.isBlank()) appliedSearchQuery = ""
                        },
                        performSearch = { performSearch() }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Category Selector and Sort Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Category Selector
                    Box(modifier = Modifier.weight(1f)) {
                        Button(
                            onClick = { isMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                        ) {
                            Row(
                                modifier = Modifier.offset(x = (-5).dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = if (isMenuExpanded) Icons.Default.ArrowDropDown else Icons.Default.ArrowRight,
                                    contentDescription = null,
                                )
                                Text(
                                    text = "Category : ${
                                        when (categoryMode) {
                                            WordCategoryMode.All -> "All"
                                            WordCategoryMode.POS -> "POS."
                                            WordCategoryMode.Song -> "Song"
                                        }
                                    }",
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = isMenuExpanded,
                            onDismissRequest = { isMenuExpanded = false },
                            modifier =
                                if(categoryMode == WordCategoryMode.All || selectedCategoryValue != null)
                                    Modifier.fillMaxWidth(0.57f)
                                else
                                    Modifier.fillMaxWidth(0.8f)
                        ) {
                            WordCategoryMode.values().forEach { mode ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            when (mode) {
                                                WordCategoryMode.All -> "All"
                                                WordCategoryMode.POS -> "POS."
                                                WordCategoryMode.Song -> "Song"
                                            }
                                        )
                                    },
                                    onClick = {
                                        isMenuExpanded = false
                                        categoryMode = mode
                                        selectedCategoryValue = null
                                    }
                                )
                            }
                        }
                    }

                    // Sort Controls (Only visible when list is shown)
                    if (categoryMode == WordCategoryMode.All || selectedCategoryValue != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SortTag(
                                icon = Icons.Default.SortByAlpha,
                                isSelected = currentSortMode == WordSortMode.Alphabetical,
                                order = if (currentSortMode == WordSortMode.Alphabetical) currentSortOrder else null,
                                onClick = {
                                    if (currentSortMode == WordSortMode.Alphabetical) {
                                        currentSortOrder = if (currentSortOrder == SortOrder.Descending) SortOrder.Ascending else SortOrder.Descending
                                    } else {
                                        currentSortMode = WordSortMode.Alphabetical
                                        currentSortOrder = SortOrder.Descending
                                    }
                                }
                            )
                            SortTag(
                                icon = Icons.Default.AccessTime,
                                isSelected = currentSortMode == WordSortMode.Time,
                                order = if (currentSortMode == WordSortMode.Time) currentSortOrder else null,
                                onClick = {
                                    if (currentSortMode == WordSortMode.Time) {
                                        currentSortOrder = if (currentSortOrder == SortOrder.Ascending) SortOrder.Descending else SortOrder.Ascending
                                    } else {
                                        currentSortMode = WordSortMode.Time
                                        currentSortOrder = SortOrder.Descending // 預設最新優先
                                    }
                                }
                            )
                        }
                    }

                    // Export all saved words as an Anki deck
                    SortTag(
                        icon = Icons.Default.IosShare,
                        isSelected = false,
                        order = null,
                        contentDescription = "Export to Anki",
                        onClick = { showAnkiExport = true }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Content Area
                Box(modifier = Modifier.fillMaxWidth(1f)) {
                    if (selectedCategoryValue != null) { //at word card level(except All mode)

                        BackHandler() {
                            focusManager.clearFocus()
                            selectedCategoryValue = null
                        }

                        // Nested View: List of words in a specific category
                        val nestedWords = savedWords.filter {
                            val inCategory = when(categoryMode) {
                                WordCategoryMode.POS -> it.partOfSpeech.contains(selectedCategoryValue!!)
                                WordCategoryMode.Song -> it.songTitle == selectedCategoryValue
                                else -> true
                            }
                            // 同時套用搜尋過濾
                            inCategory && (
                                it.word.contains(appliedSearchQuery, ignoreCase = true) ||
                                it.generalMeaning.contains(appliedSearchQuery, ignoreCase = true) ||
                                it.contextualMeaning.contains(appliedSearchQuery, ignoreCase = true)
                            )
                        }
                        
                        Column {
                            if (appliedSearchQuery.isNotBlank()) {
                                Text(
                                    text = "Search in「$selectedCategoryValue」for \"$appliedSearchQuery\"",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                            } else {
                                Text(
                                    text = "Category / $categoryMode / $selectedCategoryValue" ,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                            }
                            val sortedNested = JapaneseSortUtils.sortSavedWords(nestedWords, currentSortMode, currentSortOrder)
                            SavedWordGrid(sortedNested) { selectedWordForDetail = it }
                        }
                        
                        // Back button for nested view
                        androidx.activity.compose.BackHandler {
                            selectedCategoryValue = null
                            appliedSearchQuery = ""
                            searchQuery = ""
                        }

                    } else {
                        // Category Selection or All)
                        when (categoryMode) {
                            WordCategoryMode.All -> {

                                if (!(isReviewing || isCramming))
                                    BackHandler() {
                                        focusManager.clearFocus()
                                        onMenuClick()
                                    }

                                val filteredAll = savedWords.filter {
                                    it.word.contains(appliedSearchQuery, ignoreCase = true) ||
                                    it.generalMeaning.contains(appliedSearchQuery, ignoreCase = true) ||
                                    it.contextualMeaning.contains(appliedSearchQuery, ignoreCase = true)
                                }
                                Column {
                                    if (appliedSearchQuery.isNotBlank()) {
                                        Text(
                                            text = "Search through All : $appliedSearchQuery",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(bottom = 16.dp)
                                        )
                                    }
                                    val sortedAll = JapaneseSortUtils.sortSavedWords(filteredAll, currentSortMode, currentSortOrder)
                                    SavedWordGrid(sortedAll) { selectedWordForDetail = it }
                                }
                            }
                            WordCategoryMode.POS -> {
                                BackHandler() {
                                    focusManager.clearFocus()
                                    categoryMode = WordCategoryMode.All
                                }
                                val posGroups = savedWords.flatMap { 
                                    try { Gson().fromJson<List<String>>(it.partOfSpeech, object : com.google.gson.reflect.TypeToken<List<String>>() {}.type) } 
                                    catch (e: Exception) { emptyList() }
                                }.distinct().filter { it != "*" }
                                
                                // 過濾詞性名稱
                                val filteredPOS = posGroups.filter { it.contains(appliedSearchQuery, ignoreCase = true) }
                                CategoryGrid(filteredPOS) {
                                    selectedCategoryValue = it
                                    searchQuery = ""
                                    appliedSearchQuery = searchQuery
                                }
                            }
                            WordCategoryMode.Song -> {
                                BackHandler() {
                                    focusManager.clearFocus()
                                    categoryMode = WordCategoryMode.All
                                }
                                val songGroups = savedWords.groupBy { it.songTitle }
                                val songList = songGroups.map { it.key }
                                
                                // 過濾歌名
                                val filteredSongs = songList.filter { it.contains(appliedSearchQuery, ignoreCase = true) }
                                CategoryGrid(filteredSongs) {
                                    selectedCategoryValue = it
                                    searchQuery = ""
                                    appliedSearchQuery = searchQuery
                                }
                            }
                        }
                    }
                }
            }

            if (showAnkiExport) {
                AnkiExportDialog(
                    words = savedWords,
                    onDismiss = { showAnkiExport = false }
                )
            }

            // Word Detail BottomSheet
            if (selectedWordForDetail != null) {
                ModalBottomSheet(
                    onDismissRequest = { 
                        selectedWordForDetail = null
                        // Refresh words list in case something was deleted or notes changed
                        scope.launch {
                            savedWords = withContext(Dispatchers.IO) {
                                database.savedWordDao().getAllSavedWords()
                            }
                        }
                    },
                    sheetState = sheetState
                ) {
                    val word = selectedWordForDetail!!
                    val posList: List<String> = try { 
                        Gson().fromJson(word.partOfSpeech, object : com.google.gson.reflect.TypeToken<List<String>>() {}.type) 
                    } catch (e: Exception) { emptyList() }
                    
                    VocabularyCardContent(
                        segment = LyricSegment(word.word, word.reading, word.dictionaryForm, posList),
                        contextLine = word.contextLine,
                        settingsManager = settingsManager,
                        splitMode = word.splitMode,
                        songTitle = word.songTitle,
                        songArtist = word.songArtist
                    )
                }
            }

            // Sticky Bottom Review Button
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (dueCount > 0) {
                    Button(
                        onClick = { isReviewing = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AutoStories, contentDescription = null)
                            Text(
                                "Start Review ($dueCount due)",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                // Cram Mode Button
                Button(
                    onClick = { isCramming = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary.copy(0.22f),
                        contentColor = MaterialTheme.colorScheme.tertiary
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f))
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ElectricBolt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(
                            "Cram Mode (All Words)",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            if (isReviewing) {
                BackHandler() {
                    isReviewing = false
                    // Refresh words
                    scope.launch {
                        savedWords = withContext(Dispatchers.IO) {
                            database.savedWordDao().getAllSavedWords()
                        }
                    }
                }

                ReviewSessionScreen(
                    settingsManager = settingsManager,
                    onClose = { 
                        isReviewing = false
                        // Refresh words
                        scope.launch {
                            savedWords = withContext(Dispatchers.IO) {
                                database.savedWordDao().getAllSavedWords()
                            }
                        }
                    }
                )
            }

            if (isCramming) {
                BackHandler() {
                    isCramming = false
                    // Refresh words
                    scope.launch {
                        savedWords = withContext(Dispatchers.IO) {
                            database.savedWordDao().getAllSavedWords()
                        }
                    }
                }

                ReviewSessionScreen(
                    settingsManager = settingsManager,
                    isCramMode = true,
                    onClose = {
                        isCramming = false
                        // Refresh words
                        scope.launch {
                            savedWords = withContext(Dispatchers.IO) {
                                database.savedWordDao().getAllSavedWords()
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun SortTag(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    order: SortOrder?,
    contentDescription: String? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)) else null,
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
            if (isSelected && order != null) {
                Icon(
                    imageVector = if (order == SortOrder.Ascending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(12.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = (-2).dp, y = 2.dp)
                )
            }
        }
    }
}
