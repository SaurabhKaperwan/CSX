package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addImdbId
import com.lagradost.cloudstream3.LoadResponse.Companion.addAniListId
import com.lagradost.cloudstream3.LoadResponse.Companion.addMalId
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.runAllAsync
import kotlin.math.roundToInt
import com.lagradost.api.Log
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import com.lagradost.cloudstream3.network.CloudflareKiller
import com.megix.CineStreamExtractors.invokeVegamovies
import com.megix.CineStreamExtractors.invokeMoviesmod
import com.megix.CineStreamExtractors.invokeTopMovies
import com.megix.CineStreamExtractors.invokeMoviesdrive
import com.megix.CineStreamExtractors.invokeW4U
import com.megix.CineStreamExtractors.invokeWHVXSubs
import com.megix.CineStreamExtractors.invokeAnizone
import com.megix.CineStreamExtractors.invokeMultiAutoembed
// import com.megix.CineStreamExtractors.invokeVidbinge
import com.megix.CineStreamExtractors.invokeUhdmovies
// import com.megix.CineStreamExtractors.invokeRar
import com.megix.CineStreamExtractors.invokeAnimes
import com.megix.CineStreamExtractors.invokeMultimovies
import com.megix.CineStreamExtractors.invokeStreamify
import com.megix.CineStreamExtractors.invokeCinemaluxe
import com.megix.CineStreamExtractors.invokeBollyflix
import com.megix.CineStreamExtractors.invokeTorrentio
import com.megix.CineStreamExtractors.invokeTokyoInsider
import com.megix.CineStreamExtractors.invokeTvStream
import com.megix.CineStreamExtractors.invokeAllanime
import com.megix.CineStreamExtractors.invokeDramacool
import com.megix.CineStreamExtractors.invokeNetflix
import com.megix.CineStreamExtractors.invokePrimeVideo
import com.megix.CineStreamExtractors.invokeFlixhq
import com.megix.CineStreamExtractors.invokeSkymovies
import com.megix.CineStreamExtractors.invokeMoviesflix
import com.megix.CineStreamExtractors.invokeHdmovie2
import com.megix.CineStreamExtractors.invokeHindmoviez
import com.megix.CineStreamExtractors.invokeMostraguarda
import com.megix.CineStreamExtractors.invokePlayer4U
import com.megix.CineStreamExtractors.invokePrimeWire
import com.megix.CineStreamExtractors.invokeProtonmovies
import com.megix.CineStreamExtractors.invokeThepiratebay
import com.megix.CineStreamExtractors.invokeTom
import com.megix.CineStreamExtractors.invokeAllmovieland
import com.megix.CineStreamExtractors.invoke4khdhub
import com.megix.CineStreamExtractors.invokeVidJoy
import com.megix.CineStreamExtractors.invokeMovies4u

