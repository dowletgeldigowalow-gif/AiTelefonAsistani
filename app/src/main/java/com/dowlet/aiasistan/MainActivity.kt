package com.dowlet.aiasistan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.dowlet.aiasistan.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val MIKROFON_IZIN_KODU = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.izinButonu.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        binding.mikrofonButonu.setOnClickListener {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                MIKROFON_IZIN_KODU
            )
        }

        binding.baslatButonu.setOnClickListener {
            if (!erisilebilirlikServisiAcikMi()) {
                binding.durumMetni.text = "Önce 1. adımdaki erişilebilirlik iznini açın"
                return@setOnClickListener
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                binding.durumMetni.text = "Önce 2. adımdaki mikrofon iznini verin"
                return@setOnClickListener
            }
            ContextCompat.startForegroundService(this, Intent(this, SesDinlemeServisi::class.java))
            binding.durumMetni.text = "Dinleniyor... bir komut söyleyin"
        }

        guncelleDurumMetni()
    }

    override fun onResume() {
        super.onResume()
        guncelleDurumMetni()
    }

    private fun guncelleDurumMetni() {
        val erisilebilirlikDurumu = if (erisilebilirlikServisiAcikMi()) "✓ açık" else "✗ kapalı"
        binding.durumMetni.text = "Erişilebilirlik izni: $erisilebilirlikDurumu"
    }

    private fun erisilebilirlikServisiAcikMi(): Boolean {
        val etkinServisler = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return etkinServisler.contains(packageName)
    }
}
