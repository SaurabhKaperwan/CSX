package com.megix.settings

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.*
import com.lagradost.cloudstream3.AcraApplication.Companion.getKey
import com.megix.settings.SettingsTheme.dp

/**
 * Builds the "🎬 Providers" collapsible card and each individual provider row.
 */
internal object SettingsProviders {

    fun buildCard(
        context: Context,
        pendingChanges: MutableMap<String, Any?>,
        onRegisterCommit: (() -> Unit) -> Unit
    ): View {
        val theme = SettingsTheme
        val card  = SettingsWidgets.cardContainer(context)

        var expanded = false
        val content  = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 8.dp(context))
            visibility = View.GONE
        }

        val rows  = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val order = Settings.getOrder().toMutableList()

        onRegisterCommit {
            val stremioKeys = Settings.getStremioAddons().map { Settings.stremioAddonKey(it.name) }.toSet()
            val merged = order
                .filter { key -> !key.startsWith("stremio_") || key in stremioKeys }
                .let { filtered -> filtered + (stremioKeys - filtered.toSet()) }
            Settings.saveOrder(merged)
            // Mark all currently visible providers as seen so future new additions
            // are correctly identified and respect the new-provider default preference
            Settings.markProvidersSeen(merged)
        }

        fun newProviderDefaultOnNow(): Boolean =
            pendingChanges[Settings.NEW_PROVIDER_DEFAULT_ON] as? Boolean ?: Settings.newProviderDefaultOn()

        val seenProviders = Settings.getSeenProviders()

        fun providerEnabled(key: String): Boolean {
            val explicit = pendingChanges[key] as? Boolean ?: getKey<Boolean>(key)
            if (explicit != null) return explicit
            if (key.startsWith("stremio_")) return true
            if (key in Settings.TORRENT_KEYS) return false
            if (key !in seenProviders) return newProviderDefaultOnNow()
            return true
        }

        var searchQuery = ""

        fun rebuild() {
            rows.removeAllViews()
            val filteredOrder = if (searchQuery.isBlank()) order else order.filter {
                Settings.providerDisplayName(it).contains(searchQuery, ignoreCase = true)
            }

            filteredOrder.forEachIndexed { displayIndex, key ->
                val actualIndex = order.indexOf(key)
                val isSearching = searchQuery.isNotBlank()

                if (displayIndex > 0) rows.addView(SettingsWidgets.divider(context))
                rows.addView(buildRow(
                    context        = context,
                    label          = Settings.providerDisplayName(key),
                    key            = key,
                    index          = actualIndex + 1,
                    totalCount     = order.size,
                    isTorrent      = key in Settings.TORRENT_KEYS || Settings.isStremioTorrent(key),
                    canMoveUp      = !isSearching && actualIndex > 0,
                    canMoveDown    = !isSearching && actualIndex < order.lastIndex,
                    pendingChanges = pendingChanges,
                    onMoveUp       = { order.add(actualIndex - 1, order.removeAt(actualIndex)); rebuild() },
                    onMoveDown     = { order.add(actualIndex + 1, order.removeAt(actualIndex)); rebuild() },
                    onMoveTo       = { target ->
                        val item = order.removeAt(actualIndex)
                        order.add(target.coerceIn(0, order.size), item); rebuild()
                    }
                ))
            }
        }

        // Toolbar pill buttons
        val pillRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity     = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = 6.dp(context) }
        }
        pillRow.addView(SettingsWidgets.pillBtn(context, "✓ All",
            Color.parseColor("#4ADE80"), Color.parseColor("#0A1A0F"),
            Color.parseColor("#1A3A1F")) {
            order.forEach { pendingChanges[it] = true }; rebuild()
            Toast.makeText(context, "All providers enabled", Toast.LENGTH_SHORT).show()
        })
        pillRow.addView(SettingsWidgets.hSpacer(context, 8))
        pillRow.addView(SettingsWidgets.pillBtn(context, "✕ None", theme.DANGER_COLOR,
            Color.parseColor("#1A0A0D"), Color.parseColor("#3A1520")) {
            order.forEach { pendingChanges[it] = false }; rebuild()
            Toast.makeText(context, "All providers disabled", Toast.LENGTH_SHORT).show()
        })
        pillRow.addView(SettingsWidgets.hSpacer(context, 8))
        pillRow.addView(SettingsWidgets.pillBtn(context, "↺ Reset Order", theme.ACCENT_START,
            Color.parseColor("#1A1730"), Color.parseColor("#2E2850")) {
            val stremioKeys = Settings.getStremioAddons().map { Settings.stremioAddonKey(it.name) }
            order.clear(); order.addAll(Settings.DEFAULT_ORDER + stremioKeys); rebuild()
            Toast.makeText(context, "Order reset — tap Save to apply", Toast.LENGTH_SHORT).show()
        })

        // New built-in provider default toggle
        val newProviderToggleRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity     = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = 6.dp(context); it.bottomMargin = 2.dp(context) }
            setPadding(4.dp(context), 6.dp(context), 4.dp(context), 6.dp(context))
            background = GradientDrawable().apply {
                cornerRadius = 10f.dp(context)
                setColor(Color.parseColor("#0F1020"))
                setStroke(1, Color.parseColor("#2A2D45"))
            }

            val col = LinearLayout(context).apply {
                orientation  = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            col.addView(TextView(context).apply {
                text = "🆕 New Built-in Providers"
                textSize = 12f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(theme.TEXT_PRIMARY)
            })
            col.addView(TextView(context).apply {
                text = "When a new provider is added enable it automatically"
                textSize = 10f; setTextColor(theme.TEXT_SECONDARY)
                setPadding(0, 2.dp(context), 0, 0)
            })
            addView(col)

            val sw = SettingsWidgets.styledSwitch(context, newProviderDefaultOnNow(),
                trackOnColor = Color.parseColor("#4ADE80"))
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                isClickable = true; isFocusable = true; isFocusableInTouchMode = false
                background = theme.stateDrawable(context)
                addView(sw)
                setOnClickListener {
                    sw.isChecked = !sw.isChecked
                    pendingChanges[Settings.NEW_PROVIDER_DEFAULT_ON] = sw.isChecked
                    rebuild()
                }
            })
        }

        val toolbar = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16.dp(context), 8.dp(context), 16.dp(context), 4.dp(context))
            addView(pillRow)
            addView(newProviderToggleRow)
            addView(TextView(context).apply {
                text      = "🧲 = off by default  ·  🔌 = Stremio addon  ·  ↑↓ or # = order\n💡 Higher position = loads faster"
                textSize  = 10f
                setTextColor(Color.parseColor("#44475A"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = 4.dp(context) }
            })
        }

        val sep = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
                .also { it.setMargins(16.dp(context), 0, 16.dp(context), 4.dp(context)) }
            setBackgroundColor(theme.DIVIDER_COLOR)
        }

        val searchInput = EditText(context).apply {
            hint = "Search providers..."
            textSize = 13f
            setTextColor(theme.TEXT_PRIMARY)
            setHintTextColor(theme.TEXT_SECONDARY)
            setSingleLine(true)
            setPadding(14.dp(context), 10.dp(context), 14.dp(context), 10.dp(context))
            background = SettingsTheme.inputBackground(context)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(16.dp(context), 8.dp(context), 16.dp(context), 4.dp(context)) }

            addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    searchQuery = s?.toString()?.trim() ?: ""
                    rebuild()
                }
                override fun afterTextChanged(s: android.text.Editable?) {}
            })
        }

        rebuild()
        content.addView(toolbar)
        content.addView(sep)
        content.addView(searchInput)
        content.addView(rows)

        // Header row
        val chevron = TextView(context).apply { text = "▼"; textSize = 11f; setTextColor(theme.TEXT_SECONDARY) }
        val summary = TextView(context).apply {
            textSize = 11f; setTextColor(Color.parseColor("#5A5E7A"))
            setPadding(0, 0, 8.dp(context), 0)
        }
        fun updateSummary() { summary.text = "${order.count { providerEnabled(it) }} / ${order.size} on" }
        updateSummary()

        card.addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20.dp(context), 16.dp(context), 16.dp(context), 16.dp(context))
            gravity     = Gravity.CENTER_VERTICAL
            isClickable = true; isFocusable = true; isFocusableInTouchMode = false
            background  = theme.stateDrawable(context)

            addView(SettingsWidgets.accentBar(context, theme.ACCENT_START, theme.ACCENT_END))
            addView(TextView(context).apply {
                text = "🎬  Providers"; textSize = 12f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(theme.TEXT_SECONDARY); letterSpacing = 0.08f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(summary); addView(chevron)

            setOnClickListener {
                expanded = !expanded; chevron.text = if (expanded) "▲" else "▼"
                updateSummary()
                SettingsWidgets.animateExpand(content, expanded) { updateSummary() }
            }
        })

        card.addView(content)
        SettingsWidgets.fadeInSlide(card)
        return card
    }

    // ── Provider row ─────────────────────────────────────────

    fun buildRow(
        context: Context, label: String, key: String,
        index: Int, totalCount: Int, isTorrent: Boolean,
        canMoveUp: Boolean, canMoveDown: Boolean,
        pendingChanges: MutableMap<String, Any?>,
        onMoveUp: () -> Unit, onMoveDown: () -> Unit,
        onMoveTo: (Int) -> Unit
    ): View {
        val theme = SettingsTheme
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16.dp(context), 10.dp(context), 12.dp(context), 10.dp(context))
            gravity     = Gravity.CENTER_VERTICAL

            // Position badge (tappable → move-to dialog)
            addView(TextView(context).apply {
                text = "$index"; textSize = 11f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(if (isTorrent) Color.parseColor("#5A3E1E") else theme.ACCENT_START)
                gravity  = Gravity.CENTER
                setPadding(4.dp(context), 4.dp(context), 4.dp(context), 4.dp(context))
                background = GradientDrawable().apply {
                    cornerRadius = 6f.dp(context)
                    setColor(Color.parseColor("#1A1730"))
                    setStroke(1, if (isTorrent) Color.parseColor("#3A2810") else Color.parseColor("#2E2850"))
                }
                minWidth = 28.dp(context)
                isClickable = true; isFocusable = true; isFocusableInTouchMode = false
                setOnClickListener { showMoveToDialog(context, label, index, totalCount, onMoveTo) }
            })

            addView(SettingsWidgets.hSpacer(context, 10))

            addView(TextView(context).apply {
                text = label; textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(if (isTorrent) Color.parseColor("#C87C3A") else theme.TEXT_PRIMARY)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })

            fun arrowBtn(sym: String, active: Boolean, action: () -> Unit) =
                TextView(context).apply {
                    text = sym; textSize = 17f; gravity = Gravity.CENTER
                    setTextColor(if (active) theme.ACCENT_START else Color.parseColor("#252840"))
                    setPadding(9.dp(context), 6.dp(context), 9.dp(context), 6.dp(context))
                    isClickable = active; isFocusable = active; isFocusableInTouchMode = false
                    if (active) {
                        background = theme.stateDrawable(context)
                        setOnClickListener {
                            animate().scaleX(0.75f).scaleY(0.75f).setDuration(60).withEndAction {
                                animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                            }.start(); action()
                        }
                    }
                }
            addView(arrowBtn("↑", canMoveUp,   onMoveUp))
            addView(arrowBtn("↓", canMoveDown, onMoveDown))

            val effectiveChecked = pendingChanges[key] as? Boolean ?: Settings.enabled(key)
            val sw = SettingsWidgets.styledSwitch(context, effectiveChecked,
                trackOnColor = if (isTorrent) Color.parseColor("#8B5E3C") else theme.SWITCH_ON)
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                isClickable = true; isFocusable = true; isFocusableInTouchMode = false
                background = theme.stateDrawable(context)
                addView(sw)
                setOnClickListener { sw.isChecked = !sw.isChecked; pendingChanges[key] = sw.isChecked }
            })
        }
    }

    // ── Private helpers ──────────────────────────────────────

    private fun showMoveToDialog(
        context: Context, label: String, index: Int, totalCount: Int,
        onMoveTo: (Int) -> Unit
    ) {
        val theme = SettingsTheme
        val input = EditText(context).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            hint = "1 – $totalCount"; setText("$index")
            setTextColor(theme.TEXT_PRIMARY); setHintTextColor(theme.TEXT_SECONDARY); selectAll()
            setPadding(16.dp(context), 12.dp(context), 16.dp(context), 12.dp(context))
            isFocusable = true; isFocusableInTouchMode = true
            background = GradientDrawable().apply {
                cornerRadius = 10f.dp(context)
                setColor(Color.parseColor("#1A1E28"))
                setStroke(1, Color.parseColor("#2E2850"))
            }
        }
        val wrapper = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24.dp(context), 16.dp(context), 24.dp(context), 8.dp(context))
            addView(TextView(context).apply {
                text = "Move \"$label\" to position"; textSize = 13f
                setTextColor(theme.TEXT_SECONDARY); setPadding(0, 0, 0, 10.dp(context))
            })
            addView(input)
        }
        val d = AlertDialog.Builder(context, android.R.style.Theme_Material_Dialog)
            .setView(wrapper)
            .setPositiveButton("Move") { _, _ ->
                val t = input.text.toString().toIntOrNull()
                if (t != null && t in 1..totalCount) onMoveTo(t - 1)
                else Toast.makeText(context, "Enter 1 – $totalCount", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null).create()
        d.window?.setBackgroundDrawable(theme.roundRect(theme.BG_DARK, 16f.dp(context))); d.show()
        d.getButton(AlertDialog.BUTTON_POSITIVE)?.apply { setTextColor(theme.ACCENT_START); isAllCaps = false }
        d.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply { setTextColor(theme.TEXT_SECONDARY); isAllCaps = false }
        input.requestFocus()
        (context.getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager)
            .showSoftInput(input, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }

}
