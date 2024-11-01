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
import com.megix.CineStreamExtractors.invokeVadaPav
import com.megix.CineStreamExtractors.invokeNetflix
import com.megix.CineStreamExtractors.invokePrimeVideo
import com.megix.CineStreamExtractors.invokeDramaCool
import com.megix.CineStreamExtractors.invokeW4U
import com.megix.CineStreamExtractors.invokeWHVXSubs
import com.megix.CineStreamExtractors.invokeWYZIESubs
import com.megix.CineStreamExtractors.invokeAutoembed
//import com.megix.CineStreamExtractors.invokeNova
//import com.megix.CineStreamExtractors.invokeAstra
import com.megix.CineStreamExtractors.invokeUhdmovies
import com.megix.CineStreamExtractors.invokeVidSrcNL
import com.megix.CineStreamExtractors.invokeMovies
import com.megix.CineStreamExtractors.invoke2embed
//import com.megix.CineStreamExtractors.invokeFilmyxy
import com.megix.CineStreamExtractors.invokeAutoembedDrama
import com.megix.CineStreamExtractors.invokeRar
import com.megix.CineStreamExtractors.invokeAnimes

open class CineStreamProvider : MainAPI() {
    override var mainUrl = "https://cinemeta-catalogs.strem.io"
    override var name = "CineStream"
    override val hasMainPage = true
    override var lang = "en"
    override val hasDownloadSupport = true
    val cinemeta_url = "https://v3-cinemeta.strem.io"
    val cyberflix_url = "https://cyberflix.elfhosted.com/c/catalogs"
    val kitsu_url = "https://anime-kitsu.strem.fun"
    val haglund_url = "https://arm.haglund.dev/api/v2"
    //val jikan_url = "https://api.jikan.moe/v4"
    companion object {
        const val malsyncAPI = "https://api.malsync.moe"

        const val vegaMoviesAPI = "https://vegamovies.si"
        const val rogMoviesAPI = "https://rogmovies.fun"
        const val MovieDrive_API="https://moviesdrive.world"
        const val topmoviesAPI = "https://topmovies.icu"
        const val MoviesmodAPI = "https://moviesmod.bid"
        const val Full4MoviesAPI = "https://www.full4movies.my"
        const val VadapavAPI = "https://vadapav.mov"
        const val netflixAPI = "https://iosmirror.cc"
        const val W4UAPI = "https://world4ufree.joburg"
        const val WHVXSubsAPI = "https://subs.whvx.net"
        const val WYZIESubsAPI = "https://subs.wyzie.ru"
        const val AutoembedAPI = "https://autoembed.cc"
        //const val WHVXAPI = "https://api.whvx.net"
        const val uhdmoviesAPI = "https://uhdmovies.icu"
        const val myConsumetAPI = BuildConfig.CONSUMET_API
        const val moviesAPI = "https://moviesapi.club"
        const val TwoEmbedAPI = "https://2embed.wafflehacker.io"
        //const val FilmyxyAPI = "https://filmxy.wafflehacker.io"
        const val AutoembedDramaAPI = "https://asian-drama.autoembed.cc"
        const val RarAPI = "https://rar.to"
        const val hianimeAPI = "https://hianime.to"
        const val animepaheAPI = "https://animepahe.ru"
    }
    val wpRedisInterceptor by lazy { CloudflareKiller() }
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
        TvType.AsianDrama,
        TvType.Anime
    )

    override val mainPage = mainPageOf(
        "$mainUrl/top/catalog/movie/top.json" to "Top Movies",
        "$mainUrl/top/catalog/series/top.json" to "Top Series",
        "$mainUrl/imdbRating/catalog/movie/imdbRating.json" to "Top IMDb Movies",
        "$mainUrl/imdbRating/catalog/series/imdbRating.json" to "Top IMDb Series",
        "$cyberflix_url/catalog/Indian/indian.new.movie.json" to "New Indian Movie",
        "$cyberflix_url/catalog/Indian/indian.new.series.json" to "New Indian Series",
        "$cyberflix_url/catalog/Netflix/netflix.new.series.json" to "Netflix Series",
        "$cyberflix_url/catalog/Netflix/netflix.new.movie.json" to "Netflix Movie",
        "$cyberflix_url/catalog/Amazon%20Prime/amazon_prime.new.movie.json" to "Amazon Prime Movie",
        "$cyberflix_url/catalog/Amazon%20Prime/amazon_prime.new.series.json" to "Amazon Prime Series",
        "$cyberflix_url/catalog/Disney%20Plus/disney_plus.new.movie.json" to "Disney Plus Movie",
        "$cyberflix_url/catalog/Disney%20Plus/disney_plus.new.series.json" to "Disney Plus Series",
        "$cyberflix_url/catalog/Asian/asian.new.movie.json" to "New Asian Movie",
        "$cyberflix_url/catalog/Asian/asian.new.series.json" to "New Asian Series",
        BuildConfig.ANILIST_POPULAR to "AniList Popular",
        BuildConfig.ANILIST_TRENDING to "AniList Trending",
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val metaType = if(request.name.contains("AniList")) "kitsu" else "cinemeta"
        val json = app.get(request.data).text

        val movies = parseJson<Home>(json)
        val home = movies.metas.mapNotNull { movie ->
            newMovieSearchResponse(movie.name, PassData(movie.id, movie.type).toJson(), TvType.Movie) {
                this.posterUrl = movie.poster.toString()
            }
        }
        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = home
            ),
            hasNext = false
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

        return searchResponse
    }


    override suspend fun load(url: String): LoadResponse? {
        val movie = parseJson<PassData>(url)
        val tvtype = movie.type
        var id = movie.id
        val meta_url = if(movie.id.contains("kitsu")) kitsu_url else cinemeta_url
        val isKitsu = if(meta_url == kitsu_url) true else false
        val externalIds = if(isKitsu) getExternalIds(id.substringAfter("kitsu:"),"kitsu") else null
        val malId = if(externalIds != null) externalIds?.myanimelist else null
        val anilistId = if(externalIds != null) externalIds?.anilist else null
        id = if(isKitsu) id.replace(":", "%3A") else id
        val json = app.get("$meta_url/meta/$tvtype/$id.json").text
        val movieData = tryParseJson<ResponseData>(json)
        val title = movieData ?.meta ?.name.toString()
        val posterUrl = movieData ?.meta?.poster.toString()
        val imdbRating = movieData?.meta?.imdbRating
        val year = movieData?.meta?.year.toString()
        val tmdbId = movieData?.meta?.moviedb_id
        val releaseInfo = movieData?.meta?.releaseInfo.toString()
        var description = movieData?.meta?.description.toString()
        val cast : List<String> = movieData?.meta?.cast ?: emptyList()
        val genre : List<String> = movieData?.meta?.genre ?: emptyList()
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
                this.year = year?.toIntOrNull() ?: releaseInfo.toIntOrNull()
                this.backgroundPosterUrl = background
                this.duration = movieData?.meta?.runtime?.replace(" min", "")?.toIntOrNull()
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
                this.year = year?.substringBefore("–")?.toIntOrNull() ?: releaseInfo.substringBefore("–").toIntOrNull()
                this.backgroundPosterUrl = background
                this.duration = movieData?.meta?.runtime?.replace(" min", "")?.toIntOrNull()
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
        val year = if(res.tvtype == "movie") res.year.toIntOrNull() else res.firstAired?.substringBefore("-")?.toIntOrNull()
        val firstYear = if(res.tvtype == "movie") res.year.toIntOrNull() else res.year.substringBefore("–").toIntOrNull()
        argamap(
            {
                if(!res.isBollywood && !res.isKitsu) invokeVegamovies(
                    res.title,
                    year,
                    res.season,
                    res.episode,
                    subtitleCallback,
                    callback
                )
            },
            {
                if(res.isBollywood && !res.isKitsu) invokeRogmovies(
                    res.title,
                    year,
                    res.season,
                    res.episode,
                    subtitleCallback,
                    callback
                )
            },
            {
                if(res.isBollywood && !res.isKitsu) invokeTopMovies(
                    res.title,
                    year,
                    res.season,
                    res.episode,
                    subtitleCallback,
                    callback
                )
            },
            {
                if(!res.isBollywood && !res.isKitsu) invokeMoviesmod(
                    res.title,
                    year,
                    res.season,
                    res.episode,
                    subtitleCallback,
                    callback
                )
            },
            {
                if(!res.isKitsu) invokeMoviesdrive(
                    res.title,
                    res.season,
                    res.episode,
                    year,
                    subtitleCallback,
                    callback
                )
            },
            {
                if(!res.isKitsu) invokeRar(
                    res.title,
                    firstYear,
                    res.season,
                    res.episode,
                    callback
                )
            },
            {
                if(!res.isAnime && !res.isKitsu) invokeFull4Movies(
                    res.title,
                    year,
                    res.season,
                    res.episode,
                    subtitleCallback,
                    callback
                )
            },
            {
                if(!res.isKitsu) invokeVadaPav(
                    res.title,
                    year,
                    res.season,
                    res.episode,
                    subtitleCallback,
                    callback
                )
            },
            {
                if(!res.isKitsu) invokeNetflix(
                    res.title,
                    firstYear,
                    res.season,
                    res.episode,
                    subtitleCallback,
                    callback
                )
            },
            {
                if(!res.isKitsu) invokePrimeVideo(
                    res.title,
                    firstYear,
                    res.season,
                    res.episode,
                    subtitleCallback,
                    callback
                )
            },
            {
                if(res.isAsian && !res.isKitsu) invokeDramaCool(
                    res.title,
                    year,
                    res.season,
                    res.episode,
                    subtitleCallback,
                    callback
                )
            },
            {
                if(!res.isAnime && !res.isKitsu) invokeW4U(
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
                if(!res.isKitsu) invokeWHVXSubs(
                    res.id,
                    res.season,
                    res.episode,
                    subtitleCallback
                )
            },
            {
                if(!res.isKitsu) invokeWYZIESubs(
                    res.id,
                    res.season,
                    res.episode,
                    subtitleCallback
                )
            },
            {
                if(!res.isKitsu) invokeAutoembed(
                    res.tmdbId,
                    res.season,
                    res.episode,
                    callback,
                    subtitleCallback
                )
            },
            // {
            //     if(!res.isKitsu) invokeNova(
            //         res.title,
            //         res.id,
            //         res.tmdbId,
            //         firstYear,
            //         res.season,
            //         res.episode,
            //         callback,
            //         subtitleCallback
            //     )
            // },
            // {
            //     if(!res.isKitsu) invokeAstra(
            //         res.title,
            //         res.id,
            //         res.tmdbId,
            //         firstYear,
            //         res.season,
            //         res.episode,
            //         callback,
            //         subtitleCallback
            //     )
            // },
            {
                if(!res.isKitsu) invokeUhdmovies(
                    res.title,
                    year,
                    res.season,
                    res.episode,
                    callback,
                    subtitleCallback
                )
            },
            {
                if(!res.isKitsu) invokeVidSrcNL(
                    res.tmdbId,
                    res.season,
                    res.episode,
                    callback,
                )
            },
            {
                if (!res.isAnime && !res.isKitsu) invokeMovies(
                    res.tmdbId,
                    res.season,
                    res.episode,
                    subtitleCallback,
                    callback
                )
            },
            {
                if(!res.isKitsu) invoke2embed(
                    res.id,
                    res.season,
                    res.episode,
                    callback,
                    subtitleCallback
                )
            },
            // {
            //     if(!res.isKitsu) invokeFilmyxy(
            //         res.id,
            //         res.season,
            //         res.episode,
            //         callback,
            //         subtitleCallback
            //     )   
            // },
            {
                if(res.isAsian && !res.isKitsu) invokeAutoembedDrama(
                    res.title,
                    year,
                    res.season,
                    res.episode,
                    subtitleCallback,
                    callback
                )
            },
            {
                if(res.isKitsu) invokeAnimes(
                    res.malId,
                    res.anilistId,
                    res.season,
                    res.episode,
                    subtitleCallback,
                    callback
                )
            },
        )
        return true
    }

    data class LoadLinksData(
        val title: String,
        val id: String,
        val tmdbId: Int?,
        val tvtype: String,
        val year: String,
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
}

