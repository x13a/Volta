package me.lucky.volta

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

import me.lucky.volta.databinding.ActivityMainBinding
import me.lucky.volta.fragment.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: Preferences

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
        replaceFragment(MainFragment())
    }

    private fun setup() = binding.apply {
        appBar.setNavigationOnClickListener {
            drawer.open()
        }
        navigation.setNavigationItemSelectedListener {
            replaceFragment(getFragment(it.itemId))
            it.isChecked = true
            drawer.close()
            true
        }
    }

    private fun showProminentDisclosure() =
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

    private fun replaceFragment(f: Fragment) =
        supportFragmentManager
            .beginTransaction()
            .replace(binding.fragment.id, f)
            .commit()

    private fun getFragment(id: Int) = when (id) {
        R.id.nav_main -> MainFragment()
        R.id.nav_track -> TrackFragment()
        R.id.nav_double_up -> DoubleUpFragment()
        R.id.nav_double_down -> DoubleDownFragment()
        else -> MainFragment()
    }
}