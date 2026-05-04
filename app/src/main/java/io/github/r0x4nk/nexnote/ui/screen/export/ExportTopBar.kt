package io.github.r0x4nk.nexnote.ui.screen.export

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import io.github.r0x4nk.nexnote.ui.component.NexIconButton
import io.github.r0x4nk.nexnote.ui.component.nexTopAppBarColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExportTopBar(onBack: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = "Export",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        navigationIcon = {
            NexIconButton(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                onClick = onBack
            )
        },
        colors = nexTopAppBarColors()
    )
}
