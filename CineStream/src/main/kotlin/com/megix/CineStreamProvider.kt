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
import com.lagradost.cloudstream3.argamap
import com.lagradost.cloudstream3.network.CloudflareKiller
import com.megix.CineStreamExtractors.invokeVegamovies
import com.megix.CineStreamExtractors.invokeRogmovies
import com.megix.CineStreamExtractors.invokeMoviesmod
import com.megix.CineStreamExtractors.invokeTopMovies
import com.megix.CineStreamExtractors.invokeMoviesdrive
import com.megix.CineStreamExtractors.invokeFull4Movies
import com.megix.CineStreamExtractors.invokeW4U
import com.megix.CineStreamExtractors.invokeWHVXSubs
import com.megix.CineStreamExtractors.invokeWYZIESubs
import com.megix.CineStreamExtractors.invokeAutoembed
import com.megix.CineStreamExtractors.invokeVidbinge
import com.megix.CineStreamExtractors.invokeUhdmovies
import com.megix.CineStreamExtractors.invokeVidSrcNL
import com.megix.CineStreamExtractors.invokeMovies
import com.megix.CineStreamExtractors.invoke2embed
import com.megix.CineStreamExtractors.invokeRar
import com.megix.CineStreamExtractors.invokeAnimes
import com.megix.CineStreamExtractors.invokeVite
import com.megix.CineStreamExtractors.invokeMultimovies
import com.megix.CineStreamExtractors.invokeStreamify
import com.megix.CineStreamExtractors.invokeCinemaluxe
import com.megix.CineStreamExtractors.invokeBollyflix
import com.megix.CineStreamExtractors.invokeTom
import com.megix.CineStreamExtractors.invokeTorrentio
import com.megix.CineStreamExtractors.invokeDramaCool

