package com.dowlet.aiasistan

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.NotificationCompat

class SesDinlemeServisi : Service() {

    private var tanimaMotoru: SpeechRecognizer? = null
    private val bildirimKanaliId = "asistan_kanali"

    override fun onCreate() {
        super.onCreate()
        bildirimKanaliOlustur()
        startForeground(1, bildirimOlustur())
        dinlemeyeBasla()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun bildirimKanaliOlustur() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val kanal = NotificationChannel(
                bildirimKanaliId,
                "AI Asistan",
                NotificationManager.IMPORTANCE_LOW
            )
            val yonetici = getSystemService(NotificationManager::class.java)
            yonetici.createNotificationChannel(kanal)
        }
    }

    private fun bildirimOlustur(): Notification {
        return NotificationCompat.Builder(this, bildirimKanaliId)
            .setContentTitle("AI Asistan dinliyor")
            .setContentText("Komut vermek için konuşun")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()
    }

    private fun dinlemeyeBasla() {
        tanimaMotoru = SpeechRecognizer.createSpeechRecognizer(this)
        val niyet = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
        }

        tanimaMotoru?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(sonuclar: android.os.Bundle?) {
                val liste = sonuclar?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val soylenenMetin = liste?.firstOrNull()
                if (!soylenenMetin.isNullOrBlank()) {
                    komutuIsle(soylenenMetin)
                } else {
                    dinlemeyeBasla()
                }
            }

            override fun onError(hataKodu: Int) {
                dinlemeyeBasla()
            }

            override fun onReadyForSpeech(p0: android.os.Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(p0: Float) {}
            override fun onBufferReceived(p0: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(p0: android.os.Bundle?) {}
            override fun onEvent(p0: Int, p1: android.os.Bundle?) {}
        })

        tanimaMotoru?.startListening(niyet)
    }

    private fun komutuIsle(metin: String) {
        val apiAnahtari = BuildConfig.CLAUDE_API_KEY
        if (apiAnahtari.isBlank()) {
            dinlemeyeBasla()
            return
        }

        val istemci = ClaudeApiClient(apiAnahtari)
        istemci.komutYorumla(metin, object : ClaudeApiClient.Callback {
            override fun onSonuc(eylem: String, hedef: String) {
                val servis = AsistanAccessibilityService.calisanOrnek
                when (eylem) {
                    "uygulama_ac" -> servis?.uygulamaAc(hedef)
                    "mesaj_yaz" -> servis?.odaklanmisKutuyaYaz(hedef)
                }
                dinlemeyeBasla()
            }

            override fun onHata(mesaj: String) {
                dinlemeyeBasla()
            }
        })
    }

    override fun onDestroy() {
        tanimaMotoru?.destroy()
        super.onDestroy()
    }
}
