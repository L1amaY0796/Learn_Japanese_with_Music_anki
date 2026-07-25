package com.learn_japanese_with_music.core.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ReturnButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledIconButton (
        onClick = onClick,
        modifier = modifier.size(59.dp),
        shape = CircleShape,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
    ) {
        Icon(
            imageVector = Icons.Default.ArrowBack,
            contentDescription = "Return",
            modifier = Modifier.size(35.dp)
        )
    }
}