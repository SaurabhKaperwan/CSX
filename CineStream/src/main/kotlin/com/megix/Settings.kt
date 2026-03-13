package com.megix

import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.*
import android.graphics.drawable.*
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.*
import com.lagradost.cloudstream3.AcraApplication.Companion.getKey
import com.lagradost.cloudstream3.AcraApplication.Companion.setKey

object Settings {

    // --- THEME COLORS ---
    private val BG_DARK        = Color.parseColor("#0D0F14")
    private val BG_CARD        = Color.parseColor("#13161E")
    private val ACCENT_START   = Color.parseColor("#6C63FF")
    private val ACCENT_END     = Color.parseColor("#A855F7")
    private val TEXT_PRIMARY   = Color.parseColor("#F0F2FF")
    private val TEXT_SECONDARY = Color.parseColor("#7B82A0")
    private val SWITCH_ON      = Color.parseColor("#6C63FF")
    private val SWITCH_OFF     = Color.parseColor("#2A2D3E")
    private val DIVIDER_COLOR  = Color.parseColor("#1F2235")
    private val DANGER_COLOR   = Color.parseColor("#FF4E6A")

    // --- DATABASE KEYS: Global ---
    const val DOWNLOAD_ENABLE     = "DownloadEnable"
    const val PROVIDER_CINESTREAM = "ProviderCineStream"
    const val PROVIDER_SIMKL      = "ProviderSimkl"
    const val PROVIDER_TMDB       = "ProviderTmdb"
    private const val COOKIE_KEY    = "nf_cookie"
    private const val TIMESTAMP_KEY = "nf_cookie_timestamp"
    const val SHOWBOX_TOKEN_KEY     = "showbox_ui_token"
    const val STREMIO_ADDONS_KEY    = "stremio_addons"

    // --- DATABASE KEYS: Providers ---
    const val P_TORRENTIO     = "p_torrentio"
    const val P_TORRENTSDB    = "p_torrentsdb"
    const val P_ANIMETOSHO    = "p_animetosho"
    const val P_VIDFLIX       = "p_vidflix"
    const val P_MOVIEBOX      = "p_moviebox"
    const val P_WYZIESUBS     = "p_wyziesubs"
    const val P_STREMIOSUBS   = "p_stremiosubs"
    const val P_CINEMACITY    = "p_cinemacity"
    const val P_WEBSTREAMR    = "p_webstreamr"
    const val P_STREAMVIX     = "p_streamvix"
    const val P_NOTORRENT     = "p_notorrent"
    const val P_CASTLE        = "p_castle"
    const val P_CINE          = "p_cine"
    const val P_ALLMOVIELAND  = "p_allmovieland"
    const val P_MADPLAYCDN    = "p_madplaycdn"
    const val P_VIDFASTPRO    = "p_vidfastpro"
    const val P_HEXA          = "p_hexa"
    const val P_YFLIX         = "p_yflix"
    const val P_XPASS         = "p_xpass"
    const val P_PLAYSRC       = "p_playsrc"
    const val P_2EMBED        = "p_2embed"
    const val P_DRAMAFULL     = "p_dramafull"
    const val P_VIDEASY       = "p_videasy"
    const val P_CINEMAOS      = "p_cinemaos"
    const val P_VICSRCWTF     = "p_vicsrcwtf"
    const val P_VIDLINK       = "p_vidlink"
    const val P_MAPPLE        = "p_mapple"
    const val P_VIDSTACK      = "p_vidstack"
    const val P_KISSKH        = "p_kisskh"
    const val P_NETFLIX       = "p_netflix"
    const val P_PRIMEVIDEO    = "p_primevideo"
    const val P_DISNEY        = "p_disney"
    const val P_BOLLYWOOD     = "p_bollywood"
    const val P_VIDZEE        = "p_vidzee"
    const val P_XDMOVIES      = "p_xdmovies"
    const val P_4KHDHUB       = "p_4khdhub"
    const val P_FLIXINDIA     = "p_flixindia"
    const val P_MOVIESDRIVE   = "p_moviesdrive"
    const val P_VEGAMOVIES    = "p_vegamovies"
    const val P_ROGMOVIES     = "p_rogmovies"
    const val P_BOLLYFLIX     = "p_bollyflix"
    const val P_TOPMOVIES     = "p_topmovies"
    const val P_MOVIESMOD     = "p_moviesmod"
    const val P_MOVIES4U      = "p_movies4u"
    const val P_UHDMOVIES     = "p_uhdmovies"
    const val P_PRIMESRC      = "p_primesrc"
    const val P_PROJECTFREETV = "p_projectfreetv"
    const val P_HINDMOVIEZ    = "p_hindmoviez"
    const val P_LEVIDIA       = "p_levidia"
    const val P_DAHMERMOVIES  = "p_dahmermovies"
    const val P_MULTIMOVIES   = "p_multimovies"
    const val P_PROTONMOVIES  = "p_protonmovies"
    const val P_AKWAM         = "p_akwam"
    const val P_RTALLY        = "p_rtally"
    const val P_TOONSTREAM    = "p_toonstream"
    const val P_ASIAFLIX      = "p_asiaflix"
    const val P_SKYMOVIES     = "p_skymovies"
    const val P_HDMOVIE2      = "p_hdmovie2"
    const val P_MOSTRAGUARDA  = "p_mostraguarda"
    const val P_ALLANIME      = "p_allanime"
    const val P_SUDATCHI      = "p_sudatchi"
    const val P_TOKYOINSIDER  = "p_tokyoinsider"
    const val P_ANIZONE       = "p_anizone"
    const val P_ANIMES        = "p_animes"
    const val P_GOJO          = "p_gojo"
    const val P_ANIMEWORLD    = "p_animeworld"
    const val P_SHOWBOX       = "p_showbox"

    private const val PROVIDER_ORDER_KEY = "provider_order"
    private val TORRENT_KEYS = setOf(P_TORRENTIO, P_TORRENTSDB, P_ANIMETOSHO)

    val PROVIDER_NAMES = linkedMapOf(
        P_TORRENTIO     to "🧲 Torrentio",
        P_TORRENTSDB    to "🧲 TorrentsDB",
        P_ANIMETOSHO    to "🧲 AnimeTosho",
        P_WEBSTREAMR    to "WebStreamr",
        P_STREAMVIX     to "Streamvix",
        P_NOTORRENT     to "NoTorrent",
        P_CASTLE        to "Castle",
        P_CINE          to "Cine",
        P_ANIMEWORLD    to "AnimeWorld",
        P_SHOWBOX       to "ShowBox",
        P_VIDFLIX       to "Vidflix",
        P_MOVIEBOX      to "Moviebox",
        P_CINEMACITY    to "Cinemacity",
        P_ALLMOVIELAND  to "Allmovieland",
        P_MADPLAYCDN    to "MadplayCDN",
        P_VIDFASTPRO    to "VidFastPro",
        P_HEXA          to "Hexa",
        P_YFLIX         to "Yflix",
        P_XPASS         to "Xpass",
        P_PLAYSRC       to "Playsrc",
        P_2EMBED        to "2Embed",
        P_VIDEASY       to "Videasy",
        P_CINEMAOS      to "CinemaOS",
        P_VICSRCWTF     to "VicSrcWtf",
        P_VIDLINK       to "Vidlink",
        P_MAPPLE        to "Mapple",
        P_VIDSTACK      to "Vidstack",
        P_VIDZEE        to "Vidzee",
        P_WYZIESUBS     to "WYZIESubs",
        P_STREMIOSUBS   to "StremioSubs",
        P_NETFLIX       to "Netflix",
        P_PRIMEVIDEO    to "Prime Video",
        P_DISNEY        to "Hotstar",
        P_BOLLYWOOD     to "Gramcinema",
        P_FLIXINDIA     to "FlixIndia",
        P_VEGAMOVIES    to "VegaMovies",
        P_ROGMOVIES     to "RogMovies",
        P_BOLLYFLIX     to "Bollyflix",
        P_TOPMOVIES     to "TopMovies",
        P_MOVIESMOD     to "Moviessmod",
        P_MOVIES4U      to "Movies4u",
        P_UHDMOVIES     to "UHDMovies",
        P_MOVIESDRIVE   to "MoviesDrive",
        P_HINDMOVIEZ    to "Hindmoviez",
        P_4KHDHUB       to "4KHDHub",
        P_XDMOVIES      to "XDMovies",
        P_PRIMESRC      to "PrimeSrc",
        P_PROJECTFREETV to "ProjectFreeTV",
        P_LEVIDIA       to "Levidia",
        P_DAHMERMOVIES  to "DahmerMovies",
        P_MULTIMOVIES   to "Multimovies",
        P_PROTONMOVIES  to "Protonmovies",
        P_AKWAM         to "Akwam",
        P_RTALLY        to "Rtally",
        P_ASIAFLIX      to "Asiaflix",
        P_SKYMOVIES     to "SkyMovies",
        P_HDMOVIE2      to "HDMovie2",
        P_MOSTRAGUARDA  to "Mostraguarda",
        P_TOONSTREAM    to "Toonstream",
        P_ALLANIME      to "AllAnime",
        P_SUDATCHI      to "Sudatchi",
        P_TOKYOINSIDER  to "TokyoInsider",
        P_ANIZONE       to "Anizone",
        P_ANIMES        to "Animes",
        P_GOJO          to "Animetsu",
        P_KISSKH        to "KissKH",
        P_DRAMAFULL     to "Dramafull",
    )

