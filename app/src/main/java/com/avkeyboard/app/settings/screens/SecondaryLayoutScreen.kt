// SPDX-License-Identifier: GPL-3.0-only
package com.avkeyboard.app.settings.screens

import android.content.Context
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.avkeyboard.app.latin.R
import com.avkeyboard.app.latin.settings.Settings
import com.avkeyboard.app.latin.utils.LayoutType
import com.avkeyboard.app.latin.utils.LayoutType.Companion.displayNameId
import com.avkeyboard.app.latin.utils.LayoutUtilsCustom
import com.avkeyboard.app.latin.utils.Log
import com.avkeyboard.app.latin.utils.getActivity
import com.avkeyboard.app.latin.utils.getStringResourceOrName
import com.avkeyboard.app.latin.utils.prefs
import com.avkeyboard.app.settings.SearchSettingsScreen
import com.avkeyboard.app.settings.Setting
import com.avkeyboard.app.settings.SettingsActivity
import com.avkeyboard.app.latin.utils.Theme
import com.avkeyboard.app.settings.dialogs.LayoutPickerDialog
import com.avkeyboard.app.settings.initPreview
import com.avkeyboard.app.settings.preferences.Preference
import com.avkeyboard.app.latin.utils.previewDark

@Composable
fun SecondaryLayoutScreen(
    onClickBack: () -> Unit,
) {
    // no main layouts in here
    // could be added later, but need to decide how to do it (showing all main layouts is too much)
    SearchSettingsScreen(
        onClickBack = onClickBack,
        title = stringResource(R.string.settings_screen_secondary_layouts),
        settings = LayoutType.entries.filter { it != LayoutType.MAIN }.map { Settings.PREF_LAYOUT_PREFIX + it.name }
    )
}

fun createLayoutSettings(context: Context) = LayoutType.entries.filter { it != LayoutType.MAIN }.map { layoutType ->
    Setting(context, Settings.PREF_LAYOUT_PREFIX + layoutType, layoutType.displayNameId) { setting ->
        val ctx = LocalContext.current
        val prefs = ctx.prefs()
        val b = (ctx.getActivity() as? SettingsActivity)?.prefChanged?.collectAsState()
        if ((b?.value ?: 0) < 0)
            Log.v("irrelevant", "stupid way to trigger recomposition on preference change")
        var showDialog by rememberSaveable { mutableStateOf(false) }
        val currentLayout = Settings.readDefaultLayoutName(layoutType, prefs)
        val displayName = if (LayoutUtilsCustom.isCustomLayout(currentLayout)) LayoutUtilsCustom.getDisplayName(currentLayout)
            else currentLayout.getStringResourceOrName("layout_", ctx)
        Preference(
            name = setting.title,
            description = displayName,
            onClick = { showDialog = true }
        )
        if (showDialog)
            LayoutPickerDialog(
                onDismissRequest = { showDialog = false },
                setting = setting,
                layoutType = layoutType
            )
    }
}

@Preview
@Composable
private fun Preview() {
    initPreview(LocalContext.current)
    Theme(previewDark) {
        Surface {
            SecondaryLayoutScreen { }
        }
    }
}
