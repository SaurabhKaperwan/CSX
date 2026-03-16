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
    const val DOWNLOAD_ENABLE          = "DownloadEnable"
    const val PROVIDER_CINESTREAM      = "ProviderCineStream"
    const val PROVIDER_SIMKL           = "ProviderSimkl"
    const val PROVIDER_TMDB            = "ProviderTmdb"
    const val SHOWBOX_TOKEN_KEY        = "showbox_ui_token"
    const val STREMIO_ADDONS_KEY       = "stremio_addons"
    const val NEW_PROVIDER_DEFAULT_ON  = "new_provider_default_on"

    private const val COOKIE_KEY         = "nf_cookie"
    private const val TIMESTAMP_KEY      = "nf_cookie_timestamp"
    private const val SEEN_PROVIDERS_KEY = "seen_providers"
    private const val PROVIDER_ORDER_KEY = "provider_order"

    // ── Configuration Getters ────────────────────────────────
    val allowDownloadLinks: Boolean
        get() = getKey<Boolean>(DOWNLOAD_ENABLE) ?: false

    val activeProviderOrder: List<String>
        get() = getOrder().filter { enabled(it) }

    // ── Dynamic Provider Maps ────────────────────────────────
    // We dynamically pull these from our single source of truth (ProviderRegistry)!
    val TORRENT_KEYS: Set<String> get() = ProviderRegistry.torrentKeys
    val PROVIDER_NAMES: Map<String, String> get() = ProviderRegistry.namesMap
    val DEFAULT_ORDER: List<String> get() = ProviderRegistry.keys

    // ── Provider ordering ────────────────────────────────────

    fun newProviderDefaultOn(): Boolean = getKey<Boolean>(NEW_PROVIDER_DEFAULT_ON) ?: true

    fun getSeenProviders(): Set<String> =
        getKey<String>(SEEN_PROVIDERS_KEY)
            ?.split(",")?.filter { it.isNotBlank() }?.toSet()
            ?: emptySet()

    fun initSeenProviders() {
        val stremioKeys = getStremioAddons().map { stremioAddonKey(it.name) }
        val allKnown    = (DEFAULT_ORDER + stremioKeys).toSet()
        val currentSeen = getSeenProviders()

        if (currentSeen.isEmpty()) {
            setKey(SEEN_PROVIDERS_KEY, allKnown.joinToString(","))
            return
        }

        val defaultState = newProviderDefaultOn()

        allKnown.forEach { key ->
            if (key !in currentSeen
                && !key.startsWith("stremio_")
                && key !in TORRENT_KEYS
                && getKey<Boolean>(key) == null
            ) {
                setKey(key, defaultState)
            }
        }

        val removedKeys = currentSeen - allKnown
        removedKeys.forEach { setKey(it, null as Boolean?) }
        val prunedSeen  = (currentSeen - removedKeys) + allKnown
        setKey(SEEN_PROVIDERS_KEY, prunedSeen.joinToString(","))
    }

    fun markProvidersSeen(keys: Collection<String>) {
        val currentlySeen = getSeenProviders()
        val defaultState  = newProviderDefaultOn()
        keys.forEach { key ->
            if (key !in currentlySeen
                && !key.startsWith("stremio_")
                && key !in TORRENT_KEYS
                && getKey<Boolean>(key) == null
            ) {
                setKey(key, defaultState)
            }
        }
        val merged = currentlySeen + keys
        setKey(SEEN_PROVIDERS_KEY, merged.joinToString(","))
    }

    fun enabled(key: String): Boolean {
        val explicit = getKey<Boolean>(key)
        if (explicit != null) return explicit
        if (key.startsWith("stremio_")) return true
        if (key in TORRENT_KEYS) return false
        if (key !in getSeenProviders()) return newProviderDefaultOn()
        return true
    }

    fun getOrder(): List<String> {
        val stremioKeys = getStremioAddons().map { stremioAddonKey(it.name) }
        val allKnown    = DEFAULT_ORDER + stremioKeys
        val saved       = getKey<String>(PROVIDER_ORDER_KEY)
            ?.split(",")?.filter { it.isNotBlank() }
            ?: return allKnown

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
