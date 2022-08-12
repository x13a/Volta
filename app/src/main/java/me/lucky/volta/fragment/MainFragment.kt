package me.lucky.volta.fragment

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar

import me.lucky.volta.Preferences
import me.lucky.volta.R
import me.lucky.volta.databinding.FragmentMainBinding

class MainFragment : Fragment() {
    private lateinit var binding: FragmentMainBinding
    private lateinit var ctx: Context
    private lateinit var prefs: Preferences
    private var accessibilityManager: AccessibilityManager? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentMainBinding.inflate(inflater, container, false)
        init()
        setup()
        return binding.root
    }

    private fun init() {
        ctx = requireContext()
        prefs = Preferences(ctx)
        accessibilityManager = ctx.getSystemService(AccessibilityManager::class.java)
        binding.apply {
            track.isChecked = prefs.isTrackEnabled
            doubleUp.isChecked = prefs.isDoubleUpEnabled
            doubleDown.isChecked = prefs.isDoubleDownEnabled
            toggle.isChecked = prefs.isEnabled
        }
    }

    private fun setup() = binding.apply {
        track.setOnCheckedChangeListener { _, isChecked ->
            prefs.isTrackEnabled = isChecked
        }
        doubleUp.setOnCheckedChangeListener { _, isChecked ->
            prefs.isDoubleUpEnabled = isChecked
        }
        doubleDown.setOnCheckedChangeListener { _, isChecked ->
            prefs.isDoubleDownEnabled = isChecked
        }
        toggle.setOnCheckedChangeListener { _, isChecked ->
            prefs.isEnabled = isChecked
            if (isChecked && !hasPermissions())
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
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
            if (info.resolveInfo.serviceInfo.packageName == ctx.packageName) return true
        }
        return false
    }
}