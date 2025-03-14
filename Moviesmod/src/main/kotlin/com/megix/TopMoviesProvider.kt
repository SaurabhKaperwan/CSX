package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import com.lagradost.cloudstream3.LoadResponse.Companion.addImdbUrl

class TopmoviesProvider : MoviesmodProvider() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://topmovies.wales"
    override var name = "TopMovies"
    override val hasMainPage = true
    override var lang = "hi"
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries
    )

    override val mainPage = mainPageOf(
        "$mainUrl/page/" to "Home",
        "$mainUrl/web-series/page/" to "Latest Web Series",
        "$mainUrl/movies/hindi-movies/page/" to "Latest Hindi Movies",
    )
}