    private val DEFAULT_ORDER = PROVIDER_NAMES.keys.toList()

    fun enabled(key: String): Boolean = getKey<Boolean>(key) ?: (key !in TORRENT_KEYS)

    fun getOrder(): List<String> {
        val stremioKeys = getStremioAddons().map { stremioAddonKey(it.name) }
        val allKnown = DEFAULT_ORDER + stremioKeys
        val saved = getKey<String>(PROVIDER_ORDER_KEY)
            ?.split(",")?.filter { it.isNotBlank() }
            ?: return allKnown
        return saved + (allKnown - saved.toSet())
    }

    fun saveOrder(order: List<String>) = setKey(PROVIDER_ORDER_KEY, order.joinToString(","))

    // Key for a stremio addon: "stremio_<name normalised>"
    fun stremioAddonKey(name: String): String =
        "stremio_${name.trim().lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')}"

    fun providerDisplayName(key: String): String {
        PROVIDER_NAMES[key]?.let { return it }
        if (key.startsWith("stremio_")) {
            val addon = getStremioAddons().firstOrNull { stremioAddonKey(it.name) == key }
            if (addon != null) {
                val icon = when (addon.type) {
                    AddonType.TORRENT  -> "🧲"
                    AddonType.DEBRID   -> "☁️"
                    AddonType.SUBTITLE -> "📝"
                    AddonType.HTTPS    -> "🔌"
                }
                return "$icon ${addon.name}"
            }
            return "🔌 " + key.removePrefix("stremio_").replace("_", " ")
                .replaceFirstChar { it.uppercaseChar() }
        }
        return key
    }

    // True when the key belongs to a stremio addon of TORRENT type
    fun isStremioTorrent(key: String): Boolean {
        if (!key.startsWith("stremio_")) return false
        return getStremioAddons().firstOrNull { stremioAddonKey(it.name) == key }
            ?.type == AddonType.TORRENT
    }

    // =========================================================
    // NETMIRROR COOKIE HELPERS
    // =========================================================

    fun saveCookie(cookie: String) {
        setKey(COOKIE_KEY, cookie)
        setKey(TIMESTAMP_KEY, System.currentTimeMillis())
    }

    fun getCookie(): Pair<String?, Long> =
        Pair(getKey<String>(COOKIE_KEY), getKey<Long>(TIMESTAMP_KEY) ?: 0L)

    fun clearCookie() {
        setKey(COOKIE_KEY, null)
        setKey(TIMESTAMP_KEY, null)
    }

    fun saveShowboxToken(token: String) = setKey(SHOWBOX_TOKEN_KEY, token.trim())
    fun getShowboxToken(): String?       = getKey<String>(SHOWBOX_TOKEN_KEY)?.takeIf { it.isNotBlank() }
    fun clearShowboxToken()              = setKey(SHOWBOX_TOKEN_KEY, null)

    // =========================================================
    //  STREMIO ADDON HELPERS
    // =========================================================

    enum class AddonType { HTTPS, TORRENT, DEBRID, SUBTITLE }

    data class StremioAddon(
        val name: String,
        val url: String,
        val type: AddonType
    )

