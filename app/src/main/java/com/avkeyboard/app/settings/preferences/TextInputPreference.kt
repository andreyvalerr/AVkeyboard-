// SPDX-License-Identifier: GPL-3.0-only
package com.avkeyboard.app.settings.preferences

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.avkeyboard.app.keyboard.KeyboardSwitcher
import com.avkeyboard.app.latin.utils.prefs
import com.avkeyboard.app.settings.Setting
import com.avkeyboard.app.settings.dialogs.TextInputDialog
import androidx.core.content.edit

@Composable
fun TextInputPreference(setting: Setting, default: String, info: String? = null, checkTextValid: (String) -> Boolean = { true }) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    val prefs = LocalContext.current.prefs()
    Preference(
        name = setting.title,
        onClick = { showDialog = true },
        description = prefs.getString(setting.key, default)?.takeIf { it.isNotEmpty() }
    )
    if (showDialog) {
        TextInputDialog(
            onDismissRequest = { showDialog = false },
            onConfirmed = {
                prefs.edit { putString(setting.key, it) }
                KeyboardSwitcher.getInstance().setThemeNeedsReload()
            },
            initialText = prefs.getString(setting.key, default) ?: "",
            title = { Text(setting.title) },
            description = if (info == null) null else { { Text(info) } },
            checkTextValid = checkTextValid
        )
    }
}
