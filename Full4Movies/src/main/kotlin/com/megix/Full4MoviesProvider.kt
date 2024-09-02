package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.network.CloudflareKiller

class Full4MoviesProvider : MainAPI() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://www.full4movies.love"
    override var name = "Full4Movies"
    override val hasMainPage = true
    override var lang = "hi"
    override val hasDownloadSupport = true
    private val cfInterceptor = CloudflareKiller()
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries
    )

    override val mainPage = mainPageOf(
        "$mainUrl/page/%d/" to "Home",
        "$mainUrl/category/web-series/page/%d/" to "Web Series",
        "$mainUrl/category/south-indian-hindi-dubbed-movies/page/%d/" to "South Hindi Dubbed",
        "$mainUrl/category/bollywood-movies-download/page/%d/" to "Bollywood Movies",
        "$mainUrl/category/hollywood-movies/page/%d/" to "Hollywood Movies",
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val document = app.get(request.data.format(page), interceptor = cfInterceptor).document
        val home = document.select("div.article-content-col").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("img")?.attr("title") ?: return null
        val href = fixUrl(this.selectFirst("a")?.attr("href").toString())
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()

        for (i in 1..4) {
            val document = app.get("$mainUrl/page/$i/?s=$query").document

            val results = document.select("div.article-content-col").mapNotNull { it.toSearchResult() }

            searchResponse.addAll(results)

            if (results.isEmpty()) break
        }

        return searchResponse
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val infoDiv = document.selectFirst("div.wp-block-image")

        val imdbRatingText = infoDiv?.select("td:contains(IMDb) + td")?.text()
        val imdbRating = imdbRatingText?.substringBefore("/")?.toRatingInt()

        val plot = infoDiv?.select("td:contains(Plot) + td")?.text()?.toString()

        val genresText = infoDiv?.select("td:contains(Genres) + td")?.text()
        val genresList = genresText?.split(",")?.map { it.trim() } ?: emptyList()

        val castText = infoDiv?.select("td:contains(Cast) + td")?.text()
        val castList = castText?.split(",")?.map { it.trim() } ?: emptyList()

        val actors = castList.map {
            ActorData(
                Actor(it),
            )
        }
        
        val title = document.selectFirst("h1.title")?.text() ?: return null
        val posterUrl = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))

        val regex = Regex("""<a[^>]*href="([^"]*)"[^>]*>(?:WCH|Watch)<\/a>""")

        val tvType = if (document.selectFirst("meta[property=article:section]")?.attr("content") == "Web series" || regex.containsMatchIn(document.html())) {
            TvType.TvSeries
        } else {
            TvType.Movie
        }

        return if (tvType == TvType.TvSeries) {

            val urls = regex.findAll(document.html()).map { it.groupValues[1] }.toList()

            val episodes = urls.mapNotNull { link ->
                newEpisode(url){"Episode ${urls.indexOf(link) + 1}"}
            }
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = posterUrl
                this.plot = plot
                this.rating = imdbRating
                this.actors = actors
                this.tags = genresList
            }
        }
        else {
            val movieUrl = Regex("""<a\s+class="myButton"\s+href="([^"]+)".*?>Watch Online 1<\/a>""").find(document.html())?.groupValues?.get(1) ?: ""
            return newMovieLoadResponse(title, url, TvType.Movie, movieUrl) {
                this.posterUrl = posterUrl
                this.plot = plot
                this.rating = imdbRating
                this.actors = actors
                this.tags = genresList
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = app.get(data).document
        val link = doc.selectFirst("iframe")?.attr("src").toString()
        loadExtractor(link, referer = data, subtitleCallback, callback)
        return true
    }

}
