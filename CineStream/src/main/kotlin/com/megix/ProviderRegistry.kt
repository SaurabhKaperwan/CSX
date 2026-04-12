package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

/** Container for data fetched during MALSync requests */
data class MalSyncData(
    val title: String?,
    val zorotitle: String?,
    val hianimeurl: String?,
    val animepaheUrl: String?,
    val aniId: Int?,
    val episode: Int?,
    val year: Int?,
    val origin: String,
    val animepaheTitle: String?
)

/** * Defines a provider and its execution logic for Standard, Anime, and MALSync data.
 * The `CineStreamExtractors.` receiver allows direct access to internal scraping functions.
 */
data class ProviderDef(
    val key: String,
    val displayName: String,
    val isTorrent: Boolean = false,
    val executeStandard: (suspend CineStreamExtractors.(res: AllLoadLinksData, subCb: (SubtitleFile) -> Unit, cb: (ExtractorLink) -> Unit) -> Unit)? = null,
    val executeAnime: (suspend CineStreamExtractors.(res: AllLoadLinksData, subCb: (SubtitleFile) -> Unit, cb: (ExtractorLink) -> Unit) -> Unit)? = null,
    val executeMalSync: (suspend CineStreamExtractors.(data: MalSyncData, subCb: (SubtitleFile) -> Unit, cb: (ExtractorLink) -> Unit) -> Unit)? = null
)

object ProviderRegistry {

