package com.learn_japanese_with_music.features.vocabulary.ui

import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.learn_japanese_with_music.core.anki.AnkiExporter
import com.learn_japanese_with_music.core.anki.AnkiImporter
import com.learn_japanese_with_music.core.data.database.AppDatabase
import com.learn_japanese_with_music.core.data.database.SavedWord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Moves word cards between this app and Anki:
 * exports [words] as an Anki deck (.apkg), either straight into an installed Anki app or to a
 * file the user picks, and imports the notes of an .apkg file as saved words.
 *
 * @param splitMode stored on imported words (Anki notes carry no tokenizer mode)
 * @param onImported called after words were added, so the caller can reload its list
 */
@Composable
fun AnkiTransferDialog(
    words: List<SavedWord>,
    splitMode: String,
    onImported: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isWorking by remember { mutableStateOf(false) }

    fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    // "Save as file": the system file picker returns where to write the deck.
    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isWorking = true
                val result = withContext(Dispatchers.IO) {
                    runCatching { AnkiExporter.exportToUri(context, words, uri) }
                }
                isWorking = false
                if (result.isSuccess) {
                    toast("Exported ${words.size} cards. Import the file in Anki.")
                    onDismiss()
                } else {
                    toast("Export failed: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }

    // "Import": .apkg has no registered MIME type, so the picker has to allow any file.
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isWorking = true
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        val dao = AppDatabase.getDatabase(context).savedWordDao()
                        AnkiImporter.importFromUri(context, uri, dao, splitMode)
                    }
                }
                isWorking = false

                val summary = result.getOrNull()
                if (summary == null) {
                    toast("Import failed: ${result.exceptionOrNull()?.message}")
                } else {
                    val details = listOfNotNull(
                        "${summary.alreadySaved} already saved".takeIf { summary.alreadySaved > 0 },
                        "${summary.skipped} skipped".takeIf { summary.skipped > 0 }
                    ).joinToString(", ")
                    toast("Imported ${summary.imported} cards" + if (details.isEmpty()) "." else " ($details).")
                    if (summary.imported > 0) onImported()
                    onDismiss()
                }
            }
        }
    }

    fun openInAnki() {
        scope.launch {
            isWorking = true
            val result = withContext(Dispatchers.IO) {
                runCatching { AnkiExporter.exportToFile(context, words) }
            }
            isWorking = false

            val apkg = result.getOrNull()
            if (apkg == null) {
                toast("Export failed: ${result.exceptionOrNull()?.message}")
                return@launch
            }
            try {
                context.startActivity(AnkiExporter.createImportIntent(context, apkg))
                onDismiss()
            } catch (e: ActivityNotFoundException) {
                toast("No Anki app found. Save the file instead and import it in Anki.")
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isWorking) onDismiss() },
        title = { Text("Anki import / export") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (words.isEmpty()) {
                    Text(
                        "There are no saved words to export yet.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(
                        "${words.size} word cards will be exported as the Anki deck " +
                            "\"Uta Learning\". Exporting again later updates the same cards " +
                            "instead of duplicating them.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Cards are added as new cards; review progress in this app is not transferred.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = { openInAnki() },
                        enabled = !isWorking,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open in Anki app")
                    }

                    OutlinedButton(
                        onClick = { saveLauncher.launch(AnkiExporter.FILE_NAME) },
                        enabled = !isWorking,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save as .apkg file")
                    }

                }

                HorizontalDivider(modifier = Modifier.fillMaxWidth())

                Text(
                    "Import the cards of an Anki deck file (.apkg) as word cards. " +
                        "Words that are already saved are kept as they are.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("*/*")) },
                    enabled = !isWorking,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import .apkg file")
                }

                if (isWorking) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isWorking) {
                Text("Close")
            }
        }
    )
}
