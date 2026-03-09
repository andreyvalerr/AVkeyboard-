// SPDX-License-Identifier: GPL-3.0-only
package com.avkeyboard.app.settings.screens

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.edit
import com.avkeyboard.app.latin.R
import com.avkeyboard.app.settings.SearchSettingsScreen
import com.avkeyboard.app.settings.Setting
import com.avkeyboard.app.settings.dialogs.ListPickerDialog
import com.avkeyboard.app.settings.dialogs.SliderDialog
import com.avkeyboard.app.settings.dialogs.TextInputDialog
import com.avkeyboard.app.settings.preferences.Preference

private const val PREFS_NAME = "avkeyboard_voice"
private const val KEY_PROVIDER = "whisper_provider"
private const val KEY_API_URL = "whisper_api_url"
private const val KEY_API_KEY = "whisper_api_key"
private const val KEY_MODEL = "whisper_model"
private const val KEY_LANGUAGE = "whisper_language"
private const val KEY_MAX_DURATION = "whisper_max_duration"

private const val PROVIDER_GROQ = "groq"
private const val PROVIDER_OPENAI = "openai"
private const val PROVIDER_CUSTOM = "custom"

private const val GROQ_URL = "https://api.groq.com/openai/v1/audio/transcriptions"
private const val GROQ_MODEL = "whisper-large-v3-turbo"
private const val OPENAI_URL = "https://api.openai.com/v1/audio/transcriptions"
private const val OPENAI_MODEL = "whisper-1"

private fun Context.voicePrefs() = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