    fun getStremioAddons(): MutableList<StremioAddon> {
        val raw = getKey<String>(STREMIO_ADDONS_KEY) ?: return mutableListOf()
        return raw.lines()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split("|")
                if (parts.size < 3) return@mapNotNull null
                val type = runCatching { AddonType.valueOf(parts[2]) }.getOrDefault(AddonType.HTTPS)
                StremioAddon(name = parts[0], url = parts[1], type = type)
            }.toMutableList()
    }

    fun saveStremioAddons(addons: List<StremioAddon>) {
        if (addons.isEmpty()) { setKey(STREMIO_ADDONS_KEY, null as String?) }
        else setKey(STREMIO_ADDONS_KEY, addons.joinToString("\n") { "${it.name}|${it.url}|${it.type}" })
        val validKeys = addons.map { stremioAddonKey(it.name) }.toSet()
        val currentOrder = getKey<String>(PROVIDER_ORDER_KEY)
            ?.split(",")?.filter { it.isNotBlank() } ?: return
        val pruned = currentOrder.filter { key ->
            !key.startsWith("stremio_") || key in validKeys
        }
        if (pruned.size != currentOrder.size) saveOrder(pruned)
    }


    // =========================================================
    //  SETTINGS DIALOG
    // =========================================================

    fun showSettingsDialog(context: Context, onSave: () -> Unit) {
        var requiresRestart = false
        val pendingChanges = mutableMapOf<String, Any?>()
        var commitOrder: () -> Unit = {}
        var commitAddons: () -> Unit = {}

        val scroll = ScrollView(context).apply {
            isScrollbarFadingEnabled = true
            background = ColorDrawable(BG_DARK)
            isFocusable = false
            descendantFocusability = ScrollView.FOCUS_AFTER_DESCENDANTS
        }

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 24.dp(context))
            background = ColorDrawable(BG_DARK)
        }

        layout.addView(createHeroBanner(context))
        layout.addView(View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 8.dp(context))
        })

        // ── Scraping Settings ──
        layout.addView(createCollapsibleCard(context, "⚙️  Scraping Settings",
            accentA = Color.parseColor("#06B6D4"), accentB = Color.parseColor("#0891B2")) {
            addView(createToggleRow(context, "Download Only Links",
                "Only great for downloading (Not for Streaming)",
                DOWNLOAD_ENABLE, false, pendingChanges))
            addView(createDivider(context))
            addView(createCookieClearRow(context))
        })

        // ── Febbox / ShowBox Token ──
        layout.addView(createShowboxTokenCard(context, pendingChanges))

        // ── Restart banner ──
        val restartBanner = createRestartBanner(context).also { it.visibility = View.GONE }

        // ── Active Catalogs ──
        val onCatalogChanged = {
            requiresRestart = true
            if (restartBanner.visibility == View.GONE) {
                restartBanner.visibility = View.VISIBLE
                restartBanner.alpha = 0f
                restartBanner.translationY = (-12f).dp(context)
                restartBanner.animate()
                    .alpha(1f).translationY(0f)
                    .setDuration(350).setInterpolator(DecelerateInterpolator()).start()
            }
        }
        layout.addView(createCollapsibleCard(context, "📡  Active Catalogs",
            accentA = Color.parseColor("#10B981"), accentB = Color.parseColor("#059669")) {
            addView(createToggleRow(context, "CineStream", "Cinemeta catalog",
                PROVIDER_CINESTREAM, true, pendingChanges, onCatalogChanged))
            addView(createDivider(context))
            addView(createToggleRow(context, "CineSimkl", "Simkl catalog",
                PROVIDER_SIMKL, true, pendingChanges, onCatalogChanged))
            addView(createDivider(context))
            addView(createToggleRow(context, "CineTmdb", "TMDB catalog",
                PROVIDER_TMDB, true, pendingChanges, onCatalogChanged))
        })

        layout.addView(restartBanner)

        // ── Providers ──
        layout.addView(createProvidersCard(context, pendingChanges) { commit ->
            commitOrder = commit
        })

        // ── Stremio Addons ──
        layout.addView(createStremioAddonsCard(context) { commit -> commitAddons = commit })

        // ── Credits ──
        layout.addView(createCreditsCard(context))

        scroll.addView(layout)

        val dialog = AlertDialog.Builder(context, android.R.style.Theme_Material_Dialog)
            .setView(scroll)
            .setPositiveButton("Save") { _, _ ->
                pendingChanges.forEach { (key, value) ->
                    when {
                        key == SHOWBOX_TOKEN_KEY && value == null   -> clearShowboxToken()
                        key == SHOWBOX_TOKEN_KEY && value is String -> saveShowboxToken(value)
                        value is Boolean                            -> setKey(key, value)
                        value == null                               -> setKey(key, null as String?)
                    }
                }
                commitAddons()
                commitOrder()
                if (requiresRestart) showRestartWarning(context, onSave) else onSave()
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.window?.setBackgroundDrawable(roundRect(BG_DARK, 20f.dp(context)))
        dialog.show()
        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.95).toInt(),
            android.view.WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            ?.apply { setTextColor(ACCENT_START); isAllCaps = false }
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            ?.apply { setTextColor(TEXT_SECONDARY); isAllCaps = false }
    }

    // =========================================================
    //  COOKIE CLEAR ROW
    // =========================================================

    private fun createCookieClearRow(context: Context): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20.dp(context), 14.dp(context), 16.dp(context), 14.dp(context))
            gravity = Gravity.CENTER_VERTICAL

            val textCol = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            textCol.addView(TextView(context).apply {
                text = "Clear Netmirror Cookies"; textSize = 15f
                setTypeface(null, android.graphics.Typeface.BOLD); setTextColor(TEXT_PRIMARY)
            })
            textCol.addView(TextView(context).apply {
                text = "Remove saved Netmirror session cookies"
                textSize = 12f; setTextColor(TEXT_SECONDARY); setPadding(0, 3.dp(context), 0, 0)
            })
            addView(textCol)

            addView(pillBtn(context, "Clear", DANGER_COLOR,
                Color.parseColor("#1A0A0D"), Color.parseColor("#3A1520")) {
                clearCookie()
                Toast.makeText(context, "🍪 Cookies cleared!", Toast.LENGTH_SHORT).show()
            })
        }
    }

    // =========================================================
    //  SHOWBOX TOKEN CARD
    // =========================================================

    private fun createShowboxTokenCard(
        context: Context,
        pendingChanges: MutableMap<String, Any?>
    ): View {
        val SHOWBOX_ACCENT = Color.parseColor("#F59E0B")
        val SHOWBOX_BG     = Color.parseColor("#13100A")
        val SHOWBOX_BORDER = Color.parseColor("#3A2800")

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val m = 16.dp(context)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(m, 0, m, m) }
            background = roundRect(BG_CARD, 16f.dp(context)); elevation = 4f
        }

        var expanded = false

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16.dp(context), 4.dp(context), 16.dp(context), 16.dp(context))
            visibility = View.GONE
        }

        content.addView(TextView(context).apply {
            text = "Enter your Febbox token to enable ShowBox source"
            textSize = 12f; setTextColor(TEXT_SECONDARY)
            setPadding(4.dp(context), 0, 4.dp(context), 10.dp(context))
        })

        val initialToken = (pendingChanges[SHOWBOX_TOKEN_KEY] as? String) ?: getShowboxToken() ?: ""

        val input = EditText(context).apply {
            hint = "Paste UI token"
            setText(initialToken)
            setTextColor(TEXT_PRIMARY); setHintTextColor(TEXT_SECONDARY)
            textSize = 13f; setSingleLine(true)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            isFocusable = true; isFocusableInTouchMode = true
            setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) (v.parent?.parent as? ScrollView)?.requestChildFocus(v, v)
            }
            setPadding(14.dp(context), 12.dp(context), 14.dp(context), 12.dp(context))
            background = GradientDrawable().apply {
                cornerRadius = 10f.dp(context)
                setColor(Color.parseColor("#0D1117"))
                setStroke(1, Color.parseColor("#2E2850"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = 8.dp(context) }
        }
        content.addView(input)

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val CLIP_TEXT   = Color.parseColor("#94A3B8")
        val CLIP_BG     = Color.parseColor("#0F1520")
        val CLIP_BORDER = Color.parseColor("#1E2A3A")

        content.addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = 10.dp(context) }

            addView(pillBtn(context, "📋 Paste", CLIP_TEXT, CLIP_BG, CLIP_BORDER) {
                val clip = clipboard.primaryClip
                    ?.getItemAt(0)?.coerceToText(context)?.toString()?.trim()
                if (!clip.isNullOrBlank()) {
                    input.setText(clip)
                    input.setSelection(input.text?.length ?: 0)
                    Toast.makeText(context, "Pasted from clipboard", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                }
            })
            addView(View(context).apply { layoutParams = LinearLayout.LayoutParams(8.dp(context), 1) })

            addView(pillBtn(context, "📄 Copy", CLIP_TEXT, CLIP_BG, CLIP_BORDER) {
                val text = input.text?.toString()?.trim()
                if (!text.isNullOrBlank()) {
                    clipboard.setPrimaryClip(ClipData.newPlainText("Febbox Token", text))
                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Nothing to copy", Toast.LENGTH_SHORT).show()
                }
            })
        })

        val savedBadge = TextView(context).apply {
            text = when {
                pendingChanges.containsKey(SHOWBOX_TOKEN_KEY) ->
                    if ((pendingChanges[SHOWBOX_TOKEN_KEY] as? String) != null) "✓ Staged" else ""
                getShowboxToken() != null -> "✓ Saved"
                else -> ""
            }
            textSize = 10f; setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#4ADE80"))
            setPadding(0, 0, 8.dp(context), 0)
        }

        content.addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            var isVisible = false
            addView(TextView(context).apply {
                text = "👁 Show"; textSize = 11f
                setTypeface(null, android.graphics.Typeface.BOLD); setTextColor(TEXT_SECONDARY)
                setPadding(0, 0, 12.dp(context), 0)
                isClickable = true; isFocusable = true; isFocusableInTouchMode = false
                setOnClickListener {
                    isVisible = !isVisible
                    input.inputType = if (isVisible)
                        android.text.InputType.TYPE_CLASS_TEXT or
                                android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    else
                        android.text.InputType.TYPE_CLASS_TEXT or
                                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                    input.setSelection(input.text?.length ?: 0)
                    text = if (isVisible) "🙈 Hide" else "👁 Show"
                }
            })
            addView(View(context).apply { layoutParams = LinearLayout.LayoutParams(0, 1, 1f) })

            addView(pillBtn(context, "Clear", DANGER_COLOR,
                Color.parseColor("#1A0A0D"), Color.parseColor("#3A1520")) {
                input.setText("")
                pendingChanges[SHOWBOX_TOKEN_KEY] = null
                savedBadge.text = ""
                Toast.makeText(context, "Cleared — tap outer Save to apply",
                    Toast.LENGTH_SHORT).show()
            })
            addView(View(context).apply { layoutParams = LinearLayout.LayoutParams(8.dp(context), 1) })

            addView(pillBtn(context, "Save", SHOWBOX_ACCENT, SHOWBOX_BG, SHOWBOX_BORDER) {
                val token = input.text.toString().trim()
                if (token.isBlank()) {
                    Toast.makeText(context, "Token cannot be empty", Toast.LENGTH_SHORT).show()
                } else {
                    pendingChanges[SHOWBOX_TOKEN_KEY] = token
                    savedBadge.text = "✓ Staged"
                    Toast.makeText(context, "✓ Staged — tap outer Save to apply",
                        Toast.LENGTH_SHORT).show()
                }
            })
        })

        val chevron = TextView(context).apply {
            text = "▼"; textSize = 11f; setTextColor(TEXT_SECONDARY)
        }

        card.addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20.dp(context), 16.dp(context), 16.dp(context), 16.dp(context))
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true; isFocusable = true; isFocusableInTouchMode = false; background = stateDrawable(context)

            addView(View(context).apply {
                layoutParams = LinearLayout.LayoutParams(3.dp(context), 18.dp(context))
                    .also { it.marginEnd = 12.dp(context) }
                background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(SHOWBOX_ACCENT, Color.parseColor("#D97706")))
                    .apply { cornerRadius = 99f }
            })
            addView(TextView(context).apply {
                text = "📦  Febbox Token"; textSize = 12f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(TEXT_SECONDARY); letterSpacing = 0.08f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(savedBadge); addView(chevron)

            setOnClickListener {
                expanded = !expanded; chevron.text = if (expanded) "▲" else "▼"
                if (!expanded) {
                    savedBadge.text = when {
                        pendingChanges.containsKey(SHOWBOX_TOKEN_KEY) ->
                            if ((pendingChanges[SHOWBOX_TOKEN_KEY] as? String) != null) "✓ Staged" else ""
                        getShowboxToken() != null -> "✓ Saved"
                        else -> ""
                    }
                }
                if (expanded) {
                    content.visibility = View.VISIBLE; content.alpha = 0f
                    content.animate().alpha(1f).setDuration(200).start()
                } else {
                    content.animate().alpha(0f).setDuration(150).withEndAction {
                        content.visibility = View.GONE; content.alpha = 1f
                    }.start()
                }
            }
        })

        card.addView(content)
        card.alpha = 0f; card.translationY = 20f
        card.animate().alpha(1f).translationY(0f)
            .setDuration(300).setInterpolator(DecelerateInterpolator()).start()
        return card
    }

    // =========================================================
    //  COLLAPSIBLE CARD
    // =========================================================

    private fun createCollapsibleCard(
        context: Context,
        title: String,
        startExpanded: Boolean = false,
        accentA: Int = ACCENT_START,
        accentB: Int = ACCENT_END,
        block: LinearLayout.() -> Unit
    ): View {
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val m = 16.dp(context)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(m, 0, m, m) }
            background = roundRect(BG_CARD, 16f.dp(context)); elevation = 4f
        }

        var expanded = startExpanded
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 8.dp(context))
            visibility = if (expanded) View.VISIBLE else View.GONE
        }
        content.block()

        val chevron = TextView(context).apply {
            text = if (expanded) "▲" else "▼"; textSize = 11f; setTextColor(TEXT_SECONDARY)
        }

        card.addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20.dp(context), 16.dp(context), 16.dp(context), 16.dp(context))
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true; isFocusable = true; isFocusableInTouchMode = false
            background = stateDrawable(context)

            addView(View(context).apply {
                layoutParams = LinearLayout.LayoutParams(3.dp(context), 18.dp(context))
                    .also { it.marginEnd = 12.dp(context) }
                background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(accentA, accentB)).apply { cornerRadius = 99f }
            })
            addView(TextView(context).apply {
                text = title; textSize = 12f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(TEXT_SECONDARY); letterSpacing = 0.08f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(chevron)

            setOnClickListener {
                expanded = !expanded; chevron.text = if (expanded) "▲" else "▼"
                if (expanded) {
                    content.visibility = View.VISIBLE; content.alpha = 0f
                    content.animate().alpha(1f).setDuration(200).start()
                } else {
                    content.animate().alpha(0f).setDuration(150).withEndAction {
                        content.visibility = View.GONE; content.alpha = 1f
                    }.start()
                }
            }
        })

        card.addView(content)
        card.alpha = 0f; card.translationY = 20f
        card.animate().alpha(1f).translationY(0f)
            .setDuration(300).setInterpolator(DecelerateInterpolator()).start()
        return card
    }

    // =========================================================
    //  PROVIDERS CARD
    // =========================================================

    private fun createProvidersCard(
        context: Context,
        pendingChanges: MutableMap<String, Any?>,
        onRegisterCommit: (() -> Unit) -> Unit
    ): View {
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val m = 16.dp(context)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(m, 0, m, m) }
            background = roundRect(BG_CARD, 16f.dp(context)); elevation = 4f
        }

        var expanded = false
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 8.dp(context)); visibility = View.GONE
        }

        val rows  = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val order = getOrder().toMutableList()

        onRegisterCommit {
            val stremioKeys = getStremioAddons().map { stremioAddonKey(it.name) }.toSet()
            val merged = order
                .filter { key -> !key.startsWith("stremio_") || key in stremioKeys }
                .let { filtered -> filtered + (stremioKeys - filtered.toSet()) }
            saveOrder(merged)
        }

        fun providerEnabled(key: String): Boolean =
            pendingChanges[key] as? Boolean ?: (getKey<Boolean>(key)
                ?: (key !in TORRENT_KEYS && !isStremioTorrent(key)))

        fun rebuild() {
            rows.removeAllViews()
            order.forEachIndexed { i, key ->
                if (i > 0) rows.addView(createDivider(context))
                rows.addView(createProviderRow(
                    context        = context,
                    label          = providerDisplayName(key),
                    key            = key,
                    index          = i + 1,
                    totalCount     = order.size,
                    isTorrent      = key in TORRENT_KEYS || isStremioTorrent(key),
                    canMoveUp      = i > 0,
                    canMoveDown    = i < order.lastIndex,
                    pendingChanges = pendingChanges,
                    onMoveUp   = { order.add(i - 1, order.removeAt(i)); rebuild() },
                    onMoveDown = { order.add(i + 1, order.removeAt(i)); rebuild() },
                    onMoveTo   = { target ->
                        val item = order.removeAt(i)
                        order.add(target.coerceIn(0, order.size), item); rebuild()
                    }
                ))
            }
        }

        val pillRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = 6.dp(context) }
        }

        // ✓ All — stage true, no saveOrder()
        pillRow.addView(pillBtn(context, "✓ All",
            Color.parseColor("#4ADE80"), Color.parseColor("#0A1A0F"),
            Color.parseColor("#1A3A1F")) {
            order.forEach { pendingChanges[it] = true }; rebuild()
            Toast.makeText(context, "All providers enabled", Toast.LENGTH_SHORT).show()
        })
        pillRow.addView(View(context).apply { layoutParams = LinearLayout.LayoutParams(8.dp(context), 1) })

        // ✕ None — stage false, no saveOrder()
        pillRow.addView(pillBtn(context, "✕ None", DANGER_COLOR,
            Color.parseColor("#1A0A0D"), Color.parseColor("#3A1520")) {
            order.forEach { pendingChanges[it] = false }; rebuild()
            Toast.makeText(context, "All providers disabled", Toast.LENGTH_SHORT).show()
        })
        pillRow.addView(View(context).apply { layoutParams = LinearLayout.LayoutParams(8.dp(context), 1) })

        // ↺ Reset Order — reset in-memory list only, no saveOrder()
        pillRow.addView(pillBtn(context, "↺ Reset Order", ACCENT_START,
            Color.parseColor("#1A1730"), Color.parseColor("#2E2850")) {
            val stremioKeys = getStremioAddons().map { stremioAddonKey(it.name) }
            order.clear(); order.addAll(DEFAULT_ORDER + stremioKeys); rebuild()
            Toast.makeText(context, "Order reset — tap Save to apply", Toast.LENGTH_SHORT).show()
        })

        val toolbar = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16.dp(context), 8.dp(context), 16.dp(context), 4.dp(context))
            addView(pillRow)
            addView(TextView(context).apply {
                text = "🧲 = off by default  ·  🔌 = Stremio addon  ·  ↑↓ or # = order"
                textSize = 10f; setTextColor(Color.parseColor("#44475A"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = 4.dp(context) }
            })
        }

        val sep = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
                .also { it.setMargins(16.dp(context), 0, 16.dp(context), 4.dp(context)) }
            setBackgroundColor(DIVIDER_COLOR)
        }

        rebuild()
        content.addView(toolbar); content.addView(sep); content.addView(rows)

        val chevron = TextView(context).apply {
            text = "▼"; textSize = 11f; setTextColor(TEXT_SECONDARY)
        }
        val summary = TextView(context).apply {
            textSize = 11f; setTextColor(Color.parseColor("#5A5E7A"))
            setPadding(0, 0, 8.dp(context), 0)
        }
        fun updateSummary() {
            summary.text = "${order.count { providerEnabled(it) }} / ${order.size} on"
        }
        updateSummary()

        card.addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20.dp(context), 16.dp(context), 16.dp(context), 16.dp(context))
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true; isFocusable = true; isFocusableInTouchMode = false; background = stateDrawable(context)

            addView(View(context).apply {
                layoutParams = LinearLayout.LayoutParams(3.dp(context), 18.dp(context))
                    .also { it.marginEnd = 12.dp(context) }
                background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(ACCENT_START, ACCENT_END)).apply { cornerRadius = 99f }
            })
            addView(TextView(context).apply {
                text = "🎬  Providers"; textSize = 12f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(TEXT_SECONDARY); letterSpacing = 0.08f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(summary); addView(chevron)

            setOnClickListener {
                expanded = !expanded; chevron.text = if (expanded) "▲" else "▼"
                updateSummary()
                if (expanded) {
                    content.visibility = View.VISIBLE; content.alpha = 0f
                    content.animate().alpha(1f).setDuration(220).start()
                } else {
                    content.animate().alpha(0f).setDuration(160).withEndAction {
                        content.visibility = View.GONE; content.alpha = 1f; updateSummary()
                    }.start()
                }
            }
        })

        card.addView(content)
        card.alpha = 0f; card.translationY = 20f
        card.animate().alpha(1f).translationY(0f)
            .setDuration(300).setInterpolator(DecelerateInterpolator()).start()
        return card
    }

    // =========================================================
    //  PROVIDER ROW
    // =========================================================

    private fun createProviderRow(
        context: Context, label: String, key: String,
        index: Int, totalCount: Int, isTorrent: Boolean,
        canMoveUp: Boolean, canMoveDown: Boolean,
        pendingChanges: MutableMap<String, Any?>,
        onMoveUp: () -> Unit, onMoveDown: () -> Unit,
        onMoveTo: (Int) -> Unit
    ): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16.dp(context), 10.dp(context), 12.dp(context), 10.dp(context))
            gravity = Gravity.CENTER_VERTICAL

            addView(TextView(context).apply {
                text = "$index"; textSize = 11f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(if (isTorrent) Color.parseColor("#5A3E1E") else ACCENT_START)
                gravity = Gravity.CENTER
                setPadding(4.dp(context), 4.dp(context), 4.dp(context), 4.dp(context))
                background = GradientDrawable().apply {
                    cornerRadius = 6f.dp(context); setColor(Color.parseColor("#1A1730"))
                    setStroke(1, if (isTorrent) Color.parseColor("#3A2810")
                    else Color.parseColor("#2E2850"))
                }
                minWidth = 28.dp(context)
                isClickable = true; isFocusable = true; isFocusableInTouchMode = false
                setOnClickListener {
                    val moveInput = EditText(context).apply {
                        inputType = android.text.InputType.TYPE_CLASS_NUMBER
                        hint = "1 – $totalCount"; setText("$index")
                        setTextColor(TEXT_PRIMARY); setHintTextColor(TEXT_SECONDARY); selectAll()
                        setPadding(16.dp(context), 12.dp(context), 16.dp(context), 12.dp(context))
                        isFocusable = true; isFocusableInTouchMode = true
                        background = GradientDrawable().apply {
                            cornerRadius = 10f.dp(context); setColor(Color.parseColor("#1A1E28"))
                            setStroke(1, Color.parseColor("#2E2850"))
                        }
                    }
                    val wrapper = LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(24.dp(context), 16.dp(context), 24.dp(context), 8.dp(context))
                        addView(TextView(context).apply {
                            text = "Move \"$label\" to position"; textSize = 13f
                            setTextColor(TEXT_SECONDARY); setPadding(0, 0, 0, 10.dp(context))
                        })
                        addView(moveInput)
                    }
                    val d = AlertDialog.Builder(context, android.R.style.Theme_Material_Dialog)
                        .setView(wrapper)
                        .setPositiveButton("Move") { _, _ ->
                            val t = moveInput.text.toString().toIntOrNull()
                            if (t != null && t in 1..totalCount) onMoveTo(t - 1)
                            else Toast.makeText(context, "Enter 1 – $totalCount",
                                Toast.LENGTH_SHORT).show()
                        }
                        .setNegativeButton("Cancel", null).create()
                    d.window?.setBackgroundDrawable(roundRect(BG_DARK, 16f.dp(context))); d.show()
                    d.getButton(AlertDialog.BUTTON_POSITIVE)
                        ?.apply { setTextColor(ACCENT_START); isAllCaps = false }
                    d.getButton(AlertDialog.BUTTON_NEGATIVE)
                        ?.apply { setTextColor(TEXT_SECONDARY); isAllCaps = false }
                    moveInput.requestFocus()
                    (context.getSystemService(Context.INPUT_METHOD_SERVICE)
                            as android.view.inputmethod.InputMethodManager)
                        .showSoftInput(moveInput,
                            android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
                }
            })

            addView(View(context).apply { layoutParams = LinearLayout.LayoutParams(10.dp(context), 1) })

            addView(TextView(context).apply {
                text = label; textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(if (isTorrent) Color.parseColor("#C87C3A") else TEXT_PRIMARY)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })

            fun arrowBtn(sym: String, active: Boolean, action: () -> Unit) =
                TextView(context).apply {
                    text = sym; textSize = 17f; gravity = Gravity.CENTER
                    setTextColor(if (active) ACCENT_START else Color.parseColor("#252840"))
                    setPadding(9.dp(context), 6.dp(context), 9.dp(context), 6.dp(context))
                    isClickable = active
                    isFocusable = active
                    isFocusableInTouchMode = false
                    if (active) {
                        background = stateDrawable(context)
                        setOnClickListener {
                            animate().scaleX(0.75f).scaleY(0.75f).setDuration(60).withEndAction {
                                animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                            }.start(); action()
                        }
                    }
                }
            addView(arrowBtn("↑", canMoveUp, onMoveUp))
            addView(arrowBtn("↓", canMoveDown, onMoveDown))

            val effectiveChecked = pendingChanges[key] as? Boolean
                ?: getKey<Boolean>(key) ?: (key !in TORRENT_KEYS)

            val sw = Switch(context).apply {
                isChecked = effectiveChecked
                isClickable = false
                isFocusable = false
                isFocusableInTouchMode = false
                thumbTintList = android.content.res.ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                    intArrayOf(Color.WHITE, Color.parseColor("#9099B8"))
                )
                trackTintList = android.content.res.ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                    intArrayOf(
                        if (isTorrent) Color.parseColor("#8B5E3C") else SWITCH_ON,
                        SWITCH_OFF
                    )
                )
            }
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                isClickable = true
                isFocusable = true
                isFocusableInTouchMode = false
                background = stateDrawable(context)
                addView(sw)
                setOnClickListener {
                    sw.isChecked = !sw.isChecked
                    pendingChanges[key] = sw.isChecked
                }
            })
        }
    }

    // =========================================================
    //  TOGGLE ROW
    // =========================================================

    private fun createToggleRow(
        context: Context, label: String, subtitle: String,
        databaseKey: String, defaultState: Boolean,
        pendingChanges: MutableMap<String, Any?>,
        onChanged: () -> Unit = {}
    ): View {
        val effectiveChecked = pendingChanges[databaseKey] as? Boolean
            ?: getKey<Boolean>(databaseKey) ?: defaultState

        val sw = Switch(context).apply {
            isChecked = effectiveChecked
            isClickable = false
            isFocusable = false
            isFocusableInTouchMode = false
            thumbTintList = android.content.res.ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                intArrayOf(Color.WHITE, Color.parseColor("#9099B8"))
            )
            trackTintList = android.content.res.ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                intArrayOf(SWITCH_ON, SWITCH_OFF)
            )
        }

        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20.dp(context), 14.dp(context), 16.dp(context), 14.dp(context))
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true
            isFocusable = true
            isFocusableInTouchMode = false
            background = stateDrawable(context)

            val textCol = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            textCol.addView(TextView(context).apply {
                text = label; textSize = 15f
                setTypeface(null, android.graphics.Typeface.BOLD); setTextColor(TEXT_PRIMARY)
            })
            textCol.addView(TextView(context).apply {
                text = subtitle; textSize = 12f; setTextColor(TEXT_SECONDARY)
                setPadding(0, 3.dp(context), 0, 0)
            })
            addView(textCol)
            addView(sw)

            setOnClickListener {
                sw.isChecked = !sw.isChecked
                pendingChanges[databaseKey] = sw.isChecked
                sw.animate().scaleX(0.92f).scaleY(0.92f).setDuration(80).withEndAction {
                    sw.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                }.start()
                onChanged()
            }
        }
    }

    // =========================================================
    //  STREMIO ADDONS CARD
    // =========================================================

    private fun createStremioAddonsCard(context: Context, onRegisterCommit: (() -> Unit) -> Unit): View {
        val ADDON_ACCENT  = Color.parseColor("#22D3EE")
        val ADDON_BG      = Color.parseColor("#0A1820")
        val ADDON_BORDER  = Color.parseColor("#1A3040")
        val TYPE_HTTPS    = Color.parseColor("#4ADE80")
        val TYPE_TORRENT  = Color.parseColor("#FACC15")
        val TYPE_DEBRID   = Color.parseColor("#F472B6")

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val m = 16.dp(context)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(m, 0, m, m) }
            background = roundRect(BG_CARD, 16f.dp(context)); elevation = 4f
        }

        var expanded = false

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 8.dp(context))
            visibility = android.view.View.GONE
        }

        val addons = getStremioAddons()

        val addonRows = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }

        val TYPE_SUBTITLE = Color.parseColor("#C084FC")

        fun typeColor(t: AddonType) = when (t) {
            AddonType.HTTPS     -> TYPE_HTTPS
            AddonType.TORRENT   -> TYPE_TORRENT
            AddonType.DEBRID    -> TYPE_DEBRID
            AddonType.SUBTITLE  -> TYPE_SUBTITLE
        }

        fun AddonType.next() = when (this) {
            AddonType.HTTPS     -> AddonType.TORRENT
            AddonType.TORRENT   -> AddonType.DEBRID
            AddonType.DEBRID    -> AddonType.SUBTITLE
            AddonType.SUBTITLE  -> AddonType.HTTPS
        }

        fun rebuildRows() {
            addonRows.removeAllViews()
            addons.forEachIndexed { i, addon ->
                if (i > 0) addonRows.addView(createDivider(context))

                val row = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(16.dp(context), 12.dp(context), 16.dp(context), 12.dp(context))
                }

                val header = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                    ).also { it.bottomMargin = 8.dp(context) }
                }

                val typePill = TextView(context).apply {
                    text = addon.type.name
                    textSize = 10f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setTextColor(typeColor(addon.type))
                    setPadding(10.dp(context), 4.dp(context), 10.dp(context), 4.dp(context))
                    background = GradientDrawable().apply {
                        cornerRadius = 99f
                        setColor(Color.parseColor("#0D1117"))
                        setStroke(1, typeColor(addon.type))
                    }
                    isClickable = true; isFocusable = true; isFocusableInTouchMode = false
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
                    ).also { it.marginEnd = 10.dp(context) }
                    setOnClickListener {
                        val next = addons[i].type.next()
                        addons[i] = addons[i].copy(type = next)
                        text = next.name
                        setTextColor(typeColor(next))
                        (background as? GradientDrawable)?.setStroke(1, typeColor(next))
                        (background as? GradientDrawable)?.setColor(Color.parseColor("#0D1117"))
                    }
                }
                header.addView(typePill)

                // Name label
                header.addView(TextView(context).apply {
                    text = addon.name.ifBlank { "Unnamed" }
                    textSize = 13f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setTextColor(if (addon.name.isBlank()) TEXT_SECONDARY else TEXT_PRIMARY)
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })

                // Delete button
                header.addView(pillBtn(context, "✕", DANGER_COLOR,
                    Color.parseColor("#1A0A0D"), Color.parseColor("#3A1520")) {
                    addons.removeAt(i); rebuildRows()
                })

                row.addView(header)

                // ── Name field ──
                row.addView(TextView(context).apply {
                    text = "Name"; textSize = 11f; setTextColor(TEXT_SECONDARY)
                    setPadding(0, 0, 0, 4.dp(context))
                })
                val nameField = EditText(context).apply {
                    setText(addon.name)
                    hint = "e.g. Torrentio"; textSize = 13f
                    setTextColor(TEXT_PRIMARY); setHintTextColor(TEXT_SECONDARY)
                    setSingleLine(true)
                    isFocusable = true; isFocusableInTouchMode = true
                    setOnFocusChangeListener { v, hasFocus ->
                        if (hasFocus) (v.parent?.parent?.parent?.parent as? ScrollView)
                            ?.requestChildFocus(v, v)
                    }
                    addTextChangedListener(object : android.text.TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                        override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {
                            addons[i] = addons[i].copy(name = s?.toString() ?: "")
                        }
                        override fun afterTextChanged(s: android.text.Editable?) {}
                    })
                    setPadding(12.dp(context), 10.dp(context), 12.dp(context), 10.dp(context))
                    background = GradientDrawable().apply {
                        cornerRadius = 8f.dp(context)
                        setColor(Color.parseColor("#0D1117"))
                        setStroke(1, Color.parseColor("#2E2850"))
                    }
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                    ).also { it.bottomMargin = 8.dp(context) }
                }
                row.addView(nameField)

                row.addView(TextView(context).apply {
                    text = "URL"; textSize = 11f; setTextColor(TEXT_SECONDARY)
                    setPadding(0, 0, 0, 4.dp(context))
                })
                // Strip trailing /manifest.json before storing — user pastes the full
                fun String.stripManifest() = trimEnd('/')
                    .removeSuffix("/manifest.json").trimEnd('/')

                val urlField = EditText(context).apply {
                    // Display stored base URL; user may paste full manifest URL
                    setText(addon.url)
                    hint = "https://xyz.com/manifest.json"; textSize = 12f
                    setTextColor(TEXT_PRIMARY); setHintTextColor(TEXT_SECONDARY)
                    setSingleLine(true)
                    inputType = android.text.InputType.TYPE_CLASS_TEXT or
                            android.text.InputType.TYPE_TEXT_VARIATION_URI
                    isFocusable = true; isFocusableInTouchMode = true
                    setOnFocusChangeListener { v, hasFocus ->
                        if (hasFocus) (v.parent?.parent?.parent?.parent as? ScrollView)
                            ?.requestChildFocus(v, v)
                    }
                    addTextChangedListener(object : android.text.TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                        override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {
                            // Store base URL — /manifest.json
                            addons[i] = addons[i].copy(url = (s?.toString() ?: "").stripManifest())
                        }
                        override fun afterTextChanged(s: android.text.Editable?) {}
                    })
                    setPadding(12.dp(context), 10.dp(context), 12.dp(context), 10.dp(context))
                    background = GradientDrawable().apply {
                        cornerRadius = 8f.dp(context)
                        setColor(Color.parseColor("#0D1117"))
                        setStroke(1, Color.parseColor("#2E2850"))
                    }
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                    ).also { it.bottomMargin = 6.dp(context) }
                }
                row.addView(urlField)

                // TV clipboard row for URL
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                row.addView(LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                    ).also { it.bottomMargin = 4.dp(context) }

                    addView(pillBtn(context, "📋 Paste URL",
                        Color.parseColor("#94A3B8"), Color.parseColor("#0F1520"),
                        Color.parseColor("#1E2A3A")) {
                        val clip = clipboard.primaryClip
                            ?.getItemAt(0)?.coerceToText(context)?.toString()?.trim()
                        if (!clip.isNullOrBlank()) {
                            val stripped = clip.stripManifest()
                            urlField.setText(stripped)
                            urlField.setSelection(urlField.text?.length ?: 0)
                            addons[i] = addons[i].copy(url = stripped)
                            Toast.makeText(context, "URL pasted", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Clipboard empty", Toast.LENGTH_SHORT).show()
                        }
                    })
                    addView(android.view.View(context).apply {
                        layoutParams = LinearLayout.LayoutParams(8.dp(context), 1)
                    })
                    addView(pillBtn(context, "📄 Copy URL",
                        Color.parseColor("#94A3B8"), Color.parseColor("#0F1520"),
                        Color.parseColor("#1E2A3A")) {
                        val text = urlField.text?.toString()?.trim()
                        if (!text.isNullOrBlank()) {
                            clipboard.setPrimaryClip(ClipData.newPlainText("Addon URL", text))
                            Toast.makeText(context, "URL copied", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Nothing to copy", Toast.LENGTH_SHORT).show()
                        }
                    })
                })

                addonRows.addView(row)
            }

            // Empty state hint
            if (addons.isEmpty()) {
                addonRows.addView(TextView(context).apply {
                    text = "No addons yet — tap Add Addon to add one"
                    textSize = 12f; setTextColor(TEXT_SECONDARY)
                    gravity = Gravity.CENTER
                    setPadding(0, 16.dp(context), 0, 16.dp(context))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                })
            }
        }

        // ── Toolbar: Add + Save All ──
        val toolbar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16.dp(context), 10.dp(context), 16.dp(context), 6.dp(context))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        toolbar.addView(pillBtn(context, "+ Add Addon", ADDON_ACCENT, ADDON_BG, ADDON_BORDER) {
            addons.add(StremioAddon(name = "", url = "", type = AddonType.HTTPS))
            rebuildRows()
        })

        val sep = android.view.View(context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
                .also { it.setMargins(16.dp(context), 4.dp(context), 16.dp(context), 0) }
            setBackgroundColor(DIVIDER_COLOR)
        }

        onRegisterCommit {
            val invalid = addons.indexOfFirst { it.url.isBlank() }
            if (invalid != -1) {
                Toast.makeText(context, "Addon ${invalid + 1} has empty URL — not saved",
                    Toast.LENGTH_SHORT).show()
            } else {
                saveStremioAddons(addons)
            }
        }

        rebuildRows()

        // ── IMDB note banner ──
        val noteBanner = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(14.dp(context), 12.dp(context), 14.dp(context), 12.dp(context))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(16.dp(context), 6.dp(context), 16.dp(context), 8.dp(context)) }
            background = GradientDrawable().apply {
                cornerRadius = 10f.dp(context)
                setColor(Color.parseColor("#0C1A2E"))
                setStroke(2, Color.parseColor("#1D4ED8"))
            }
            addView(View(context).apply {
                layoutParams = LinearLayout.LayoutParams(8.dp(context), 8.dp(context)).also {
                    it.marginEnd = 10.dp(context); it.topMargin = 2.dp(context)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#3B82F6"))
                }
            })
            val noteCol = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            noteCol.addView(TextView(context).apply {
                text = "IMDB IDs Required"
                textSize = 12f; setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(Color.parseColor("#60A5FA"))
            })
            noteCol.addView(TextView(context).apply {
                text = "Only addons that support IMDB IDs are compatible & no catalog addons are supported"
                textSize = 11f; setTextColor(Color.parseColor("#93C5FD"))
                setPadding(0, 3.dp(context), 0, 0)
            })
            addView(noteCol)
        }

        content.addView(toolbar); content.addView(sep); content.addView(noteBanner); content.addView(addonRows)

        val chevron = TextView(context).apply {
            text = "▼"; textSize = 11f; setTextColor(TEXT_SECONDARY)
        }
        val summary = TextView(context).apply {
            textSize = 11f; setTextColor(Color.parseColor("#5A5E7A"))
            setPadding(0, 0, 8.dp(context), 0)
            text = if (addons.isEmpty()) "" else "${addons.size} addon${if (addons.size == 1) "" else "s"}"
        }

        card.addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20.dp(context), 16.dp(context), 16.dp(context), 16.dp(context))
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true; isFocusable = true; isFocusableInTouchMode = false
            background = stateDrawable(context)

            addView(android.view.View(context).apply {
                layoutParams = LinearLayout.LayoutParams(3.dp(context), 18.dp(context))
                    .also { it.marginEnd = 12.dp(context) }
                background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(ADDON_ACCENT, Color.parseColor("#0891B2")))
                    .apply { cornerRadius = 99f }
            })
            addView(TextView(context).apply {
                text = "🔌  Stremio Addons"; textSize = 12f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(TEXT_SECONDARY); letterSpacing = 0.08f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(summary); addView(chevron)

            setOnClickListener {
                expanded = !expanded; chevron.text = if (expanded) "▲" else "▼"
                summary.text = if (addons.isEmpty()) "" else "${addons.size} addon${if (addons.size == 1) "" else "s"}"
                if (expanded) {
                    content.visibility = android.view.View.VISIBLE; content.alpha = 0f
                    content.animate().alpha(1f).setDuration(220).start()
                } else {
                    content.animate().alpha(0f).setDuration(160).withEndAction {
                        content.visibility = android.view.View.GONE; content.alpha = 1f
                        summary.text = if (addons.isEmpty()) "" else "${addons.size} addon${if (addons.size == 1) "" else "s"}"
                    }.start()
                }
            }
        })

        card.addView(content)
        card.alpha = 0f; card.translationY = 20f
        card.animate().alpha(1f).translationY(0f)
            .setDuration(300).setInterpolator(DecelerateInterpolator()).start()
        return card
    }

    // =========================================================
    //  CREDITS CARD
    // =========================================================

    private fun createCreditsCard(context: Context): View {
        val contributors = listOf(
            Triple("phisher98",     "For multi-source plugin inspiration", "github.com/phisher98"),
            Triple("AzartX47",      "For providing multiple APIs",         "github.com/AzartX47"),
            Triple("yogesh-hacker", "For providing reference",             "github.com/yogesh-hacker"),
        )

        val CREDIT_ACCENT = Color.parseColor("#38BDF8")
        val CREDIT_BG     = Color.parseColor("#0A1420")
        val CREDIT_BORDER = Color.parseColor("#1A3040")

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val m = 16.dp(context)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(m, 0, m, m) }
            background = roundRect(BG_CARD, 16f.dp(context)); elevation = 4f
        }

        var expanded = false
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16.dp(context), 4.dp(context), 16.dp(context), 16.dp(context))
            visibility = View.GONE
        }

        content.addView(TextView(context).apply {
            text = "Special thanks to these developers whose work served as reference and inspiration ❤️"
            textSize = 12f; setTextColor(TEXT_SECONDARY)
            setPadding(4.dp(context), 4.dp(context), 4.dp(context), 14.dp(context))
        })

        contributors.forEachIndexed { i, (name, role, url) ->
            if (i > 0) content.addView(createDivider(context))
            content.addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(4.dp(context), 12.dp(context), 4.dp(context), 12.dp(context))
                gravity = Gravity.CENTER_VERTICAL

                addView(TextView(context).apply {
                    text = name.first().uppercaseChar().toString()
                    textSize = 14f; setTypeface(null, android.graphics.Typeface.BOLD)
                    setTextColor(CREDIT_ACCENT); gravity = Gravity.CENTER
                    val size = 36.dp(context)
                    layoutParams = LinearLayout.LayoutParams(size, size)
                        .also { it.marginEnd = 14.dp(context) }
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(CREDIT_BG); setStroke(2, CREDIT_BORDER)
                    }
                })

                val col = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                col.addView(TextView(context).apply {
                    text = name; textSize = 14f
                    setTypeface(null, android.graphics.Typeface.BOLD); setTextColor(TEXT_PRIMARY)
                })
                col.addView(TextView(context).apply {
                    text = role; textSize = 11f; setTextColor(TEXT_SECONDARY)
                    setPadding(0, 2.dp(context), 0, 0)
                })
                addView(col)

                addView(pillBtn(context, "GitHub", CREDIT_ACCENT, CREDIT_BG, CREDIT_BORDER) {
                    try {
                        context.startActivity(android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://$url")))
                    } catch (_: Exception) {
                        Toast.makeText(context, url, Toast.LENGTH_SHORT).show()
                    }
                })
            })
        }

        val chevron = TextView(context).apply {
            text = "▼"; textSize = 11f; setTextColor(TEXT_SECONDARY)
        }

        card.addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20.dp(context), 16.dp(context), 16.dp(context), 16.dp(context))
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true; isFocusable = true; isFocusableInTouchMode = false; background = stateDrawable(context)

            addView(View(context).apply {
                layoutParams = LinearLayout.LayoutParams(3.dp(context), 18.dp(context))
                    .also { it.marginEnd = 12.dp(context) }
                background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(CREDIT_ACCENT, Color.parseColor("#0EA5E9")))
                    .apply { cornerRadius = 99f }
            })
            addView(TextView(context).apply {
                text = "🙏  Credits & Thanks"; textSize = 12f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(TEXT_SECONDARY); letterSpacing = 0.08f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(chevron)

            setOnClickListener {
                expanded = !expanded; chevron.text = if (expanded) "▲" else "▼"
                if (expanded) {
                    content.visibility = View.VISIBLE; content.alpha = 0f
                    content.animate().alpha(1f).setDuration(200).start()
                } else {
                    content.animate().alpha(0f).setDuration(150).withEndAction {
                        content.visibility = View.GONE; content.alpha = 1f
                    }.start()
                }
            }
        })

        card.addView(content)
        card.alpha = 0f; card.translationY = 20f
        card.animate().alpha(1f).translationY(0f)
            .setDuration(300).setInterpolator(DecelerateInterpolator()).start()
        return card
    }

    // =========================================================
    //  REUSABLE PILL BUTTON
    // =========================================================

    private fun pillBtn(
        context: Context, label: String,
        textColor: Int, bgColor: Int, borderColor: Int,
        onClick: () -> Unit
    ) = TextView(context).apply {
        text = label; textSize = 11f
        setTypeface(null, android.graphics.Typeface.BOLD); setTextColor(textColor)
        setPadding(12.dp(context), 6.dp(context), 12.dp(context), 6.dp(context))
        background = GradientDrawable().apply {
            cornerRadius = 99f; setColor(bgColor); setStroke(1, borderColor)
        }
        isClickable = true; isFocusable = true; isFocusableInTouchMode = false
        setOnClickListener {
            animate().scaleX(0.88f).scaleY(0.88f).setDuration(70).withEndAction {
                animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            }.start(); onClick()
        }
    }

    // =========================================================
    //  HERO BANNER
    // =========================================================

    private fun createHeroBanner(context: Context): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28.dp(context), 32.dp(context), 28.dp(context), 24.dp(context))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.parseColor("#1A1730"), BG_DARK))
            addView(View(context).apply {
                layoutParams = LinearLayout.LayoutParams(48.dp(context), 4.dp(context))
                    .also { it.bottomMargin = 16.dp(context) }
                background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                    intArrayOf(ACCENT_START, ACCENT_END)).apply { cornerRadius = 99f }
            })
            addView(TextView(context).apply {
                text = "Plugin Settings"; textSize = 22f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(TEXT_PRIMARY); letterSpacing = -0.02f
            })
            addView(TextView(context).apply {
                text = "Configure sources, catalogs & cookies"
                textSize = 13f; setTextColor(TEXT_SECONDARY); setPadding(0, 6.dp(context), 0, 0)
            })
        }
    }

    // =========================================================
    //  RESTART BANNER
    // =========================================================

    private fun createRestartBanner(context: Context): LinearLayout {
        val WARN_BG     = Color.parseColor("#13100A")
        val WARN_BORDER = Color.parseColor("#4A3200")
        val WARN_ACCENT = Color.parseColor("#F5A623")
        val WARN_DIM    = Color.parseColor("#9E7A30")
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            val m = 16.dp(context)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(m, 0, m, m) }
            background = GradientDrawable().apply {
                cornerRadius = 14f.dp(context); setColor(WARN_BG); setStroke(1, WARN_BORDER)
            }
            setPadding(16.dp(context), 14.dp(context), 16.dp(context), 14.dp(context))

            val dot = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(8.dp(context), 8.dp(context)).also {
                    it.marginEnd = 12.dp(context); it.topMargin = 2.dp(context)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL; setColor(WARN_ACCENT)
                }
            }
            addView(dot)
            ObjectAnimator.ofFloat(dot, "alpha", 1f, 0.25f, 1f).apply {
                duration = 1200; repeatCount = ObjectAnimator.INFINITE
                interpolator = DecelerateInterpolator()
            }.start()

            val textCol = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            textCol.addView(TextView(context).apply {
                text = "Restart Required"; textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD); setTextColor(WARN_ACCENT)
            })
            textCol.addView(TextView(context).apply {
                text = "Fully close & reopen Cloudstream to apply catalog changes"
                textSize = 11f; setTextColor(WARN_DIM); setPadding(0, 3.dp(context), 0, 0)
            })
            addView(textCol)
            addView(TextView(context).apply {
                text = "↺"; textSize = 22f; setTextColor(WARN_ACCENT)
                setPadding(12.dp(context), 0, 0, 0); alpha = 0.85f
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
    //  DIVIDER
    // =========================================================

    private fun createDivider(context: Context): View {
        return View(context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
                .also { it.setMargins(20.dp(context), 0, 20.dp(context), 0) }
            setBackgroundColor(DIVIDER_COLOR)
        }
    }

    // =========================================================
    //  DRAWING HELPERS
    // =========================================================

    private fun roundRect(color: Int, radius: Float) = GradientDrawable().apply {
        cornerRadius = radius; setColor(color)
    }

    private fun stateDrawable(context: Context) = StateListDrawable().apply {
        addState(
            intArrayOf(android.R.attr.state_pressed),
            GradientDrawable().apply { setColor(Color.parseColor("#2A2D45")) }
        )
        addState(
            intArrayOf(android.R.attr.state_focused),
            GradientDrawable().apply {
                setColor(Color.parseColor("#1F2235"))
                setStroke(2, ACCENT_START)
            }
        )
        addState(
            intArrayOf(),
            GradientDrawable().apply { setColor(Color.TRANSPARENT) }
        )
    }

    // =========================================================
    //  EXTENSION HELPERS
    // =========================================================

    private fun Int.dp(context: Context): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(),
            context.resources.displayMetrics).toInt()

    private fun Float.dp(context: Context): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this,
            context.resources.displayMetrics)
}
