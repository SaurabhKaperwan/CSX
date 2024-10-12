package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addImdbId
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
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

open class CineStreamProvider : MainAPI() {
    override var mainUrl = "https://cinemeta-catalogs.strem.io"
    override var name = "CineStream"
    override val hasMainPage = true
    override var lang = "en"
    override val hasDownloadSupport = true
    val cinemeta_url = "https://v3-cinemeta.strem.io"
    val cyberflix_url = "https://cyberflix.elfhosted.com/c/catalogs"
    companion object {
        const val vegaMoviesAPI = "https://vegamovies.fans"
        const val rogMoviesAPI = "https://rogmovies.top"
        const val MovieDrive_API="https://moviesdrive.world"
        const val topmoviesAPI = "https://topmovies.mov"
        const val MoviesmodAPI = "https://moviesmod.day"
        const val Full4MoviesAPI = "https://www.full4movies.forum"
        const val VadapavAPI = "https://vadapav.mov"
        const val netflixAPI = "https://iosmirror.cc"
        const val myConsumetAPI = "https://consumet-seven-ashy.vercel.app"
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
        "$cyberflix_url/catalog/Anime/anime.new.movie.json" to "New Anime Movie",
        "$cyberflix_url/catalog/Anime/anime.trending.movie.json" to "Trending Anime Movie",
        "$cyberflix_url/catalog/Anime/anime.new.series.json" to "New Anime Series",
        "$cyberflix_url/catalog/Anime/anime.trending.series.json" to "Trending Anime Series",
        "$cyberflix_url/catalog/Netflix/netflix.new.series.json" to "Netflix Series",
        "$cyberflix_url/catalog/Netflix/netflix.new.movie.json" to "Netflix Movie",
        "$cyberflix_url/catalog/Amazon%20Prime/amazon_prime.new.movie.json" to "Amazon Prime Movie",
        "$cyberflix_url/catalog/Amazon%20Prime/amazon_prime.new.series.json" to "Amazon Prime Series",
        "$cyberflix_url/catalog/Disney%20Plus/disney_plus.new.movie.json" to "Disney Plus Movie",
        "$cyberflix_url/catalog/Disney%20Plus/disney_plus.new.series.json" to "Disney Plus Series",
        "$cyberflix_url/catalog/Asian/asian.new.movie.json" to "New Asian Movie",
        "$cyberflix_url/catalog/Asian/asian.new.series.json" to "New Asian Series",
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
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
        val id = movie.id
        val json = app.get("$cinemeta_url/meta/$tvtype/$id.json").text
        val movieData = parseJson<ResponseData>(json)

        val title = movieData.meta.name.toString()
        val posterUrl = movieData.meta.poster.toString()
        val imdbRating = movieData.meta.imdbRating
        val year = movieData.meta.year
        val releaseInfo = movieData.meta.releaseInfo.toString()
        var description = movieData.meta.description.toString()
        val cast : List<String> = movieData.meta.cast ?: emptyList()
        val genre : List<String> = movieData.meta.genre ?: emptyList()
        val background = movieData.meta.background.toString()
        val isCartoon = genre.any { it.contains("Animation", true) }
        val isAnime = (movieData.meta.country.toString().contains("Japan", true) || 
            movieData.meta.country.toString().contains("China", true)) && isCartoon
        val isBollywood = movieData.meta.country.toString().contains("India", true)
        val isAsian = (movieData.meta.country.toString().contains("Korea", true) ||
                movieData.meta.country.toString().contains("China", true)) && !isAnime

        if(tvtype == "movie") {
            val data = LoadLinksData(
                title,
                id,
                tvtype,
                year ?: releaseInfo,
                null,
                null,
                null,
                isAnime,
                isBollywood,
                isAsian,
                isCartoon   
            ).toJson()
            return newMovieLoadResponse(title, url, TvType.Movie, data) {
                this.posterUrl = posterUrl
                this.plot = description
                this.tags = genre
                this.rating = imdbRating.toRatingInt()
                this.year = year?.toIntOrNull() ?: releaseInfo.toIntOrNull()
                this.backgroundPosterUrl = background
                addActors(cast)
                addImdbId(id)
            }
        }
        else {
            val episodes = movieData.meta.videos?.map { ep ->
                newEpisode(
                    LoadLinksData(
                        title,
                        id,
                        tvtype,
                        year ?: releaseInfo,
                        ep.season,
                        ep.episode,
                        ep.firstAired,
                        isAnime,
                        isBollywood,
                        isAsian,
                        isCartoon
                    ).toJson()
                ) {
                    this.name = ep.name ?: ep.title
                    this.season = ep.season
                    this.episode = ep.episode
                    this.posterUrl = ep.thumbnail
                    this.description = ep.overview
                }
            } ?: emptyList()

            return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = posterUrl
                this.plot = description
                this.tags = genre
                this.rating = imdbRating.toRatingInt()
                this.year = year?.substringBefore("–")?.toIntOrNull() ?: releaseInfo.substringBefore("–").toIntOrNull()
                this.backgroundPosterUrl = background
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
        val year = if(res.tvtype == "movie") res.year.toIntOrNull() else res.firstAired?.substringBefore("-")?.toIntOrNull()
        val firstYear = if(res.tvtype == "movie") res.year.toIntOrNull() else res.year.substringBefore("–").toIntOrNull()
        argamap(
            {
                if(!res.isBollywood) invokeVegamovies(
                    res.title,
                    year,
                    res.season,
                    res.episode,
                    subtitleCallback,
                    callback
                )
            },
            {
                if(res.isBollywood) invokeRogmovies(
                    res.title,
                    year,
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
                    res.title,
                    year,
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
                invokeVadaPav(
                    res.title,
                    year,
                    res.season,
                    res.episode,
                    subtitleCallback,
                    callback
                )
            },
            {
                invokeNetflix(
                    res.title,
                    firstYear,
                    res.season,
                    res.episode,
                    subtitleCallback,
                    callback
                )
            },
            {
                invokePrimeVideo(
                    res.title,
                    firstYear,
                    res.season,
                    res.episode,
                    subtitleCallback,
                    callback
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
        )
        return true
    }

    data class LoadLinksData(
        val title: String,
        val id: String,
        val tvtype: String,
        val year: String,
        val season: Int? = null,
        val episode: Int? = null,
        val firstAired: String? = null,
        val isAnime: Boolean = false,
        val isBollywood: Boolean = false,
        val isAsian: Boolean = false,
        val isCartoon: Boolean = false
    )

    data class PassData(
        val id: String,
        val type: String
    )

    data class Meta(
        val id: String?,
        val imdb_id: String?,
        val type: String?,
        val poster: String?,
        val logo: String?,
        val background: String?,
        val moviedb_id: Int?,
        val name: String?,
        val description: String?,
        val genre: List<String>?,
        val releaseInfo: String?,
        val status: String?,
        val runtime: String?,
        val cast: List<String>?,
        val language: String?,
        val country: String?,
        val imdbRating: String?,
        val slug: String?,
        val year: String?,
        val videos: List<EpisodeDetails>?
    )

    data class SearchResult(
        val query: String,
        val rank: Double,
        val cacheMaxAge: Long,
        val metas: List<Media>
    )

    data class Media(
        val id: String,
        val imdb_id: String,
        val type: String,
        val name: String,
        val releaseInfo: String?,
        val poster: String?,
        val slug: String?,
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
        val moviedb_id: Int?
    )

    data class ResponseData(
        val meta: Meta,
    )

    data class Home(
        val metas: List<Media>
    )
}

