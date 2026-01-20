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
    override val providerType = ProviderType.MetaProvider
    override val hasDownloadSupport = true
    private val skipMap: MutableMap<String, Int> = mutableMapOf()
    val cinemeta_url = "https://v3-cinemeta.strem.io"
    val kitsu_url = "https://anime-kitsu.strem.fun"
    val haglund_url = "https://arm.haglund.dev/api/v2"
    val aiometa_url = "https://aiometadata.elfhosted.com/stremio/9197a4a9-2f5b-4911-845e-8704c520bdf7"

    companion object {
        const val malsyncAPI = "https://api.malsync.moe"
        const val tokyoInsiderAPI = "https://www.tokyoinsider.com"
        const val WYZIESubsAPI = "https://sub.wyzie.ru"
        const val MostraguardaAPI = "https://mostraguarda.stream"
        const val CONSUMET_API = BuildConfig.CONSUMET_API
        const val animepaheAPI = "https://animepahe.si"
        const val allmovielandAPI = "https://allmovieland.io"
        const val torrentioAPI = "https://torrentio.strem.fun"
        const val anizoneAPI = "https://anizone.to"
        const val AllanimeAPI = "https://api.allanime.day/api"
        const val torrentioCONFIG = "sort=seeders"
        const val PrimeSrcApi = "https://primesrc.me"
        const val asiaflixAPI = "https://asiaflix.net"
        const val twoembedAPI = "https://2embed.cc"
        const val xprimeBaseAPI = "https://xprime.today"
        const val xprimeAPI = "https://backend.xprime.today"
        const val sudatchiAPI = "https://sudatchi.com"
        const val aniversehdAPI = "https://aniversehd.com"
        const val animezAPI = "https://animeyy.com"
        const val webStreamrAPI = """https://webstreamr.hayd.uk/{"multi":"on","al":"on","de":"on","es":"on","fr":"on","hi":"on","it":"on","mx":"on","mediaFlowProxyUrl":"","mediaFlowProxyPassword":"","disableExtractor_hubcloud":"on"}"""
        const val mp4MoviezAPI = "https://www.mp4moviez.talk"
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
        const val animetoshoAPI = "https://feed.animetosho.org"
        const val anizipAPI = "https://api.ani.zip"
        const val mappleAPI = "https://mapple.uk"
        const val vidzeeApi = "https://player.vidzee.wtf"
        const val nodebridAPI = "https://nodebrid.fly.dev"
        const val animeWorldAPI = "https://anime-world-stremio-addon.onrender.com"
        const val kissKhAPI = "https://kisskh.ws"
        const val bollywoodAPI = "https://tga-hd.api.hashhackers.com"
        const val bollywoodBaseAPI = "https://bollywood.eu.org"
        const val vadapavAPI = "https://vadapav.mov"
        const val YflixAPI = "https://solarmovie.fi"
        const val notorrentAPI = "https://addon-osvh.onrender.com"
        var leviathanAPI = """
        https://leviathanaddon.dpdns.org/eyJzZXJ2aWNlIjoicmQiLCJrZXkiOi
        IiLCJ0bWRiIjoiIiwic29ydCI6ImJhbGFuY2VkIiwiYWlvc3RyZWFtc19tb2RlIj
        pmYWxzZSwibWVkaWFmbG93Ijp7InVybCI6IiIsInBhc3MiOiIiLCJwcm94eURlYn
        JpZCI6ZmFsc2V9LCJmaWx0ZXJzIjp7ImFsbG93RW5nIjpmYWxzZSwibm80ayI6Zm
        Fsc2UsIm5vMTA4MCI6ZmFsc2UsIm5vNzIwIjpmYWxzZSwibm9TY3IiOmZhbHNlLC
        Jub0NhbSI6ZmFsc2UsImVuYWJsZVZpeCI6dHJ1ZSwiZW5hYmxlR2hkIjp0cnVlLC
        JlbmFibGVHcyI6dHJ1ZSwidml4TGFzdCI6ZmFsc2UsInNjUXVhbGl0eSI6ImFsbC
        IsIm1heFBlclF1YWxpdHkiOjAsIm1heFNpemVHQiI6bnVsbH19
        """.trimIndent().replace("\n", "")

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
        val animekaiAPI get() = api("animekai")
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
                this.posterUrl = movie.poster
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
                        posterUrl = it.poster
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
        val movieData = tryParseJson<ResponseData>(json)?.meta
        if(isKitsu && id.contains("mal")) {
           id = movieData?.id ?: id
        }
        val externalIds = if(isKitsu) getExternalIds(id.substringAfter("kitsu:"),"kitsu") else  null
        val malId = if(externalIds != null) externalIds.myanimelist else null
        val anilistId = if(externalIds != null) externalIds.anilist else null
        val title = movieData?.name.toString()
        val engTitle = movieData?.aliases?.firstOrNull() ?: title
        val posterUrl = movieData?.poster
        val logo = movieData?.logo
        val imdbRating = movieData?.imdbRating?.toDoubleOrNull()
        val year = movieData?.year
        val releaseInfo = movieData?.releaseInfo
        val tmdbId = movieData?.moviedb_id
        id = if(!isKitsu) movieData?.imdb_id.toString() else id
        var description = movieData?.description

        var actors = if(isKitsu) {
            null
        } else {
           parseCastData(tvtype, id)
        }

        if(actors == null && !isKitsu) {
            actors = movieData?.cast?.mapNotNull { name ->
                ActorData(
                    actor = Actor(name, null),
                    roleString = null
                )
            } ?: emptyList()
        }

        val country = movieData?.country ?: ""
        val genre = movieData?.genre ?: movieData?.genres ?: emptyList()
        val background = movieData?.background
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
                try { this.logoUrl = logo} catch(_:Throwable){}
                this.duration = movieData?.runtime?.replace(" min", "")?.toIntOrNull()
                this.contentRating = if(isKitsu) "Kitsu" else "IMDB"
                this.actors = actors
                addAniListId(anilistId)
                addMalId(malId)
                addImdbId(id)
            }
        }
        else {
            val episodes = movieData?.videos?.map { ep ->
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
                try { this.logoUrl = logo} catch(_:Throwable){}
                this.duration = movieData?.runtime?.replace(" min", "")?.toIntOrNull()
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
        val logo: String?,
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