@Composable
fun VoiceInputScreen(
    onClickBack: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { context.voicePrefs() }

    var provider by remember { mutableStateOf(prefs.getString(KEY_PROVIDER, PROVIDER_GROQ) ?: PROVIDER_GROQ) }
    var apiUrl by remember { mutableStateOf(prefs.getString(KEY_API_URL, GROQ_URL) ?: GROQ_URL) }
    var apiKey by remember { mutableStateOf(prefs.getString(KEY_API_KEY, "") ?: "") }
    var model by remember { mutableStateOf(prefs.getString(KEY_MODEL, GROQ_MODEL) ?: GROQ_MODEL) }
    var language by remember { mutableStateOf(prefs.getString(KEY_LANGUAGE, "") ?: "") }
    var maxDuration by remember { mutableStateOf(prefs.getInt(KEY_MAX_DURATION, 30)) }

    // Dialogs
    var showProviderDialog by remember { mutableStateOf(false) }
    var showApiUrlDialog by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showModelDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showDurationDialog by remember { mutableStateOf(false) }

    val providerItems = listOf(
        "Groq" to PROVIDER_GROQ,
        "OpenAI" to PROVIDER_OPENAI,
        stringResource(R.string.voice_provider_custom) to PROVIDER_CUSTOM
    )

    val languageItems = listOf(
        stringResource(R.string.voice_language_auto) to "",
        "English" to "en",
        "Русский" to "ru",
        "Українська" to "uk",
        "Deutsch" to "de",
        "Français" to "fr",
        "Español" to "es",
        "Italiano" to "it",
        "Português" to "pt",
        "中文" to "zh",
        "日本語" to "ja",
        "한국어" to "ko",
        "العربية" to "ar",
        "हिन्दी" to "hi",
        "Türkçe" to "tr",
        "Polski" to "pl",
    )

    fun providerDisplayName(p: String) = providerItems.firstOrNull { it.second == p }?.first ?: p
    fun languageDisplayName(l: String) = languageItems.firstOrNull { it.second == l }?.first ?: l

    fun applyProvider(newProvider: String) {
        provider = newProvider
        prefs.edit { putString(KEY_PROVIDER, newProvider) }
        when (newProvider) {
            PROVIDER_GROQ -> {
                apiUrl = GROQ_URL; model = GROQ_MODEL
                prefs.edit { putString(KEY_API_URL, GROQ_URL); putString(KEY_MODEL, GROQ_MODEL) }
            }
            PROVIDER_OPENAI -> {
                apiUrl = OPENAI_URL; model = OPENAI_MODEL
                prefs.edit { putString(KEY_API_URL, OPENAI_URL); putString(KEY_MODEL, OPENAI_MODEL) }
            }
        }
    }

    SearchSettingsScreen(
        onClickBack = onClickBack,
        title = stringResource(R.string.settings_screen_voice_input),
        settings = emptyList(),
    ) {
        Scaffold(contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)) { innerPadding ->
            Column(
                Modifier.verticalScroll(rememberScrollState()).then(Modifier.padding(innerPadding))
            ) {
                // Provider
                Preference(
                    name = stringResource(R.string.voice_api_provider),
                    description = providerDisplayName(provider),
                    onClick = { showProviderDialog = true }
                )

                // API URL
                Preference(
                    name = stringResource(R.string.voice_api_url),
                    description = apiUrl,
                    onClick = { showApiUrlDialog = true }
                )

                // API Key
                Preference(
                    name = stringResource(R.string.voice_api_key),
                    description = if (apiKey.isNotEmpty()) "••••••••" else stringResource(R.string.voice_api_key_not_set),
                    onClick = { showApiKeyDialog = true }
                )

                // Model
                Preference(
                    name = stringResource(R.string.voice_model),
                    description = model,
                    onClick = { showModelDialog = true }
                )

                // Language
                Preference(
                    name = stringResource(R.string.voice_language),
                    description = languageDisplayName(language),
                    onClick = { showLanguageDialog = true }
                )

                // Max duration
                Preference(
                    name = stringResource(R.string.voice_max_duration),
                    description = stringResource(R.string.voice_max_duration_value, maxDuration),
                    onClick = { showDurationDialog = true }
                )
            }
        }
    }

    // Provider dialog
    if (showProviderDialog) {
        ListPickerDialog(
            onDismissRequest = { showProviderDialog = false },
            items = providerItems,
            onItemSelected = {
                applyProvider(it.second)
            },
            selectedItem = providerItems.firstOrNull { it.second == provider },
            title = { Text(stringResource(R.string.voice_api_provider)) },
            getItemName = { it.first }
        )
    }

    // API URL dialog
    if (showApiUrlDialog) {
        TextInputDialog(
            onDismissRequest = { showApiUrlDialog = false },
            onConfirmed = {
                apiUrl = it
                prefs.edit { putString(KEY_API_URL, it) }
            },
            initialText = apiUrl,
            title = { Text(stringResource(R.string.voice_api_url)) },
        )
    }

    // API Key dialog
    if (showApiKeyDialog) {
        TextInputDialog(
            onDismissRequest = { showApiKeyDialog = false },
            onConfirmed = {
                apiKey = it
                prefs.edit { putString(KEY_API_KEY, it) }
            },
            initialText = apiKey,
            title = { Text(stringResource(R.string.voice_api_key)) },
        )
    }

    // Model dialog
    if (showModelDialog) {
        TextInputDialog(
            onDismissRequest = { showModelDialog = false },
            onConfirmed = {
                model = it
                prefs.edit { putString(KEY_MODEL, it) }
            },
            initialText = model,
            title = { Text(stringResource(R.string.voice_model)) },
        )
    }

    // Language dialog
    if (showLanguageDialog) {
        ListPickerDialog(
            onDismissRequest = { showLanguageDialog = false },
            items = languageItems,
            onItemSelected = {
                language = it.second
                prefs.edit { putString(KEY_LANGUAGE, it.second) }
            },
            selectedItem = languageItems.firstOrNull { it.second == language },
            title = { Text(stringResource(R.string.voice_language)) },
            getItemName = { it.first }
        )
    }

    // Duration dialog
    if (showDurationDialog) {
        SliderDialog(
            onDismissRequest = { showDurationDialog = false },
            onDone = {
                maxDuration = it.toInt()
                prefs.edit { putInt(KEY_MAX_DURATION, it.toInt()) }
            },
            initialValue = maxDuration.toFloat(),
            range = 5f..60f,
            positionString = { stringResource(R.string.voice_max_duration_value, it.toInt()) },
        )
    }
}
