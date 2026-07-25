package com.learn_japanese_with_music.core.components

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.learn_japanese_with_music.core.theme.appTheme

@Composable
fun SearchBar(query : String,
              onQueryChange : (String) -> Unit,
              performSearch : () -> Unit
) {

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text("Search",
                color = MaterialTheme.colorScheme.primaryContainer,
            )
        },
        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.08f),
        shape = CircleShape,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.primaryContainer,
            fontSize = 27.sp
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.primaryContainer,
            unfocusedTextColor = MaterialTheme.colorScheme.primaryContainer,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = { performSearch() }
        ),
        trailingIcon = {
            FilledIconButton (
                onClick = { performSearch() },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.size(55.dp).offset(x = -5.dp),
                shape = CircleShape
            ) {
                Icon(Icons.Default.Search,
                    contentDescription = "Search",
                    modifier = Modifier.size(30.dp)
                )
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
fun SearchBarPreview() {
    appTheme {
        SearchBar(
            query = "Sample Search",
            onQueryChange = {},
            performSearch = {}
        )
    }
}
