package me.lucky.volta.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment

import me.lucky.volta.*
import me.lucky.volta.databinding.FragmentDoubleUpBinding

class DoubleUpFragment : Fragment() {
    private lateinit var binding: FragmentDoubleUpBinding
    private lateinit var ctx: Context
    private lateinit var prefs: Preferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentDoubleUpBinding.inflate(inflater, container, false)
        init()
        setup()
        return binding.root
    }

    private fun init() {
        ctx = requireContext()
        prefs = Preferences(ctx)
        selectInterface()
        binding.apply {
            vibrate.isChecked = prefs.doubleUpOptions.and(Option.VIBRATE.value) != 0
            mode.check(when (prefs.doubleUpMode) {
                Mode.FLASHLIGHT.value -> R.id.flashlight
                Mode.BROADCAST.value -> R.id.broadcast
                else -> R.id.flashlight
            })
            action.editText?.setText(prefs.broadcastUpAction)
            receiver.editText?.setText(prefs.broadcastUpReceiver)
            extraKey.editText?.setText(prefs.broadcastUpExtraKey)
            extraValue.editText?.setText(prefs.broadcastUpExtraValue)
        }
    }

    private fun setup() = binding.apply {
        vibrate.setOnCheckedChangeListener { _, isChecked ->
            prefs.doubleUpOptions =
                Utils.setFlag(prefs.doubleUpOptions, Option.VIBRATE.value, isChecked)
        }
        mode.setOnCheckedChangeListener { _, checkedId ->
            prefs.doubleUpMode = when (checkedId) {
                R.id.flashlight -> Mode.FLASHLIGHT.value
                R.id.broadcast -> Mode.BROADCAST.value
                else -> Mode.FLASHLIGHT.value
            }
            selectInterface()
        }
        action.editText?.doAfterTextChanged {
            prefs.broadcastUpAction = it?.toString()?.trim() ?: return@doAfterTextChanged
        }
        receiver.editText?.doAfterTextChanged {
            prefs.broadcastUpReceiver = it?.toString()?.trim() ?: return@doAfterTextChanged
        }
        extraKey.editText?.doAfterTextChanged {
            prefs.broadcastUpExtraKey = it?.toString()?.trim() ?: return@doAfterTextChanged
        }
        extraValue.editText?.doAfterTextChanged {
            prefs.broadcastUpExtraValue = it?.toString()?.trim() ?: return@doAfterTextChanged
        }
    }

    private fun selectInterface() {
        val v = when (prefs.doubleUpMode) {
            Mode.BROADCAST.value -> View.VISIBLE
            Mode.FLASHLIGHT.value -> View.GONE
            else -> View.GONE
        }
        binding.apply {
            action.visibility = v
            receiver.visibility = v
            extraKey.visibility = v
            extraValue.visibility = v
            space1.visibility = v
            space2.visibility = v
            space3.visibility = v
        }
    }
}