package com.avkeyboard.app.voice

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile

class AudioRecorder(
    private val context: Context,
    private val maxDurationMs: Long = 60_000L
) {
    private var mediaRecorder: MediaRecorder? = null
    private var audioRecord: AudioRecord? = null
    private var outputFile: File? = null
    private var isRecording = false
    private var recordingJob: Job? = null
    private var recordingScope: CoroutineScope? = null

    fun start() {
        if (isRecording) return
        Log.d("AVkeyboard", "AudioRecorder.start() useMediaRecorder=${useMediaRecorder()}, maxDurationMs=$maxDurationMs")
        val file = File.createTempFile("voice_", if (useMediaRecorder()) ".ogg" else ".wav", context.cacheDir)
        outputFile = file
        isRecording = true

        try {
            if (useMediaRecorder()) {
                startMediaRecorder(file)
            } else {
                startAudioRecord(file)
            }
        } catch (e: Exception) {
            Log.e("AVkeyboard", "AudioRecorder.start() failed", e)
            isRecording = false
            outputFile = null
            file.delete()
            throw e
        }

        recordingScope = CoroutineScope(Dispatchers.IO)
        recordingJob = recordingScope?.launch {
            delay(maxDurationMs)
            if (isRecording) {
                withContext(Dispatchers.Main) { stopInternal() }
            }
        }
    }

    fun stop(): File {
        if (!isRecording) throw IllegalStateException("Not recording")
        stopInternal()
        return outputFile ?: throw IllegalStateException("No output file")
    }

    fun cancel() {
        if (!isRecording) return
        stopInternal()
        outputFile?.delete()
        outputFile = null
    }

    fun cleanup() {
        outputFile?.delete()
        outputFile = null
    }

    private fun stopInternal() {
        isRecording = false
        recordingJob?.cancel()
        recordingJob = null
        recordingScope?.cancel()
        recordingScope = null

        if (useMediaRecorder()) {
            try {
                mediaRecorder?.stop()
            } catch (_: Exception) {}
            mediaRecorder?.release()
            mediaRecorder = null
        } else {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        }
    }

    private fun useMediaRecorder(): Boolean = Build.VERSION.SDK_INT >= 29

    private fun startMediaRecorder(file: File) {
        @Suppress("DEPRECATION")
        val recorder = if (Build.VERSION.SDK_INT >= 31) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }
        recorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.OGG)
            setAudioEncoder(MediaRecorder.AudioEncoder.OPUS)
            setAudioSamplingRate(16000)
            setAudioEncodingBitRate(32000)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        mediaRecorder = recorder
    }

    private fun startAudioRecord(file: File) {
        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        @Suppress("MissingPermission")
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate, channelConfig, audioFormat, bufferSize
        )
        audioRecord = recorder
        recorder.startRecording()

        recordingScope = CoroutineScope(Dispatchers.IO)
        recordingScope?.launch {
            val fos = FileOutputStream(file)
            // Write WAV header placeholder (44 bytes)
            val header = ByteArray(44)
            fos.write(header)

            val buffer = ByteArray(bufferSize)
            var totalBytes = 0L
            while (isRecording && isActive) {
                val read = recorder.read(buffer, 0, buffer.size)
                if (read > 0) {
                    fos.write(buffer, 0, read)
                    totalBytes += read
                }
            }
            fos.close()

            // Write proper WAV header
            writeWavHeader(file, totalBytes, sampleRate, 1, 16)
        }
    }

    private fun writeWavHeader(file: File, dataSize: Long, sampleRate: Int, channels: Int, bitsPerSample: Int) {
        val raf = RandomAccessFile(file, "rw")
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val totalSize = dataSize + 36

        raf.seek(0)
        raf.writeBytes("RIFF")
        raf.writeIntLE(totalSize.toInt())
        raf.writeBytes("WAVE")
        raf.writeBytes("fmt ")
        raf.writeIntLE(16) // subchunk size
        raf.writeShortLE(1) // PCM
        raf.writeShortLE(channels)
        raf.writeIntLE(sampleRate)
        raf.writeIntLE(byteRate)
        raf.writeShortLE(blockAlign)
        raf.writeShortLE(bitsPerSample)
        raf.writeBytes("data")
        raf.writeIntLE(dataSize.toInt())
        raf.close()
    }

    private fun RandomAccessFile.writeIntLE(value: Int) {
        write(value and 0xFF)
        write((value shr 8) and 0xFF)
        write((value shr 16) and 0xFF)
        write((value shr 24) and 0xFF)
    }

    private fun RandomAccessFile.writeShortLE(value: Int) {
        write(value and 0xFF)
        write((value shr 8) and 0xFF)
    }
}
