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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import com.learn_japanese_with_music.core.data.database.SavedWord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Exports [words] as an Anki deck (.apkg), either straight into an installed Anki app
 * or to a file the user picks.
 */
@Composable
fun AnkiExportDialog(
    words: List<SavedWord>,
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
        title = { Text("Export to Anki") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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

                    if (isWorking) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
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
