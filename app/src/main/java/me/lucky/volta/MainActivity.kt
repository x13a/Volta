package me.lucky.volta

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.accessibility.AccessibilityManager
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

import me.lucky.volta.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: Preferences
    private var accessibilityManager: AccessibilityManager? = null
    private var cameraManager: CameraManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()
        setup()
        if (prefs.isShowProminentDisclosure) showProminentDisclosure()
    }

    private fun init() {
        prefs = Preferences(this)
        accessibilityManager = getSystemService(AccessibilityManager::class.java)
        cameraManager = getSystemService(CameraManager::class.java)
        if (!hasFlashlight()) hideFlashlight()
        binding.apply {
            flashlight.isChecked = prefs.isFlashlightChecked
            toggle.isChecked = prefs.isServiceEnabled
        }
    }

    private fun hasFlashlight(): Boolean {
        return try {
            cameraManager?.cameraIdList?.any {
                cameraManager
                    ?.getCameraCharacteristics(it)
                    ?.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
            } ?: false
        } catch (exc: Exception) { false }
    }

    private fun setup() {
        binding.apply {
            flashlight.setOnCheckedChangeListener { _, isChecked ->
                prefs.isFlashlightChecked = isChecked
            }
            flashlight.setOnLongClickListener {
                showFlashlightSettings()
                true
            }
            toggle.setOnCheckedChangeListener { _, isChecked ->
                prefs.isServiceEnabled = isChecked
                if (isChecked && !hasPermissions())
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
    }

    private fun showProminentDisclosure() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.prominent_disclosure_title)
            .setMessage(R.string.prominent_disclosure_message)
            .setPositiveButton(R.string.accept) { _, _ ->
                prefs.isShowProminentDisclosure = false
            }
            .setNegativeButton(R.string.exit) { _, _ ->
                finishAndRemoveTask()
            }
            .show()
    }

    private fun showFlashlightSettings() {
        var flag = prefs.flashlightFlag
        val values = FlashlightFlag.values()
        MaterialAlertDialogBuilder(this)
            .setMultiChoiceItems(
                resources.getStringArray(R.array.flashlight_flag),
                values.map { flag.and(it.value) != 0 }.toBooleanArray()
            ) { _, index, isChecked ->
                val value = values[index]
                flag = when (isChecked) {
                    true -> flag.or(value.value)
                    false -> flag.and(value.value.inv())
                }
            }
            .setPositiveButton(android.R.string.ok) { _, _ ->
                prefs.flashlightFlag = flag
            }
            .show()
    }

    private fun hideFlashlight() {
        binding.flashlight.visibility = View.GONE
        binding.flashlightDescription.visibility = View.GONE
    }

    override fun onStart() {
        super.onStart()
        update()
    }

    private fun update() {
        if (prefs.isServiceEnabled && !hasPermissions())
            Snackbar.make(
                binding.toggle,
                R.string.service_unavailable_popup,
                Snackbar.LENGTH_SHORT,
            ).show()
    }

    private fun hasPermissions(): Boolean {
        for (info in accessibilityManager?.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_GENERIC,
        ) ?: return true) {
            if (info.resolveInfo.serviceInfo.packageName == packageName) return true
        }
        return false
    }
}
