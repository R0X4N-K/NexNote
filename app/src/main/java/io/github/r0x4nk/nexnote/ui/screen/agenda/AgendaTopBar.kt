package io.github.r0x4nk.nexnote.ui.screen.agenda

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.ui.component.nexTopAppBarColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AgendaTopBar(actions: AgendaActions) {
    TopAppBar(
        title = {
            Text(
                text = "Calendar",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        actions = { GoToTodayButton(actions.onGoToToday) },
        colors = nexTopAppBarColors()
    )
}

@Composable
private fun GoToTodayButton(onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Outlined.Today,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text("Today")
    }
}
