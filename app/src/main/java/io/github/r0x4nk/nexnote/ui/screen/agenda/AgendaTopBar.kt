package io.github.r0x4nk.nexnote.ui.screen.agenda

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import io.github.r0x4nk.nexnote.R
import io.github.r0x4nk.nexnote.ui.component.NexIconButton
import io.github.r0x4nk.nexnote.ui.component.nexTopAppBarColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AgendaTopBar(
    title: String,
    onToday: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        actions = { GoToTodayButton(onToday) },
        colors = nexTopAppBarColors()
    )
}

@Composable
private fun GoToTodayButton(onClick: () -> Unit) {
    NexIconButton(
        imageVector = Icons.Outlined.Today,
        contentDescription = stringResource(R.string.agenda_today),
        onClick = onClick
    )
}
