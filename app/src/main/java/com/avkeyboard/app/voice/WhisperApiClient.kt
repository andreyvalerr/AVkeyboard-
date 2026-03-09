package com.avkeyboard.app.voice

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class WhisperApiClient(
    private val apiKey: String,
    private val baseUrl: String = "https://api.groq.com/openai/v1/audio/transcriptions",
    private val model: String = "whisper-large-v3-turbo"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun transcribe(file: File, language: String? = null): String = withContext(Dispatchers.IO) {
        var lastException: Exception? = null
        repeat(2) { attempt ->
            try {
                return@withContext doTranscribe(file, language)
            } catch (e: Exception) {
                lastException = e
                if (attempt == 0 && e is IOException) {
                    // retry once on IO error
                } else {
                    throw e
                }
            }
        }
        throw lastException!!
    }

    private fun doTranscribe(file: File, language: String?): String {
        val mediaType = if (file.name.endsWith(".ogg")) "audio/ogg".toMediaType()
        else "audio/wav".toMediaType()

        val bodyBuilder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, file.asRequestBody(mediaType))
            .addFormDataPart("model", model)

        if (language != null) {
            bodyBuilder.addFormDataPart("language", language)
        }

        val request = Request.Builder()
            .url(baseUrl)
            .header("Authorization", "Bearer $apiKey")
            .post(bodyBuilder.build())
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            throw WhisperApiException(
                when (response.code) {
                    401 -> "Неверный API ключ"
                    429 -> "Лимит запросов"
                    in 500..599 -> "Сервер недоступен"
                    else -> "Ошибка API: ${response.code} $body"
                },
                response.code
            )
        }

        return try {
            JSONObject(body).getString("text")
        } catch (e: Exception) {
            throw WhisperApiException("Неверный формат ответа: $body", response.code)
        }
    }
}

class WhisperApiException(message: String, val code: Int = 0) : Exception(message)