open class CineStreamProvider : MainAPI() {
    override var mainUrl = "https://cinemeta-catalogs.strem.io"
    override var name = "CineStream"
    override val hasMainPage = true
    override var lang = "en"
    override val hasDownloadSupport = true
    private val skipMap: MutableMap<String, Int> = mutableMapOf()
    val cinemeta_url = "https://v3-cinemeta.strem.io"
    val kitsu_url = "https://anime-kitsu.strem.fun"
    val haglund_url = "https://arm.haglund.dev/api/v2"
    val streamio_TMDB = "https://94c8cb9f702d-tmdb-addon.baby-beamup.club"
    val mediaFusion = "https://mediafusion.elfhosted.com"
    val animeCatalog = "https://1fe84bc728af-stremio-anime-catalogs.baby-beamup.club"
    companion object {
        const val malsyncAPI = "https://api.malsync.moe"
        const val vegaMoviesAPI = "https://vegamovies.bot"
        const val rogMoviesAPI = "https://rogmovies.lol"
        const val MovieDrive_API = "https://moviesdrive.solutions"
        const val tokyoInsiderAPI = "https://www.tokyoinsider.com"
        const val topmoviesAPI = "https://topmovies.tips"
        const val MoviesmodAPI = "https://moviesmod.email"
        const val protonmoviesAPI = "https://m2.protonmovies.top"
        const val stremifyAPI = "https://stremify.hayd.uk/YnVpbHQtaW4sZnJlbWJlZCxmcmVuY2hjbG91ZCxtZWluZWNsb3VkLGtpbm9raXN0ZSxjaW5laGRwbHVzLHZlcmhkbGluayxndWFyZGFoZCx2aXNpb25jaW5lLHdlY2ltYSxha3dhbSxkcmFtYWNvb2wsZHJhbWFjb29sX2NhdGFsb2csZ29nb2FuaW1lLGdvZ29hbmltZV9jYXRhbG9n/stream"
        const val W4UAPI = "https://world4ufree.rodeo"
        const val WHVXSubsAPI = "https://subs.whvx.net"
        const val WYZIESubsAPI = "https://subs.wyzie.ru"
        const val MultiembedAPI = "https://hin.autoembed.cc"
        const val MostraguardaAPI = "https://mostraguarda.stream"
        const val WHVXAPI = "https://api.whvx.net"
        const val TomAPI = "https://tom.autoembed.cc"
        const val uhdmoviesAPI = "https://uhdmovies.tips"
        const val fourkhdhubAPI = "https://4khdhub.fans"
        const val BYPASS_API = BuildConfig.BYPASS_API
        const val CONSUMET_API = BuildConfig.CONSUMET_API
        // const val RarAPI = "https://nepu.to"
        const val multimoviesAPI = "https://multimovies.guru"
        const val animepaheAPI = "https://animepahe.ru"
        const val allmovielandAPI = "https://allmovieland.fun"
        const val cinemaluxeAPI = "https://cinemaluxe.foo"
        const val bollyflixAPI = "https://bollyflix.yoga"
        const val torrentioAPI = "https://torrentio.strem.fun"
        const val anizoneAPI = "https://anizone.to"
        const val netflixAPI = "https://netfree2.cc"
        const val AllanimeAPI = "https://api.allanime.day/api"
        const val skymoviesAPI = "https://skymovieshd.pink"
        const val hindMoviezAPI = "https://hindmoviez.email"
        const val jaduMoviesAPI = "https://jadumovies.com"
        const val moviesflixAPI = "https://themoviesflix.at"
        const val hdmoviesflixAPI = "https://hdmoviesflix.center"
        const val hdmovie2API = "https://hdmovie2.science"
        const val stremio_Dramacool = "https://stremio-dramacool-addon.xyz"
        const val TRACKER_LIST_URL = "https://raw.githubusercontent.com/ngosang/trackerslist/master/trackers_all_ip.txt"
        const val torrentioCONFIG = "providers=yts,eztv,rarbg,1337x,thepiratebay,kickasstorrents,torrentgalaxy,magnetdl,horriblesubs,nyaasi,tokyotosho,anidex|sort=seeders|qualityfilter=threed,480p,other,scr,cam,unknown|limit=10"
        const val Player4uApi = "https://player4u.xyz"
        const val Primewire = "https://www.primewire.tf"
        const val ThePirateBayApi = "https://thepiratebay-plus.strem.fun"
        const val VidJoyApi ="https://vidjoy.pro"
        const val movies4uAPI = "https://movies4u.show"
    }
    val wpRedisInterceptor by lazy { CloudflareKiller() }
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
        TvType.AsianDrama,
        TvType.Anime,
        TvType.Torrent,
        TvType.Live,
    )

    override val mainPage = mainPageOf(
        "$streamio_TMDB/catalog/movie/tmdb.trending/skip=###&genre=Day" to "Trending Movies Today",
        "$streamio_TMDB/catalog/series/tmdb.trending/skip=###&genre=Day" to "Trending Series Today",
        "$mainUrl/top/catalog/movie/top/skip=###" to "Top Movies",
        "$mainUrl/top/catalog/series/top/skip=###" to "Top Series",
        "$mediaFusion/catalog/movie/hindi_hdrip/skip=###" to "Trending Movie in India",
        "$mediaFusion/catalog/series/hindi_series/skip=###" to "Trending Series in India",
        // "$kitsu_url/catalog/anime/kitsu-anime-airing/skip=###" to "Top Airing Anime",
        """$animeCatalog/{"anisearch_trending":"on"}/catalog/anime/anisearch_trending/skip=###""" to "Trending Anime",
        "$kitsu_url/catalog/anime/kitsu-anime-trending/skip=###" to "Top Anime",
        "$streamio_TMDB/catalog/series/tmdb.language/skip=###&genre=Korean" to "Trending Korean Series",
        "$mediaFusion/catalog/tv/live_tv/skip=###" to "Live TV",
        "$mediaFusion/catalog/events/live_sport_events/skip=###" to "Live Sports Events",
        "$mainUrl/top/catalog/movie/top/skip=###&genre=Action" to "Top Action Movies",
        "$mainUrl/top/catalog/series/top/skip=###&genre=Action" to "Top Action Series",
        "$mainUrl/top/catalog/movie/top/skip=###&genre=Comedy" to "Top Comedy Movies",
        "$mainUrl/top/catalog/series/top/skip=###&genre=Comedy" to "Top Comedy Series",
        "$mainUrl/top/catalog/movie/top/skip=###&genre=Romance" to "Top Romance Movies",
        "$mainUrl/top/catalog/series/top/skip=###&genre=Romance" to "Top Romance Series",
        "$mainUrl/top/catalog/movie/top/skip=###&genre=Horror" to "Top Horror Movies",
        "$mainUrl/top/catalog/series/top/skip=###&genre=Horror" to "Top Horror Series",
        "$mainUrl/top/catalog/movie/top/skip=###&genre=Thriller" to "Top Thriller Movies",
        "$mainUrl/top/catalog/series/top/skip=###&genre=Thriller" to "Top Thriller Series",
        "$mainUrl/top/catalog/movie/top/skip=###&genre=Sci-Fi" to "Top Sci-Fi Movies",
        "$mainUrl/top/catalog/series/top/skip=###&genre=Sci-Fi" to "Top Sci-Fi Series",
        "$mainUrl/top/catalog/movie/top/skip=###&genre=Fantasy" to "Top Fantasy Movies",
        "$mainUrl/top/catalog/series/top/skip=###&genre=Fantasy" to "Top Fantasy Series",
        "$mainUrl/top/catalog/movie/top/skip=###&genre=Mystery" to "Top Mystery Movies",
        "$mainUrl/top/catalog/series/top/skip=###&genre=Mystery" to "Top Mystery Series",
        "$mainUrl/top/catalog/movie/top/skip=###&genre=Crime" to "Top Crime Movies",
        "$mainUrl/top/catalog/series/top/skip=###&genre=Crime" to "Top Crime Series",
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        if(page == 1) skipMap.clear()
        val skip = skipMap[request.name] ?: 0
        val newRequestData = request.data.replace("###", skip.toString())
        val json = app.get("$newRequestData.json").text
        val movies = tryParseJson<Home>(json) ?: return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = emptyList(),
            ),
            hasNext = false
        )
        val movieCount = movies.metas.size
        skipMap[request.name] = skip + movieCount
        val home = movies.metas.mapNotNull { movie ->
            val type =
                if(movie.type == "tv" || movie.type == "events") TvType.Live
                else if(movie.type == "movie") TvType.Movie
                else TvType.TvSeries
            val title = movie.aliases?.firstOrNull() ?: movie.name ?: movie.description ?: "Empty"

            newMovieSearchResponse(title, PassData(movie.id, movie.type).toJson(), type) {
                this.posterUrl = movie.poster.toString()
            }
        }
        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = home,
            ),
            hasNext = true
        )
    }

    override suspend fun search(query: String): List<SearchResponse> = coroutineScope {
        val normalizedQuery = query.trim()
        val hasTmdb = "tmdb:" in normalizedQuery.lowercase()

        suspend fun fetchResults(url: String, tvType: TvType): List<SearchResponse> = runCatching {
            val json = app.get(url).text
            tryParseJson<SearchResult>(json)?.metas?.map {
                val title = it.aliases?.firstOrNull() ?: it.name ?: it.description ?: "Empty"
                newMovieSearchResponse(title, PassData(it.id, it.type).toJson(), tvType).apply {
                    posterUrl = it.poster.toString()
                }
            } ?: emptyList()
        }.getOrDefault(emptyList())

        val tmdbCleanQuery = normalizedQuery.replace("(?i)tmdb:".toRegex(), "").trim()

        val endpoints = if (hasTmdb) {
            listOf(
                "$streamio_TMDB/catalog/movie/tmdb.top/search=$tmdbCleanQuery.json" to TvType.Movie,
                "$streamio_TMDB/catalog/series/tmdb.top/search=$tmdbCleanQuery.json" to TvType.TvSeries
            )
        } else {
            listOf(
                "$kitsu_url/catalog/anime/kitsu-anime-airing/search=$normalizedQuery.json" to TvType.Anime,
                "$cinemeta_url/catalog/movie/top/search=$normalizedQuery.json" to TvType.Movie,
                "$cinemeta_url/catalog/series/top/search=$normalizedQuery.json" to TvType.TvSeries,
                "$mediaFusion/catalog/tv/mediafusion_search_tv/search=$normalizedQuery.json" to TvType.Live
            )
        }

        val allRequests = endpoints.map { (url, type) ->
            async { fetchResults(url, type) }
        }

        allRequests.awaitAll()
            .flatten()
            .sortedByDescending { calculateRelevanceScore(it.name, query) }
    }


    override suspend fun load(url: String): LoadResponse? {
        val movie = parseJson<PassData>(url)
        val tvtype = movie.type
        var id = movie.id
        val type =
                if(movie.type == "tv" || movie.type == "events") TvType.Live
                else if(movie.type == "movie") TvType.Movie
                else TvType.TvSeries
        val meta_url =
            if(id.contains("kitsu")) kitsu_url
            else if(id.contains("tmdb")) streamio_TMDB
            else if(id.contains("mf")) mediaFusion
            else cinemeta_url
        val isKitsu = if(meta_url == kitsu_url) true else false
        val isTMDB = if(meta_url == streamio_TMDB) true else false
        val externalIds = if(isKitsu) getExternalIds(id.substringAfter("kitsu:"),"kitsu") else  null
        val malId = if(externalIds != null) externalIds.myanimelist else null
        val anilistId = if(externalIds != null) externalIds.anilist else null
        id = if(isKitsu) id.replace(":", "%3A") else id
        val json = app.get("$meta_url/meta/$tvtype/$id.json").text
        val movieData = tryParseJson<ResponseData>(json)
        val title = movieData?.meta?.name.toString()
        val engTitle = movieData?.meta?.aliases?.firstOrNull() ?: title
        val posterUrl = movieData ?.meta?.poster.toString()
        val imdbRating = movieData?.meta?.imdbRating
        val year = movieData?.meta?.year
        val releaseInfo = movieData?.meta?.releaseInfo
        val tmdbId = if(!isKitsu && isTMDB) id.replace("tmdb:", "").toIntOrNull() else movieData?.meta?.moviedb_id
        id = if(!isKitsu && isTMDB) movieData?.meta?.imdb_id.toString() else id
        var description = movieData?.meta?.description.toString()
        val cast : List<String> = movieData?.meta?.cast ?: emptyList()
        val genre : List<String> = movieData?.meta?.genre ?: movieData?.meta?.genres ?: emptyList()
        val background = movieData?.meta?.background.toString()
        val isCartoon = genre.any { it.contains("Animation", true) }
        var isAnime = (movieData?.meta?.country.toString().contains("Japan", true) ||
            movieData?.meta?.country.toString().contains("China", true)) && isCartoon
        isAnime = if(isKitsu) true else isAnime
        val isBollywood = movieData?.meta?.country.toString().contains("India", true)
        val isAsian = (movieData?.meta?.country.toString().contains("Korea", true) ||
                movieData?.meta?.country.toString().contains("China", true)) && !isAnime

        if(tvtype == "movie" || tvtype == "tv" || tvtype == "events") {
            val data = LoadLinksData(
                title,
                id,
                tmdbId,
                tvtype,
                year ?: releaseInfo,
                null,
                null,
                null,
                isAnime,
                isBollywood,
                isAsian,
                isCartoon,
                null,
                null,
                null,
                isKitsu,
                anilistId,
                malId
            ).toJson()
            return newMovieLoadResponse(engTitle, url, if(isAnime) TvType.AnimeMovie  else type, data) {
                this.posterUrl = posterUrl
                this.plot = description
                this.tags = genre
                this.rating = imdbRating.toRatingInt()
                this.year = year ?.toIntOrNull() ?: releaseInfo?.toIntOrNull() ?: year?.substringBefore("-")?.toIntOrNull()
                this.backgroundPosterUrl = background
                this.duration = movieData?.meta?.runtime?.replace(" min", "")?.toIntOrNull()
                this.contentRating = if(isKitsu) "Kitsu" else if(isTMDB) "TMDB" else if(meta_url == mediaFusion) "Mediafusion" else "IMDB"
                addActors(cast)
                addAniListId(anilistId)
                addMalId(malId)
                addImdbId(id)
            }
        }
        else {
            val episodes = movieData?.meta?.videos?.map { ep ->
                newEpisode(
                    LoadLinksData(
                        title,
                        id,
                        tmdbId,
                        tvtype,
                        year ?: releaseInfo,
                        ep.season,
                        ep.episode,
                        ep.firstAired ?: ep.released,
                        isAnime,
                        isBollywood,
                        isAsian,
                        isCartoon,
                        ep.imdb_id,
                        ep.imdbSeason,
                        ep.imdbEpisode,
                        isKitsu,
                        anilistId,
                        malId
                    ).toJson()
                ) {
                    this.name = ep.name ?: ep.title
                    this.season = ep.season
                    this.episode = ep.episode
                    this.posterUrl = ep.thumbnail
                    this.description = ep.overview
                    this.rating = ep.rating?.toFloat()?.times(10)?.roundToInt()
                    addDate(ep.firstAired ?: ep.released)
                }
            } ?: emptyList()
            if(isAnime) {
                return newAnimeLoadResponse(engTitle, url, TvType.Anime) {
                    addEpisodes(DubStatus.Subbed, episodes)
                    this.posterUrl = posterUrl
                    this.backgroundPosterUrl = background
                    this.year = year?.substringBefore("–")?.toIntOrNull() ?: releaseInfo?.substringBefore("–")?.toIntOrNull() ?: year?.substringBefore("-")?.toIntOrNull()
                    this.plot = description
                    this.tags = genre
                    this.duration = movieData?.meta?.runtime?.replace(" min", "")?.toIntOrNull()
                    this.rating = imdbRating.toRatingInt()
                    this.contentRating = if(isKitsu) "Kitsu" else if(isTMDB) "TMDB" else if(meta_url == mediaFusion) "Mediafusion" else "IMDB"
                    addActors(cast)
                    addAniListId(anilistId)
                    addMalId(malId)
                    addImdbId(id)
                }
            }

            return newTvSeriesLoadResponse(engTitle, url, type, episodes) {
                this.posterUrl = posterUrl
                this.plot = description
                this.tags = genre
                this.rating = imdbRating.toRatingInt()
                this.year = year?.substringBefore("–")?.toIntOrNull() ?: releaseInfo?.substringBefore("–")?.toIntOrNull() ?: year?.substringBefore("-")?.toIntOrNull()
                this.backgroundPosterUrl = background
                this.duration = movieData?.meta?.runtime?.replace(" min", "")?.toIntOrNull()
                this.contentRating = if(isKitsu) "Kitsu" else if(isTMDB) "TMDB" else if(meta_url == mediaFusion) "Mediafusion" else "IMDB"
                addActors(cast)
                addImdbId(id)
            }
        }
    }

    override suspend fun loadLinks(
    data: String,
    isCasting: Boolean,
    subtitleCallback: (SubtitleFile) -> Unit,
    callback: (ExtractorLink) -> Unit
): Boolean {
        val res = parseJson<LoadLinksData>(data)
        val year = getYear(res)
        val seasonYear = getSeasonYear(res)

        return when {
            res.tvtype in listOf("tv", "events") -> {
                invokeTvStream(res.id, mediaFusion, res.tvtype, subtitleCallback, callback)
                true
            }

            res.isKitsu -> {
                runKitsuInvokers(res, year, seasonYear, subtitleCallback, callback)
                true
            }

            else -> {
                runGeneralInvokers(res, year, seasonYear, subtitleCallback, callback)
                true
            }
        }
    }

    data class LoadLinksData(
        val title: String,
        val id: String,
        val tmdbId: Int?,
        val tvtype: String,
        val year: String? = null,
        val season: Int? = null,
        val episode: Int? = null,
        val firstAired: String? = null,
        val isAnime: Boolean = false,
        val isBollywood: Boolean = false,
        val isAsian: Boolean = false,
        val isCartoon: Boolean = false,
        val imdb_id : String? = null,
        val imdbSeason : Int? = null,
        val imdbEpisode : Int? = null,
        val isKitsu : Boolean = false,
        val anilistId : Int? = null,
        val malId : Int? = null,
    )

    data class PassData(
        val id: String,
        val type: String,
    )

    data class Meta(
        val id: String?,
        val imdb_id: String?,
        val type: String?,
        val aliases: ArrayList<String>?,
        val poster: String?,
        val background: String?,
        val moviedb_id: Int?,
        val name: String?,
        val description: String?,
        val genre: List<String>?,
        val genres: List<String>?,
        val releaseInfo: String?,
        val status: String?,
        val runtime: String?,
        val cast: List<String>?,
        val language: String?,
        val country: String?,
        val imdbRating: String?,
        val year: String?,
        val videos: List<EpisodeDetails>?,
    )

    data class SearchResult(
        val metas: List<Media>
    )

    data class Media(
        val id: String,
        val type: String,
        val name: String?,
        val poster: String?,
        val description: String?,
        val aliases: ArrayList<String>?,
    )

    data class EpisodeDetails(
        val id: String?,
        val name: String?,
        val title: String?,
        val season: Int,
        val episode: Int,
        val rating: String?,
        val released: String?,
        val firstAired: String?,
        val overview: String?,
        val thumbnail: String?,
        val moviedb_id: Int?,
        val imdb_id: String?,
        val imdbSeason: Int?,
        val imdbEpisode: Int?,
    )

    data class ResponseData(
        val meta: Meta,
    )

    data class Home(
        val metas: List<Media>
    )

    data class ExtenalIds(
        val anilist: Int?,
        val anidb: Int?,
        val myanimelist: Int?,
        val kitsu: Int?,
        val anisearch: Int?,
        val livechart: Int?,
        val themoviedb: Int?,
    )

    suspend fun getExternalIds(id: String, type: String) : ExtenalIds? {
        val url = "$haglund_url/ids?source=$type&id=$id"
        val json = app.get(url).text
        return tryParseJson<ExtenalIds>(json) ?: return null
    }

    private fun calculateRelevanceScore(name: String, query: String): Int {
        fun normalize(text: String): String {
            return text.lowercase()
                .replace(Regex("[^a-z0-9 ]"), "")
                .replace(Regex("\\s+"), " ")
                .trim()
        }

        fun levenshtein(a: String, b: String): Int {
            val dp = Array(a.length + 1) { IntArray(b.length + 1) }
            for (i in 0..a.length) dp[i][0] = i
            for (j in 0..b.length) dp[0][j] = j
            for (i in 1..a.length) {
                for (j in 1..b.length) {
                    dp[i][j] = minOf(
                        dp[i - 1][j] + 1,        // deletion
                        dp[i][j - 1] + 1,        // insertion
                        dp[i - 1][j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1 // substitution
                    )
                }
            }
            return dp[a.length][b.length]
        }

        val normalizedName = normalize(name)
        val normalizedQuery = normalize(query)
        var score = 0

        if (normalizedName == normalizedQuery) {
            score += 100
        }

        if (normalizedName.contains(normalizedQuery)) {
            score += 50
            val index = normalizedName.indexOf(normalizedQuery)
            score += when {
                index == 0 -> 20
                index in 1..4 -> 10
                else -> 0
            }

            val queryWords = normalizedQuery.split(" ")
            queryWords.forEach { word ->
                if (normalizedName.contains(word)) {
                    score += 5
                }
            }

            if (queryWords.all { normalizedName.contains(it) }) {
                score += 15
            }
        }

        // Fuzzy matching bonus (if not already matched well)
        if (score < 50) {
            val distance = levenshtein(normalizedName, normalizedQuery)
            if (distance in 1..3) {
                score += 30 - (distance * 5) // closer = higher score
            }
        }

        // Slight penalty for long irrelevant names
        if (score < 50 && normalizedName.length > 30) {
            score -= 10
        }

        return score
    }

    private fun getYear(res: LoadLinksData): Int? {
        return if (res.tvtype == "movie") res.year?.toIntOrNull()
        else res.year?.substringBefore("-")?.toIntOrNull() ?: res.year?.substringBefore("–")?.toIntOrNull()
    }

    private fun getSeasonYear(res: LoadLinksData): Int? {
        return if (res.tvtype == "movie") getYear(res)
        else res.firstAired?.substringBefore("-")?.toIntOrNull() ?: res.firstAired?.substringBefore("–")?.toIntOrNull()
    }

    private suspend fun runKitsuInvokers(
        res: LoadLinksData,
        year: Int?,
        seasonYear: Int?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        var imdbTitle: String? = null
        var imdbYear: Int? = null
        var tmdbId: Int? = null

        try {
            val json = app.get("$cinemeta_url/meta/${res.tvtype}/${res.imdb_id}.json").text
            val movieData = tryParseJson<ResponseData>(json)

            movieData?.meta?.let { meta ->
                imdbTitle = meta.name
                imdbYear = meta.year?.substringBefore("-")?.toIntOrNull() ?: meta.year?.toIntOrNull()
                tmdbId = meta.moviedb_id
            }
        } catch (e: Exception) {
            println("Cinemeta API failed: ${e.localizedMessage}")
        }

        runAllAsync(
            { invokeAnimes(res.malId, res.anilistId, res.episode, year, "kitsu", subtitleCallback, callback) },
            { invokeTokyoInsider(res.title, res.episode, subtitleCallback, callback) },
            { invokeAllanime(res.title, year, res.episode, subtitleCallback, callback) },
            { invokeAnizone(res.title, res.episode, subtitleCallback, callback) },
            { invokeTorrentio(res.imdb_id, res.imdbSeason, res.imdbEpisode, callback) },
            { invokeWHVXSubs(WHVXSubsAPI, res.imdb_id, res.imdbSeason, res.imdbEpisode, subtitleCallback) },
            { invokeWHVXSubs(WYZIESubsAPI, res.imdb_id, res.imdbSeason, res.imdbEpisode, subtitleCallback) },
            { invokeMoviesmod(res.imdb_id, res.imdbSeason, res.imdbEpisode, subtitleCallback, callback) },
            { invokeTom(tmdbId, res.imdbSeason, res.imdbEpisode, callback, subtitleCallback) },
            { invokeMovies4u(res.imdb_id, imdbTitle, imdbYear, res.imdbSeason, res.imdbEpisode, subtitleCallback, callback) },
            { invokeBollyflix(res.imdb_id, res.imdbSeason, res.imdbEpisode, subtitleCallback, callback) },
            { invokeAllmovieland(res.imdb_id, res.imdbSeason, res.imdbEpisode, callback) },
            { invokeProtonmovies(res.imdb_id, res.imdbSeason, res.imdbEpisode, subtitleCallback, callback) },
            { invokeHindmoviez("HindMoviez", hindMoviezAPI, res.imdb_id, res.imdbSeason, res.imdbEpisode, callback) },
            { invokeVegamovies(vegaMoviesAPI, "VegaMovies", res.imdb_id, res.imdbSeason, res.imdbEpisode, subtitleCallback, callback) },
            { invokeMoviesdrive(imdbTitle, res.imdbSeason, res.imdbEpisode, imdbYear, subtitleCallback, callback) },
            { invokePrimeWire(res.imdb_id, res.imdbSeason, res.imdbEpisode, subtitleCallback, callback) },
            { invokePlayer4U(imdbTitle, res.imdbSeason, res.imdbEpisode, seasonYear, callback) },
            { invokeCinemaluxe(imdbTitle, imdbYear, res.imdbSeason, res.imdbEpisode, callback, subtitleCallback) },
            { invokeUhdmovies(imdbTitle, imdbYear, res.imdbSeason, res.imdbEpisode, callback, subtitleCallback) },
        )
    }

    private suspend fun runGeneralInvokers(
        res: LoadLinksData,
        year: Int?,
        seasonYear: Int?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val isBollywood = res.isBollywood
        val isAnime = res.isAnime
        val isAsian = res.isAsian

        runAllAsync(
            { if (!isBollywood) invokeVegamovies(vegaMoviesAPI, "VegaMovies", res.id, res.season, res.episode, subtitleCallback, callback) },
            { if (isBollywood) invokeVegamovies(rogMoviesAPI, "RogMovies", res.id, res.season, res.episode, subtitleCallback, callback) },
            { invokeNetflix(res.title, year, res.season, res.episode, subtitleCallback, callback) },
            { invokePrimeVideo(res.title, year, res.season, res.episode, subtitleCallback, callback) },
            { if (res.season == null) invokeStreamify(res.id, callback) },
            { if (isBollywood) invokeTopMovies(res.title, year, res.season, res.episode, subtitleCallback, callback) },
            { if (!isBollywood) invokeMoviesmod(res.id, res.season, res.episode, subtitleCallback, callback) },
            { if (isAsian && res.season != null) invokeDramacool(res.title, "kdhd", res.season, res.episode, subtitleCallback, callback) },
            { invokeMoviesdrive(res.title, res.season, res.episode, year, subtitleCallback, callback) },
            { invokeCinemaluxe(res.title, year, res.season, res.episode, callback, subtitleCallback) },
            { if (!isAnime) invokeSkymovies(res.title, seasonYear, res.episode, subtitleCallback, callback) },
            { if (!isAnime) invokeHdmovie2(res.title, seasonYear, res.episode, subtitleCallback, callback) },
            { if (!isAnime) invokeFlixhq(res.title, res.season, res.episode, subtitleCallback, callback) },
            { invokeBollyflix(res.id, res.season, res.episode, subtitleCallback, callback) },
            { invokeMovies4u(res.id, res.title, year, res.season, res.episode, subtitleCallback, callback) },
            { invokeTorrentio(res.id, res.season, res.episode, callback) },
            { if (!isBollywood) invokeHindmoviez("HindMoviez", hindMoviezAPI, res.id, res.season, res.episode, callback) },
            { invokeW4U(res.title, year, res.id, res.season, res.episode, subtitleCallback, callback) },
            { invokeWHVXSubs(WHVXSubsAPI, res.id, res.season, res.episode, subtitleCallback) },
            { invokeWHVXSubs(WYZIESubsAPI, res.id, res.season, res.episode, subtitleCallback) },
            { if (isAnime) {
                val (aniId, malId) = convertTmdbToAnimeId(res.title, year, res.firstAired, if (res.tvtype == "movie") TvType.AnimeMovie else TvType.Anime)
                invokeAnimes(malId, aniId, res.episode, seasonYear, "imdb", subtitleCallback, callback)
            }},
            { invokePrimeWire(res.id, res.season, res.episode, subtitleCallback, callback) },
            { invokeTom(res.tmdbId, res.season, res.episode, callback, subtitleCallback) },
            { invokePlayer4U(res.title, res.season, res.episode, seasonYear, callback) },
            { invokeThepiratebay(res.id, res.season, res.episode, callback) },
            { if (!isAnime) invokeVidJoy(res.tmdbId, res.season, res.episode, callback) },
            { invokeProtonmovies(res.id, res.season, res.episode, subtitleCallback, callback) },
            { invokeAllmovieland(res.id, res.season, res.episode, callback) },
            { invokeMultiAutoembed(res.id, res.season, res.episode, subtitleCallback, callback) },
            { invokeMostraguarda(res.id, res.season, res.episode, subtitleCallback, callback) },
            { if (!isBollywood || !isAnime) invokeMoviesflix("Moviesflix", moviesflixAPI, res.id, res.season, res.episode, subtitleCallback, callback) },
            { if (isBollywood) invokeMoviesflix("Hdmoviesflix", hdmoviesflixAPI, res.id, res.season, res.episode, subtitleCallback, callback) },
            { if (!isBollywood) invokeUhdmovies(res.title, year, res.season, res.episode, callback, subtitleCallback) },
            { if (!isBollywood) invoke4khdhub(res.title, year, res.season, res.episode, subtitleCallback, callback) }
        )
    }
}

