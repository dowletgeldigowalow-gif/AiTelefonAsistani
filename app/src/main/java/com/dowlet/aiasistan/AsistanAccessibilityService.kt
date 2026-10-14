package com.dowlet.aiasistan

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AsistanAccessibilityService : AccessibilityService() {

    companion object {
        var calisanOrnek: AsistanAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        calisanOrnek = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    }

    override fun onInterrupt() {
        calisanOrnek = null
    }

    fun uygulamaAc(paketAdi: String): Boolean {
        val baslatmaIntenti = packageManager.getLaunchIntentForPackage(paketAdi) ?: return false
        baslatmaIntenti.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(baslatmaIntenti)
        return true
    }

    fun metneGoreTikla(aranacakMetin: String): Boolean {
        val kok: AccessibilityNodeInfo = rootInActiveWindow ?: return false
        val bulunanlar = kok.findAccessibilityNodeInfosByText(aranacakMetin)
        if (bulunanlar.isNullOrEmpty()) return false
        val hedef = bulunanlar[0]
        return hedef.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    fun odaklanmisKutuyaYaz(metin: String): Boolean {
        val kok: AccessibilityNodeInfo = rootInActiveWindow ?: return false
        val odakliDugum = bulOdakliDugum(kok) ?: return false
        val arguman = android.os.Bundle()
        arguman.putCharSequence(
            AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
            metin
        )
        return odakliDugum.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguman)
    }

    private fun bulOdakliDugum(dugum: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (dugum.isFocused) return dugum
        for (i in 0 until dugum.childCount) {
            val cocuk = dugum.getChild(i) ?: continue
            val sonuc = bulOdakliDugum(cocuk)
            if (sonuc != null) return sonuc
        }
        return null
    }
}
