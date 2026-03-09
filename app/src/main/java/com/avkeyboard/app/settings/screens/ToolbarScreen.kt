// SPDX-License-Identifier: GPL-3.0-only
package com.avkeyboard.app.settings.screens

import android.content.Context
import android.graphics.drawable.VectorDrawable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.avkeyboard.app.keyboard.KeyboardSwitcher
import com.avkeyboard.app.keyboard.internal.KeyboardIconsSet
import com.avkeyboard.app.latin.R
import com.avkeyboard.app.latin.settings.Defaults
import com.avkeyboard.app.latin.settings.Settings
import com.avkeyboard.app.latin.utils.Log
import com.avkeyboard.app.latin.utils.ToolbarMode
import com.avkeyboard.app.latin.utils.getActivity
import com.avkeyboard.app.latin.utils.getStringResourceOrName
import com.avkeyboard.app.latin.utils.prefs
import com.avkeyboard.app.settings.SearchSettingsScreen
import com.avkeyboard.app.settings.Setting
import com.avkeyboard.app.settings.SettingsActivity
import com.avkeyboard.app.latin.utils.Theme
import com.avkeyboard.app.settings.dialogs.ToolbarKeysCustomizer
import com.avkeyboard.app.settings.initPreview
import com.avkeyboard.app.settings.preferences.ListPreference
import com.avkeyboard.app.settings.preferences.Preference
import com.avkeyboard.app.settings.preferences.ReorderSwitchPreference
import com.avkeyboard.app.settings.preferences.SwitchPreference
import com.avkeyboard.app.latin.utils.previewDark

@Composable
fun ToolbarScreen(
    onClickBack: () -> Unit,
) {
    val prefs = LocalContext.current.prefs()
    val b = (LocalContext.current.getActivity() as? SettingsActivity)?.prefChanged?.collectAsState()
    if ((b?.value ?: 0) < 0)
        Log.v("irrelevant", "stupid way to trigger recomposition on preference change")
    val toolbarMode = Settings.readToolbarMode(prefs)
    val clipboardToolbarVisible = toolbarMode != ToolbarMode.HIDDEN
        || !prefs.getBoolean(Settings.PREF_TOOLBAR_HIDING_GLOBAL, Defaults.PREF_TOOLBAR_HIDING_GLOBAL)
    val items = listOf(
        Settings.PREF_TOOLBAR_MODE,
        if (toolbarMode == ToolbarMode.HIDDEN) Settings.PREF_TOOLBAR_HIDING_GLOBAL else null,
        if (toolbarMode in listOf(ToolbarMode.EXPANDABLE, ToolbarMode.TOOLBAR_KEYS))
            Settings.PREF_TOOLBAR_KEYS else null,
        if (toolbarMode in listOf(ToolbarMode.EXPANDABLE, ToolbarMode.SUGGESTION_STRIP))
            Settings.PREF_PINNED_TOOLBAR_KEYS else null,
        if (clipboardToolbarVisible) Settings.PREF_CLIPBOARD_TOOLBAR_KEYS else null,
        if (clipboardToolbarVisible) Settings.PREF_TOOLBAR_CUSTOM_KEY_CODES else null,
        if (toolbarMode == ToolbarMode.EXPANDABLE) Settings.PREF_QUICK_PIN_TOOLBAR_KEYS else null,
        if (toolbarMode == ToolbarMode.EXPANDABLE) Settings.PREF_AUTO_SHOW_TOOLBAR else null,
        if (toolbarMode == ToolbarMode.EXPANDABLE) Settings.PREF_AUTO_HIDE_TOOLBAR else null,
        if (toolbarMode != ToolbarMode.HIDDEN) Settings.PREF_VARIABLE_TOOLBAR_DIRECTION else null,
    )
    SearchSettingsScreen(
        onClickBack = onClickBack,
        title = stringResource(R.string.settings_screen_toolbar),
        settings = items
    )
}

