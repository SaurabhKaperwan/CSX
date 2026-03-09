package com.megix

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import com.lagradost.cloudstream3.AcraApplication.Companion.getKey
import com.lagradost.cloudstream3.AcraApplication.Companion.setKey

object Settings {
    // --- DATABASE KEYS ---
    const val DOWNLOAD_ENABLE = "DownloadEnable"
    const val TORRENT_ENABLE = "TorrentEnable"

    const val PROVIDER_CINESTREAM = "ProviderCineStream"
    const val PROVIDER_SIMKL = "ProviderSimkl"
    const val PROVIDER_TMDB = "ProviderTmdb"

    private const val COOKIE_KEY = "nf_cookie"
    private const val TIMESTAMP_KEY = "nf_cookie_timestamp"

    // --- STORAGE FUNCTIONS ---
    fun saveCookie(cookie: String) {
        setKey(COOKIE_KEY, cookie)
        setKey(TIMESTAMP_KEY, System.currentTimeMillis())
    }

    fun getCookie(): Pair<String?, Long> {
        val cookie = getKey<String>(COOKIE_KEY)
        val timestamp = getKey<Long>(TIMESTAMP_KEY) ?: 0L
        return Pair(cookie, timestamp)
    }

    fun clearCookie() {
        setKey(COOKIE_KEY, null)
        setKey(TIMESTAMP_KEY, null)
    }

    // --- UI POPUP DIALOG ---
    fun showSettingsDialog(context: Context, onSave: () -> Unit) {
        var requiresRestart = false

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 40, 60, 40)
        }

        layout.addView(createHeader(context, "Scraping Settings"))
        layout.addView(createToggle(context, "Enable Download Only Links", DOWNLOAD_ENABLE, false))
        layout.addView(createToggle(context, "Enable Torrents", TORRENT_ENABLE, false))

        layout.addView(createHeader(context, "Active Catalogs"))

        val onCatalogChanged = { requiresRestart = true }

        layout.addView(createToggle(context, "Enable CineStream", PROVIDER_CINESTREAM, true, onCatalogChanged))
        layout.addView(createToggle(context, "Enable CineSimkl", PROVIDER_SIMKL, true, onCatalogChanged))
        layout.addView(createToggle(context, "Enable CineTmdb", PROVIDER_TMDB, true, onCatalogChanged))

        val clearCookieText = TextView(context).apply {
            text = "Clear NF Saved Cookies"
            textSize = 14f

            setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
            setPadding(0, 60, 0, 10)
            isClickable = true

            setOnClickListener {
                clearCookie()
                Toast.makeText(context, "Cookies Cleared!", Toast.LENGTH_SHORT).show()
            }
        }

        layout.addView(clearCookieText)

        AlertDialog.Builder(context)
            .setTitle("Plugin Settings")
            .setView(layout)
            .setPositiveButton("Save & Reload") { _, _ ->
                if (requiresRestart) {
                    AlertDialog.Builder(context)
                        .setTitle("Restart Required \u26A0\uFE0F")
                        .setMessage("You have changed your Active Catalogs.\n\nPlease completely close and restart Cloudstream for the providers to update.")
                        .setPositiveButton("Understood") { _, _ -> onSave() }
                        .setCancelable(false)
                        .show()
                } else {
                    onSave()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // --- HELPER FUNCTIONS ---

    private fun createHeader(context: Context, title: String): TextView {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.colorAccent, typedValue, true)
        val themeColor = typedValue.data

        return TextView(context).apply {
            text = title.uppercase()
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            letterSpacing = 0.05f
            setTextColor(themeColor)
            setPadding(0, 50, 0, 15)
        }
    }

    private fun createToggle(
        context: Context,
        label: String,
        databaseKey: String,
        defaultState: Boolean,
        onChanged: () -> Unit = {}
    ): Switch {
        return Switch(context).apply {
            text = label
            textSize = 16f
            setPadding(0, 10, 0, 10)

            isChecked = getKey<Boolean>(databaseKey) ?: defaultState

            setOnCheckedChangeListener { _, isNowChecked ->
                setKey(databaseKey, isNowChecked)
                onChanged()
            }
        }
    }
}
