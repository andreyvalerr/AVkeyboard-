package com.avkeyboard.app.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*

class VoiceInputController(
    private val context: Context,
    private val onTextResult: (String) -> Unit
) {
    enum class State { IDLE, RECORDING, PROCESSING }

    private var state = State.IDLE
    private var audioRecorder: AudioRecorder? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun toggle() {
        when (state) {
            State.IDLE -> startRecording()
            State.RECORDING -> stopAndProcess()
            State.PROCESSING -> {
                showToast("Обработка, подождите...")
            }
        }
    }

    private fun startRecording() {
        // Check permission
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            showToast("Нужно разрешение на запись аудио")
            return
        }

        // Check API key
        val prefs = context.getSharedPreferences("avkeyboard_voice", Context.MODE_PRIVATE)
        val apiKey = prefs.getString("whisper_api_key", "") ?: ""
        if (apiKey.isEmpty()) {
            showToast("Настройте API ключ в настройках AVKeyboard")
            return
        }

        try {
            audioRecorder = AudioRecorder(context).also { it.start() }
            state = State.RECORDING
            showToast("🎤 Запись... Нажмите 🎤 для остановки")
        } catch (e: Exception) {
            showToast("Ошибка записи: ${e.message}")
            state = State.IDLE
        }
    }

    private fun stopAndProcess() {
        val file = try {
            audioRecorder?.stop()
        } catch (e: Exception) {
            showToast("Ошибка остановки записи")
            state = State.IDLE
            return
        }
        state = State.PROCESSING
        showToast("⏳ Обработка...")

        if (file == null) {
            state = State.IDLE
            return
        }

        val prefs = context.getSharedPreferences("avkeyboard_voice", Context.MODE_PRIVATE)
        val apiKey = prefs.getString("whisper_api_key", "") ?: ""
        val endpoint = prefs.getString("whisper_api_url", "https://api.groq.com/openai/v1") ?: "https://api.groq.com/openai/v1"
        val model = prefs.getString("whisper_model", "whisper-large-v3-turbo") ?: "whisper-large-v3-turbo"

        val client = WhisperApiClient(apiKey, endpoint, model)

        scope.launch {
            try {
                val text = client.transcribe(file)
                if (text.isNotBlank()) {
                    onTextResult(text)
                } else {
                    showToast("Речь не распознана")
                }
            } catch (e: WhisperApiException) {
                showToast("Ошибка API: ${e.message}")
            } catch (e: Exception) {
                showToast("Ошибка: ${e.message}")
            } finally {
                file.delete()
                audioRecorder?.cleanup()
                audioRecorder = null
                state = State.IDLE
            }
        }
    }

    fun cancel() {
        audioRecorder?.cancel()
        audioRecorder = null
        state = State.IDLE
    }

    fun destroy() {
        cancel()
        scope.cancel()
    }

    private fun showToast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }
}