fun createToolbarSettings(context: Context) = listOf(
    Setting(context, Settings.PREF_TOOLBAR_MODE, R.string.toolbar_mode) { setting ->
        val ctx = LocalContext.current
        val items =
            ToolbarMode.entries.map { it.name.lowercase().getStringResourceOrName("toolbar_mode_", ctx) to it.name }
        ListPreference(
            setting,
            items,
            Defaults.PREF_TOOLBAR_MODE
        ) {
            KeyboardSwitcher.getInstance().setThemeNeedsReload()
        }
    },
    Setting(context, Settings.PREF_TOOLBAR_HIDING_GLOBAL, R.string.toolbar_hiding_global) {
        SwitchPreference(it, Defaults.PREF_TOOLBAR_HIDING_GLOBAL) {
            KeyboardSwitcher.getInstance().setThemeNeedsReload()
        }
    },
    Setting(context, Settings.PREF_TOOLBAR_KEYS, R.string.toolbar_keys) {
        ReorderSwitchPreference(it, Defaults.PREF_TOOLBAR_KEYS)
    },
    Setting(context, Settings.PREF_PINNED_TOOLBAR_KEYS, R.string.pinned_toolbar_keys) {
        ReorderSwitchPreference(it, Defaults.PREF_PINNED_TOOLBAR_KEYS)
    },
    Setting(context, Settings.PREF_CLIPBOARD_TOOLBAR_KEYS, R.string.clipboard_toolbar_keys) {
        ReorderSwitchPreference(it, Defaults.PREF_CLIPBOARD_TOOLBAR_KEYS)
    },
    Setting(context, Settings.PREF_TOOLBAR_CUSTOM_KEY_CODES, R.string.customize_toolbar_key_codes) {
        var showDialog by rememberSaveable { mutableStateOf(false) }
        Preference(
            name = it.title,
            onClick = { showDialog = true },
        )
        if (showDialog)
            ToolbarKeysCustomizer(
                key = it.key,
                onDismissRequest = { showDialog = false }
            )
    },
    Setting(context, Settings.PREF_QUICK_PIN_TOOLBAR_KEYS,
        R.string.quick_pin_toolbar_keys, R.string.quick_pin_toolbar_keys_summary)
    {
        SwitchPreference(it, Defaults.PREF_QUICK_PIN_TOOLBAR_KEYS) { KeyboardSwitcher.getInstance().setThemeNeedsReload() }
    },
    Setting(context, Settings.PREF_AUTO_SHOW_TOOLBAR, R.string.auto_show_toolbar, R.string.auto_show_toolbar_summary)
    {
        SwitchPreference(it, Defaults.PREF_AUTO_SHOW_TOOLBAR)
    },
    Setting(context, Settings.PREF_AUTO_HIDE_TOOLBAR, R.string.auto_hide_toolbar, R.string.auto_hide_toolbar_summary)
    {
        SwitchPreference(it, Defaults.PREF_AUTO_HIDE_TOOLBAR)
    },
    Setting(context, Settings.PREF_VARIABLE_TOOLBAR_DIRECTION,
        R.string.var_toolbar_direction, R.string.var_toolbar_direction_summary)
    {
        SwitchPreference(it, Defaults.PREF_VARIABLE_TOOLBAR_DIRECTION)
    }
)

@Composable
fun KeyboardIconsSet.GetIcon(name: String?) {
    val ctx = LocalContext.current
    val drawable = getNewDrawable(name, ctx)
    Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
        if (drawable is VectorDrawable)
            Icon(painterResource(iconIds[name?.lowercase()]!!), name, Modifier.fillMaxSize(0.8f))
        else if (drawable != null) {
            val px = with(LocalDensity.current) { 40.dp.toPx() }.toInt()
            Icon(drawable.toBitmap(px, px).asImageBitmap(), name, Modifier.fillMaxSize(0.8f))
        }
    }
}

@Preview
@Composable
private fun Preview() {
    initPreview(LocalContext.current)
    Theme(previewDark) {
        Surface {
            ToolbarScreen { }
        }
    }
}
