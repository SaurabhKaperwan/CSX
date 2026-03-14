package com.megix

import android.content.Context
import com.lagradost.cloudstream3.AcraApplication.Companion.getKey
import com.lagradost.cloudstream3.AcraApplication.Companion.setKey

/**
 * Public API surface for CineStream settings.
 * UI is delegated to SettingsDialog; theme helpers live in SettingsTheme.
 */
object Settings {

    // ── Global keys ──────────────────────────────────────────
    const val DOWNLOAD_ENABLE     = "DownloadEnable"
    const val PROVIDER_CINESTREAM = "ProviderCineStream"
    const val PROVIDER_SIMKL      = "ProviderSimkl"
    const val PROVIDER_TMDB       = "ProviderTmdb"
    const val SHOWBOX_TOKEN_KEY        = "showbox_ui_token"
    const val STREMIO_ADDONS_KEY       = "stremio_addons"
    const val NEW_PROVIDER_DEFAULT_ON  = "new_provider_default_on"
    private const val COOKIE_KEY        = "nf_cookie"
    private const val TIMESTAMP_KEY     = "nf_cookie_timestamp"
    private const val SEEN_PROVIDERS_KEY = "seen_providers"

    // ── Provider keys ────────────────────────────────────────
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
    const val P_HIANIME       = "p_hianime"
    const val P_ANIMEPAHE     = "p_animepahe"
    const val P_ANIMEZ        = "p_animez"
    const val P_ANIMEKAI      = "p_animekai"
    const val P_VIDSRCCC      = "p_vidsrccc"

    private const val PROVIDER_ORDER_KEY = "provider_order"

