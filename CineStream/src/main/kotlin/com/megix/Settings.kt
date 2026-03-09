package com.megix

import android.app.AlertDialog
import android.content.Context
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.Toast
import com.lagradost.cloudstream3.AcraApplication.Companion.getKey
import com.lagradost.cloudstream3.AcraApplication.Companion.setKey

object Settings {
    // --- DATABASE KEYS ---
    const val DOWNLOAD_ENABLE = "DownloadEnable"
    const val TORRENT_ENABLE = "TorrentEnable"
    private const val COOKIE_KEY = "nf_cookie"
    private const val TIMESTAMP_KEY = "nf_cookie_timestamp"

    // --- STORAGE FUNCTIONS FOR NETMIRROR ---
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

    //---- STORAGE FUNCTIONS FOR NETMIRROR ----

    // --- UI POPUP ---
    fun showSettingsDialog(context: Context, onSave: () -> Unit) {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 40, 60, 40)
        }

        // 1. Download Toggle
        val downloadToggle = Switch(context).apply {
            text = "Enable Download Only Links"
            textSize = 18f
            setPadding(0, 0, 0, 40)

            isChecked = getKey<Boolean>(DOWNLOAD_ENABLE) ?: false
            setOnCheckedChangeListener { _, isNowChecked ->
                setKey(DOWNLOAD_ENABLE, isNowChecked)
            }
        }
        layout.addView(downloadToggle)

        // 2. Torrent Toggle
        val torrentToggle = Switch(context).apply {
            text = "Enable Torrents"
            textSize = 18f
            setPadding(0, 0, 0, 40)

            isChecked = getKey<Boolean>(TORRENT_ENABLE) ?: false
            setOnCheckedChangeListener { _, isNowChecked ->
                setKey(TORRENT_ENABLE, isNowChecked)
            }
        }
        layout.addView(torrentToggle)

        val clearCookieButton = Button(context).apply {
            text = "Clear Saved Netmirror Cookies"

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 20, 0, 0)
            }

            setOnClickListener {
                clearCookie()
                Toast.makeText(context, "Netmirror Cookies Cleared!", Toast.LENGTH_SHORT).show()
            }
        }
        layout.addView(clearCookieButton)

        AlertDialog.Builder(context)
            .setTitle("Provider Settings")
            .setView(layout)
            .setPositiveButton("Save & Reload") { _, _ ->
                onSave()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
