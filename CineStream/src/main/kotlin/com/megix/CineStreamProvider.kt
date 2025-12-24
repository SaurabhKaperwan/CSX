package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addImdbId
import com.lagradost.cloudstream3.LoadResponse.Companion.addAniListId
import com.lagradost.cloudstream3.LoadResponse.Companion.addMalId
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import org.json.JSONObject
import com.lagradost.api.Log
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import com.lagradost.cloudstream3.network.CloudflareKiller
import com.megix.CineStreamExtractors.invokeAllSources
import com.megix.CineStreamExtractors.invokeAllAnimeSources
import com.megix.CineStreamExtractors.invokeAnimes

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
    val aiometa_url = "https://aiometadata.elfhosted.com/stremio/9197a4a9-2f5b-4911-845e-8704c520bdf7"
    val image_proxy = "https://wsrv.nl/?url="

    companion object {
        const val malsyncAPI = "https://api.malsync.moe"
        const val tokyoInsiderAPI = "https://www.tokyoinsider.com"
        const val stremifyAPI = "https://stremify.hayd.uk/YnVpbHQtaW4sZnJlbWJlZCxmcmVuY2hjbG91ZCxtZWluZWNsb3VkLGtpbm9raXN0ZSxjaW5laGRwbHVzLHZlcmhkbGluayxndWFyZGFoZCx2aXNpb25jaW5lLHdlY2ltYSxha3dhbSxkcmFtYWNvb2wsZHJhbWFjb29sX2NhdGFsb2csZ29nb2FuaW1lLGdvZ29hbmltZV9jYXRhbG9n/stream"
        const val WYZIESubsAPI = "https://sub.wyzie.ru"
        const val MostraguardaAPI = "https://mostraguarda.stream"
        const val CONSUMET_API = BuildConfig.CONSUMET_API
        const val RarAPI = "https://nepu.to"
        const val animepaheAPI = "https://animepahe.si"
        const val allmovielandAPI = "https://allmovieland.ac"
        const val torrentioAPI = "https://torrentio.strem.fun"
        const val anizoneAPI = "https://anizone.to"
        const val AllanimeAPI = "https://api.allanime.day/api"
        const val StreamAsiaAPI = "https://stremio-dramacool-addon.xyz/eyJraXNza2gtY2F0YWxvZ3MiOlsia2toLXNlYXJjaC1yZXN1bHRzIiwia2toLWtvcmVhbi1kcmFtYSIsImtraC1rb3JlYW4tbW92aWVzIl0sImtkaGQtY2F0YWxvZ3MiOlsia2RoZC1zZWFyY2gtcmVzdWx0cyJdLCJvdHR2LWNhdGFsb2dzIjpbIm90dHYtc2VhcmNoLXJlc3VsdHMiXSwiZGRsLWNhdGFsb2dzIjpbXSwidHJha3RDb2RlIjpudWxsLCJzaG93VE1EQlNlYXNvbiI6dHJ1ZSwiZW5hYmxlT3BlbnN1YnMiOnRydWUsImhpZGVVcGNvbWluZ1Nob3dzIjp0cnVlLCJkZWJ1Z0ZsYWdzIjoiIiwibWVkaWFmbG93UHJveHlDb25maWdzIjpbXSwiZGVicmlkQ29uZmlnIjpbXSwiaGlkZVVuc3VwcG9ydGVkSG9zdGVycyI6ZmFsc2UsInZlcnNpb24iOiIxLjMuMSJ9"
        const val torrentioCONFIG = "sort=seeders"
        const val Player4uApi = "https://player4u.xyz"
        const val PrimeSrcApi = "https://primesrc.me"
        //const val VidJoyApi = "https://vidjoy.pro"
        const val soaperAPI = "https://soaper.live"
        const val asiaflixAPI = "https://asiaflix.net"
        const val twoembedAPI = "https://2embed.cc"
        const val xprimeBaseAPI = "https://xprime.today"
        const val xprimeAPI = "https://backend.xprime.today"
        const val sudatchiAPI = "https://sudatchi.com"
        const val aniversehdAPI = "https://aniversehd.com"
        const val animezAPI = "https://animeyy.com"
        const val webStreamrAPI = """https://webstreamr.hayd.uk/{"multi":"on","de":"on","en":"on","es":"on","fr":"on","it":"on","mx":"on","mediaFlowProxyUrl":"","mediaFlowProxyPassword":"","proxyConfig":"","disableExtractor_hubcloud":"on"}"""
        const val mp4MoviezAPI = "https://www.mp4moviez.zip"
        const val Film1kApi = "https://www.film1k.com"
        const val cinemaOSApi = "https://cinemaos.tech"
        const val tripleOneMoviesApi = "https://111movies.com"
        const val vidfastProApi = "https://vidfast.pro"
        const val vidPlusApi = "https://player.vidplus.to"
        const val multiEmbededApi = "https://multiembed.mov"
        const val vidSrcApi = "https://api.rgshows.ru"
        const val vidSrcHindiApi = "https://hindi.rgshows.ru"
        const val dahmerMoviesAPI = "https://a.111477.xyz"
        const val netflix2API = "https://net51.cc"
        const val hexaAPI = "https://themoviedb.hexa.su"
        const val videasyAPI = "https://api.videasy.net"
        const val vidlinkAPI = "https://vidlink.pro"
        const val multiDecryptAPI = "https://enc-dec.app/api"
        const val torrentsDBAPI = "https://torrentsdb.com/eyJsYW5ndWFnZSI6WyJoaW5kaSJdLCJsaW1pdCI6IjUifQ=="
        const val cometAPI = "https://comet.elfhosted.com"
        const val animetoshoAPI = "https://feed.animetosho.org"
        const val anizipAPI = "https://api.ani.zip"
        const val animekaiAPI = "https://anikai.to"
        const val mappleAPI = "https://mapple.mov"
        const val vidzeeApi = "https://player.vidzee.wtf"

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
        val nuvioStreamsAPI get() = api("nuvio")
        val XDmoviesAPI get() = api("xdmovies")
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
        "$aiometa_url/catalog/movie/tvdb.trending/skip=###" to "Trending Movies",
        "$aiometa_url/catalog/series/tvdb.trending/skip=###" to "Trending Series",
        "$mainUrl/top/catalog/movie/top/skip=###" to "Top Movies",
        "$mainUrl/top/catalog/series/top/skip=###" to "Top Series",
        "$aiometa_url/catalog/anime/mal.airing/skip=###" to "Top Airing Anime",
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

    private fun getPoster(url: String? = null): String? {
        if (url.isNullOrBlank()) return null

        if(url.contains("metahub.space")) {
            return image_proxy + url.replace("/small/", "/large/").replace("/medium/", "/large/")
        } else {
            return url
        }
    }

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
            val title = movie.aliases?.firstOrNull() ?: movie.name ?: ""
            newMovieSearchResponse(title, PassData(movie.id, movie.type).toJson(), type) {
                this.posterUrl = getPoster(movie.poster)
                this.score = Score.from10(movie.imdbRating)
            }
        }
        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = home,
            ),
            hasNext = movies.hasMore
        )
    }

    override suspend fun search(query: String): List<SearchResponse> = coroutineScope {

        suspend fun fetchResults(url: String): List<SearchResponse> {
            val result = runCatching {
                val json = app.get(url).text
                tryParseJson<SearchResult>(json)?.metas?.map {
                    val title = it.aliases?.firstOrNull() ?: it.name ?: ""
                    newMovieSearchResponse(title, PassData(it.id, it.type).toJson()).apply {
                        posterUrl = getPoster(it.poster)
                        this.score = Score.from10(it.imdbRating)
                    }
                } ?: emptyList()
            }.getOrDefault(emptyList())

            if (result.isNotEmpty()) return result
            return emptyList()
        }

        val endpoints = listOf(
            "$cinemeta_url/catalog/movie/top/search=$query.json",
            "$cinemeta_url/catalog/series/top/search=$query.json",
            "$kitsu_url/catalog/anime/kitsu-anime-airing/search=$query.json"
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
            if(id.contains("kitsu") || id.contains("mal")) kitsu_url
            else cinemeta_url
        val isKitsu = if(meta_url == kitsu_url) true else false
        val kitsuId = if(isKitsu) id.substringAfter("kitsu:") else null
        id = if(isKitsu) id.replace(":", "%3A") else id
        val json = app.get("$meta_url/meta/$tvtype/$id.json").text
        val movieData = tryParseJson<ResponseData>(json)
        if(isKitsu && id.contains("mal")) {
           id = movieData?.meta?.id ?: id
        }
        val externalIds = if(isKitsu) getExternalIds(id.substringAfter("kitsu:"),"kitsu") else  null
        val malId = if(externalIds != null) externalIds.myanimelist else null
        val anilistId = if(externalIds != null) externalIds.anilist else null
        val title = movieData?.meta?.name.toString()
        val engTitle = movieData?.meta?.aliases?.firstOrNull() ?: title
        val posterUrl = getPoster(movieData ?.meta?.poster)
        val imdbRating = movieData?.meta?.imdbRating?.toDoubleOrNull()
        val year = movieData?.meta?.year
        val releaseInfo = movieData?.meta?.releaseInfo
        val tmdbId = movieData?.meta?.moviedb_id
        id = if(!isKitsu) movieData?.meta?.imdb_id.toString() else id
        var description = movieData?.meta?.description

        var actors = if(isKitsu) {
            null
        } else {
           parseCastData(tvtype, id)
        }

        if(actors == null && !isKitsu) {
            actors = movieData?.meta?.cast?.mapNotNull { name ->
                ActorData(
                    actor = Actor(name, null),
                    roleString = null
                )
            } ?: emptyList()
        }

        val country = movieData?.meta?.country ?: ""
        val genre = movieData?.meta?.genre ?: movieData?.meta?.genres ?: emptyList()
        val background = getPoster(movieData?.meta?.background)
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
                malId,
                kitsuId,
            ).toJson()
            return newMovieLoadResponse(engTitle, url, if(isAnime) TvType.AnimeMovie  else type, data) {
                this.posterUrl = posterUrl
                this.plot = description
                this.tags = genre
                this.score = Score.from10(imdbRating)
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
                        malId,
                        kitsuId
                    ).toJson()
                ) {
                    this.name = ep.name ?: ep.title
                    this.season = ep.season
                    this.episode = ep.episode
                    this.posterUrl = ep.thumbnail
                    this.description = ep.overview
                    this.score = Score.from10(ep.rating?.toDoubleOrNull())
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
                this.score = Score.from10(imdbRating)
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
                runAllAsync(
                    {
                        invokeAllSources(
                            AllLoadLinksData(
                                res.title,
                                res.id,
                                res.tmdbId,
                                res.anilistId,
                                res.malId,
                                res.kitsuId,
                                year,
                                seasonYear,
                                res.season,
                                res.episode,
                                res.isAnime,
                                res.isBollywood,
                                res.isAsian,
                                res.isCartoon,
                                null,
                                null,
                                null,
                                null,
                                null,
                            ),
                            subtitleCallback,
                            callback
                        )
                    },
                    {
                        if (res.isAnime) {
                            val (aniId, malId) = convertImdbToAnimeId(res.title, year, res.firstAired, if (res.tvtype == "movie") TvType.AnimeMovie else TvType.Anime)
                            invokeAnimes(malId, aniId, res.episode, seasonYear, "imdb", subtitleCallback, callback)
                        }
                    }
                )
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
        val kitsuId : String? = null,
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
        val imdbRating: String?,
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
        val metas: List<Media>,
        val hasMore: Boolean = true,
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
                imdbYear = meta.year?.substringBefore("-")?.toIntOrNull()
                            ?: meta.year?.substringBefore("–")?.toIntOrNull()
                            ?: meta.year?.toIntOrNull()
                tmdbId = meta.moviedb_id
            }
        } catch (e: Exception) {
            println("Cinemeta API failed: ${e.localizedMessage}")
        }

        invokeAllAnimeSources(
            AllLoadLinksData(
                res.title,
                res.imdb_id,
                tmdbId,
                res.anilistId,
                res.malId,
                res.kitsuId,
                year,
                seasonYear,
                res.season,
                res.episode,
                res.isAnime,
                res.isBollywood,
                res.isAsian,
                res.isCartoon,
                null,
                imdbTitle,
                res.imdbSeason,
                res.imdbEpisode,
                imdbYear,
            ),
            subtitleCallback,
            callback
        )
    }
}

