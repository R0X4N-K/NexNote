package io.github.r0x4nk.nexnote.ui.screen.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.R

@Composable
internal fun DeviceColorsPreference(enabled: Boolean, onChange: (Boolean) -> Unit) {
    val supported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    Row(
        modifier = Modifier.fillMaxWidth()
            .toggleable(value = enabled && supported, enabled = supported, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.settings_device_colors), style = MaterialTheme.typography.bodyLarge)
            Text(
                if (supported) {
                    stringResource(R.string.settings_device_colors_summary)
                } else {
                    stringResource(R.string.settings_device_colors_unavailable)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = enabled && supported, onCheckedChange = null, enabled = supported)
    }
}
