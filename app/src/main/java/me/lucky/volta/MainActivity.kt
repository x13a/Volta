package me.lucky.volta

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar

import me.lucky.volta.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: Preferences
    private var am: AccessibilityManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()
        setup()
    }

    private fun init() {
        prefs = Preferences(this)
        am = getSystemService(AccessibilityManager::class.java)
        binding.apply {
            toggle.isChecked = prefs.isServiceEnabled
        }
    }

    private fun setup() {
        binding.apply {
            toggle.setOnCheckedChangeListener { _, isChecked ->
                prefs.isServiceEnabled = isChecked
                if (isChecked && !hasPermissions())
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
    }

    override fun onStart() {
        super.onStart()
        update()
    }

    private fun update() {
        if (prefs.isServiceEnabled && !hasPermissions())
            Snackbar.make(
                binding.toggle,
                getString(R.string.service_unavailable_popup),
                Snackbar.LENGTH_SHORT,
            ).show()
    }

    private fun hasPermissions(): Boolean {
        for (info in am?.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_GENERIC,
        ) ?: return true) {
            if (info.resolveInfo.serviceInfo.packageName == packageName) return true
        }
        return false
    }
}