    val builtInProviders = listOf(
        // ── Torrents ──────────────────────────────────────────────
        ProviderDef(
            key = "p_meteor", displayName = "🧲 Meteor", isTorrent = true,
            executeStandard = { res, _, cb -> invokeStremioTorrents("Meteor", meteorAPI, res.imdbId, res.season, res.episode, cb) },
            executeAnime = { res, _, cb -> invokeStremioTorrents("Meteor", meteorAPI, "kitsu:${res.kitsuId}", res.season, res.episode, cb) }
        ),
        ProviderDef(
            key = "p_torrentsdb", displayName = "🧲 TorrentsDB", isTorrent = true,
            executeStandard = { res, _, cb -> invokeStremioTorrents("TorrentsDB", torrentsdbAPI, res.imdbId, res.season, res.episode, cb) },
            executeAnime = { res, _, cb -> invokeStremioTorrents("TorrentsDB", torrentsdbAPI, "kitsu:${res.kitsuId}", res.season, res.episode, cb) }
        ),
        ProviderDef(
            key = "p_animetosho", displayName = "🧲 AnimeTosho", isTorrent = true,
            executeAnime = { res, _, cb -> invokeAnimetosho(res.kitsuId, res.malId, res.episode, cb) }
        ),

        // ── Stremio Addons & Subtitles ────────────────────────────
        ProviderDef(
            key = "p_streamvix", displayName = "Streamvix",
            executeStandard = { res, subCb, cb -> invokeStremioStreams("Streamvix", streamvixAPI, res.imdbId, res.season, res.episode, subCb, cb) }
        ),
        ProviderDef(
            key = "p_notorrent", displayName = "NoTorrent",
            executeStandard = { res, subCb, cb -> invokeStremioStreams("NoTorrent", notorrentAPI, res.imdbId, res.season, res.episode, subCb, cb) }
        ),
        ProviderDef(
            key = "p_castle", displayName = "Castle",
            executeStandard = { res, subCb, cb -> invokeStremioStreams("Castle", CASTLE_API, res.imdbId, res.season, res.episode, subCb, cb) },
            executeAnime = { res, subCb, cb -> if (res.imdbSeason == null || res.imdbSeason == 1) invokeStremioStreams("Castle", CASTLE_API, res.imdbId, res.imdbSeason, res.imdbEpisode, subCb, cb) }
        ),
        ProviderDef(
            key = "p_cine", displayName = "Cine",
            executeStandard = { res, subCb, cb -> invokeStremioStreams("Cine", CINE_API, res.imdbId, res.season, res.episode, subCb, cb) }
        ),
        ProviderDef(
            key = "p_animeworld", displayName = "AnimeWorld",
            executeAnime = { res, subCb, cb -> invokeStremioStreams("Anime World Multi Audio 🌐", animeWorldAPI, res.imdbId, res.imdbSeason, res.imdbEpisode, subCb, cb) }
        ),
        ProviderDef(
            key = "p_wyziesubs", displayName = "WYZIESubs",
            executeStandard = { res, subCb, _ -> invokeWYZIESubs(res.imdbId, res.season, res.episode, subCb) },
            executeAnime = { res, subCb, _ -> invokeWYZIESubs(res.imdbId, res.imdbSeason, res.imdbEpisode, subCb) }
        ),
        ProviderDef(
            key = "p_stremiosubs", displayName = "StremioSubs",
            executeStandard = { res, subCb, _ -> invokeStremioSubtitles(res.imdbId, res.season, res.episode, subCb) },
            executeAnime = { res, subCb, _ -> invokeStremioSubtitles(res.imdbId, res.imdbSeason, res.imdbEpisode, subCb) }
        ),

        // ── Direct HTTP Providers ─────────────────────────────────
        // ProviderDef(
        //     key = "p_xdmovies", displayName = "XDMovies",
        //     executeStandard = { res, subCb, cb -> invokeXDmovies(res.title, res.tmdbId, res.season, res.episode, subCb, cb) },
        //     executeAnime = { res, subCb, cb -> invokeXDmovies(res.imdbTitle, res.tmdbId, res.imdbSeason, res.imdbEpisode, subCb, cb) }
        // ),
        ProviderDef(
            key = "p_showbox", displayName = "ShowBox",
            executeStandard = { res, subCb, cb -> invokeShowbox(res.tmdbId, res.season, res.episode, subCb, cb) },
            executeAnime = { res, subCb, cb -> invokeShowbox(res.tmdbId, res.imdbSeason, res.imdbEpisode, subCb, cb) }
        ),
        ProviderDef(
            key = "p_vidflix", displayName = "Vidflix",
            executeStandard = { res, _, cb -> invokeVidflix(res.tmdbId, res.season, res.episode, cb) }
        ),
        ProviderDef(
            key = "p_vidrock", displayName = "Vidrock",
            executeStandard = { res, _, cb -> invokeVidrock(res.tmdbId, res.season, res.episode, cb) }
        ),
        ProviderDef(
            key = "p_moviebox", displayName = "Moviebox",
            executeStandard = { res, subCb, cb -> invokeMoviebox(res.title, res.season, res.episode, subCb, cb) },
            executeAnime = { res, subCb, cb -> invokeMoviebox(res.imdbTitle, res.imdbSeason, res.imdbEpisode, subCb, cb) }
        ),
        ProviderDef(
            key = "p_cinemacity", displayName = "Cinemacity",
            executeStandard = { res, subCb, cb -> invokeCinemacity(res.imdbId, res.season, res.episode, subCb, cb) },
            executeAnime = { res, subCb, cb -> invokeCinemacity(res.imdbId, res.imdbSeason, res.imdbEpisode, subCb, cb) }
        ),
        ProviderDef(
            key = "p_allmovieland", displayName = "Allmovieland",
            executeStandard = { res, _, cb -> invokeAllmovieland(res.imdbId, res.season, res.episode, cb) },
        ),
        ProviderDef(
            key = "p_madplaycdn", displayName = "MadplayCDN",
            executeStandard = { res, _, cb -> invokeMadplayCDN(res.tmdbId, res.season, res.episode, cb) }
        ),
        ProviderDef(
            key = "p_hexa", displayName = "Hexa",
            executeStandard = { res, _, cb -> invokeHexa(res.tmdbId, res.season, res.episode, cb) },
        ),
        ProviderDef(
            key = "p_yflix", displayName = "Yflix",
            executeStandard = { res, subCb, cb -> invokeYflix(res.tmdbId, res.season, res.episode, subCb, cb) }
        ),
        ProviderDef(
            key = "p_xpass", displayName = "Xpass",
            executeStandard = { res, subCb, cb -> invokeXpass(res.tmdbId, res.season, res.episode, subCb, cb) }
        ),
        ProviderDef(
            key = "p_playsrc", displayName = "Playsrc",
            executeStandard = { res, _, cb -> invokePlaysrc(res.tmdbId, res.season, res.episode, cb) }
        ),
        ProviderDef(
            key = "p_2embed", displayName = "2Embed",
            executeStandard = { res, _, cb -> if (!res.isAnime) invoke2embed(res.imdbId, res.season, res.episode, cb) }
        ),
        ProviderDef(
            key = "p_videasy", displayName = "Videasy",
            executeStandard = { res, subCb, cb -> invokeVideasy(res.title, res.tmdbId, res.imdbId, res.year, res.season, res.episode, subCb, cb) }
        ),
        ProviderDef(
            key = "p_cinemaos", displayName = "CinemaOS",
            executeStandard = { res, subCb, cb -> invokeCinemaOS(res.imdbId, res.tmdbId, res.season, res.episode, subCb, cb) }
        ),
        ProviderDef(
            key = "p_vicsrcwtf", displayName = "VicSrcWtf",
            executeStandard = { res, subCb, cb -> invokeVicSrcWtf(res.tmdbId, res.season, res.episode, cb, subCb) }
        ),
        ProviderDef(
            key = "p_vidlink", displayName = "Vidlink",
            executeStandard = { res, subCb, cb -> invokeVidlink(res.tmdbId, res.season, res.episode, subCb, cb) },
        ),
        ProviderDef(
            key = "p_pulp", displayName = "Pulp",
            executeStandard = { res, subCb, cb -> invokePulp(res.tmdbId, res.season, res.episode, subCb, cb) },
            executeAnime = { res, subCb, cb -> invokePulp(res.tmdbId, res.imdbSeason, res.imdbEpisode, subCb, cb) },
        ),
        // ProviderDef(
        //     key = "p_mapple", displayName = "Mapple",
        //     executeStandard = { res, _, cb -> invokeMapple(res.tmdbId, res.season, res.episode, cb) },
        // ),
        ProviderDef(
            key = "p_vidstack", displayName = "Vidstack",
            executeStandard = { res, subCb, cb -> invokeVidstack(res.imdbId, res.season, res.episode, subCb, cb) }
        ),
        ProviderDef(
            key = "p_vidzee", displayName = "Vidzee",
            executeStandard = { res, subCb, cb -> invokeVidzee(res.tmdbId, res.season, res.episode, subCb, cb) }
        ),
        ProviderDef(
            key = "p_netflix", displayName = "Netflix",
            executeStandard = { res, subCb, cb -> invokeNetflix(res.title, res.year, res.season, res.episode, subCb, cb) },
            executeAnime = { res, subCb, cb -> invokeNetflix(res.imdbTitle, res.year, res.imdbSeason, res.imdbEpisode, subCb, cb) }
        ),
        ProviderDef(
            key = "p_primevideo", displayName = "Prime Video",
            executeStandard = { res, subCb, cb -> invokePrimeVideo(res.title, res.year, res.season, res.episode, subCb, cb) },
            executeAnime = { res, subCb, cb -> invokePrimeVideo(res.imdbTitle, res.year, res.imdbSeason, res.imdbEpisode, subCb, cb) }
        ),
        ProviderDef(
            key = "p_disney", displayName = "Hotstar",
            executeStandard = { res, subCb, cb -> invokeDisney(res.title, res.year, res.season, res.episode, subCb, cb) },
            executeAnime = { res, subCb, cb -> invokeDisney(res.imdbTitle, res.year, res.imdbSeason, res.imdbEpisode, subCb, cb) }
        ),
        ProviderDef(
            key = "p_bollywood", displayName = "Gramcinema",
            executeStandard = { res, _, cb -> invokeBollywood(res.title, res.year, res.season, res.episode, cb) },
            executeAnime = { res, _, cb -> invokeBollywood(res.imdbTitle, res.year, res.imdbSeason, res.imdbEpisode, cb) }
        ),
        ProviderDef(
            key = "p_flixindia", displayName = "FlixIndia",
            executeStandard = { res, subCb, cb -> invokeFlixIndia(res.title, res.year, res.season, res.episode, subCb, cb) },
            executeAnime = { res, subCb, cb -> invokeFlixIndia(res.imdbTitle, res.year, res.imdbSeason, res.imdbEpisode, subCb, cb) }
        ),
        ProviderDef(
            key = "p_vegamovies", displayName = "VegaMovies",
            executeStandard = { res, subCb, cb -> if (!res.isBollywood) invokeVegamovies("VegaMovies", res.imdbId, res.season, res.episode, subCb, cb) },
            executeAnime = { res, subCb, cb -> invokeVegamovies("VegaMovies", res.imdbId, res.imdbSeason, res.imdbEpisode, subCb, cb) }
        ),
        ProviderDef(
            key = "p_rogmovies", displayName = "RogMovies",
            executeStandard = { res, subCb, cb -> if (res.isBollywood) invokeVegamovies("RogMovies", res.imdbId, res.season, res.episode, subCb, cb) }
        ),
        ProviderDef(
            key = "p_bollyflix", displayName = "Bollyflix",
            executeStandard = { res, subCb, cb -> invokeBollyflix(res.imdbId, res.season, res.episode, subCb, cb) },
            executeAnime = { res, subCb, cb -> invokeBollyflix(res.imdbId, res.imdbSeason, res.imdbEpisode, subCb, cb) }
        ),
        ProviderDef(
            key = "p_topmovies", displayName = "TopMovies",
            executeStandard = { res, subCb, cb -> if (res.isBollywood) invokeTopMovies(res.imdbId, res.season, res.episode, subCb, cb) }
        ),
        ProviderDef(
            key = "p_moviesmod", displayName = "Moviesmod",
            executeStandard = { res, subCb, cb -> if (!res.isBollywood) invokeMoviesmod(res.imdbId, res.season, res.episode, subCb, cb) },
            executeAnime = { res, subCb, cb -> invokeMoviesmod(res.imdbId, res.imdbSeason, res.imdbEpisode, subCb, cb) }
        ),
        ProviderDef(
            key = "p_movies4u", displayName = "Movies4u",
            executeStandard = { res, subCb, cb -> invokeMovies4u(res.imdbId, res.title, res.year, res.season, res.episode, subCb, cb) },
            executeAnime = { res, subCb, cb -> invokeMovies4u(res.imdbId, res.imdbTitle, res.imdbYear, res.imdbSeason, res.imdbEpisode, subCb, cb) }
        ),
        ProviderDef(
            key = "p_uhdmovies", displayName = "UHDMovies",
            executeStandard = { res, subCb, cb -> if (!res.isBollywood) invokeUhdmovies(res.title, res.year, res.season, res.episode, cb, subCb) },
            executeAnime = { res, subCb, cb -> invokeUhdmovies(res.imdbTitle, res.imdbYear, res.imdbSeason, res.imdbEpisode, cb, subCb) }
        ),
        ProviderDef(
            key = "p_moviesdrive", displayName = "MoviesDrive",
            executeStandard = { res, subCb, cb -> invokeMoviesdrive(res.title, res.imdbId, res.season, res.episode, subCb, cb) },
            executeAnime = { res, subCb, cb -> invokeMoviesdrive(res.imdbTitle, res.imdbId, res.imdbSeason, res.imdbEpisode, subCb, cb) }
        ),
        ProviderDef(
            key = "p_hindmoviez", displayName = "Hindmoviez",
            executeStandard = { res, _, cb -> if (!res.isBollywood) invokeHindmoviez(res.imdbId, res.season, res.episode, cb) },
            executeAnime = { res, _, cb -> invokeHindmoviez(res.imdbId, res.imdbSeason, res.imdbEpisode, cb) }
        ),
        ProviderDef(
            key = "p_4khdhub", displayName = "4KHDHub",
            executeStandard = { res, subCb, cb -> if (!res.isBollywood) invoke4khdhub(res.title, res.year, res.season, res.episode, subCb, cb) },
            executeAnime = { res, subCb, cb -> invoke4khdhub(res.imdbTitle, res.imdbYear, res.imdbSeason, res.imdbEpisode, subCb, cb) }
        ),
        ProviderDef(
            key = "p_primesrc", displayName = "PrimeSrc",
            executeStandard = { res, subCb, cb -> invokePrimeSrc(res.imdbId, res.season, res.episode, subCb, cb) },
            executeAnime = { res, subCb, cb -> invokePrimeSrc(res.imdbId, res.imdbSeason, res.imdbEpisode, subCb, cb) }
        ),
        ProviderDef(
            key = "p_projectfreetv", displayName = "ProjectFreeTV",
            executeStandard = { res, subCb, cb -> invokeProjectfreetv(res.title, res.airedYear, res.season, res.episode, subCb, cb) }
        ),
        ProviderDef(
            key = "p_levidia", displayName = "Levidia",
            executeStandard = { res, subCb, cb -> invokeLevidia(res.title, res.year, res.season, res.episode, subCb, cb) },
        ),
        ProviderDef(
            key = "p_dahmermovies", displayName = "DahmerMovies",
            executeStandard = { res, _, cb -> invokeDahmerMovies(res.title, res.year, res.season, res.episode, cb) },
            executeAnime = { res, _, cb -> invokeDahmerMovies(res.imdbTitle, res.imdbYear, res.imdbSeason, res.imdbEpisode, cb) }
        ),
        ProviderDef(
            key = "p_multimovies", displayName = "Multimovies",
            executeStandard = { res, subCb, cb -> invokeMultimovies(res.title, res.season, res.episode, subCb, cb) },
            executeAnime = { res, subCb, cb -> invokeMultimovies(res.imdbTitle, res.imdbSeason, res.imdbEpisode, subCb, cb) }
        ),
        ProviderDef(
            key = "p_protonmovies", displayName = "Protonmovies",
            executeStandard = { res, subCb, cb -> invokeProtonmovies(res.imdbId, res.season, res.episode, subCb, cb) },
            executeAnime = { res, subCb, cb -> invokeProtonmovies(res.imdbId, res.imdbSeason, res.imdbEpisode, subCb, cb) }
        ),
        ProviderDef(
            key = "p_akwam", displayName = "Akwam",
            executeStandard = { res, subCb, cb -> invokeAkwam(res.imdbId, res.title, res.airedYear, res.season, res.episode, subCb, cb) }
        ),
        ProviderDef(
            key = "p_rtally", displayName = "Rtally",
            executeStandard = { res, subCb, cb -> invokeRtally(res.title, res.season, res.episode, subCb, cb) }
        ),
        ProviderDef(
            key = "p_asiaflix", displayName = "Asiaflix",
            executeStandard = { res, subCb, cb -> if (!res.isAnime) invokeAsiaflix(res.title, res.season, res.episode, res.airedYear, subCb, cb) }
        ),
        ProviderDef(
            key = "p_skymovies", displayName = "SkyMovies",
            executeStandard = { res, subCb, cb -> if (!res.isAnime) invokeSkymovies(res.title, res.airedYear, res.episode, subCb, cb) }
        ),
        ProviderDef(
            key = "p_hdmovie2", displayName = "HDMovie2",
            executeStandard = { res, subCb, cb -> if (!res.isAnime) invokeHdmovie2(res.title, res.airedYear, res.episode, subCb, cb) }
        ),
        ProviderDef(
            key = "p_mostraguarda", displayName = "Mostraguarda",
            executeStandard = { res, subCb, cb -> if (res.season == null) invokeMostraguarda(res.imdbId, subCb, cb) }
        ),
        ProviderDef(
            key = "p_vidsrccc", displayName = "VidsrcCC",
            executeStandard = { res, _, cb -> invokeVidsrcCC(res.imdbId, res.season, res.episode, cb) }
        ),
        ProviderDef(
            key = "p_autoembed", displayName = "AutoEmbed",
            executeStandard = { res, subCb, cb -> invokeAutoembed(res.imdbId, res.season, res.episode, subCb, cb) },
        ),
        ProviderDef(
            key = "p_watch32", displayName = "Watch32",
            executeStandard = { res, subCb, cb -> invokeWatch32(res.title, res.season, res.episode, subCb, cb) },
        ),
        // ProviderDef(
        //     key = "p_multiembeded", displayName = "Multiembeded",
        //     executeStandard = { res, subCb, cb -> invokeMultiEmbeded(res.tmdbId, res.season, res.episode, subCb, cb) },
        // ),

        // ── Asian Drama & Anime Specific (Including MALSync logic) ─
        ProviderDef(
            key = "p_kisskh", displayName = "KissKH",
            executeStandard = { res, subCb, cb -> if (res.isAsian) invokeKisskh(res.title, res.year, res.season, res.episode, subCb, cb) }
        ),
        ProviderDef(
            key = "p_dramafull", displayName = "Dramafull",
            executeStandard = { res, subCb, cb -> if (res.isAsian) invokeDramafull(res.title, res.year, res.season, res.episode, subCb, cb) }
        ),
        ProviderDef(
            key = "p_toonstream", displayName = "Toonstream",
            executeStandard = { res, subCb, cb -> if (res.isAnime || res.isCartoon) invokeToonstream(res.title, res.season, res.episode, subCb, cb) },
            executeAnime = { res, subCb, cb -> invokeToonstream(res.imdbTitle, res.imdbSeason, res.imdbEpisode, subCb, cb) }
        ),
        ProviderDef(
            key = "p_kaido", displayName = "Kaido",
            executeMalSync = { data, subCb, cb -> invokeKaido(data.hianimeurl, data.animepaheTitle ?: data.title, data.episode, subCb, cb) }
        ),
        ProviderDef(
            key = "p_animepahe", displayName = "AnimePahe",
            executeMalSync = { data, subCb, cb -> invokeAnimepahe(data.animepaheUrl, data.episode, subCb, cb) }
        ),
        ProviderDef(
            key = "p_animekai", displayName = "Animekai",
            executeMalSync = { data, subCb, cb -> invokeAnimekai(data.zorotitle ?: data.title, data.episode, subCb, cb) }
        ),
        ProviderDef(
            key = "p_animez", displayName = "AnimeZ",
            executeAnime = { res, _, cb -> invokeAnimez(res.title, res.episode, cb) },
            executeMalSync = { data, _, cb -> invokeAnimez(data.title ?: data.zorotitle, data.episode, cb) }
        ),
        ProviderDef(
            key = "p_allanime", displayName = "AllAnime",
            executeAnime = { res, subCb, cb -> invokeAllanime(res.originalTitle ?: res.title, res.year, res.episode, subCb, cb) },
            executeMalSync = { data, subCb, cb -> if (data.origin == "imdb") invokeAllanime(data.title, data.year, data.episode, subCb, cb) }
        ),
        ProviderDef(
            key = "p_tokyoinsider", displayName = "TokyoInsider",
            executeAnime = { res, subCb, cb -> invokeTokyoInsider(res.originalTitle ?: res.title, res.episode, subCb, cb) },
            executeMalSync = { data, subCb, cb -> if (data.origin == "imdb") invokeTokyoInsider(data.title, data.episode, subCb, cb) }
        ),
        ProviderDef(
            key = "p_anizone", displayName = "Anizone",
            executeAnime = { res, subCb, cb -> invokeAnizone(res.originalTitle ?: res.title, res.episode, subCb, cb) },
            executeMalSync = { data, subCb, cb -> if (data.origin == "imdb") invokeAnizone(data.title, data.episode, subCb, cb) }
        ),
        ProviderDef(
            key = "p_kuudere", displayName = "Kuudere",
            executeAnime = { res, subCb, cb -> invokeKuudere(res.originalTitle ?: res.title, res.year, res.episode, subCb, cb) },
        ),
        ProviderDef(
            key = "p_animes", displayName = "Animes*",
            executeAnime = { res, subCb, cb -> invokeAnimes(res.malId, res.anilistId, res.episode, res.year, "kitsu", subCb, cb) }
        ),
        ProviderDef(
            key = "p_gojo", displayName = "Animetsu",
            executeAnime = { res, subCb, cb -> invokeGojo(res.title, res.anilistId, res.episode, subCb, cb) },
            executeMalSync = { data, subCb, cb -> if (data.origin == "imdb") invokeGojo(data.title, data.aniId, data.episode, subCb, cb) }
        ),
        ProviderDef(
            key = "p_animekizz", displayName = "Animekizz",
            executeAnime = { res, subCb, cb -> invokeAnimekizz(res.title, res.anilistId, res.episode, subCb, cb) },
            executeMalSync = { data, subCb, cb -> if (data.origin == "imdb") invokeAnimekizz(data.title, data.aniId, data.episode, subCb, cb) }
        ),
        ProviderDef(
            key = "p_sudatchi", displayName = "Sudatchi",
            executeAnime = { res, subCb, cb -> invokeSudatchi(res.anilistId, res.episode, subCb, cb) },
            executeMalSync = { data, subCb, cb -> if (data.origin == "imdb") invokeSudatchi(data.aniId, data.episode, subCb, cb) }
        )
    )

    // Dynamically provided to Settings.kt
    val keys get() = builtInProviders.map { it.key }
    val namesMap get() = builtInProviders.associate { it.key to it.displayName }
    val torrentKeys get() = builtInProviders.filter { it.isTorrent }.map { it.key }.toSet()
}
