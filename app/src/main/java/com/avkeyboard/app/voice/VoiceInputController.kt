package com.avkeyboard.app.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*

class VoiceInputController(
    private val context: Context,
    private val onTextResult: (String) -> Unit
) {
    companion object {
        private const val TAG = "AVkeyboard"
    }

    enum class State { IDLE, RECORDING, PROCESSING }

    private var state = State.IDLE
    private var audioRecorder: AudioRecorder? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())

    fun toggle() {
        Log.d(TAG, "VoiceInputController.toggle() state=$state")
        when (state) {
            State.IDLE -> startRecording()
            State.RECORDING -> stopAndProcess()
            State.PROCESSING -> {
                showToast("Обработка, подождите...")
            }
        }
    }

    private fun startRecording() {
        Log.d(TAG, "VoiceInputController.startRecording()")

        // Check permission - launch activity if not granted
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "RECORD_AUDIO permission not granted, launching VoicePermissionActivity")
            try {
                val intent = Intent(context, VoicePermissionActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to launch VoicePermissionActivity", e)
            }
            return
        }

        // Check API key
        val prefs = context.getSharedPreferences("avkeyboard_voice", Context.MODE_PRIVATE)
        val apiKey = prefs.getString("whisper_api_key", "") ?: ""
        if (apiKey.isEmpty()) {
            Log.w(TAG, "Whisper API key is empty")
            showToast("Настройте API ключ в настройках AVKeyboard")
            return
        }

        val maxDuration = prefs.getInt("whisper_max_duration", 30)
        Log.d(TAG, "Starting recording, maxDuration=${maxDuration}s")

        try {
            audioRecorder = AudioRecorder(context, maxDurationMs = maxDuration * 1000L).also { it.start() }
            state = State.RECORDING
            Log.d(TAG, "Recording started successfully")
            showToast("🎤 Запись... Нажмите 🎤 для остановки")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording", e)
            showToast("Ошибка записи: ${e.message}")
            state = State.IDLE
        }
    }

    private fun stopAndProcess() {
        Log.d(TAG, "VoiceInputController.stopAndProcess()")
        val file = try {
            audioRecorder?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
            showToast("Ошибка остановки записи")
            state = State.IDLE
            return
        }
        state = State.PROCESSING
        showToast("⏳ Обработка...")

        if (file == null) {
            Log.w(TAG, "stopAndProcess: file is null")
            state = State.IDLE
            return
        }

        Log.d(TAG, "Audio file: ${file.absolutePath}, size=${file.length()}")

        val prefs = context.getSharedPreferences("avkeyboard_voice", Context.MODE_PRIVATE)
        val apiKey = prefs.getString("whisper_api_key", "") ?: ""
        val endpoint = prefs.getString("whisper_api_url", "https://api.groq.com/openai/v1/audio/transcriptions") ?: "https://api.groq.com/openai/v1/audio/transcriptions"
        val model = prefs.getString("whisper_model", "whisper-large-v3-turbo") ?: "whisper-large-v3-turbo"
        val language = prefs.getString("whisper_language", "") ?: ""

        val client = WhisperApiClient(apiKey, endpoint, model)

        scope.launch {
            try {
                Log.d(TAG, "Sending to Whisper API: endpoint=$endpoint, model=$model")
                val text = client.transcribe(file, language.ifEmpty { null })
                if (text.isNotBlank()) {
                    Log.d(TAG, "Transcription result: $text")
                    onTextResult(text)
                } else {
                    Log.w(TAG, "Transcription returned empty text")
                    showToast("Речь не распознана")
                }
            } catch (e: WhisperApiException) {
                Log.e(TAG, "Whisper API error", e)
                showToast("Ошибка API: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Transcription error", e)
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
        Log.d(TAG, "Toast: $msg")
        mainHandler.post {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }
}
