package com.megix

import android.util.Log
import com.lagradost.cloudstream3.app
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

object ApiConstants {

    // ── 1. Static APIs ──────────────────────────────────────────
    const val malsyncAPI = "https://api.malsync.moe"
    const val tokyoInsiderAPI = "https://www.tokyoinsider.com"
    const val WYZIESubsAPI = "https://sub.wyzie.io"
    const val MostraguardaAPI = "https://mostraguarda.stream"
    const val CC_COOKIE = BuildConfig.CC_COOKIE
    const val CINE_API = BuildConfig.CINE_API
    const val CASTLE_API = BuildConfig.CASTLE_API
    const val animepaheAPI = "https://animepahe.si"
    const val allmovielandAPI = "https://allmovieland.you"
    const val torrentioAPI = "https://torrentio.strem.fun/sort=seeders"
    const val anizoneAPI = "https://anizone.to"
    const val AllanimeAPI = "https://api.allanime.day/api"
    const val PrimeSrcApi = "https://primesrc.me"
    const val asiaflixAPI = "https://asiaflix.net"
    const val twoembedAPI = "https://2embed.cc"
    const val sudatchiAPI = "https://sudatchi.com"
    const val aniversehdAPI = "https://aniversehd.com"
    const val animezAPI = "https://animeyy.com"
    const val webStreamrAPI = """https://webstreamr.hayd.uk/{"multi":"on","al":"on","de":"on","es":"on","fr":"on","it":"on","mx":"on","mediaFlowProxyUrl":"","mediaFlowProxyPassword":"","disableExtractor_hubcloud":"on","disableExtractor_hubdrive":"on"}"""
    const val cinemaOSApi = "https://cinemaos.tech"
    const val vidfastProApi = "https://vidfast.pro"
    const val multiEmbededApi = "https://multiembed.mov"
    const val vidSrcApi = "https://api.rgshows.ru"
    const val vidSrcHindiApi = "https://hindi.rgshows.ru"
    const val dahmerMoviesAPI = "https://a.111477.xyz"
    const val hexaAPI = "https://theemoviedb.hexa.su"
    const val videasyAPI = "https://api.videasy.net"
    const val vidlinkAPI = "https://vidlink.pro"
    const val multiDecryptAPI = "https://enc-dec.app/api"
    const val animetoshoAPI = "https://feed.animetosho.org"
    const val anizipAPI = "https://api.ani.zip"
    const val mappleAPI = "https://mapple.uk"
    const val vidzeeApi = "https://player.vidzee.wtf"
    const val animeWorldAPI = "https://anime-world-stremio-addon.onrender.com"
    const val kissKhAPI = "https://kisskh.ws"
    const val bollywoodAPI = "https://tga-hd.api.hashhackers.com"
    const val bollywoodBaseAPI = "https://bollywood.eu.org"
    const val YflixAPI = "https://solarmovie.fi"
    const val vidstackAPI = "https://api.smashystream.top/api/v1"
    const val vidstackBaseAPI = "https://smashyplayer.top"
    const val notorrentAPI = "https://addon-osvh.onrender.com"
    const val xpassAPI = "https://play.xpass.top"
    const val cinemacityAPI = "https://cinemacity.cc"
    const val dramafullAPI = "https://dramafull.cc"
    const val akwamAPI = "https://ak.sv"
    const val flixIndiaAPI = "https://m.flixindia.xyz"
    const val levidiaAPI = "https://www.levidia.ch"
    const val femBoxAPI = "https://fembox.aether.mom"
    const val streamvixAPI = "https://streamvix.hayd.uk"
    const val projectfreetvAPI = "https://projectfreetv.sx"
    const val vidsrcCCAPI = "https://vidsrc.cc"
    const val autoembedAPI = "https://player2.autoembed.cc"
    const val watch32API = "https://watch32.sx"
    const val kuudereAPI = "https://kuudere.to"
    const val vidrockAPI = "https://vidrock.net"
    const val torrentsdbAPI = "https://torrentsdb.com/eyJsaW1pdCI6IjMiLCJkZWJyaWRvcHRpb25zIjpbIm5vZG93bmxvYWRsaW5rcyJdfQ=="

    // ── 2. Private Background Logic ──────────────────────────────
    private val apiConfig by lazy {
        runBlocking(Dispatchers.IO) {
            runCatching {
                JSONObject(app.get("https://raw.githubusercontent.com/SaurabhKaperwan/Utils/refs/heads/main/urls.json").text)
            }.getOrElse {
                Log.e("CineStream", "Error loading API URLs")
                JSONObject()
            }
        }
    }

    private fun api(key: String) = apiConfig.optString(key)

    // ── 3. Dynamic APIs ──────────────────────────────────────────
    val protonmoviesAPI get() = api("protonmovies")
    val fourkhdhubAPI get() = api("4khdhub")
    val multimoviesAPI get() = api("multimovies")
    val bollyflixAPI get() = api("bollyflix")
    val movies4uAPI get() = api("movies4u")
    val skymoviesAPI get() = api("skymovies")
    val hindMoviezAPI get() = api("hindmoviez")
    val hdmovie2API get() = api("hdmovie2")
    val netflixAPI get() = api("nfmirror")
    val netflix2API get() = api("nfmirror2")
    val moviesdriveAPI get() = api("moviesdrive")
    val gojoBaseAPI get() = api("gojo_base")
    val vegamoviesAPI get() = api("vegamovies")
    val rogmoviesAPI get() = api("rogmovies")
    val uhdmoviesAPI get() = api("uhdmovies")
    val moviesmodAPI get() = api("moviesmod")
    val topmoviesAPI get() = api("topmovies")
    val toonStreamAPI get() = api("toonstream")
    val hianimeAPI get() = api("hianime")
    val XDmoviesAPI get() = api("xdmovies")
    val animekaiAPI get() = api("animekai")
    val rtallyAPI get() = api("rtally")
}
