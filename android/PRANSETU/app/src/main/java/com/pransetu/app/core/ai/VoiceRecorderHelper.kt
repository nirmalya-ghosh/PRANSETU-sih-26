package com.pransetu.app.core.ai

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.io.IOException

class VoiceRecorderHelper(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    @Throws(IOException::class)
    fun startRecording(): File {
        val cacheDir = context.cacheDir
        outputFile = File.createTempFile("sos_voice_", ".m4a", cacheDir)

        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFile?.absolutePath)
            prepare()
            start()
        }
        
        return outputFile!!
    }

    fun stopRecording(): File? {
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        recorder = null
        return outputFile
    }
}
