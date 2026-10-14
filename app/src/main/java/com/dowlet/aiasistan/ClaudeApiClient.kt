package com.dowlet.aiasistan

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class ClaudeApiClient(private val apiKey: String) {

    private val client = OkHttpClient()
    private val endpoint = "https://api.anthropic.com/v1/messages"

    interface Callback {
        fun onSonuc(eylem: String, hedef: String)
        fun onHata(mesaj: String)
    }

    fun komutYorumla(sesliKomut: String, callback: Callback) {
        val sistemTalimati = """
            Kullanıcının sesli komutunu analiz et ve SADECE şu JSON formatında cevap ver:
            {"eylem": "uygulama_ac" | "mesaj_yaz" | "arama_yap" | "bilinmiyor", "hedef": "..."}
            Başka hiçbir açıklama ekleme, sadece JSON döndür.
        """.trimIndent()

        val mesajlar = JSONArray()
        mesajlar.put(
            JSONObject().apply {
                put("role", "user")
                put("content", sesliKomut)
            }
        )

        val govde = JSONObject().apply {
            put("model", "claude-sonnet-5")
            put("max_tokens", 200)
            put("system", sistemTalimati)
            put("messages", mesajlar)
        }

        val body = govde.toString().toRequestBody("application/json".toMediaType())

        val istek = Request.Builder()
            .url(endpoint)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(body)
            .build()

        client.newCall(istek).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                callback.onHata("Ağ hatası: ${e.message}")
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    val govdeMetni = it.body?.string() ?: ""
                    if (!it.isSuccessful) {
                        callback.onHata("API hatası (${it.code}): $govdeMetni")
                        return
                    }
                    try {
                        val json = JSONObject(govdeMetni)
                        val icerik = json.getJSONArray("content")
                            .getJSONObject(0)
                            .getString("text")
                        val sonucJson = JSONObject(icerik)
                        callback.onSonuc(
                            sonucJson.optString("eylem", "bilinmiyor"),
                            sonucJson.optString("hedef", "")
                        )
                    } catch (e: Exception) {
                        callback.onHata("Cevap okunamadı: ${e.message}")
                    }
                }
            }
        })
    }
}
