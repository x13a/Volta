package me.lucky.volta.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

import me.lucky.volta.Option
import me.lucky.volta.Preferences
import me.lucky.volta.Utils
import me.lucky.volta.databinding.FragmentTrackBinding

class TrackFragment : Fragment() {
    private lateinit var binding: FragmentTrackBinding
    private lateinit var ctx: Context
    private lateinit var prefs: Preferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentTrackBinding.inflate(inflater, container, false)
        init()
        setup()
        return binding.root
    }

    private fun init() {
        ctx = requireContext()
        prefs = Preferences(ctx)
        binding.apply {
            vibrate.isChecked = prefs.trackOptions.and(Option.VIBRATE.value) != 0
        }
    }

    private fun setup() = binding.apply {
        vibrate.setOnCheckedChangeListener { _, isChecked ->
            prefs.trackOptions = Utils.setFlag(prefs.trackOptions, Option.VIBRATE.value, isChecked)
        }
    }
}