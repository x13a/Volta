package me.lucky.volta.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment

import me.lucky.volta.Preferences
import me.lucky.volta.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {
    private lateinit var binding: FragmentSettingsBinding
    private lateinit var ctx: Context
    private lateinit var prefs: Preferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        init()
        setup()
        return binding.root
    }

    private fun init() {
        ctx = this.requireContext()
        prefs = Preferences(ctx)
        binding.apply {
            doublePressDuration.editText?.setText(prefs.doublePressDuration.toString())
            vibeDuration.editText?.setText(prefs.vibeDuration.toString())
            torchDisableDelay.editText?.setText(prefs.torchDisableDelay.toString())
        }
    }

    private fun setup() = binding.apply {
        doublePressDuration.editText?.doAfterTextChanged {
            prefs.doublePressDuration = it?.toString()?.toLongOrNull() ?: return@doAfterTextChanged
        }
        vibeDuration.editText?.doAfterTextChanged {
            prefs.vibeDuration = it?.toString()?.toLongOrNull() ?: return@doAfterTextChanged
        }
        torchDisableDelay.editText?.doAfterTextChanged {
            prefs.torchDisableDelay = it?.toString()?.toIntOrNull() ?: return@doAfterTextChanged
        }
    }
}