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
            track.isChecked = prefs.isTrackChecked
            flashlight.isChecked = prefs.isFlashlightChecked
            toggle.isChecked = prefs.isEnabled
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
            track.setOnCheckedChangeListener { _, isChecked ->
                prefs.isTrackChecked = isChecked
            }
            track.setOnLongClickListener {
                showTrackOptions()
                true
            }
            flashlight.setOnCheckedChangeListener { _, isChecked ->
                prefs.isFlashlightChecked = isChecked
            }
            flashlight.setOnLongClickListener {
                showFlashlightOptions()
                true
            }
            toggle.setOnCheckedChangeListener { _, isChecked ->
                prefs.isEnabled = isChecked
                if (isChecked && !hasPermissions())
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
    }

    private fun showTrackOptions() {
        var options = prefs.trackOptions
        val values = TrackOption.values()
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.track)
            .setMultiChoiceItems(
                resources.getStringArray(R.array.track_options),
                values.map { options.and(it.value) != 0 }.toBooleanArray()
            ) { _, index, isChecked ->
                val flag = values[index]
                options = when (isChecked) {
                    true -> options.or(flag.value)
                    false -> options.and(flag.value.inv())
                }
            }
            .setPositiveButton(android.R.string.ok) { _, _ ->
                prefs.trackOptions = options
            }
            .show()
    }

    private fun showFlashlightOptions() {
        var options = prefs.flashlightOptions
        val values = TrackOption.values()
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.flashlight)
            .setMultiChoiceItems(
                resources.getStringArray(R.array.flashlight_options),
                values.map { options.and(it.value) != 0 }.toBooleanArray()
            ) { _, index, isChecked ->
                val flag = values[index]
                options = when (isChecked) {
                    true -> options.or(flag.value)
                    false -> options.and(flag.value.inv())
                }
            }
            .setPositiveButton(android.R.string.ok) { _, _ ->
                prefs.flashlightOptions = options
            }
            .show()
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

    private fun hideFlashlight() {
        binding.flashlight.visibility = View.GONE
        binding.flashlightDescription.visibility = View.GONE
    }

    override fun onStart() {
        super.onStart()
        update()
    }

    private fun update() {
        if (prefs.isEnabled && !hasPermissions())
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
