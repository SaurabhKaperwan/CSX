package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.LoadResponse.Companion.addImdbId
import com.lagradost.cloudstream3.LoadResponse.Companion.addTMDbId
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.megix.CineStreamExtractors.invokeVegamovies
import com.megix.CineStreamExtractors.invokeMoviesmod
import com.megix.CineStreamExtractors.invokeTopMovies
import com.megix.CineStreamExtractors.invokeMoviesdrive
import com.megix.CineStreamExtractors.invokeW4U
import com.megix.CineStreamExtractors.invokeWYZIESubs
import com.megix.CineStreamExtractors.invokeAnizone
import com.megix.CineStreamExtractors.invokeUhdmovies
import com.megix.CineStreamExtractors.invokeAnimes
import com.megix.CineStreamExtractors.invokeMultimovies
import com.megix.CineStreamExtractors.invokeStreamify
import com.megix.CineStreamExtractors.invokeBollyflix
import com.megix.CineStreamExtractors.invokeTorrentio
import com.megix.CineStreamExtractors.invokeTokyoInsider
import com.megix.CineStreamExtractors.invokeAllanime
import com.megix.CineStreamExtractors.invokeStreamAsia
import com.megix.CineStreamExtractors.invokeNetflix
import com.megix.CineStreamExtractors.invokePrimeVideo
import com.megix.CineStreamExtractors.invokeDisney
import com.megix.CineStreamExtractors.invokeSkymovies
import com.megix.CineStreamExtractors.invokeMoviesflix
import com.megix.CineStreamExtractors.invokeHdmovie2
import com.megix.CineStreamExtractors.invokeHindmoviez
import com.megix.CineStreamExtractors.invokeMostraguarda
import com.megix.CineStreamExtractors.invokePlayer4U
import com.megix.CineStreamExtractors.invokeProtonmovies
import com.megix.CineStreamExtractors.invokeThepiratebay
import com.megix.CineStreamExtractors.invokeAllmovieland
import com.megix.CineStreamExtractors.invoke4khdhub
import com.megix.CineStreamExtractors.invokeMovies4u
import com.megix.CineStreamExtractors.invokeSoaper
import com.megix.CineStreamExtractors.invokeAsiaflix
import com.megix.CineStreamExtractors.invoke2embed
import com.megix.CineStreamExtractors.invokePrimebox
import com.megix.CineStreamExtractors.invokePrimenet
import com.megix.CineStreamExtractors.invokeCinemaOS
import com.megix.CineStreamExtractors.invokeGojo
import com.megix.CineStreamExtractors.invokeSudatchi
import com.megix.CineStreamExtractors.invokeKatMovieHd
import com.megix.CineStreamExtractors.invokeMadplay
import com.megix.CineStreamExtractors.invokeStremioSubtitles
import com.megix.CineStreamExtractors.invokeToonstream
import com.megix.CineStreamExtractors.invokeDramadrip
import com.megix.CineStreamExtractors.invokeFilm1k
import com.megix.CineStreamExtractors.invokeMp4Moviez
import com.megix.CineStreamExtractors.invokeMultiEmbeded
import com.megix.CineStreamExtractors.invokeWebStreamr
import com.megix.CineStreamExtractors.invokeNuvioStreams
import com.megix.CineStreamExtractors.invokePrimeSrc
import com.megix.CineStreamExtractors.invokeTripleOneMovies
import com.megix.CineStreamExtractors.invokeVidFastPro
import com.megix.CineStreamExtractors.invokeVidPlus
import com.megix.CineStreamExtractors.invokeVicSrcWtf
import com.megix.CineStreamExtractors.invokeXDmovies
import com.megix.CineStreamExtractors.invokeDahmerMovies