    val TORRENT_KEYS = setOf(P_TORRENTIO, P_TORRENTSDB, P_ANIMETOSHO)

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
        P_HIANIME       to "Hianime",
        P_ANIMEPAHE     to "AnimePahe",
        P_ANIMEZ        to "AnimeZ",
        P_ANIMEKAI      to "Animekai",
        P_VIDSRCCC      to "Vidsrccc",
    )

    val DEFAULT_ORDER: List<String> get() = PROVIDER_NAMES.keys.toList()

    // ── Provider ordering ────────────────────────────────────

    /** Whether new (previously unseen) built-in providers are enabled by default. */
    fun newProviderDefaultOn(): Boolean = getKey<Boolean>(NEW_PROVIDER_DEFAULT_ON) ?: true

    /** Keys the user has already seen/encountered at least once. */
    fun getSeenProviders(): Set<String> =
        getKey<String>(SEEN_PROVIDERS_KEY)
            ?.split(",")?.filter { it.isNotBlank() }?.toSet()
            ?: emptySet()

    /**
     * On first install the seen set is empty — seed it with all current
     * providers WITHOUT baking explicit values so natural defaults apply
     * (torrent = off, everything else = on).
     * On updates storage is intact so this is effectively a no-op.
     */
    fun initSeenProviders() {
        val stremioKeys = getStremioAddons().map { stremioAddonKey(it.name) }
        val allKnown    = (DEFAULT_ORDER + stremioKeys).toSet()
        val currentSeen = getSeenProviders()

        if (currentSeen.isEmpty()) {
            // First install — seed without baking, natural defaults apply
            setKey(SEEN_PROVIDERS_KEY, allKnown.joinToString(","))
            return
        }

        val defaultState = newProviderDefaultOn()

        // Bake explicit value for any new provider the moment the plugin loads.
        // This guarantees the user's preference is respected even if they never
        // open Settings after an update.
        allKnown.forEach { key ->
            if (key !in currentSeen
                && !key.startsWith("stremio_")
                && key !in TORRENT_KEYS
                && getKey<Boolean>(key) == null
            ) {
                setKey(key, defaultState)
            }
        }

        // Prune removed provider keys from seen set and wipe their stored values
        val removedKeys = currentSeen - allKnown
        removedKeys.forEach { setKey(it, null as Boolean?) }
        val prunedSeen  = (currentSeen - removedKeys) + allKnown
        setKey(SEEN_PROVIDERS_KEY, prunedSeen.joinToString(","))
    }

    /**
     * Call on every Save so newly appeared keys become "seen".
     * For any key that is genuinely new (wasn't seen before, no explicit saved
     * value), immediately persists the current newProviderDefaultOn() as its
     * explicit state. This means the preference is evaluated exactly once —
     * at the moment of first encounter — and survives all future loads and reinstalls.
     */
    fun markProvidersSeen(keys: Collection<String>) {
        val currentlySeen = getSeenProviders()
        val defaultState  = newProviderDefaultOn()
        keys.forEach { key ->
            if (key !in currentlySeen                // genuinely new
                && !key.startsWith("stremio_")       // stremio has its own rule
                && key !in TORRENT_KEYS              // torrents are always off
                && getKey<Boolean>(key) == null      // no explicit value yet
            ) {
                setKey(key, defaultState)            // bake in the preference permanently
            }
        }
        val merged = currentlySeen + keys
        setKey(SEEN_PROVIDERS_KEY, merged.joinToString(","))
    }

    fun enabled(key: String): Boolean {
        // 1. Explicit user choice always wins
        val explicit = getKey<Boolean>(key)
        if (explicit != null) return explicit
        // 2. Stremio addons are always on by default
        if (key.startsWith("stremio_")) return true
        // 3. Torrent keys are always off by default
        if (key in TORRENT_KEYS) return false
        // 4. Unseen built-in providers respect the new-provider preference
        if (key !in getSeenProviders()) return newProviderDefaultOn()
        // 5. Known provider with no explicit value → on
        return true
    }

    fun getOrder(): List<String> {
        val stremioKeys = getStremioAddons().map { stremioAddonKey(it.name) }
        val allKnown    = DEFAULT_ORDER + stremioKeys
        val saved       = getKey<String>(PROVIDER_ORDER_KEY)
            ?.split(",")?.filter { it.isNotBlank() }
            ?: return allKnown
        // Strip keys that no longer exist — removed built-in providers or deleted stremio addons
        val validSaved = saved.filter { it in allKnown }
        return validSaved + (allKnown - validSaved.toSet())
    }

    fun saveOrder(order: List<String>) =
        setKey(PROVIDER_ORDER_KEY, order.joinToString(","))

    // ── Stremio addon helpers ────────────────────────────────

    enum class AddonType { HTTPS, TORRENT, DEBRID, SUBTITLE }

    data class StremioAddon(val name: String, val url: String, val type: AddonType)

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

    fun isStremioTorrent(key: String): Boolean {
        if (!key.startsWith("stremio_")) return false
        return getStremioAddons().firstOrNull { stremioAddonKey(it.name) == key }
            ?.type == AddonType.TORRENT
    }

    fun getStremioAddons(): MutableList<StremioAddon> {
        val raw = getKey<String>(STREMIO_ADDONS_KEY) ?: return mutableListOf()
        return raw.lines().filter { it.isNotBlank() }.mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size < 3) return@mapNotNull null
            val type = runCatching { AddonType.valueOf(parts[2]) }.getOrDefault(AddonType.HTTPS)
            StremioAddon(name = parts[0], url = parts[1], type = type)
        }.toMutableList()
    }

    fun saveStremioAddons(addons: List<StremioAddon>) {
        if (addons.isEmpty()) setKey(STREMIO_ADDONS_KEY, null as String?)
        else setKey(STREMIO_ADDONS_KEY, addons.joinToString("\n") { "${it.name}|${it.url}|${it.type}" })
        val validKeys = addons.map { stremioAddonKey(it.name) }.toSet()
        val current   = getKey<String>(PROVIDER_ORDER_KEY)
            ?.split(",")?.filter { it.isNotBlank() } ?: return
        val pruned = current.filter { !it.startsWith("stremio_") || it in validKeys }
        if (pruned.size != current.size) saveOrder(pruned)
    }

    // ── Cookie helpers ───────────────────────────────────────

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

    // ── ShowBox token helpers ────────────────────────────────

    fun saveShowboxToken(token: String) = setKey(SHOWBOX_TOKEN_KEY, token.trim())
    fun getShowboxToken(): String?       = getKey<String>(SHOWBOX_TOKEN_KEY)?.takeIf { it.isNotBlank() }
    fun clearShowboxToken()              = setKey(SHOWBOX_TOKEN_KEY, null)

    // ── Entry point ──────────────────────────────────────────

    fun showSettingsDialog(context: Context, onSave: () -> Unit) =
        SettingsDialog.show(context, onSave)
}


