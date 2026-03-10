package com.megix

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.AlertDialog
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

    // --- DATABASE KEYS: Providers ---
    const val P_TORRENTIO    = "p_torrentio"
    const val P_TORRENTSDB   = "p_torrentsdb"
    const val P_ANIMETOSHO   = "p_animetosho"
    const val P_VIDFLIX      = "p_vidflix"
    const val P_MOVIEBOX     = "p_moviebox"
    const val P_WYZIESUBS    = "p_wyziesubs"
    const val P_STREMIOSUBS  = "p_stremiosubs"
    const val P_CINEMACITY   = "p_cinemacity"
    const val P_WEBSTREAMR   = "p_webstreamr"
    const val P_STREAMVIX    = "p_streamvix"
    const val P_NOTORRENT    = "p_notorrent"
    const val P_CASTLE       = "p_castle"
    const val P_CINE         = "p_cine"
    const val P_ALLMOVIELAND = "p_allmovieland"
    const val P_MADPLAYCDN   = "p_madplaycdn"
    const val P_VIDFASTPRO   = "p_vidfastpro"
    const val P_HEXA         = "p_hexa"
    const val P_YFLIX        = "p_yflix"
    const val P_XPASS        = "p_xpass"
    const val P_PLAYSRC      = "p_playsrc"
    const val P_2EMBED       = "p_2embed"
    const val P_DRAMAFULL    = "p_dramafull"
    const val P_VIDEASY      = "p_videasy"
    const val P_CINEMAOS     = "p_cinemaos"
    const val P_VICSRCWTF    = "p_vicsrcwtf"
    const val P_VIDLINK      = "p_vidlink"
    const val P_MAPPLE       = "p_mapple"
    const val P_VIDSTACK     = "p_vidstack"
    const val P_KISSKH       = "p_kisskh"
    const val P_NETFLIX      = "p_netflix"
    const val P_PRIMEVIDEO   = "p_primevideo"
    const val P_DISNEY       = "p_disney"
    const val P_BOLLYWOOD    = "p_bollywood"
    const val P_VIDZEE       = "p_vidzee"
    const val P_XDMOVIES     = "p_xdmovies"
    const val P_4KHDHUB      = "p_4khdhub"
    const val P_FLIXINDIA    = "p_flixindia"
    const val P_MOVIESDRIVE  = "p_moviesdrive"
    const val P_VEGAMOVIES   = "p_vegamovies"
    const val P_ROGMOVIES    = "p_rogmovies"
    const val P_BOLLYFLIX    = "p_bollyflix"
    const val P_TOPMOVIES    = "p_topmovies"
    const val P_MOVIESMOD    = "p_moviesmod"
    const val P_MOVIES4U     = "p_movies4u"
    const val P_UHDMOVIES    = "p_uhdmovies"
    const val P_PRIMESRC     = "p_primesrc"
    const val P_PROJECTFREETV = "p_projectfreetv"
    const val P_HINDMOVIEZ   = "p_hindmoviez"
    const val P_LEVIDIA      = "p_levidia"
    const val P_DAHMERMOVIES = "p_dahmermovies"
    const val P_MULTIMOVIES  = "p_multimovies"
    const val P_PROTONMOVIES = "p_protonmovies"
    const val P_AKWAM        = "p_akwam"
    const val P_RTALLY       = "p_rtally"
    const val P_TOONSTREAM   = "p_toonstream"
    const val P_ASIAFLIX     = "p_asiaflix"
    const val P_SKYMOVIES    = "p_skymovies"
    const val P_HDMOVIE2     = "p_hdmovie2"
    const val P_MOSTRAGUARDA = "p_mostraguarda"
    const val P_ALLANIME     = "p_allanime"
    const val P_SUDATCHI     = "p_sudatchi"
    const val P_TOKYOINSIDER = "p_tokyoinsider"
    const val P_ANIZONE      = "p_anizone"
    const val P_ANIMES       = "p_animes"
    const val P_GOJO         = "p_gojo"
    const val P_ANIMEWORLD   = "p_animeworld"

    private const val PROVIDER_ORDER_KEY = "provider_order"
    private val TORRENT_KEYS = setOf(P_TORRENTIO, P_TORRENTSDB, P_ANIMETOSHO)

    val PROVIDER_NAMES = linkedMapOf(
        P_TORRENTIO    to "🧲 Torrentio",
        P_TORRENTSDB   to "🧲 TorrentsDB",
        P_ANIMETOSHO   to "🧲 AnimeTosho",
        P_WEBSTREAMR   to "WebStreamr",
        P_STREAMVIX    to "Streamvix",
        P_NOTORRENT    to "NoTorrent",
        P_CASTLE       to "Castle",
        P_CINE         to "Cine",
        P_ANIMEWORLD   to "AnimeWorld",
        P_VIDFLIX      to "Vidflix",
        P_MOVIEBOX     to "Moviebox",
        P_CINEMACITY   to "Cinemacity",
        P_ALLMOVIELAND to "Allmovieland",
        P_MADPLAYCDN   to "MadplayCDN",
        P_VIDFASTPRO   to "VidFastPro",
        P_HEXA         to "Hexa",
        P_YFLIX        to "Yflix",
        P_XPASS        to "Xpass",
        P_PLAYSRC      to "Playsrc",
        P_2EMBED       to "2Embed",
        P_VIDEASY      to "Videasy",
        P_CINEMAOS     to "CinemaOS",
        P_VICSRCWTF    to "VicSrcWtf",
        P_VIDLINK      to "Vidlink",
        P_MAPPLE       to "Mapple",
        P_VIDSTACK     to "Vidstack",
        P_VIDZEE       to "Vidzee",
        P_WYZIESUBS    to "WYZIESubs",
        P_STREMIOSUBS  to "StremioSubs",
        P_NETFLIX      to "Netflix",
        P_PRIMEVIDEO   to "Prime Video",
        P_DISNEY       to "Hotstar",
        P_BOLLYWOOD    to "Gramcinema",
        P_FLIXINDIA    to "FlixIndia",
        P_VEGAMOVIES   to "VegaMovies",
        P_ROGMOVIES    to "RogMovies",
        P_BOLLYFLIX    to "Bollyflix",
        P_TOPMOVIES    to "TopMovies",
        P_MOVIESMOD    to "Moviessmod",
        P_MOVIES4U     to "Movies4u",
        P_UHDMOVIES    to "UHDMovies",
        P_MOVIESDRIVE  to "MoviesDrive",
        P_HINDMOVIEZ   to "Hindmoviez",
        P_4KHDHUB      to "4KHDHub",
        P_XDMOVIES     to "XDMovies",
        P_PRIMESRC     to "PrimeSrc",
        P_PROJECTFREETV to "ProjectFreeTV",
        P_LEVIDIA      to "Levidia",
        P_DAHMERMOVIES to "DahmerMovies",
        P_MULTIMOVIES  to "Multimovies",
        P_PROTONMOVIES to "Protonmovies",
        P_AKWAM        to "Akwam",
        P_RTALLY       to "Rtally",
        P_ASIAFLIX     to "Asiaflix",
        P_SKYMOVIES    to "SkyMovies",
        P_HDMOVIE2     to "HDMovie2",
        P_MOSTRAGUARDA to "Mostraguarda",
        P_TOONSTREAM   to "Toonstream",
        P_ALLANIME     to "AllAnime",
        P_SUDATCHI     to "Sudatchi",
        P_TOKYOINSIDER to "TokyoInsider",
        P_ANIZONE      to "Anizone",
        P_ANIMES       to "Animes",
        P_GOJO         to "Animetsu",
        P_KISSKH       to "KissKH",
        P_DRAMAFULL    to "Dramafull",
    )

    private val DEFAULT_ORDER = PROVIDER_NAMES.keys.toList()

    fun enabled(key: String): Boolean = getKey<Boolean>(key) ?: (key !in TORRENT_KEYS)

    fun getOrder(): List<String> {
        val saved = getKey<String>(PROVIDER_ORDER_KEY)
            ?.split(",")?.filter { it.isNotBlank() }
            ?: return DEFAULT_ORDER
        return saved + (DEFAULT_ORDER - saved.toSet())
    }

    fun saveOrder(order: List<String>) = setKey(PROVIDER_ORDER_KEY, order.joinToString(","))

    // =========================================================
    //  NETMIRROR COOKIE HELPERS
    // =========================================================

    fun saveCookie(cookie: String) {
        setKey(COOKIE_KEY, cookie)
        setKey(TIMESTAMP_KEY, System.currentTimeMillis())
    }

    fun getCookie(): Pair<String?, Long> {
        return Pair(getKey<String>(COOKIE_KEY), getKey<Long>(TIMESTAMP_KEY) ?: 0L)
    }

    fun clearCookie() {
        setKey(COOKIE_KEY, null)
        setKey(TIMESTAMP_KEY, null)
    }

    // =========================================================
    //  SETTINGS DIALOG
    // =========================================================

    fun showSettingsDialog(context: Context, onSave: () -> Unit) {
        var requiresRestart = false

        val scroll = ScrollView(context).apply {
            isScrollbarFadingEnabled = true
            background = ColorDrawable(BG_DARK)
        }

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 24.dp(context))
            background = ColorDrawable(BG_DARK)
        }

        layout.addView(createHeroBanner(context))

        layout.addView(createSectionCard(context, "⚙️  Scraping Settings") {
            addView(createToggleRow(context, "Download Only Links", "Only great for downloading (Not for Streaming)", DOWNLOAD_ENABLE, false))
        })

        val restartBanner = createRestartBanner(context).also { it.visibility = View.GONE }

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
        layout.addView(createCollapsibleCard(
            context  = context,
            title    = "📡  Active Catalogs",
            startExpanded = false
        ) {
            addView(createToggleRow(context, "CineStream", "Cinemeta catalog",  PROVIDER_CINESTREAM, true, onCatalogChanged))
            addView(createDivider(context))
            addView(createToggleRow(context, "CineSimkl",  "Simkl catalog",     PROVIDER_SIMKL,      true, onCatalogChanged))
            addView(createDivider(context))
            addView(createToggleRow(context, "CineTmdb",   "TMDB catalog",      PROVIDER_TMDB,       true, onCatalogChanged))
        })

        layout.addView(restartBanner)

        layout.addView(createProvidersCard(context))

        layout.addView(createDangerCard(context))

        scroll.addView(layout)

        val dialog = AlertDialog.Builder(context, android.R.style.Theme_Material_Dialog)
            .setView(scroll)
            .setPositiveButton("Save & Reload") { _, _ ->
                if (requiresRestart) showRestartWarning(context, onSave) else onSave()
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.window?.setBackgroundDrawable(roundRect(BG_DARK, 20f.dp(context)))
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply { setTextColor(ACCENT_START); isAllCaps = false }
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply { setTextColor(TEXT_SECONDARY); isAllCaps = false }
    }

    // =========================================================
    //  COLLAPSIBLE CARD  (used by Catalogs)
    // =========================================================

    private fun createCollapsibleCard(
        context: Context,
        title: String,
        startExpanded: Boolean = false,
        block: LinearLayout.() -> Unit
    ): View {
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val m = 16.dp(context)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(m, 0, m, m) }
            background = roundRect(BG_CARD, 16f.dp(context))
            elevation = 4f
        }

        var expanded = startExpanded

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 8.dp(context))
            visibility = if (expanded) View.VISIBLE else View.GONE
        }
        content.block()

        val chevron = TextView(context).apply {
            text = if (expanded) "▲" else "▼"
            textSize = 11f
            setTextColor(TEXT_SECONDARY)
        }

        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20.dp(context), 16.dp(context), 16.dp(context), 16.dp(context))
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true; isFocusable = true
            background = stateDrawable(context)

            addView(View(context).apply {
                layoutParams = LinearLayout.LayoutParams(3.dp(context), 18.dp(context)).also { it.marginEnd = 12.dp(context) }
                background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(ACCENT_START, ACCENT_END)).apply { cornerRadius = 99f }
            })
            addView(TextView(context).apply {
                text = title; textSize = 12f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(TEXT_SECONDARY); letterSpacing = 0.08f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(chevron)

            setOnClickListener {
                expanded = !expanded
                chevron.text = if (expanded) "▲" else "▼"
                if (expanded) {
                    content.visibility = View.VISIBLE
                    content.alpha = 0f
                    content.animate().alpha(1f).setDuration(200).start()
                } else {
                    content.animate().alpha(0f).setDuration(150).withEndAction {
                        content.visibility = View.GONE
                        content.alpha = 1f
                    }.start()
                }
            }
        }

        card.addView(header)
        card.addView(content)

        card.alpha = 0f; card.translationY = 20f
        card.animate().alpha(1f).translationY(0f).setDuration(300).setInterpolator(DecelerateInterpolator()).start()
        return card
    }

    // =========================================================
    //  PROVIDERS CARD
    // =========================================================

    private fun createProvidersCard(context: Context): View {
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val m = 16.dp(context)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(m, 0, m, m) }
            background = roundRect(BG_CARD, 16f.dp(context))
            elevation = 4f
        }

        var expanded = false

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 8.dp(context))
            visibility = View.GONE
        }

        val toolbar = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16.dp(context), 8.dp(context), 16.dp(context), 4.dp(context))
        }

        val pillRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = 6.dp(context) }
        }

        val rows = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

        val order = getOrder().toMutableList()

        fun rebuild() {
            rows.removeAllViews()
            order.forEachIndexed { i, key ->
                if (i > 0) rows.addView(createDivider(context))
                rows.addView(createProviderRow(
                    context,
                    label       = PROVIDER_NAMES[key] ?: key,
                    key         = key,
                    index       = i + 1,
                    totalCount  = order.size,
                    isTorrent   = key in TORRENT_KEYS,
                    canMoveUp   = i > 0,
                    canMoveDown = i < order.lastIndex,
                    onMoveUp    = { order.add(i - 1, order.removeAt(i)); saveOrder(order); rebuild() },
                    onMoveDown  = { order.add(i + 1, order.removeAt(i)); saveOrder(order); rebuild() },
                    onMoveTo    = { target ->
                        val item = order.removeAt(i)
                        order.add(target.coerceIn(0, order.size), item)
                        saveOrder(order); rebuild()
                    }
                ))
            }
        }

        pillRow.addView(pillBtn(context, "✓ All", Color.parseColor("#4ADE80"), Color.parseColor("#0A1A0F"), Color.parseColor("#1A3A1F")) {
            order.forEach { setKey(it, true) }
            rebuild()
            Toast.makeText(context, "All providers enabled", Toast.LENGTH_SHORT).show()
        })
        pillRow.addView(View(context).apply { layoutParams = LinearLayout.LayoutParams(8.dp(context), 1) })
        pillRow.addView(pillBtn(context, "✕ None", DANGER_COLOR, Color.parseColor("#1A0A0D"), Color.parseColor("#3A1520")) {
            order.forEach { setKey(it, false) }
            rebuild()
            Toast.makeText(context, "All providers disabled", Toast.LENGTH_SHORT).show()
        })
        pillRow.addView(View(context).apply { layoutParams = LinearLayout.LayoutParams(8.dp(context), 1) })
        pillRow.addView(pillBtn(context, "↺ Reset Order", ACCENT_START, Color.parseColor("#1A1730"), Color.parseColor("#2E2850")) {
            order.clear()
            order.addAll(DEFAULT_ORDER)
            saveOrder(order)
            rebuild()
            Toast.makeText(context, "Order reset", Toast.LENGTH_SHORT).show()
        })

        val noteRow = TextView(context).apply {
            text = "🧲 = off by default  ·  ↑↓ = scraping order"
            textSize = 10f
            setTextColor(Color.parseColor("#44475A"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = 4.dp(context) }
        }

        toolbar.addView(pillRow)
        toolbar.addView(noteRow)

        val sep = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
                .also { it.setMargins(16.dp(context), 0, 16.dp(context), 4.dp(context)) }
            setBackgroundColor(DIVIDER_COLOR)
        }

        rebuild()

        content.addView(toolbar)
        content.addView(sep)
        content.addView(rows)

        val chevron = TextView(context).apply {
            text = "▼"; textSize = 11f; setTextColor(TEXT_SECONDARY)
        }

        val summary = TextView(context).apply {
            textSize = 11f; setTextColor(Color.parseColor("#5A5E7A"))
            setPadding(0, 0, 8.dp(context), 0)
        }

        fun updateSummary() {
            val enabledCount = order.count { enabled(it) }
            summary.text = "$enabledCount / ${order.size} enabled"
        }
        updateSummary()

        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20.dp(context), 16.dp(context), 16.dp(context), 16.dp(context))
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true; isFocusable = true
            background = stateDrawable(context)

            addView(View(context).apply {
                layoutParams = LinearLayout.LayoutParams(3.dp(context), 18.dp(context)).also { it.marginEnd = 12.dp(context) }
                background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(ACCENT_START, ACCENT_END)).apply { cornerRadius = 99f }
            })
            addView(TextView(context).apply {
                text = "🎬  PROVIDERS"; textSize = 12f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(TEXT_SECONDARY); letterSpacing = 0.08f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(summary)
            addView(chevron)

            setOnClickListener {
                expanded = !expanded
                chevron.text = if (expanded) "▲" else "▼"
                updateSummary()
                if (expanded) {
                    content.visibility = View.VISIBLE
                    content.alpha = 0f
                    content.animate().alpha(1f).setDuration(220).start()
                } else {
                    content.animate().alpha(0f).setDuration(160).withEndAction {
                        content.visibility = View.GONE
                        content.alpha = 1f
                        updateSummary()
                    }.start()
                }
            }
        }

        card.addView(header)
        card.addView(content)

        card.alpha = 0f; card.translationY = 20f
        card.animate().alpha(1f).translationY(0f).setDuration(300).setInterpolator(DecelerateInterpolator()).start()
        return card
    }

    // =========================================================
    //  PROVIDER ROW
    // =========================================================

    private fun createProviderRow(
        context: Context,
        label: String,
        key: String,
        index: Int,
        totalCount: Int,
        isTorrent: Boolean,
        canMoveUp: Boolean,
        canMoveDown: Boolean,
        onMoveUp: () -> Unit,
        onMoveDown: () -> Unit,
        onMoveTo: (Int) -> Unit
    ): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16.dp(context), 10.dp(context), 12.dp(context), 10.dp(context))
            gravity = Gravity.CENTER_VERTICAL

            // Index badge — tap to jump to position
            addView(TextView(context).apply {
                text = "$index"; textSize = 11f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(if (isTorrent) Color.parseColor("#5A3E1E") else ACCENT_START)
                gravity = Gravity.CENTER
                setPadding(4.dp(context), 4.dp(context), 4.dp(context), 4.dp(context))
                background = GradientDrawable().apply {
                    cornerRadius = 6f.dp(context)
                    setColor(Color.parseColor("#1A1730"))
                    setStroke(1, if (isTorrent) Color.parseColor("#3A2810") else Color.parseColor("#2E2850"))
                }
                minWidth = 28.dp(context)
                isClickable = true; isFocusable = true
                setOnClickListener {
                    val input = EditText(context).apply {
                        inputType = android.text.InputType.TYPE_CLASS_NUMBER
                        hint = "1 – $totalCount"
                        setText("$index")
                        setTextColor(TEXT_PRIMARY)
                        setHintTextColor(TEXT_SECONDARY)
                        selectAll()
                        setPadding(16.dp(context), 12.dp(context), 16.dp(context), 12.dp(context))
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
                            text = "Move \"$label\" to position"
                            textSize = 13f
                            setTextColor(TEXT_SECONDARY)
                            setPadding(0, 0, 0, 10.dp(context))
                        })
                        addView(input)
                    }

                    val d = AlertDialog.Builder(context, android.R.style.Theme_Material_Dialog)
                        .setView(wrapper)
                        .setPositiveButton("Move") { _, _ ->
                            val target = input.text.toString().toIntOrNull()
                            if (target != null && target in 1..totalCount) {
                                onMoveTo(target - 1)
                            } else {
                                Toast.makeText(context, "Enter a number between 1 and $totalCount", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .setNegativeButton("Cancel", null)
                        .create()

                    d.window?.setBackgroundDrawable(roundRect(BG_DARK, 16f.dp(context)))
                    d.show()
                    d.getButton(AlertDialog.BUTTON_POSITIVE)?.apply { setTextColor(ACCENT_START); isAllCaps = false }
                    d.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply { setTextColor(TEXT_SECONDARY); isAllCaps = false }

                    // Auto-show keyboard
                    input.requestFocus()
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    imm.showSoftInput(input, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
                }
            })

            addView(View(context).apply { layoutParams = LinearLayout.LayoutParams(10.dp(context), 1) })

            addView(TextView(context).apply {
                text = label; textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(if (isTorrent) Color.parseColor("#C87C3A") else TEXT_PRIMARY)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })

            fun arrowBtn(symbol: String, active: Boolean, action: () -> Unit) = TextView(context).apply {
                text = symbol; textSize = 17f; gravity = Gravity.CENTER
                setTextColor(if (active) ACCENT_START else Color.parseColor("#252840"))
                setPadding(9.dp(context), 6.dp(context), 9.dp(context), 6.dp(context))
                isClickable = active; isFocusable = active
                if (active) setOnClickListener {
                    animate().scaleX(0.75f).scaleY(0.75f).setDuration(60).withEndAction {
                        animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    }.start()
                    action()
                }
            }

            addView(arrowBtn("↑", canMoveUp, onMoveUp))
            addView(arrowBtn("↓", canMoveDown, onMoveDown))

            addView(Switch(context).apply {
                isChecked = getKey<Boolean>(key) ?: (key !in TORRENT_KEYS)
                thumbTintList = android.content.res.ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                    intArrayOf(Color.WHITE, Color.parseColor("#9099B8"))
                )
                trackTintList = android.content.res.ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                    intArrayOf(if (isTorrent) Color.parseColor("#8B5E3C") else SWITCH_ON, SWITCH_OFF)
                )
                setOnCheckedChangeListener { _, v -> setKey(key, v) }
            })
        }
    }

    private fun pillBtn(
        context: Context,
        label: String,
        textColor: Int,
        bgColor: Int,
        borderColor: Int,
        onClick: () -> Unit
    ) = TextView(context).apply {
        text = label; textSize = 11f
        setTypeface(null, android.graphics.Typeface.BOLD)
        setTextColor(textColor)
        setPadding(12.dp(context), 6.dp(context), 12.dp(context), 6.dp(context))
        background = GradientDrawable().apply {
            cornerRadius = 99f; setColor(bgColor); setStroke(1, borderColor)
        }
        isClickable = true; isFocusable = true
        setOnClickListener {
            animate().scaleX(0.88f).scaleY(0.88f).setDuration(70).withEndAction {
                animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            }.start()
            onClick()
        }
    }

    private fun createHeroBanner(context: Context): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28.dp(context), 32.dp(context), 28.dp(context), 24.dp(context))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.parseColor("#1A1730"), BG_DARK))
            addView(View(context).apply {
                layoutParams = LinearLayout.LayoutParams(48.dp(context), 4.dp(context)).also { it.bottomMargin = 16.dp(context) }
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
                textSize = 13f; setTextColor(TEXT_SECONDARY)
                setPadding(0, 6.dp(context), 0, 0)
            })
        }
    }

    private fun createSectionCard(context: Context, title: String, block: LinearLayout.() -> Unit): View {
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val m = 16.dp(context)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(m, 0, m, m) }
            background = roundRect(BG_CARD, 16f.dp(context)); elevation = 4f
        }
        card.addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20.dp(context), 16.dp(context), 20.dp(context), 12.dp(context))
            gravity = Gravity.CENTER_VERTICAL
            addView(View(context).apply {
                layoutParams = LinearLayout.LayoutParams(3.dp(context), 18.dp(context)).also { it.marginEnd = 12.dp(context) }
                background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(ACCENT_START, ACCENT_END)).apply { cornerRadius = 99f }
            })
            addView(TextView(context).apply {
                text = title; textSize = 12f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(TEXT_SECONDARY); letterSpacing = 0.08f
            })
        })
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL; setPadding(0, 0, 0, 8.dp(context))
        }
        content.block(); card.addView(content)
        card.alpha = 0f; card.translationY = 20f
        card.animate().alpha(1f).translationY(0f).setDuration(300).setInterpolator(DecelerateInterpolator()).start()
        return card
    }

    private fun createToggleRow(
        context: Context, label: String, subtitle: String,
        databaseKey: String, defaultState: Boolean, onChanged: () -> Unit = {}
    ): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20.dp(context), 14.dp(context), 16.dp(context), 14.dp(context))
            gravity = Gravity.CENTER_VERTICAL; background = stateDrawable(context)
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
            addView(Switch(context).apply {
                isChecked = getKey<Boolean>(databaseKey) ?: defaultState
                thumbTintList = android.content.res.ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                    intArrayOf(Color.WHITE, Color.parseColor("#9099B8"))
                )
                trackTintList = android.content.res.ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                    intArrayOf(SWITCH_ON, SWITCH_OFF)
                )
                setOnCheckedChangeListener { _, isChecked ->
                    setKey(databaseKey, isChecked)
                    animate().scaleX(0.92f).scaleY(0.92f).setDuration(80).withEndAction {
                        animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                    }.start()
                    onChanged()
                }
            })
        }
    }

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
                background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(WARN_ACCENT) }
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

    private fun createDangerCard(context: Context): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val m = 16.dp(context)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(m, 0, m, m) }
            background = GradientDrawable().apply {
                cornerRadius = 16f.dp(context)
                setColor(Color.parseColor("#140A0D")); setStroke(1, Color.parseColor("#3D1520"))
            }
            setPadding(20.dp(context), 18.dp(context), 20.dp(context), 18.dp(context))
            addView(TextView(context).apply {
                text = "⚠️  Danger Zone"; textSize = 11f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(DANGER_COLOR); letterSpacing = 0.1f
                setPadding(0, 0, 0, 12.dp(context))
            })
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                val desc = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                desc.addView(TextView(context).apply {
                    text = "Clear Netmirror Cookies"; textSize = 15f
                    setTypeface(null, android.graphics.Typeface.BOLD); setTextColor(TEXT_PRIMARY)
                })
                desc.addView(TextView(context).apply {
                    text = "Remove saved Netmirror session cookies"
                    textSize = 12f; setTextColor(TEXT_SECONDARY); setPadding(0, 3.dp(context), 0, 0)
                })
                addView(desc)
                addView(TextView(context).apply {
                    text = "Clear"; textSize = 13f
                    setTypeface(null, android.graphics.Typeface.BOLD); setTextColor(DANGER_COLOR)
                    setPadding(18.dp(context), 10.dp(context), 18.dp(context), 10.dp(context))
                    background = GradientDrawable().apply {
                        cornerRadius = 99f; setColor(Color.parseColor("#2A0A11"))
                        setStroke(1, Color.parseColor("#5C1525"))
                    }
                    isClickable = true; isFocusable = true
                    setOnClickListener {
                        animate().alpha(0.5f).setDuration(80).withEndAction {
                            animate().alpha(1f).setDuration(120).start()
                        }.start()
                        clearCookie()
                        Toast.makeText(context, "🍪 Cookies cleared!", Toast.LENGTH_SHORT).show()
                    }
                })
            })
        }
    }

    private fun createDivider(context: Context): View {
        return View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1
            ).also { it.setMargins(20.dp(context), 0, 20.dp(context), 0) }
            setBackgroundColor(DIVIDER_COLOR)
        }
    }

    private fun showRestartWarning(context: Context, onSave: () -> Unit) {
        AlertDialog.Builder(context)
            .setTitle("Restart Required ⚠️")
            .setMessage("You've changed your Active Catalogs.\n\nPlease fully close and reopen Cloudstream for providers to update.")
            .setPositiveButton("Got it") { _, _ -> onSave() }
            .setCancelable(false).show()
    }

    private fun roundRect(color: Int, radius: Float) = GradientDrawable().apply {
        cornerRadius = radius; setColor(color)
    }

    private fun stateDrawable(context: Context) = StateListDrawable().apply {
        addState(intArrayOf(android.R.attr.state_pressed),
            GradientDrawable().apply { setColor(Color.parseColor("#1F2235")) })
        addState(intArrayOf(), GradientDrawable().apply { setColor(Color.TRANSPARENT) })
    }

    private fun Int.dp(context: Context): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), context.resources.displayMetrics).toInt()

    private fun Float.dp(context: Context): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this, context.resources.displayMetrics)
}