class CineTmdbProvider: MainAPI() {
    override var name = "CineTmdb"
    override var mainUrl = "https://www.themoviedb.org"
    override var supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
        TvType.Anime,
        TvType.AsianDrama,
        TvType.Torrent
    )
    override var lang = "en"
    override val hasMainPage = true
    override val hasQuickSearch = true
    private val apiUrl = "https://api.themoviedb.org/3"

    companion object {
        private const val apiKey = BuildConfig.TMDB_KEY
    }

    override val mainPage = mainPageOf(
        "trending/all/day?api_key=$apiKey&region=US" to "Trending",
        "trending/movie/week?api_key=$apiKey&region=US" to "Popular Movies",
        "trending/tv/week?api_key=$apiKey&region=US" to "Popular TV Shows",
        "discover/tv?api_key=$apiKey&with_original_language=ko" to "Korean Shows",
        "discover/tv?api_key=$apiKey&with_networks=213" to "Netflix",
        "discover/tv?api_key=$apiKey&with_networks=1024" to "Amazon",
        "discover/tv?api_key=$apiKey&with_networks=2739" to "Disney+",
        "discover/tv?api_key=$apiKey&with_watch_providers=2336&watch_region=IN" to "JioHotstar",
        "discover/tv?api_key=$apiKey&with_networks=453" to "Hulu",
        "discover/tv?api_key=$apiKey&with_networks=2552" to "Apple TV+",
        "discover/tv?api_key=$apiKey&with_networks=49" to "HBO",
        "discover/tv?api_key=$apiKey&with_networks=4330" to "Paramount+",
        "discover/tv?api_key=$apiKey&with_networks=3353" to "Peacock",
        "discover/movie?api_key=$apiKey&language=en-US&page=1&sort_by=popularity.desc&with_origin_country=IN&release_date.gte=${getDate().lastWeekStart}&release_date.lte=${getDate().today}" to "Trending Indian Movies",
        "discover/tv?api_key=$apiKey&with_keywords=210024|222243&sort_by=popularity.desc&air_date.lte=${getDate().today}&air_date.gte=${getDate().today}" to "Airing Today Anime",
        "discover/tv?api_key=$apiKey&with_keywords=210024|222243&sort_by=popularity.desc&air_date.lte=${getDate().nextWeek}&air_date.gte=${getDate().today}" to "On The Air Anime",
        "discover/movie?api_key=$apiKey&with_keywords=210024|222243" to "Anime Movies",
        "movie/top_rated?api_key=$apiKey&region=US" to "Top Rated Movies",
        "tv/top_rated?api_key=$apiKey&region=US" to "Top Rated TV Shows",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val type = if (request.data.contains("/movie")) "movie" else "tv"
        val home = app.get("$apiUrl/${request.data}&page=$page", timeout = 10000)
            .parsedSafe<Results>()?.results?.mapNotNull { media ->
                media.toSearchResponse(type)
            } ?: throw ErrorLoadingException("Invalid Json reponse")
        return newHomePageResponse(request.name, home)
    }

     private fun Media.toSearchResponse(type: String? = null): SearchResponse? {
        return newMovieSearchResponse(
            title ?: name ?: originalTitle ?: return null,
            Data(id = id, type = mediaType ?: type).toJson(),
            TvType.Movie,
        ) {
            this.posterUrl = getImageUrl(posterPath)
            this.score= Score.from10(voteAverage)
        }
    }

    private fun getImageUrl(link: String?): String? {
        if (link == null) return null
        return if (link.startsWith("/")) "https://image.tmdb.org/t/p/w500/$link" else link
    }

    private fun getOriImageUrl(link: String?): String? {
        if (link == null) return null
        return if (link.startsWith("/")) "https://image.tmdb.org/t/p/original/$link" else link
    }

    fun getType(t: String?): TvType {
        return when (t) {
            "movie" -> TvType.Movie
            else -> TvType.TvSeries
        }
    }

    fun getStatus(t: String?): ShowStatus {
        return when (t) {
            "Returning Series" -> ShowStatus.Ongoing
            else -> ShowStatus.Completed
        }
    }


    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun search(query: String, page: Int): SearchResponseList? {
        return app.get("$apiUrl/search/multi?api_key=$apiKey&language=en-US&query=$query&page=$page")
            .parsedSafe<Results>()?.results?.mapNotNull { media ->
                media.toSearchResponse()
            }?.toNewSearchResponseList()
    }

    override suspend fun load(url: String): LoadResponse? {
        val data = parseJson<Data>(url)
        val type = getType(data.type)
        val append = "alternative_titles,credits,external_ids,videos,recommendations"

        val resUrl = if (type == TvType.Movie) {
            "$apiUrl/movie/${data.id}?api_key=$apiKey&append_to_response=$append"
        } else {
            "$apiUrl/tv/${data.id}?api_key=$apiKey&append_to_response=$append"
        }

        val res = app.get(resUrl).parsedSafe<MediaDetail>()
            ?: throw ErrorLoadingException("Invalid Json Response")
        var title = res.title ?: res.name ?: return null
        val poster = getOriImageUrl(res.posterPath)
        val bgPoster = getOriImageUrl(res.backdropPath)
        val orgTitle = res.originalTitle ?: res.originalName ?: return null
        val releaseDate = res.releaseDate ?: res.firstAirDate
        val year = releaseDate?.split("-")?.first()?.toIntOrNull()
        val genres = res.genres?.mapNotNull { it.name }

        val isCartoon = genres?.contains("Animation") ?: false
        val isAnime = isCartoon && (res.original_language == "zh" || res.original_language == "ja" || res.original_language == "ko")
        val isAsian = !isAnime && (res.original_language == "zh" || res.original_language == "ko")
        val isTurkish = res.original_language == "tr"
        val isBollywood = res.production_countries?.any { it.name == "India" } ?: false

        if(isTurkish) title = orgTitle ?: title

        val keywords = res.keywords?.results?.mapNotNull { it.name }.orEmpty()
            .ifEmpty { res.keywords?.keywords?.mapNotNull { it.name } }

        val actors = res.credits?.cast?.mapNotNull { cast ->
            val name = cast.name ?: cast.originalName ?: return@mapNotNull null
            ActorData(
                Actor(name, getImageUrl(cast.profilePath)), roleString = cast.character
            )
        } ?: emptyList()

        val recommendations =
            res.recommendations?.results?.mapNotNull { media -> media.toSearchResponse() }

        val trailer = res.videos?.results.orEmpty()
            .filter { it.type == "Trailer" }
            .map { "https://www.youtube.com/watch?v=${it.key}" }
            .reversed()
            .ifEmpty {
                res.videos?.results?.map { "https://www.youtube.com/watch?v=${it.key}" } ?: emptyList()
            }

         if (type == TvType.TvSeries) {
            val lastSeason = res.last_episode_to_air?.season_number
            val episodes = res.seasons?.mapNotNull { season ->
                app.get("$apiUrl/${data.type}/${data.id}/season/${season.seasonNumber}?api_key=$apiKey")
                    .parsedSafe<MediaDetailEpisodes>()?.episodes?.map { eps ->
                        newEpisode(
                            LinkData(
                                data.id,
                                res.external_ids?.imdb_id,
                                res.external_ids?.tvdb_id,
                                data.type,
                                eps.seasonNumber,
                                eps.episodeNumber,
                                eps.id,
                                title = title,
                                year = season.airDate?.split("-")?.first()?.toIntOrNull(),
                                orgTitle = orgTitle,
                                isAnime = isAnime,
                                airedYear = year,
                                lastSeason = lastSeason,
                                epsTitle = eps.name,
                                jpTitle = res.alternative_titles?.results?.find { it.iso_3166_1 == "JP" }?.title,
                                date = season.airDate,
                                airedDate = res.releaseDate ?: res.firstAirDate,
                                isAsian = isAsian,
                                isBollywood = isBollywood,
                                isCartoon = isCartoon,
                                alttitle = res.title,
                                nametitle = res.name
                            ).toJson()
                        ) {
                            this.name =
                                eps.name + if (isUpcoming(eps.airDate)) " • [UPCOMING]" else ""
                            this.season = eps.seasonNumber
                            this.episode = eps.episodeNumber
                            this.posterUrl = getImageUrl(eps.stillPath)
                            this.score = Score.from10(eps.voteAverage)
                            this.description = eps.overview
                        }.apply {
                            this.addDate(eps.airDate)
                        }
                    }
            }?.flatten() ?: listOf()

            return newAnimeLoadResponse(title, url, if(isAnime) TvType.Anime else TvType.TvSeries) {
                addEpisodes(DubStatus.Subbed, episodes)
                this.posterUrl = poster
                this.backgroundPosterUrl = bgPoster
                this.year = year
                this.plot = res.overview
                this.tags = keywords?.map { word -> word.replaceFirstChar { it.titlecase() } }
                    ?.takeIf { it.isNotEmpty() } ?: genres
                this.score = Score.from10(res.vote_average.toString())
                this.showStatus = getStatus(res.status)
                this.recommendations = recommendations
                this.actors = actors
                addTrailer(trailer)
                addTMDbId(data.id.toString())
                addImdbId(res.external_ids?.imdb_id)
            }
        } else {
            return newMovieLoadResponse(
                title,
                url,
                if(isAnime) TvType.AnimeMovie  else TvType.Movie,
                LinkData(
                    data.id,
                    res.external_ids?.imdb_id,
                    res.external_ids?.tvdb_id,
                    data.type,
                    title = title,
                    year = year,
                    orgTitle = orgTitle,
                    isAnime = isAnime,
                    jpTitle = res.alternative_titles?.results?.find { it.iso_3166_1 == "JP" }?.title,
                    airedDate = res.releaseDate ?: res.firstAirDate,
                    isAsian = isAsian,
                    isBollywood = isBollywood,
                    alttitle = res.title,
                    nametitle = res.name
                ).toJson(),
            ) {
                this.posterUrl = poster
                this.backgroundPosterUrl = bgPoster
                this.comingSoon = isUpcoming(releaseDate)
                this.year = year
                this.plot = res.overview
                this.duration = res.runtime
                this.tags = keywords?.map { word -> word.replaceFirstChar { it.titlecase() } }
                    ?.takeIf { it.isNotEmpty() } ?: genres

                this.score = Score.from10(res.vote_average.toString())
                this.recommendations = recommendations
                this.actors = actors
                addTrailer(trailer)
                addTMDbId(data.id.toString())
                addImdbId(res.external_ids?.imdb_id)
            }
        }
    }


     override suspend fun loadLinks(
            data: String,
            isCasting: Boolean,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ): Boolean {
        val res = parseJson<LinkData>(data)
        //year : first episode year
        //seasonYear : current episode year

        val year = res.airedYear ?: res.year
        val seasonYear = res.year ?: res.airedYear

        runAllAsync(
            { if (!res.isBollywood) invokeVegamovies("VegaMovies", res.imdbId, res.season, res.episode, subtitleCallback, callback) },
            { if (res.isBollywood) invokeVegamovies("RogMovies", res.imdbId, res.season, res.episode, subtitleCallback, callback) },
            { invokeNetflix(res.title, year, res.season, res.episode, subtitleCallback, callback) },
            { invokePrimeVideo(res.title, year, res.season, res.episode, subtitleCallback, callback) },
            { invokeDisney(res.title, year, res.season, res.episode, subtitleCallback, callback) },
            { if (res.season == null) invokeStreamify(res.imdbId, callback) },
            { invokeMultimovies(res.title, res.season, res.episode, subtitleCallback, callback) },
            { if (res.isBollywood) invokeTopMovies(res.title, year, res.season, res.episode, subtitleCallback, callback) },
            { if (!res.isBollywood) invokeMoviesmod(res.imdbId, res.season, res.episode, subtitleCallback, callback) },
            { if (res.isAsian) invokeDramadrip(res.imdbId, res.season, res.episode, subtitleCallback, callback) },
            { if (res.isAsian && res.season != null) invokeStreamAsia(res.title, "kdhd", res.season, res.episode, subtitleCallback, callback) },
            { invokeMoviesdrive(res.title, res.imdbId ,res.season, res.episode, subtitleCallback, callback) },
            { if(res.isAnime || res.isCartoon) invokeToonstream(res.title, res.season, res.episode, subtitleCallback, callback) },
            { if(!res.isAnime) invokeAsiaflix(res.title, res.season, res.episode, seasonYear, subtitleCallback, callback) },
            { invokeXDmovies(res.id, res.season, res.episode, subtitleCallback, callback) },
            { invokeDahmerMovies(res.title, year, res.season, res.episode, callback) },
            { if (!res.isAnime) invokeSkymovies(res.title, seasonYear, res.episode, subtitleCallback, callback) },
            { if (!res.isAnime) invokeHdmovie2(res.title, seasonYear, res.episode, subtitleCallback, callback) },
            { invokeBollyflix(res.imdbId, res.season, res.episode, subtitleCallback, callback) },
            { invokeMovies4u(res.imdbId, res.title, year, res.season, res.episode, subtitleCallback, callback) },
            { invokeTorrentio(res.imdbId, res.season, res.episode, callback) },
            { if (!res.isBollywood) invokeHindmoviez("HindMoviez", res.imdbId, res.title, res.season, res.episode, callback) },
            { if (!res.isBollywood && !res.isAnime) invokeKatMovieHd("KatMovieHd", res.imdbId, res.season, res.episode, subtitleCallback ,callback) },
            { if (res.isBollywood) invokeKatMovieHd("Moviesbaba", res.imdbId, res.season, res.episode, subtitleCallback ,callback) },
            { invokeW4U(res.title, year, res.imdbId, res.season, res.episode, subtitleCallback, callback) },
            { invokeWYZIESubs(res.imdbId, res.season, res.episode, subtitleCallback) },
            { invokeStremioSubtitles(res.imdbId, res.season, res.episode, subtitleCallback) },
            { if (res.isAnime) {
                val (aniId, malId) = convertTmdbToAnimeId(res.title, res.date, res.airedDate, if (res.season == null) TvType.AnimeMovie else TvType.Anime)
                invokeAnimes(malId, aniId, res.episode, res.airedYear, "imdb", subtitleCallback, callback)
            }},
            { invokePrimebox(res.title, year, res.season, res.episode, subtitleCallback, callback) },
            { invokePrimeSrc(res.imdbId, res.season, res.episode, subtitleCallback, callback) },
            { if (!res.isAnime) invoke2embed(res.imdbId, res.season, res.episode, callback) },
            { invokeSoaper(res.imdbId, res.id, res.title, res.season, res.episode, subtitleCallback, callback) },
            { if(!res.isAnime) invokeMadplay(res.id, res.season, res.episode, callback) },
            { invokePrimenet(res.id, res.season, res.episode, callback) },
            { invokePlayer4U(res.title, res.season, res.episode, year, callback) },
            { invokeThepiratebay(res.imdbId, res.season, res.episode, callback) },
            { invokeMp4Moviez(res.title, res.season, res.episode, year, callback, subtitleCallback) },
            { invokeFilm1k(res.title, res.season, res.year, subtitleCallback, callback) },
            { invokeCinemaOS(res.imdbId, res.id, res.title, res.season, res.episode, year, callback, subtitleCallback) },
            { invokeTripleOneMovies( res.id, res.season,res.episode, callback,subtitleCallback) },
            { invokeVidFastPro( res.id, res.season,res.episode, callback,subtitleCallback) },
            { invokeVidPlus( res.id, res.season,res.episode, callback,subtitleCallback) },
            { invokeMultiEmbeded( res.id, res.season, res.episode, callback,subtitleCallback) },
            { invokeVicSrcWtf( res.id, res.season,res.episode, callback, subtitleCallback) },
            { invokeProtonmovies(res.imdbId, res.season, res.episode, subtitleCallback, callback) },
            { invokeWebStreamr(res.imdbId, res.season, res.episode, subtitleCallback, callback) },
            { invokeNuvioStreams(res.imdbId, res.season, res.episode, subtitleCallback, callback) },
            { invokeAllmovieland(res.imdbId, res.season, res.episode, callback) },
            { if(res.season == null) invokeMostraguarda(res.imdbId, subtitleCallback, callback) },
            { if (!res.isBollywood && !res.isAnime) invokeMoviesflix("Moviesflix", res.imdbId, res.season, res.episode, subtitleCallback, callback) },
            { if (res.isBollywood) invokeMoviesflix("Hdmoviesflix", res.imdbId, res.season, res.episode, subtitleCallback, callback) },
            { if (!res.isBollywood) invokeUhdmovies(res.title, year, res.season, res.episode, callback, subtitleCallback) },
            { if (!res.isBollywood) invoke4khdhub(res.title, year, res.season, res.episode, subtitleCallback, callback) }
        )
        return true
    }

     data class LinkData(
        val id: Int? = null,
        val imdbId: String? = null,
        val tvdbId: Int? = null,
        val type: String? = null,
        val season: Int? = null,
        val episode: Int? = null,
        val epid: Int? = null,
        val aniId: String? = null,
        val animeId: String? = null,
        val title: String? = null,
        val year: Int? = null,
        val orgTitle: String? = null,
        val isAnime: Boolean = false,
        val airedYear: Int? = null,
        val lastSeason: Int? = null,
        val epsTitle: String? = null,
        val jpTitle: String? = null,
        val date: String? = null,
        val airedDate: String? = null,
        val isAsian: Boolean = false,
        val isBollywood: Boolean = false,
        val isCartoon: Boolean = false,
        val alttitle: String? = null,
        val nametitle: String? = null,
    )

    data class Data(
        val id: Int? = null,
        val type: String? = null,
        val aniId: String? = null,
        val malId: Int? = null,
    )

    data class Results(
        @JsonProperty("results") val results: ArrayList<Media>? = arrayListOf(),
    )

    data class Media(
        @JsonProperty("id") val id: Int? = null,
        @JsonProperty("name") val name: String? = null,
        @JsonProperty("title") val title: String? = null,
        @JsonProperty("original_title") val originalTitle: String? = null,
        @JsonProperty("media_type") val mediaType: String? = null,
        @JsonProperty("poster_path") val posterPath: String? = null,
        @JsonProperty("vote_average") val voteAverage: Double? = null,
    )

    data class Genres(
        @JsonProperty("id") val id: Int? = null,
        @JsonProperty("name") val name: String? = null,
    )

    data class Keywords(
        @JsonProperty("id") val id: Int? = null,
        @JsonProperty("name") val name: String? = null,
    )

    data class KeywordResults(
        @JsonProperty("results") val results: ArrayList<Keywords>? = arrayListOf(),
        @JsonProperty("keywords") val keywords: ArrayList<Keywords>? = arrayListOf(),
    )

    data class LastEpisodeToAir(
        @JsonProperty("episode_number") val episode_number: Int? = null,
        @JsonProperty("season_number") val season_number: Int? = null,
    )

    data class Seasons(
        @JsonProperty("id") val id: Int? = null,
        @JsonProperty("name") val name: String? = null,
        @JsonProperty("season_number") val seasonNumber: Int? = null,
        @JsonProperty("air_date") val airDate: String? = null,
    )

    data class Trailers(
        @JsonProperty("key") val key: String? = null,
        @JsonProperty("type") val type: String? = null,
    )

    data class ResultsTrailer(
        @JsonProperty("results") val results: ArrayList<Trailers>? = arrayListOf(),
    )

    data class ExternalIds(
        @JsonProperty("imdb_id") val imdb_id: String? = null,
        @JsonProperty("tvdb_id") val tvdb_id: Int? = null,
    )

    data class Credits(
        @JsonProperty("cast") val cast: ArrayList<Cast>? = arrayListOf(),
    )

    data class Cast(
        @JsonProperty("id") val id: Int? = null,
        @JsonProperty("name") val name: String? = null,
        @JsonProperty("original_name") val originalName: String? = null,
        @JsonProperty("character") val character: String? = null,
        @JsonProperty("known_for_department") val knownForDepartment: String? = null,
        @JsonProperty("profile_path") val profilePath: String? = null,
    )

    data class ResultsRecommendations(
        @JsonProperty("results") val results: ArrayList<Media>? = arrayListOf(),
    )

    data class AltTitles(
        @JsonProperty("iso_3166_1") val iso_3166_1: String? = null,
        @JsonProperty("title") val title: String? = null,
        @JsonProperty("type") val type: String? = null,
    )

    data class ResultsAltTitles(
        @JsonProperty("results") val results: ArrayList<AltTitles>? = arrayListOf(),
    )

    data class ProductionCountries(
        @JsonProperty("name") val name: String? = null,
    )


    data class MediaDetail(
        @JsonProperty("id") val id: Int? = null,
        @JsonProperty("imdb_id") val imdbId: String? = null,
        @JsonProperty("title") val title: String? = null,
        @JsonProperty("name") val name: String? = null,
        @JsonProperty("original_title") val originalTitle: String? = null,
        @JsonProperty("original_name") val originalName: String? = null,
        @JsonProperty("poster_path") val posterPath: String? = null,
        @JsonProperty("backdrop_path") val backdropPath: String? = null,
        @JsonProperty("release_date") val releaseDate: String? = null,
        @JsonProperty("first_air_date") val firstAirDate: String? = null,
        @JsonProperty("overview") val overview: String? = null,
        @JsonProperty("runtime") val runtime: Int? = null,
        @JsonProperty("vote_average") val vote_average: Any? = null,
        @JsonProperty("original_language") val original_language: String? = null,
        @JsonProperty("status") val status: String? = null,
        @JsonProperty("genres") val genres: ArrayList<Genres>? = arrayListOf(),
        @JsonProperty("keywords") val keywords: KeywordResults? = null,
        @JsonProperty("last_episode_to_air") val last_episode_to_air: LastEpisodeToAir? = null,
        @JsonProperty("seasons") val seasons: ArrayList<Seasons>? = arrayListOf(),
        @JsonProperty("videos") val videos: ResultsTrailer? = null,
        @JsonProperty("external_ids") val external_ids: ExternalIds? = null,
        @JsonProperty("credits") val credits: Credits? = null,
        @JsonProperty("recommendations") val recommendations: ResultsRecommendations? = null,
        @JsonProperty("alternative_titles") val alternative_titles: ResultsAltTitles? = null,
        @JsonProperty("production_countries") val production_countries: ArrayList<ProductionCountries>? = arrayListOf(),
    )

    data class Episodes(
        @JsonProperty("id") val id: Int? = null,
        @JsonProperty("name") val name: String? = null,
        @JsonProperty("overview") val overview: String? = null,
        @JsonProperty("air_date") val airDate: String? = null,
        @JsonProperty("still_path") val stillPath: String? = null,
        @JsonProperty("vote_average") val voteAverage: Double? = null,
        @JsonProperty("episode_number") val episodeNumber: Int? = null,
        @JsonProperty("season_number") val seasonNumber: Int? = null,
    )

     data class MediaDetailEpisodes(
        @JsonProperty("episodes") val episodes: ArrayList<Episodes>? = arrayListOf(),
    )
}