open class CineStreamProvider : MainAPI() {
    override var mainUrl = "https://cinemeta-catalogs.strem.io"
    override var name = "CineStream"
    override val hasMainPage = true
    override var lang = "en"
    override val hasDownloadSupport = true
    val skipMap: MutableMap<String, Int> = mutableMapOf()
    val cinemeta_url = "https://v3-cinemeta.strem.io"
    val kitsu_url = "https://anime-kitsu.strem.fun"
    val haglund_url = "https://arm.haglund.dev/api/v2"
    val streamio_TMDB = "https://94c8cb9f702d-tmdb-addon.baby-beamup.club"
    val mediaFusion = "https://mediafusion.elfhosted.com"
    companion object {
        const val malsyncAPI = "https://api.malsync.moe"
        const val vegaMoviesAPI = "https://vegamovies.ms"
        const val rogMoviesAPI = "https://rogmovies.com"
        const val MovieDrive_API = "https://moviesdrive.pro"
        const val topmoviesAPI = "https://topmovies.bet"
        const val MoviesmodAPI = "https://moviesmod.red"
        const val Full4MoviesAPI = "https://www.full4movies.delivery"
        const val stremifyAPI = "https://stremify.hayd.uk/stream"
        const val W4UAPI = "https://world4ufree.observer"
        const val WHVXSubsAPI = "https://subs.whvx.net"
        const val WYZIESubsAPI = "https://subs.wyzie.ru"
        const val AutoembedAPI = "https://autoembed.cc"
        const val WHVXAPI = "https://api.whvx.net"
        const val uhdmoviesAPI = "https://uhdmovies.bet"
        const val WHVX_TOKEN = BuildConfig.WHVX_TOKEN
        const val CONSUMET_API = BuildConfig.CONSUMET_API
        const val moviesAPI = "https://moviesapi.club"
        const val TwoEmbedAPI = "https://2embed.wafflehacker.io"
        const val RarAPI = "https://nepu.to"
        const val hianimeAPI = "https://hianime.to"
        const val animepaheAPI = "https://animepahe.ru"
        const val viteAPI = "https://viet.autoembed.cc"
        const val multimoviesAPI = "https://multimovies.lat"
        const val anitaku = "https://anitaku.pe"
        const val cinemaluxeAPI = "https://cinemaluxe.fans"
        const val bollyflixAPI = "https://bollyflix.diy"
        const val TomAPI = "https://tom.autoembed.cc"
        const val torrentioAPI = "https://torrentio.strem.fun"
        const val TRACKER_LIST_URL = "https://raw.githubusercontent.com/ngosang/trackerslist/master/trackers_all_ip.txt"
        const val torrentioCONFIG = "providers=yts,eztv,rarbg,1337x,thepiratebay,kickasstorrents,torrentgalaxy,magnetdl,horriblesubs,nyaasi,tokyotosho,anidex|sort=seeders|qualityfilter=threed,480p,other,scr,cam,unknown|limit=10"
    }
    val wpRedisInterceptor by lazy { CloudflareKiller() }
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
        TvType.AsianDrama,
        TvType.Anime,
        TvType.Torrent
    )

    override val mainPage = mainPageOf(
        "$streamio_TMDB/catalog/movie/tmdb.trending/skip=###&genre=Day" to "Trending Movies Today",
        "$streamio_TMDB/catalog/series/tmdb.trending/skip=###&genre=Day" to "Trending Series Today",
        "$mainUrl/top/catalog/movie/top/skip=###" to "Top Movies",
        "$mainUrl/top/catalog/series/top/skip=###" to "Top Series",
        "$mediaFusion/catalog/movie/hindi_hdrip/skip=###" to "Trending Movie in India",
        "$mediaFusion/catalog/series/hindi_series/skip=###" to "Trending Series in India",
        "$kitsu_url/catalog/anime/kitsu-anime-airing/skip=###" to "Top Airing Anime",
        "$kitsu_url/catalog/anime/kitsu-anime-trending/skip=###" to "Trending Anime",
        "$streamio_TMDB/catalog/series/tmdb.language/skip=###&genre=Korean" to "Trending Korean Series",
        "$mainUrl/imdbRating/catalog/movie/imdbRating/skip=###&genre=Action" to "Top Action Movies",
        "$mainUrl/imdbRating/catalog/series/imdbRating/skip=###&genre=Action" to "Top Action Series",
        "$mainUrl/imdbRating/catalog/movie/imdbRating/skip=###&genre=Comedy" to "Top Comedy Movies",
        "$mainUrl/imdbRating/catalog/series/imdbRating/skip=###&genre=Comedy" to "Top Comedy Series",
        "$mainUrl/imdbRating/catalog/movie/imdbRating/skip=###&genre=Romance" to "Top Romance Movies",
        "$mainUrl/imdbRating/catalog/series/imdbRating/skip=###&genre=Romance" to "Top Romance Series",
        "$mainUrl/imdbRating/catalog/movie/imdbRating/skip=###&genre=Horror" to "Top Horror Movies",
        "$mainUrl/imdbRating/catalog/series/imdbRating/skip=###&genre=Horror" to "Top Horror Series",
        "$mainUrl/imdbRating/catalog/movie/imdbRating/skip=###&genre=Thriller" to "Top Thriller Movies",
        "$mainUrl/imdbRating/catalog/series/imdbRating/skip=###&genre=Thriller" to "Top Thriller Series",
        "$mainUrl/imdbRating/catalog/movie/imdbRating/skip=###&genre=Sci-Fi" to "Top Sci-Fi Movies",
        "$mainUrl/imdbRating/catalog/series/imdbRating/skip=###&genre=Sci-Fi" to "Top Sci-Fi Series",
        "$mainUrl/imdbRating/catalog/movie/imdbRating/skip=###&genre=Fantasy" to "Top Fantasy Movies",
        "$mainUrl/imdbRating/catalog/series/imdbRating/skip=###&genre=Fantasy" to "Top Fantasy Series",
        "$mainUrl/imdbRating/catalog/movie/imdbRating/skip=###&genre=Mystery" to "Top Mystery Movies",
        "$mainUrl/imdbRating/catalog/series/imdbRating/skip=###&genre=Mystery" to "Top Mystery Series",
        "$mainUrl/imdbRating/catalog/movie/imdbRating/skip=###&genre=Crime" to "Top Crime Movies",
        "$mainUrl/imdbRating/catalog/series/imdbRating/skip=###&genre=Crime" to "Top Crime Series",
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val skip = if(page == 1) 0 else skipMap[request.name] ?: 0
        val newRequestData = request.data.replace("###", skip.toString())
        val json = app.get("$newRequestData.json").text
        val movies = parseJson<Home>(json)
        val movieCount = movies.metas.size
        skipMap[request.name] = skip + movieCount
        val home = movies.metas.mapNotNull { movie ->
            if (movie.id.startsWith("mf")) {
                null
            }
            else {
                val posterUrl = if(movie.poster.toString().contains("mediafusion")) {
                    "https://images.metahub.space/poster/small/${movie.id}/img"
                }
                else {
                    movie.poster.toString()
                }
                newMovieSearchResponse(movie.name, PassData(movie.id, movie.type).toJson(), TvType.Movie) {
                    this.posterUrl = posterUrl
                }
            }
        }
        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = home
            ),
            hasNext = true
        )
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()
        val animeJson = app.get("$kitsu_url/catalog/anime/kitsu-anime-list/search=$query.json").text
        val animes = tryParseJson<SearchResult>(animeJson)
        animes?.metas ?.forEach {
            searchResponse.add(newMovieSearchResponse(it.name, PassData(it.id, it.type).toJson(), TvType.Movie) {
                this.posterUrl = it.poster.toString()
            })
        }
        val movieJson = app.get("$cinemeta_url/catalog/movie/top/search=$query.json").text
        val movies = parseJson<SearchResult>(movieJson)
        movies.metas.forEach {
            searchResponse.add(newMovieSearchResponse(it.name, PassData(it.id, it.type).toJson(), TvType.Movie) {
                this.posterUrl = it.poster.toString()
            })
        }

        val seriesJson = app.get("$cinemeta_url/catalog/series/top/search=$query.json").text
        val series = parseJson<SearchResult>(seriesJson)
        series.metas.forEach {
            searchResponse.add(newMovieSearchResponse(it.name, PassData(it.id, it.type).toJson(), TvType.Movie) {
                this.posterUrl = it.poster.toString()
            })
        }

        return searchResponse.sortedByDescending { response ->
            calculateRelevanceScore(response.name, query)
        }
    }


    override suspend fun load(url: String): LoadResponse? {
        val movie = parseJson<PassData>(url)
        val tvtype = movie.type
        var id = movie.id
        val meta_url = if(id.contains("kitsu")) kitsu_url else if(id.contains("tmdb")) streamio_TMDB else cinemeta_url
        val isKitsu = if(meta_url == kitsu_url) true else false
        val isTMDB = if(meta_url == streamio_TMDB) true else false
        val externalIds = if(isKitsu) getExternalIds(id.substringAfter("kitsu:"),"kitsu") else null
        val malId = if(externalIds != null) externalIds.myanimelist else null
        val anilistId = if(externalIds != null) externalIds.anilist else null
        id = if(isKitsu) id.replace(":", "%3A") else id
        val json = app.get("$meta_url/meta/$tvtype/$id.json").text
        val movieData = tryParseJson<ResponseData>(json)
        val title = movieData ?.meta ?.name.toString()
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
            return newMovieLoadResponse(title, url, if(isAnime) TvType.AnimeMovie  else TvType.Movie, data) {
                this.posterUrl = posterUrl
                this.plot = description
                this.tags = genre
                this.rating = imdbRating.toRatingInt()
                this.year = year ?.toIntOrNull() ?: releaseInfo?.toIntOrNull() ?: year?.substringBefore("-")?.toIntOrNull()
                this.backgroundPosterUrl = background
                this.duration = movieData?.meta?.runtime?.replace(" min", "")?.toIntOrNull()
                this.contentRating = if(isKitsu) "Kitsu" else if(isTMDB) "TMDB" else "IMDB"
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
                        ep.firstAired,
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
                    addDate(ep.firstAired?.substringBefore("T"))
                }
            } ?: emptyList()

            return newTvSeriesLoadResponse(title, url, if(isAnime) TvType.Anime else TvType.TvSeries, episodes) {
                this.posterUrl = posterUrl
                this.plot = description
                this.tags = genre
                this.rating = imdbRating.toRatingInt()
                this.year = year?.substringBefore("–")?.toIntOrNull() ?: releaseInfo?.substringBefore("–")?.toIntOrNull() ?: year?.substringBefore("-")?.toIntOrNull()
                this.backgroundPosterUrl = background
                this.duration = movieData?.meta?.runtime?.replace(" min", "")?.toIntOrNull()
                this.contentRating = if(isKitsu) "Kitsu" else if(isTMDB) "TMDB" else "IMDB"
                addActors(cast)
                addAniListId(anilistId)
                addImdbId(id)
                addMalId(malId)
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
        val year = if(res.tvtype == "movie") res.year?.toIntOrNull() else res.year?.substringBefore("-")?.toIntOrNull() ?: res.year?.substringBefore("–")?.toIntOrNull()
        //to get season year
        val seasonYear = if(res.tvtype == "movie") year else res.firstAired?.substringBefore("-")?.toIntOrNull() ?: res.firstAired?.substringBefore("–")?.toIntOrNull()

        if(res.isKitsu) {
            argamap(
                {
                    invokeAnimes(
                        res.malId,
                        res.anilistId,
                        res.episode,
                        year,
                        subtitleCallback,
                        callback
                    )
                },
                {
                    invokeTorrentio(
                        res.imdb_id,
                        res.imdbSeason,
                        res.imdbEpisode,
                        callback,
                    )
                },
                {
                    invokeWHVXSubs(
                        res.imdb_id,
                        res.imdbSeason,
                        res.imdbEpisode,
                        subtitleCallback
                    )
                },
                {
                    invokeWYZIESubs(
                        res.imdb_id,
                        res.imdbSeason,
                        res.imdbEpisode,
                        subtitleCallback
                    )
                },
                {
                    invokeMultimovies(
                        multimoviesAPI,
                        res.title,
                        res.imdbSeason,
                        res.imdbEpisode,
                        subtitleCallback,
                        callback
                    )
                },
            )
        }
        else {
            argamap(
                {
                    if(!res.isBollywood) invokeVegamovies(
                        res.id,
                        res.title,
                        res.season,
                        res.episode,
                        subtitleCallback,
                        callback
                    )
                },
                {
                    invokeTom(
                        res.tmdbId,
                        res.season,
                        res.episode,
                        callback,
                        subtitleCallback
                    )
                },
                {
                    invokeVite(
                        res.id,
                        res.season,
                        res.episode,
                        callback
                    )
                },
                {
                    invokeStreamify(
                        res.id,
                        res.season,
                        res.episode,
                        callback
                    )
                },
                {
                    if(res.isBollywood) invokeRogmovies(
                        res.id,
                        res.title,
                        res.season,
                        res.episode,
                        subtitleCallback,
                        callback
                    )
                },
                {
                    if(res.isBollywood) invokeTopMovies(
                        res.title,
                        year,
                        res.season,
                        res.episode,
                        subtitleCallback,
                        callback
                    )
                },
                {
                    if(!res.isBollywood) invokeMoviesmod(
                        res.id,
                        res.season,
                        res.episode,
                        subtitleCallback,
                        callback
                    )
                },
                {
                    invokeMoviesdrive(
                        res.title,
                        res.season,
                        res.episode,
                        year,
                        subtitleCallback,
                        callback
                    )
                },
                {
                    invokeRar(
                        res.title,
                        year,
                        res.season,
                        res.episode,
                        callback
                    )
                },
                {
                    if(!res.isAnime) invokeFull4Movies(
                        res.title,
                        year,
                        res.season,
                        res.episode,
                        subtitleCallback,
                        callback
                    )
                },
                {
                    invokeCinemaluxe(
                        res.title,
                        year,
                        res.season,
                        res.episode,
                        callback,
                        subtitleCallback,
                    )
                },
                {
                    if(res.isAsian) invokeDramaCool(
                        res.title,
                        year,
                        res.season,
                        res.episode,
                        subtitleCallback,
                        callback
                    )
                },
                {
                    invokeBollyflix(
                        res.id,
                        res.season,
                        res.episode,
                        subtitleCallback,
                        callback,
                    )
                },
                {
                    invokeTorrentio(
                        res.id,
                        res.season,
                        res.episode,
                        callback,
                    )
                },
                {
                    invokeMultimovies(
                        multimoviesAPI,
                        res.title,
                        res.season,
                        res.episode,
                        subtitleCallback,
                        callback
                    )
                },
                {
                    if(!res.isAnime) invokeW4U(
                        res.title,
                        year,
                        res.id,
                        res.season,
                        res.episode,
                        subtitleCallback,
                        callback
                    )
                },
                {
                    invokeWHVXSubs(
                        res.id,
                        res.season,
                        res.episode,
                        subtitleCallback
                    )
                },
                {
                    invokeWYZIESubs(
                        res.id,
                        res.season,
                        res.episode,
                        subtitleCallback
                    )
                },
                {
                    invokeAutoembed(
                        res.tmdbId,
                        res.season,
                        res.episode,
                        callback,
                    )
                },
                // {
                //     invokeVidbinge(
                //         res.title,
                //         res.id,
                //         res.tmdbId,
                //         year,
                //         res.season,
                //         res.episode,
                //         callback,
                //         subtitleCallback
                //     )
                // },
                {
                    invokeUhdmovies(
                        res.title,
                        year,
                        res.season,
                        res.episode,
                        callback,
                        subtitleCallback
                    )
                },
                {
                    if(!res.isAnime) invokeVidSrcNL(
                        res.tmdbId,
                        res.season,
                        res.episode,
                        callback,
                    )
                },
                {
                    if (!res.isAnime) invokeMovies(
                        res.tmdbId,
                        res.season,
                        res.episode,
                        subtitleCallback,
                        callback
                    )
                },
                {
                    invoke2embed(
                        res.id,
                        res.season,
                        res.episode,
                        callback,
                        subtitleCallback
                    )
                },
            )
        }
        return true
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
        val name: String,
        val poster: String?,
    )

    data class EpisodeDetails(
        val id: String?,
        val name: String?,
        val title: String?,
        val season: Int,
        val episode: Int,
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
        val lowerCaseName = name.lowercase()
        val lowerCaseQuery = query.lowercase()

        var score = 0

        // Exact match gives the highest score
        if (lowerCaseName == lowerCaseQuery) {
            score += 100
        }

        // Check for partial matches and their positions
        if (lowerCaseName.contains(lowerCaseQuery)) {
            score += 50 // Base score for containing the query

            // Increase score based on position of the match
            val index = lowerCaseName.indexOf(lowerCaseQuery)
            if (index == 0) {
                score += 20 // Higher score if match is at the start
            } else if (index > 0 && index < 5) {
                score += 10 // Slightly higher score if match is near the start
            }

            // Count how many words match (for multi-word queries)
            lowerCaseQuery.split(" ").forEach { word ->
                if (lowerCaseName.contains(word)) {
                    score += 5 // Incremental score for each matched word
                }
            }
        }

        return score
    }
}

