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
import org.json.JSONObject
import com.lagradost.api.Log
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import com.lagradost.cloudstream3.network.CloudflareKiller
import com.megix.CineStreamExtractors.invokeVegamovies
import com.megix.CineStreamExtractors.invokeMoviesmod
import com.megix.CineStreamExtractors.invokeTopMovies
import com.megix.CineStreamExtractors.invokeMoviesdrive
import com.megix.CineStreamExtractors.invokeW4U
import com.megix.CineStreamExtractors.invokeWYZIESubs
import com.megix.CineStreamExtractors.invokeAnizone
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
import com.megix.CineStreamExtractors.invokeAllanime
import com.megix.CineStreamExtractors.invokeStreamAsia
import com.megix.CineStreamExtractors.invokeNetflix
import com.megix.CineStreamExtractors.invokePrimeVideo
import com.megix.CineStreamExtractors.invokeDisney
// import com.megix.CineStreamExtractors.invokeFlixhq
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
// import com.megix.CineStreamExtractors.invokeVidJoy
import com.megix.CineStreamExtractors.invokeMovies4u
import com.megix.CineStreamExtractors.invokeSoaper
import com.megix.CineStreamExtractors.invokeAsiaflix
import com.megix.CineStreamExtractors.invoke2embed
import com.megix.CineStreamExtractors.invokePrimebox
import com.megix.CineStreamExtractors.invokePrimenet
import com.megix.CineStreamExtractors.invokeAnimeparadise
import com.megix.CineStreamExtractors.invokeGojo
import com.megix.CineStreamExtractors.invokeSudatchi
import com.megix.CineStreamExtractors.invokePhoenix
import com.megix.CineStreamExtractors.invokeKatMovieHd
import com.megix.CineStreamExtractors.invokeMadplay
import com.megix.CineStreamExtractors.invokeStremioSubtitles
import com.megix.CineStreamExtractors.invokeToonstream
import com.megix.CineStreamExtractors.invokeDramadrip

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
    companion object {
        const val malsyncAPI = "https://api.malsync.moe"
        const val tokyoInsiderAPI = "https://www.tokyoinsider.com"
        const val stremifyAPI = "https://stremify.hayd.uk/YnVpbHQtaW4sZnJlbWJlZCxmcmVuY2hjbG91ZCxtZWluZWNsb3VkLGtpbm9raXN0ZSxjaW5laGRwbHVzLHZlcmhkbGluayxndWFyZGFoZCx2aXNpb25jaW5lLHdlY2ltYSxha3dhbSxkcmFtYWNvb2wsZHJhbWFjb29sX2NhdGFsb2csZ29nb2FuaW1lLGdvZ29hbmltZV9jYXRhbG9n/stream"
        const val WYZIESubsAPI = "https://sub.wyzie.ru"
        const val MostraguardaAPI = "https://mostraguarda.stream"
        const val TomAPI = "https://tom.autoembed.cc"
        const val CONSUMET_API = BuildConfig.CONSUMET_API
        // const val RarAPI = "https://nepu.to"
        const val animepaheAPI = "https://animepahe.ru"
        const val allmovielandAPI = "https://allmovieland.fun"
        const val torrentioAPI = "https://torrentio.strem.fun"
        const val anizoneAPI = "https://anizone.to"
        const val AllanimeAPI = "https://api.allanime.day/api"
        const val StreamAsiaAPI = "https://stremio-dramacool-addon.xyz/eyJraXNza2gtY2F0YWxvZ3MiOlsia2toLXNlYXJjaC1yZXN1bHRzIiwia2toLWtvcmVhbi1kcmFtYSIsImtraC1rb3JlYW4tbW92aWVzIl0sImtkaGQtY2F0YWxvZ3MiOlsia2RoZC1zZWFyY2gtcmVzdWx0cyJdLCJvdHR2LWNhdGFsb2dzIjpbIm90dHYtc2VhcmNoLXJlc3VsdHMiXSwiZGRsLWNhdGFsb2dzIjpbXSwidHJha3RDb2RlIjpudWxsLCJzaG93VE1EQlNlYXNvbiI6dHJ1ZSwiZW5hYmxlT3BlbnN1YnMiOnRydWUsImhpZGVVcGNvbWluZ1Nob3dzIjp0cnVlLCJkZWJ1Z0ZsYWdzIjoiIiwibWVkaWFmbG93UHJveHlDb25maWdzIjpbXSwiZGVicmlkQ29uZmlnIjpbXSwiaGlkZVVuc3VwcG9ydGVkSG9zdGVycyI6ZmFsc2UsInZlcnNpb24iOiIxLjMuMSJ9"
        const val TRACKER_LIST_URL = "https://raw.githubusercontent.com/ngosang/trackerslist/master/trackers_all_ip.txt"
        const val torrentioCONFIG = "providers=yts,eztv,rarbg,1337x,thepiratebay,kickasstorrents,torrentgalaxy,magnetdl,horriblesubs,nyaasi,tokyotosho,anidex|sort=seeders|qualityfilter=threed,480p,other,scr,cam,unknown|limit=10"
        const val Player4uApi = "https://player4u.xyz"
        const val Primewire = "https://www.primewire.tf"
        const val ThePirateBayApi = "https://thepiratebay-plus.strem.fun"
        //const val VidJoyApi = "https://vidjoy.pro"
        const val soaperAPI = "https://soaper.cc"
        const val asiaflixAPI = "https://asiaflix.net"
        const val twoembedAPI = "https://2embed.cc"
        const val xprimeBaseAPI = "https://xprime.tv"
        const val xprimeAPI = "https://backend.xprime.tv"
        const val animeparadiseBaseAPI = "https://www.animeparadise.moe"
        const val animeparadiseAPI = "https://api.animeparadise.moe"
        const val sudatchiAPI = "https://sudatchi.com"
        const val miruroAPI = "https://www.miruro.to"
        const val animezAPI = "https://animeyy.com"
        const val proxyAPI = "https://thingproxy.freeboard.io/fetch"

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

        val protonmoviesAPI get() = api("protonmovies")
        val W4UAPI get() = api("w4u")
        val fourkhdhubAPI get() = api("4khdhub")
        val multimoviesAPI get() = api("multimovies")
        val cinemaluxeAPI get() = api("cinemaluxe")
        val bollyflixAPI get() = api("bollyflix")
        val movies4uAPI get() = api("movies4u")
        val skymoviesAPI get() = api("skymovies")
        val hindMoviezAPI get() = api("hindmoviez")
        val moviesflixAPI get() = api("moviesflix")
        val hdmoviesflixAPI get() = api("hdmoviesflix")
        val hdmovie2API get() = api("hdmovie2")
        val jaduMoviesAPI get() = api("jadumovies")
        val netflixAPI get() = api("nfmirror")
        val MovieDrive_API get() = api("moviesdrive")
        val gojoBaseAPI get() = api("gojo_base")
        val vegamoviesAPI get() = api("vegamovies")
        val rogmoviesAPI get() = api("rogmovies")
        val uhdmoviesAPI get() = api("uhdmovies")
        val MoviesmodAPI get() = api("moviesmod")
        val topmoviesAPI get() = api("topmovies")
        val katmoviehdAPI get() = api("katmoviehd")
        val moviesBabaAPI get() = api("moviesbaba")
        val toonStreamAPI get() = api("toonstream")
        val hianimeAPI get() = api("hianime")
        val dramadripAPI get() = api("dramadrip")
    }
    val wpRedisInterceptor by lazy { CloudflareKiller() }

    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
        TvType.AsianDrama,
        TvType.Anime,
        TvType.Torrent,
    )

    override val mainPage = mainPageOf(
        "$mainUrl/top/catalog/movie/top/skip=###" to "Top Movies",
        "$mainUrl/top/catalog/series/top/skip=###" to "Top Series",
        "$kitsu_url/catalog/anime/kitsu-anime-airing/skip=###" to "Top Airing Anime",
        "$kitsu_url/catalog/anime/kitsu-anime-trending/skip=###" to "Top Anime",
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
                if(movie.type == "movie") TvType.Movie
                else TvType.TvSeries
            val title = movie.aliases?.firstOrNull() ?: movie.name ?: movie.description ?: ""

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

        suspend fun fetchResults(url: String): List<SearchResponse> {
            val result = runCatching {
                val json = app.get(url).text
                tryParseJson<SearchResult>(json)?.metas?.map {
                    val title = it.aliases?.firstOrNull() ?: it.name ?: it.description ?: "Empty"
                    newMovieSearchResponse(title, PassData(it.id, it.type).toJson()).apply {
                        posterUrl = it.poster.toString()
                    }
                } ?: emptyList()
            }.getOrDefault(emptyList())

            if (result.isNotEmpty()) return result
            return emptyList()
        }

        val endpoints = listOf(
            "$cinemeta_url/catalog/movie/top/search=$normalizedQuery.json",
            "$cinemeta_url/catalog/series/top/search=$normalizedQuery.json",
            "$kitsu_url/catalog/anime/kitsu-anime-airing/search=$normalizedQuery.json"
        )

        val resultsLists = endpoints.map {
            async { fetchResults(it) }
        }.awaitAll()

        val maxSize = resultsLists.maxOfOrNull { it.size } ?: 0

        buildList {
            for (i in 0 until maxSize) {
                for (list in resultsLists) {
                    if (i < list.size) add(list[i])
                }
            }
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val movie = parseJson<PassData>(url)
        val tvtype = movie.type
        var id = movie.id
        val type = if(movie.type == "movie") TvType.Movie else TvType.TvSeries
        val meta_url =
            if(id.contains("kitsu")) kitsu_url
            else cinemeta_url
        val isKitsu = if(meta_url == kitsu_url) true else false
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
        val tmdbId = movieData?.meta?.moviedb_id
        id = if(!isKitsu) movieData?.meta?.imdb_id.toString() else id
        var description = movieData?.meta?.description

        val actors = movieData?.meta?.cast?.mapNotNull { name ->
            ActorData(
                actor = Actor(name, null),
                roleString = null
            )
        } ?: emptyList()

        val country = movieData?.meta?.country ?: ""
        val genre = movieData?.meta?.genre ?: movieData?.meta?.genres ?: emptyList()
        val background = movieData?.meta?.background
        val isCartoon = genre.any { it.contains("Animation", true) }
        var isAnime = (country.contains("Japan", true) ||
            country.contains("China", true)) && isCartoon
        isAnime = if(isKitsu) true else isAnime
        val isBollywood = country.contains("India", true)
        val isAsian = (country.contains("Korea", true) ||
                country.contains("China", true)) && !isAnime

        if(tvtype == "movie") {
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
                this.contentRating = if(isKitsu) "Kitsu" else "IMDB"
                this.actors = actors
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
                    this.description = ep.overview ?: ""
                    this.rating = ep.rating?.toFloat()?.times(10)?.roundToInt()
                    addDate(ep.firstAired ?: ep.released)
                }
            } ?: emptyList()
            return newAnimeLoadResponse(engTitle, url, if(isAnime) TvType.Anime else TvType.TvSeries) {
                addEpisodes(DubStatus.Subbed, episodes)
                this.posterUrl = posterUrl
                this.backgroundPosterUrl = background
                this.year = year?.substringBefore("–")?.toIntOrNull() ?: releaseInfo?.substringBefore("–")?.toIntOrNull() ?: year?.substringBefore("-")?.toIntOrNull()
                this.plot = description
                this.tags = genre
                this.duration = movieData?.meta?.runtime?.replace(" min", "")?.toIntOrNull()
                this.rating = imdbRating.toRatingInt()
                 this.contentRating = if(isKitsu) "Kitsu" else "IMDB"
                this.actors = actors
                addAniListId(anilistId)
                addMalId(malId)
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
        val app_extras: AppExtras? = null,
        val language: String?,
        val country: String?,
        val imdbRating: String?,
        val year: String?,
        val videos: List<EpisodeDetails>?,
    )

    data class AppExtras (
        val cast: List<Cast> = emptyList()
    )

    data class Cast (
        val name      : String? = null,
        val character : String? = null,
        val photo     : String? = null
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
            { invokeSudatchi(res.anilistId, res.episode, subtitleCallback, callback) },
            { invokeGojo(res.anilistId, res.episode, subtitleCallback ,callback) },
            { invokeAnimeparadise(res.title, res.malId, res.episode, subtitleCallback, callback) },
            { invokeTokyoInsider(res.title, res.episode, subtitleCallback, callback) },
            { invokeAllanime(res.title, year, res.episode, subtitleCallback, callback) },
            { invokeAnizone(res.title, res.episode, subtitleCallback, callback) },
            { invokeTorrentio(res.imdb_id, res.imdbSeason, res.imdbEpisode, callback) },
            { invokeWYZIESubs(res.imdb_id, res.imdbSeason, res.imdbEpisode, subtitleCallback) },
            { invokeStremioSubtitles(res.imdb_id, res.imdbSeason, res.imdbEpisode, subtitleCallback) },
            { invokeNetflix(imdbTitle, year, res.imdbSeason, res.imdbEpisode, subtitleCallback, callback) },
            { invokePrimeVideo(imdbTitle, year, res.imdbSeason, res.imdbEpisode, subtitleCallback, callback) },
            { invokeMoviesmod(res.imdb_id, res.imdbSeason, res.imdbEpisode, subtitleCallback, callback) },
            { invokeTom(tmdbId, res.imdbSeason, res.imdbEpisode, callback, subtitleCallback) },
            { invokeMovies4u(res.imdb_id, imdbTitle, imdbYear, res.imdbSeason, res.imdbEpisode, subtitleCallback, callback) },
            { invokeBollyflix(res.imdb_id, res.imdbSeason, res.imdbEpisode, subtitleCallback, callback) },
            { invokeAllmovieland(res.imdb_id, res.imdbSeason, res.imdbEpisode, callback) },
            { invokeProtonmovies(res.imdb_id, res.imdbSeason, res.imdbEpisode, subtitleCallback, callback) },
            { invokeVegamovies("VegaMovies", res.imdb_id, res.imdbSeason, res.imdbEpisode, subtitleCallback, callback) },
            { invoke4khdhub(imdbTitle, imdbYear, res.imdbSeason, res.imdbEpisode, subtitleCallback, callback) },
            { invokeMoviesdrive(imdbTitle, res.imdb_id, res.imdbSeason, res.imdbEpisode, subtitleCallback, callback) },
            { invokeToonstream(imdbTitle, res.imdbSeason, res.imdbEpisode, subtitleCallback, callback) },
            { invokeMultimovies(imdbTitle, res.imdbSeason, res.imdbEpisode, subtitleCallback, callback) },
            { invokePrimeWire(res.imdb_id, res.imdbSeason, res.imdbEpisode, subtitleCallback, callback) },
            { invokePlayer4U(imdbTitle, res.imdbSeason, res.imdbEpisode, year, callback) },
            { invokeCinemaluxe(imdbTitle, imdbYear, res.imdbSeason, res.imdbEpisode, callback, subtitleCallback) },
            { invokePrimebox(imdbTitle, imdbYear, res.imdbSeason, res.imdbEpisode, subtitleCallback, callback)},
            { invokePrimenet(tmdbId, res.imdbSeason, res.imdbEpisode, callback) },
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
        val isCartoon = res.isCartoon

        runAllAsync(
            { if (!isBollywood) invokeVegamovies("VegaMovies", res.id, res.season, res.episode, subtitleCallback, callback) },
            { if (isBollywood) invokeVegamovies("RogMovies", res.id, res.season, res.episode, subtitleCallback, callback) },
            { invokeNetflix(res.title, year, res.season, res.episode, subtitleCallback, callback) },
            { invokePrimeVideo(res.title, year, res.season, res.episode, subtitleCallback, callback) },
            { invokeDisney(res.title, year, res.season, res.episode, subtitleCallback, callback) },
            { if (res.season == null) invokeStreamify(res.id, callback) },
            { invokeMultimovies(res.title, res.season, res.episode, subtitleCallback, callback) },
            { if (isBollywood) invokeTopMovies(res.title, year, res.season, res.episode, subtitleCallback, callback) },
            { if (!isBollywood) invokeMoviesmod(res.id, res.season, res.episode, subtitleCallback, callback) },
            { if (isAsian) invokeDramadrip(res.id, res.season, res.episode, subtitleCallback, callback) },
            { if (isAsian && res.season != null) invokeStreamAsia(res.title, "kdhd", res.season, res.episode, subtitleCallback, callback) },
            { invokeMoviesdrive(res.title, res.id ,res.season, res.episode, subtitleCallback, callback) },
            { if(res.isAnime || res.isCartoon) invokeToonstream(res.title, res.season, res.episode, subtitleCallback, callback) },
            { if(!isAnime) invokeAsiaflix(res.title, res.season, res.episode, seasonYear, subtitleCallback, callback) },
            { invokeCinemaluxe(res.title, year, res.season, res.episode, callback, subtitleCallback) },
            { if (!isAnime) invokeSkymovies(res.title, seasonYear, res.episode, subtitleCallback, callback) },
            { if (!isAnime) invokeHdmovie2(res.title, seasonYear, res.episode, subtitleCallback, callback) },
            // { if (!isAnime) invokeFlixhq(res.title, res.season, res.episode, subtitleCallback, callback) },
            { invokeBollyflix(res.id, res.season, res.episode, subtitleCallback, callback) },
            { invokeMovies4u(res.id, res.title, year, res.season, res.episode, subtitleCallback, callback) },
            { invokeTorrentio(res.id, res.season, res.episode, callback) },
            { if (!isBollywood) invokeHindmoviez("HindMoviez", res.id, res.title, res.season, res.episode, callback) },
        //  { if (isBollywood) invokeHindmoviez("JaduMovies", res.id, res.season, res.episode, callback) },
            { if (!isBollywood && !isAnime) invokeKatMovieHd("KatMovieHd", res.id, res.season, res.episode, subtitleCallback ,callback) },
            { if (isBollywood) invokeKatMovieHd("Moviesbaba", res.id, res.season, res.episode, subtitleCallback ,callback) },
            { invokeW4U(res.title, year, res.id, res.season, res.episode, subtitleCallback, callback) },
            { invokeWYZIESubs(res.id, res.season, res.episode, subtitleCallback) },
            { invokeStremioSubtitles(res.id, res.season, res.episode, subtitleCallback) },
            { if (isAnime) {
                val (aniId, malId) = convertTmdbToAnimeId(res.title, year, res.firstAired, if (res.tvtype == "movie") TvType.AnimeMovie else TvType.Anime)
                invokeAnimes(malId, aniId, res.episode, seasonYear, "imdb", subtitleCallback, callback)
            }},
            { invokePrimebox(res.title, year, res.season, res.episode, subtitleCallback, callback) },
            { invokePrimeWire(res.id, res.season, res.episode, subtitleCallback, callback) },
            { if (!isAnime) invoke2embed(res.id, res.season, res.episode, callback) },
            { invokeSoaper(res.id, res.tmdbId, res.title, res.season, res.episode, subtitleCallback, callback) },
            { invokePhoenix(res.title, res.id, res.tmdbId, year, res.season, res.episode, callback) },
            { invokeTom(res.tmdbId, res.season, res.episode, callback, subtitleCallback) },
            { if(!isAnime) invokeMadplay(res.tmdbId, res.season, res.episode, callback) },
            { invokePrimenet(res.tmdbId, res.season, res.episode, callback) },
            { invokePlayer4U(res.title, res.season, res.episode, year, callback) },
            { invokeThepiratebay(res.id, res.season, res.episode, callback) },
            // { if (!isAnime) invokeVidJoy(res.tmdbId, res.season, res.episode, callback) },
            { invokeProtonmovies(res.id, res.season, res.episode, subtitleCallback, callback) },
            { invokeAllmovieland(res.id, res.season, res.episode, callback) },
            { if(res.season == null) invokeMostraguarda(res.id, subtitleCallback, callback) },
            { if (!isBollywood && !isAnime) invokeMoviesflix("Moviesflix", res.id, res.season, res.episode, subtitleCallback, callback) },
            { if (isBollywood) invokeMoviesflix("Hdmoviesflix", res.id, res.season, res.episode, subtitleCallback, callback) },
            { if (!isBollywood) invokeUhdmovies(res.title, year, res.season, res.episode, callback, subtitleCallback) },
            { if (!isBollywood) invoke4khdhub(res.title, year, res.season, res.episode, subtitleCallback, callback) }
        )
    }
}

