package com.megix

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import com.megix.SettingsTheme.dp

/**
 * Assembles the full Settings AlertDialog and every collapsible card inside it.
 * Data access goes through Settings; theme tokens come from SettingsTheme;
 * reusable widgets come from SettingsWidgets; the provider card comes from SettingsProviders.
 */
internal object SettingsDialog {

    fun show(context: Context, onSave: () -> Unit) {
        val theme          = SettingsTheme
        var requireRestart = false
        val pending        = mutableMapOf<String, Any?>()
        var commitOrder:  () -> Unit = {}
        var commitAddons: () -> Unit = {}

        val scroll = ScrollView(context).apply {
            isScrollbarFadingEnabled = true
            background               = theme.colorDrawable(theme.BG_DARK)
            isFocusable              = false
            descendantFocusability   = ScrollView.FOCUS_AFTER_DESCENDANTS
        }

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 24.dp(context))
            background = theme.colorDrawable(theme.BG_DARK)
        }

        layout.addView(buildHeroBanner(context))
        layout.addView(View(context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 8.dp(context))
        })

        // Scraping settings card
        layout.addView(buildCollapsibleCard(context, "⚙️  Scraping Settings",
            accentA = Color.parseColor("#06B6D4"), accentB = Color.parseColor("#0891B2")) {
            addView(buildToggleRow(context, "Download Only Links",
                "Only great for downloading (Not for Streaming)",
                Settings.DOWNLOAD_ENABLE, false, pending))
            addView(SettingsWidgets.divider(context))
            addView(buildConcurrencyRow(context, pending))
            addView(SettingsWidgets.divider(context))
            addView(buildCookieClearRow(context))
        })

        // ShowBox token card
        layout.addView(buildShowboxTokenCard(context, pending))

        // Active catalogs card
        val onCatalogChanged: () -> Unit = {
            requireRestart = true
        }
        layout.addView(buildCollapsibleCard(context, "📡  Active Catalogs",
            accentA = Color.parseColor("#10B981"), accentB = Color.parseColor("#059669")) {
            addView(buildToggleRow(context, "CineStream", "Cinemeta catalog",
                Settings.PROVIDER_CINESTREAM, true, pending, onCatalogChanged))
            addView(SettingsWidgets.divider(context))
            addView(buildToggleRow(context, "CineSimkl", "Simkl catalog",
                Settings.PROVIDER_SIMKL, true, pending, onCatalogChanged))
            addView(SettingsWidgets.divider(context))
            addView(buildToggleRow(context, "CineTmdb", "TMDB catalog",
                Settings.PROVIDER_TMDB, true, pending, onCatalogChanged))
        })

        // Providers card (delegated)
        layout.addView(SettingsProviders.buildCard(context, pending) { commit -> commitOrder = commit })

        // Stremio addons card
        layout.addView(buildStremioAddonsCard(context) { commit -> commitAddons = commit })

        // Credits card
        layout.addView(buildCreditsCard(context))

        scroll.addView(layout)

        val dialog = AlertDialog.Builder(context, android.R.style.Theme_Material_Dialog)
            .setView(scroll)
            .setPositiveButton("Save") { _, _ ->
                pending.forEach { (key, value) ->
                    when {
                        key == Settings.SHOWBOX_TOKEN_KEY && value == null   -> Settings.clearShowboxToken()
                        key == Settings.SHOWBOX_TOKEN_KEY && value is String -> Settings.saveShowboxToken(value)
                        value is Boolean                                     -> com.lagradost.cloudstream3.AcraApplication.setKey(key, value)
                        value is Int                                         -> com.lagradost.cloudstream3.AcraApplication.setKey(key, value)
                        value == null                                        -> com.lagradost.cloudstream3.AcraApplication.setKey(key, null as String?)
                    }
                }
                commitAddons(); commitOrder()
                if (requireRestart) showRestartWarning(context, onSave) else onSave()
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.window?.setBackgroundDrawable(theme.roundRect(theme.BG_DARK, 20f.dp(context)))
        dialog.show()
        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.95).toInt(),
            android.view.WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            ?.apply { setTextColor(theme.ACCENT_START); isAllCaps = false }
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            ?.apply { setTextColor(theme.TEXT_SECONDARY); isAllCaps = false }
    }

    // =========================================================
    //  COLLAPSIBLE CARD TEMPLATE
    // =========================================================

    fun buildCollapsibleCard(
        context: Context,
        title: String,
        startExpanded: Boolean = false,
        accentA: Int = SettingsTheme.ACCENT_START,
        accentB: Int = SettingsTheme.ACCENT_END,
        block: LinearLayout.() -> Unit
    ): View {
        val theme = SettingsTheme
        val card  = SettingsWidgets.cardContainer(context)

        var expanded = startExpanded
        val content  = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 8.dp(context))
            visibility  = if (expanded) View.VISIBLE else View.GONE
        }
        content.block()

        val chevron = TextView(context).apply {
            text = if (expanded) "▲" else "▼"; textSize = 11f; setTextColor(theme.TEXT_SECONDARY)
        }

        card.addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20.dp(context), 16.dp(context), 16.dp(context), 16.dp(context))
            gravity     = Gravity.CENTER_VERTICAL
            isClickable = true; isFocusable = true; isFocusableInTouchMode = false
            background  = theme.stateDrawable(context)

            addView(SettingsWidgets.accentBar(context, accentA, accentB))
            addView(TextView(context).apply {
                text = title; textSize = 12f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(theme.TEXT_SECONDARY); letterSpacing = 0.08f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(chevron)

            setOnClickListener {
                expanded = !expanded; chevron.text = if (expanded) "▲" else "▼"
                SettingsWidgets.animateExpand(content, expanded)
            }
        })

        card.addView(content)
        SettingsWidgets.fadeInSlide(card)
        return card
    }

    // =========================================================
    //  TOGGLE ROW
    // =========================================================

    fun buildToggleRow(
        context: Context, label: String, subtitle: String,
        databaseKey: String, defaultState: Boolean,
        pending: MutableMap<String, Any?>,
        onChanged: () -> Unit = {}
    ): View {
        val theme   = SettingsTheme
        val checked = pending[databaseKey] as? Boolean
            ?: com.lagradost.cloudstream3.AcraApplication.getKey<Boolean>(databaseKey) ?: defaultState

        val sw = SettingsWidgets.styledSwitch(context, checked)

        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20.dp(context), 14.dp(context), 16.dp(context), 14.dp(context))
            gravity     = Gravity.CENTER_VERTICAL
            isClickable = true; isFocusable = true; isFocusableInTouchMode = false
            background  = theme.stateDrawable(context)

            val textCol = LinearLayout(context).apply {
                orientation  = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            textCol.addView(TextView(context).apply {
                text = label; textSize = 15f
                setTypeface(null, android.graphics.Typeface.BOLD); setTextColor(theme.TEXT_PRIMARY)
            })
            textCol.addView(TextView(context).apply {
                text = subtitle; textSize = 12f; setTextColor(theme.TEXT_SECONDARY)
                setPadding(0, 3.dp(context), 0, 0)
            })
            addView(textCol); addView(sw)

            setOnClickListener {
                sw.isChecked = !sw.isChecked
                pending[databaseKey] = sw.isChecked
                sw.animate().scaleX(0.92f).scaleY(0.92f).setDuration(80).withEndAction {
                    sw.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                }.start()
                onChanged()
            }
        }
    }

    // =========================================================
    //  COOKIE CLEAR ROW
    // =========================================================

    private fun buildCookieClearRow(context: Context): View {
        val theme = SettingsTheme
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20.dp(context), 14.dp(context), 16.dp(context), 14.dp(context))
            gravity     = Gravity.CENTER_VERTICAL

            val textCol = LinearLayout(context).apply {
                orientation  = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            textCol.addView(TextView(context).apply {
                text = "Clear Netmirror Cookies"; textSize = 15f
                setTypeface(null, android.graphics.Typeface.BOLD); setTextColor(theme.TEXT_PRIMARY)
            })
            textCol.addView(TextView(context).apply {
                text = "Remove saved Netmirror session cookies"
                textSize = 12f; setTextColor(theme.TEXT_SECONDARY); setPadding(0, 3.dp(context), 0, 0)
            })
            addView(textCol)
            addView(SettingsWidgets.dangerBtn(context, "Clear") {
                Settings.clearCookie()
                Toast.makeText(context, "🍪 Cookies cleared!", Toast.LENGTH_SHORT).show()
            })
        }
    }

    // =========================================================
    //  SHOWBOX TOKEN CARD
    // =========================================================

    private fun buildShowboxTokenCard(
        context: Context,
        pending: MutableMap<String, Any?>
    ): View {
        val theme         = SettingsTheme
        val SHOWBOX_ACCENT = Color.parseColor("#F59E0B")
        val SHOWBOX_BG    = Color.parseColor("#13100A")
        val SHOWBOX_BORDER = Color.parseColor("#3A2800")
        val CLIP_TEXT     = Color.parseColor("#94A3B8")
        val CLIP_BG       = Color.parseColor("#0F1520")
        val CLIP_BORDER   = Color.parseColor("#1E2A3A")

        val card = SettingsWidgets.cardContainer(context)

        var expanded = false
        val content  = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16.dp(context), 4.dp(context), 16.dp(context), 16.dp(context))
            visibility  = View.GONE
        }

        // Computed in two places (badge init + collapse); extract to avoid duplication
        fun savedBadgeText() = when {
            pending.containsKey(Settings.SHOWBOX_TOKEN_KEY) ->
                if ((pending[Settings.SHOWBOX_TOKEN_KEY] as? String) != null) "✓ Staged" else ""
            Settings.getShowboxToken() != null -> "✓ Saved"
            else -> ""
        }

        content.addView(TextView(context).apply {
            text = "Enter your Febbox token to enable ShowBox source"
            textSize = 12f; setTextColor(theme.TEXT_SECONDARY)
            setPadding(4.dp(context), 0, 4.dp(context), 10.dp(context))
        })

        val initialToken = (pending[Settings.SHOWBOX_TOKEN_KEY] as? String) ?: Settings.getShowboxToken() ?: ""
        val input = EditText(context).apply {
            hint = "Paste UI token"; setText(initialToken)
            setTextColor(theme.TEXT_PRIMARY); setHintTextColor(theme.TEXT_SECONDARY)
            textSize = 13f; setSingleLine(true)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            isFocusable = true; isFocusableInTouchMode = true
            setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) (v.parent?.parent as? ScrollView)?.requestChildFocus(v, v)
            }
            setPadding(14.dp(context), 12.dp(context), 14.dp(context), 12.dp(context))
            background = theme.inputBackground(context)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = 8.dp(context) }
        }
        content.addView(input)

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        content.addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = 10.dp(context) }

            addView(SettingsWidgets.pillBtn(context, "📋 Paste", CLIP_TEXT, CLIP_BG, CLIP_BORDER) {
                val clip = clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()?.trim()
                if (!clip.isNullOrBlank()) {
                    input.setText(clip); input.setSelection(input.text?.length ?: 0)
                    Toast.makeText(context, "Pasted from clipboard", Toast.LENGTH_SHORT).show()
                } else Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
            })
            addView(SettingsWidgets.hSpacer(context, 8))
            addView(SettingsWidgets.pillBtn(context, "📄 Copy", CLIP_TEXT, CLIP_BG, CLIP_BORDER) {
                val text = input.text?.toString()?.trim()
                if (!text.isNullOrBlank()) {
                    clipboard.setPrimaryClip(ClipData.newPlainText("Febbox Token", text))
                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                } else Toast.makeText(context, "Nothing to copy", Toast.LENGTH_SHORT).show()
            })
        })

        val savedBadge = TextView(context).apply {
            text = savedBadgeText()
            textSize = 10f; setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#4ADE80"))
            setPadding(0, 0, 8.dp(context), 0)
        }

        content.addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            var isVisible = false

            addView(TextView(context).apply {
                text = "👁 Show"; textSize = 11f
                setTypeface(null, android.graphics.Typeface.BOLD); setTextColor(theme.TEXT_SECONDARY)
                setPadding(0, 0, 12.dp(context), 0)
                isClickable = true; isFocusable = true; isFocusableInTouchMode = false
                setOnClickListener {
                    isVisible = !isVisible
                    input.inputType = if (isVisible)
                        android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    else
                        android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                    input.setSelection(input.text?.length ?: 0)
                    text = if (isVisible) "🙈 Hide" else "👁 Show"
                }
            })
            addView(View(context).apply { layoutParams = LinearLayout.LayoutParams(0, 1, 1f) })
            addView(SettingsWidgets.dangerBtn(context, "Clear") {
                input.setText(""); pending[Settings.SHOWBOX_TOKEN_KEY] = null; savedBadge.text = ""
                Toast.makeText(context, "Cleared — tap outer Save to apply", Toast.LENGTH_SHORT).show()
            })
            addView(SettingsWidgets.hSpacer(context, 8))
            addView(SettingsWidgets.pillBtn(context, "Save", SHOWBOX_ACCENT, SHOWBOX_BG, SHOWBOX_BORDER) {
                val token = input.text.toString().trim()
                if (token.isBlank()) Toast.makeText(context, "Token cannot be empty", Toast.LENGTH_SHORT).show()
                else {
                    pending[Settings.SHOWBOX_TOKEN_KEY] = token; savedBadge.text = "✓ Staged"
                    Toast.makeText(context, "✓ Staged — tap outer Save to apply", Toast.LENGTH_SHORT).show()
                }
            })
        })

        val chevron = TextView(context).apply { text = "▼"; textSize = 11f; setTextColor(theme.TEXT_SECONDARY) }

        card.addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20.dp(context), 16.dp(context), 16.dp(context), 16.dp(context))
            gravity     = Gravity.CENTER_VERTICAL
            isClickable = true; isFocusable = true; isFocusableInTouchMode = false
            background  = theme.stateDrawable(context)

            addView(SettingsWidgets.accentBar(context, SHOWBOX_ACCENT, Color.parseColor("#D97706")))
            addView(TextView(context).apply {
                text = "📦  Febbox Token"; textSize = 12f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(theme.TEXT_SECONDARY); letterSpacing = 0.08f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(savedBadge); addView(chevron)

            setOnClickListener {
                expanded = !expanded; chevron.text = if (expanded) "▲" else "▼"
                if (!expanded) savedBadge.text = savedBadgeText()
                SettingsWidgets.animateExpand(content, expanded)
            }
        })

        card.addView(content)
        SettingsWidgets.fadeInSlide(card)
        return card
    }

    // =========================================================
    //  STREMIO ADDONS CARD
    // =========================================================

    private fun buildStremioAddonsCard(context: Context, onRegisterCommit: (() -> Unit) -> Unit): View {
        val theme         = SettingsTheme
        val ADDON_ACCENT  = Color.parseColor("#22D3EE")
        val ADDON_BG      = Color.parseColor("#0A1820")
        val ADDON_BORDER  = Color.parseColor("#1A3040")
        val TYPE_HTTPS    = Color.parseColor("#4ADE80")
        val TYPE_TORRENT  = Color.parseColor("#FACC15")
        val TYPE_DEBRID   = Color.parseColor("#F472B6")
        val TYPE_SUBTITLE = Color.parseColor("#C084FC")

        fun typeColor(t: Settings.AddonType) = when (t) {
            Settings.AddonType.HTTPS     -> TYPE_HTTPS
            Settings.AddonType.TORRENT   -> TYPE_TORRENT
            Settings.AddonType.DEBRID    -> TYPE_DEBRID
            Settings.AddonType.SUBTITLE  -> TYPE_SUBTITLE
        }

        fun Settings.AddonType.next() = when (this) {
            Settings.AddonType.HTTPS     -> Settings.AddonType.TORRENT
            Settings.AddonType.TORRENT   -> Settings.AddonType.DEBRID
            Settings.AddonType.DEBRID    -> Settings.AddonType.SUBTITLE
            Settings.AddonType.SUBTITLE  -> Settings.AddonType.HTTPS
        }

        fun String.stripManifest() = trimEnd('/').removeSuffix("/manifest.json").trimEnd('/')

        val card = SettingsWidgets.cardContainer(context)

        var expanded = false
        val content  = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 8.dp(context)); visibility = View.GONE
        }

        val addons    = Settings.getStremioAddons()
        val addonRows = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }

        fun rebuildRows() {
            addonRows.removeAllViews()
            if (addons.isEmpty()) {
                addonRows.addView(TextView(context).apply {
                    text = "No addons yet — tap Add Addon to add one"
                    textSize = 12f; setTextColor(theme.TEXT_SECONDARY); gravity = Gravity.CENTER
                    setPadding(0, 16.dp(context), 0, 16.dp(context))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                }); return
            }
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

            addons.forEachIndexed { i, addon ->
                if (i > 0) addonRows.addView(SettingsWidgets.divider(context))

                val row = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(16.dp(context), 12.dp(context), 16.dp(context), 12.dp(context))
                }
                val header = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                    ).also { it.bottomMargin = 8.dp(context) }
                }

                val typePill = TextView(context).apply {
                    text = addon.type.name; textSize = 10f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setTextColor(typeColor(addon.type))
                    setPadding(10.dp(context), 4.dp(context), 10.dp(context), 4.dp(context))
                    background = GradientDrawable().apply {
                        cornerRadius = 99f; setColor(Color.parseColor("#0D1117"))
                        setStroke(1, typeColor(addon.type))
                    }
                    isClickable = true; isFocusable = true; isFocusableInTouchMode = false
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
                    ).also { it.marginEnd = 10.dp(context) }
                    setOnClickListener {
                        val next = addons[i].type.next()
                        addons[i] = addons[i].copy(type = next)
                        text = next.name; setTextColor(typeColor(next))
                        (background as? GradientDrawable)?.setStroke(1, typeColor(next))
                    }
                }
                header.addView(typePill)
                header.addView(TextView(context).apply {
                    text = addon.name.ifBlank { "Unnamed" }; textSize = 13f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setTextColor(if (addon.name.isBlank()) theme.TEXT_SECONDARY else theme.TEXT_PRIMARY)
                    maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
                header.addView(SettingsWidgets.dangerBtn(context, "✕") { addons.removeAt(i); rebuildRows() })
                row.addView(header)

                // Name field
                row.addView(labelText(context, "Name"))
                val nameField = EditText(context).apply {
                    setText(addon.name); hint = "e.g. Torrentio"; textSize = 13f
                    setTextColor(theme.TEXT_PRIMARY); setHintTextColor(theme.TEXT_SECONDARY); setSingleLine(true)
                    isFocusable = true; isFocusableInTouchMode = true
                    setOnFocusChangeListener { v, hasFocus ->
                        if (hasFocus) (v.parent?.parent?.parent?.parent as? ScrollView)?.requestChildFocus(v, v)
                    }
                    addTextChangedListener(simpleWatcher { addons[i] = addons[i].copy(name = it) })
                    setPadding(12.dp(context), 10.dp(context), 12.dp(context), 10.dp(context))
                    background = GradientDrawable().apply {
                        cornerRadius = 8f.dp(context); setColor(Color.parseColor("#0D1117"))
                        setStroke(1, Color.parseColor("#2E2850"))
                    }
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                    ).also { it.bottomMargin = 8.dp(context) }
                }
                row.addView(nameField)

                // URL field
                row.addView(labelText(context, "URL"))
                val urlField = EditText(context).apply {
                    setText(addon.url); hint = "https://xyz.com/manifest.json"; textSize = 12f
                    setTextColor(theme.TEXT_PRIMARY); setHintTextColor(theme.TEXT_SECONDARY); setSingleLine(true)
                    inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
                    isFocusable = true; isFocusableInTouchMode = true
                    setOnFocusChangeListener { v, hasFocus ->
                        if (hasFocus) (v.parent?.parent?.parent?.parent as? ScrollView)?.requestChildFocus(v, v)
                    }
                    addTextChangedListener(simpleWatcher {
                        addons[i] = addons[i].copy(url = it.stripManifest())
                    })
                    setPadding(12.dp(context), 10.dp(context), 12.dp(context), 10.dp(context))
                    background = GradientDrawable().apply {
                        cornerRadius = 8f.dp(context); setColor(Color.parseColor("#0D1117"))
                        setStroke(1, Color.parseColor("#2E2850"))
                    }
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                    ).also { it.bottomMargin = 6.dp(context) }
                }
                row.addView(urlField)

                val CLIP_TEXT   = Color.parseColor("#94A3B8")
                val CLIP_BG     = Color.parseColor("#0F1520")
                val CLIP_BORDER = Color.parseColor("#1E2A3A")
                row.addView(LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                    ).also { it.bottomMargin = 4.dp(context) }
                    addView(SettingsWidgets.pillBtn(context, "📋 Paste URL", CLIP_TEXT, CLIP_BG, CLIP_BORDER) {
                        val clip = clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()?.trim()
                        if (!clip.isNullOrBlank()) {
                            val stripped = clip.stripManifest()
                            urlField.setText(stripped); urlField.setSelection(urlField.text?.length ?: 0)
                            addons[i] = addons[i].copy(url = stripped)
                            Toast.makeText(context, "URL pasted", Toast.LENGTH_SHORT).show()
                        } else Toast.makeText(context, "Clipboard empty", Toast.LENGTH_SHORT).show()
                    })
                    addView(SettingsWidgets.hSpacer(context, 8))
                    addView(SettingsWidgets.pillBtn(context, "📄 Copy URL", CLIP_TEXT, CLIP_BG, CLIP_BORDER) {
                        val text = urlField.text?.toString()?.trim()
                        if (!text.isNullOrBlank()) {
                            clipboard.setPrimaryClip(ClipData.newPlainText("Addon URL", text))
                            Toast.makeText(context, "URL copied", Toast.LENGTH_SHORT).show()
                        } else Toast.makeText(context, "Nothing to copy", Toast.LENGTH_SHORT).show()
                    })
                })

                addonRows.addView(row)
            }
        }

        onRegisterCommit {
            val invalid = addons.indexOfFirst { it.url.isBlank() }
            if (invalid != -1)
                Toast.makeText(context, "Addon ${invalid + 1} has empty URL — not saved", Toast.LENGTH_SHORT).show()
            else
                Settings.saveStremioAddons(addons)
        }

        rebuildRows()

        val toolbar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(16.dp(context), 10.dp(context), 16.dp(context), 6.dp(context))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            addView(SettingsWidgets.pillBtn(context, "+ Add Addon", ADDON_ACCENT, ADDON_BG, ADDON_BORDER) {
                addons.add(Settings.StremioAddon(name = "", url = "", type = Settings.AddonType.HTTPS))
                rebuildRows()
            })
        }

        val sep = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
                .also { it.setMargins(16.dp(context), 4.dp(context), 16.dp(context), 0) }
            setBackgroundColor(theme.DIVIDER_COLOR)
        }

        val noteBanner = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(14.dp(context), 12.dp(context), 14.dp(context), 12.dp(context))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(16.dp(context), 6.dp(context), 16.dp(context), 8.dp(context)) }
            background = GradientDrawable().apply {
                cornerRadius = 10f.dp(context); setColor(Color.parseColor("#0C1A2E"))
                setStroke(2, Color.parseColor("#1D4ED8"))
            }
            addView(View(context).apply {
                layoutParams = LinearLayout.LayoutParams(8.dp(context), 8.dp(context))
                    .also { it.marginEnd = 10.dp(context); it.topMargin = 2.dp(context) }
                background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.parseColor("#3B82F6")) }
            })
            val noteCol = LinearLayout(context).apply {
                orientation  = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            noteCol.addView(TextView(context).apply {
                text = "Only IMDB Supported Addons"; textSize = 12f
                setTypeface(null, android.graphics.Typeface.BOLD); setTextColor(Color.parseColor("#60A5FA"))
            })
            noteCol.addView(TextView(context).apply {
                text = "Only addons that have support of IMDB & No catalog addons are supported"
                textSize = 11f; setTextColor(Color.parseColor("#93C5FD")); setPadding(0, 3.dp(context), 0, 0)
            })
            noteCol.addView(TextView(context).apply {
                text = "💡 Tap the type badge on each addon to cycle between HTTPS → TORRENT → DEBRID → SUBTITLE"
                textSize = 11f; setTextColor(Color.parseColor("#7DD3FC")); setPadding(0, 6.dp(context), 0, 0)
            })
            addView(noteCol)
        }

        content.addView(toolbar); content.addView(sep); content.addView(noteBanner); content.addView(addonRows)

        val chevron = TextView(context).apply { text = "▼"; textSize = 11f; setTextColor(theme.TEXT_SECONDARY) }
        val summary = TextView(context).apply {
            textSize = 11f; setTextColor(Color.parseColor("#5A5E7A"))
            setPadding(0, 0, 8.dp(context), 0)
            text = if (addons.isEmpty()) "" else "${addons.size} addon${if (addons.size == 1) "" else "s"}"
        }

        card.addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20.dp(context), 16.dp(context), 16.dp(context), 16.dp(context))
            gravity     = Gravity.CENTER_VERTICAL
            isClickable = true; isFocusable = true; isFocusableInTouchMode = false
            background  = theme.stateDrawable(context)

            addView(SettingsWidgets.accentBar(context, ADDON_ACCENT, Color.parseColor("#0891B2")))
            addView(TextView(context).apply {
                text = "🔌  Stremio Addons"; textSize = 12f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(theme.TEXT_SECONDARY); letterSpacing = 0.08f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(summary); addView(chevron)

            setOnClickListener {
                expanded = !expanded; chevron.text = if (expanded) "▲" else "▼"
                summary.text = if (addons.isEmpty()) "" else "${addons.size} addon${if (addons.size == 1) "" else "s"}"
                SettingsWidgets.animateExpand(content, expanded) {
                    summary.text = if (addons.isEmpty()) "" else "${addons.size} addon${if (addons.size == 1) "" else "s"}"
                }
            }
        })

        card.addView(content)
        SettingsWidgets.fadeInSlide(card)
        return card
    }

    // =========================================================
    //  CREDITS CARD
    // =========================================================

    private fun buildCreditsCard(context: Context): View {
        val theme         = SettingsTheme
        val CREDIT_ACCENT = Color.parseColor("#38BDF8")
        val CREDIT_BG     = Color.parseColor("#0A1420")
        val CREDIT_BORDER = Color.parseColor("#1A3040")

        val contributors = listOf(
            Triple("phisher98",     "For multi-source plugin inspiration", "github.com/phisher98"),
            Triple("AzartX47",      "For providing multiple APIs",         "github.com/AzartX47"),
            Triple("yogesh-hacker", "For providing reference",             "github.com/yogesh-hacker"),
        )

        val card = SettingsWidgets.cardContainer(context)

        var expanded = false
        val content  = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16.dp(context), 4.dp(context), 16.dp(context), 16.dp(context))
            visibility  = View.GONE
        }

        content.addView(TextView(context).apply {
            text = "Special thanks to these developers whose work served as reference and inspiration ❤️"
            textSize = 12f; setTextColor(theme.TEXT_SECONDARY)
            setPadding(4.dp(context), 4.dp(context), 4.dp(context), 14.dp(context))
        })

        contributors.forEachIndexed { i, (name, role, url) ->
            if (i > 0) content.addView(SettingsWidgets.divider(context))
            content.addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(4.dp(context), 12.dp(context), 4.dp(context), 12.dp(context))
                gravity     = Gravity.CENTER_VERTICAL

                val size = 36.dp(context)
                addView(TextView(context).apply {
                    text = name.first().uppercaseChar().toString()
                    textSize = 14f; setTypeface(null, android.graphics.Typeface.BOLD)
                    setTextColor(CREDIT_ACCENT); gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(size, size).also { it.marginEnd = 14.dp(context) }
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL; setColor(CREDIT_BG); setStroke(2, CREDIT_BORDER)
                    }
                })

                val col = LinearLayout(context).apply {
                    orientation  = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                col.addView(TextView(context).apply {
                    text = name; textSize = 14f
                    setTypeface(null, android.graphics.Typeface.BOLD); setTextColor(theme.TEXT_PRIMARY)
                })
                col.addView(TextView(context).apply {
                    text = role; textSize = 11f; setTextColor(theme.TEXT_SECONDARY)
                    setPadding(0, 2.dp(context), 0, 0)
                })
                addView(col)
                addView(SettingsWidgets.pillBtn(context, "GitHub", CREDIT_ACCENT, CREDIT_BG, CREDIT_BORDER) {
                    try {
                        context.startActivity(android.content.Intent(
                            android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://$url")))
                    } catch (_: Exception) {
                        Toast.makeText(context, url, Toast.LENGTH_SHORT).show()
                    }
                })
            })
        }

        val chevron = TextView(context).apply { text = "▼"; textSize = 11f; setTextColor(theme.TEXT_SECONDARY) }

        card.addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20.dp(context), 16.dp(context), 16.dp(context), 16.dp(context))
            gravity     = Gravity.CENTER_VERTICAL
            isClickable = true; isFocusable = true; isFocusableInTouchMode = false
            background  = theme.stateDrawable(context)

            addView(SettingsWidgets.accentBar(context, CREDIT_ACCENT, Color.parseColor("#0EA5E9")))
            addView(TextView(context).apply {
                text = "🙏  Credits & Thanks"; textSize = 12f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(theme.TEXT_SECONDARY); letterSpacing = 0.08f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(chevron)

            setOnClickListener {
                expanded = !expanded; chevron.text = if (expanded) "▲" else "▼"
                SettingsWidgets.animateExpand(content, expanded)
            }
        })

        card.addView(content)
        SettingsWidgets.fadeInSlide(card)
        return card
    }

    // =========================================================
    //  CONCURRENCY ROW
    // =========================================================

    private fun buildConcurrencyRow(
        context: Context,
        pending: MutableMap<String, Any?>
    ): View {
        val theme = SettingsTheme
        var currentVal = pending[Settings.CONCURRENCY_KEY] as? Int ?: Settings.getConcurrency()

        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20.dp(context), 14.dp(context), 16.dp(context), 14.dp(context))
            gravity = Gravity.CENTER_VERTICAL
            background = theme.stateDrawable(context)

            val textCol = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            textCol.addView(TextView(context).apply {
                text = "Scraping Concurrency"
                textSize = 15f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(theme.TEXT_PRIMARY)
            })
            textCol.addView(TextView(context).apply {
                text = "Number of providers run concurrently\n⚠️ For low end devices set it low"
                textSize = 12f
                setTextColor(theme.TEXT_SECONDARY)
                setPadding(0, 3.dp(context), 0, 0)
            })
            addView(textCol)

            val valText = TextView(context).apply {
                text = currentVal.toString()
                textSize = 15f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(theme.ACCENT_START)
                gravity = Gravity.CENTER
                minWidth = 32.dp(context)
            }

            addView(SettingsWidgets.pillBtn(context, " - ", theme.TEXT_PRIMARY, Color.parseColor("#1A1E28"), Color.parseColor("#2E2850")) {
                if (currentVal > 1) {
                    currentVal--
                    pending[Settings.CONCURRENCY_KEY] = currentVal
                    valText.text = currentVal.toString()
                }
            })
            addView(valText)
            addView(SettingsWidgets.pillBtn(context, " + ", theme.TEXT_PRIMARY, Color.parseColor("#1A1E28"), Color.parseColor("#2E2850")) {
                if (currentVal < 50) {
                    currentVal++
                    pending[Settings.CONCURRENCY_KEY] = currentVal
                    valText.text = currentVal.toString()
                }
            })
        }
    }

    // =========================================================
    //  HERO BANNER
    // =========================================================

    private fun buildHeroBanner(context: Context): View {
        val theme = SettingsTheme
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28.dp(context), 32.dp(context), 28.dp(context), 24.dp(context))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.parseColor("#1A1730"), theme.BG_DARK))

            addView(View(context).apply {
                layoutParams = LinearLayout.LayoutParams(48.dp(context), 4.dp(context))
                    .also { it.bottomMargin = 16.dp(context) }
                background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                    intArrayOf(theme.ACCENT_START, theme.ACCENT_END)).apply { cornerRadius = 99f }
            })
            addView(TextView(context).apply {
                text = "Plugin Settings"; textSize = 22f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(theme.TEXT_PRIMARY); letterSpacing = -0.02f
            })
            addView(TextView(context).apply {
                text = "Configure sources, catalogs & cookies"
                textSize = 13f; setTextColor(theme.TEXT_SECONDARY); setPadding(0, 6.dp(context), 0, 0)
            })
        }
    }

    private fun showRestartWarning(context: Context, onSave: () -> Unit) {
        AlertDialog.Builder(context)
            .setTitle("Restart Required ⚠️")
            .setMessage("You've changed your Active Catalogs.\n\nPlease fully close and reopen Cloudstream for providers to update.")
            .setPositiveButton("Got it") { _, _ -> onSave() }
            .setCancelable(false).show()
    }

    // =========================================================
    //  MICRO HELPERS
    // =========================================================

    private fun labelText(context: Context, text: String) = TextView(context).apply {
        this.text = text; textSize = 11f; setTextColor(SettingsTheme.TEXT_SECONDARY)
        setPadding(0, 0, 0, 4.dp(context))
    }

    private fun simpleWatcher(onChange: (String) -> Unit) = object : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) { onChange(s?.toString() ?: "") }
        override fun afterTextChanged(s: android.text.Editable?) {}
    }
}
