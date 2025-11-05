package com.google.ai.edge.gallery.tts

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.CompletableDeferred
import java.util.Locale
import java.util.UUID

class TtsManager(ctx: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(ctx, this)
    private val ready = CompletableDeferred<Boolean>()

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.KOREAN
            tts?.setSpeechRate(1.0f)
            tts?.setPitch(1.0f)
            tts?.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            ready.complete(true)
        } else ready.complete(false)
    }

    suspend fun speak(text: String) {
        if (text.isBlank()) return
        if (!ready.await()) return
        val chunks = text.chunked(3500)
        chunks.forEachIndexed { i, chunk ->
            tts?.speak(
                chunk,
                if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD,
                null,
                "utt-${UUID.randomUUID()}"
            )
        }
    }
    fun isSpeaking(): Boolean = tts?.isSpeaking == true
    fun stop() { tts?.stop() }
    fun shutdown() { tts?.shutdown(); tts = null }
}
