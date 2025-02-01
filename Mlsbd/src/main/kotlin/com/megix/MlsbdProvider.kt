package com.megix

import com.lagradost.cloudstream3.Episode
import com.lagradost.cloudstream3.HomePageList
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newMovieLoadResponse
import com.lagradost.cloudstream3.newMovieSearchResponse
import com.lagradost.cloudstream3.newTvSeriesLoadResponse
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Element

//suspend fun main() {
//    val providerTester = com.lagradost.cloudstreamtest.ProviderTester(MlsbdProvider())
////    providerTester.testLoadLinks("https://checklinko.top/60382/")
////    providerTester.testAll()
//    providerTester.testMainPage(verbose = true)
////    providerTester.testSearch(query = "gun",verbose = true)
////    providerTester.testLoad("https://fullymaza.pw/2024/06/die-in-a-gunfight-2021-hdrip-hindi-dual-audio-480p-720p-1080p/")
//}

class MlsbdProvider : MainAPI() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://mlsbd
.shop"
    override var name = "Mlsbd"
    override var lang = "bn"
    override val hasMainPage = true
    override val hasDownloadSupport = true
    override val hasQuickSearch = false
    override val mainPage = mainPageOf(
        "" to "Latest Movies",
        "/category/bangla-dubbed/page/" to "Bangla Dubbed",
        "/category/dual-audio-movies/page/" to "Multi Audio Movies",
        "/category/tv-series/page/" to "TV Series",
        "/category/foreign-language-film/page/" to "Foreign Language Film",
        "/category/bollywood-movies/page/" to "Bollywood Movies",
        "/category/bangla-movies/page/" to "Bengali Movies",
        "/category/hollywood-movies/page/" to "Hollywood Movies",
        "/category/natok-teleflim/page/" to "Natok & Teleflim",
        "/category/unrated/page/" to "UnRated"
    )
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
        TvType.NSFW,
        TvType.AsianDrama,
        TvType.AnimeMovie,
    )
    private val headers =
        mapOf("user-agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36")

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse = withContext(Dispatchers.IO) {
        val url = if (request.data == "") mainUrl else "$mainUrl${request.data}$page/"
        val doc = app.get(url, cacheTime = 1440, allowRedirects = true, timeout = 60, headers = headers).document
        val homeResponse = doc.select("div.single-post")
        val home = homeResponse.mapNotNull { post ->
            toResult(post)
        }
        newHomePageResponse(HomePageList(request.name, home, isHorizontalImages = true), true)
    }

    private fun toResult(post: Element): SearchResponse {
        val title = post.selectFirst(".post-title")?.text() ?: ""
        val url = post.selectFirst(".thumb > a")?.attr("href") ?: ""
        val posterUrl = post.selectFirst(".thumb>a>picture>img:nth-child(3)")?.attr("src") ?: ""

        return newMovieSearchResponse(title, url, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> = withContext(Dispatchers.IO){
        val doc = app.get("$mainUrl/?s=$query", cacheTime = 60, timeout = 30, headers = headers).document
        val searchResponse = doc.select("div.single-post")
        searchResponse.mapNotNull { post ->
            toResult(post)
        }
    }

    override suspend fun load(url: String): LoadResponse = withContext(Dispatchers.IO) {
        val doc = app.get(url, cacheTime = 60, timeout = 30).document
        val title = doc.selectFirst(".name")?.text() ?: ""
        val year = "(?<=\\()\\d{4}(?=\\))".toRegex().find(title)?.value?.toIntOrNull()
        val image = doc.selectFirst("img.aligncenter")?.attr("src") ?: ""
        doc.select("br").append("\\n")
        val plot = buildString {
            append(doc.selectFirst(".single-post-title")?.text() ?: "")
            append("\n")
            append(doc.selectFirst(".misc")?.text() ?: "")
            append("\n")
            append(doc.selectFirst(".details")?.text()?.replace("\\n ", "\n") ?: "")
            append("\n")
            append(doc.selectFirst(".storyline")?.text() ?: "")
            append("\n")
            append(doc.selectFirst(".production")?.text()?.replace("\\n ", "\n") ?: "")
            append("\n")
            append(doc.selectFirst(".media")?.text()?.replace("\\n ", "\n") ?: "")
        }

        val episodeDivs = doc.select("div.post-section-title.download").reversed()
        var link = ""
        when (episodeDivs.size) {
            1 -> {
                val links = mutableListOf<String>()
                episodeDivs[0].nextElementSibling()?.nextElementSibling()
                    ?.select("a.Dbtn.hd, a.Dbtn.sd, a.Dbtn.hevc")
                    ?.forEach {
                        links.add(it.attr("href"))
                    }
                    link = links.joinToString(" ; ")

                newMovieLoadResponse(title, url, TvType.Movie, link) {
                    this.posterUrl = image
                    this.year = year
                    this.plot = plot
                }
            }

            0 -> newMovieLoadResponse(title, url, TvType.Movie, "") {
                this.posterUrl = image
                this.year = year
                this.plot = plot
            }

            else -> {
                val episodesData = mutableListOf<Episode>()
                episodeDivs.forEachIndexed { index, episodeDiv ->
                    var episodeUrl = ""
                    var downloadLink = episodeDiv.nextElementSibling()?.nextElementSibling()
                    val linksForEpisode = mutableListOf<String>()

                    //480p
                    downloadLink?.selectFirst("a")?.attr("href")?.let { linksForEpisode.add(it) }

                    //720p
                    downloadLink = downloadLink?.nextElementSibling()
                    downloadLink?.selectFirst("a")?.attr("href")?.let { linksForEpisode.add(it) }

                    //1080p
                    downloadLink = downloadLink?.nextElementSibling()
                    downloadLink?.selectFirst("a")?.attr("href")?.let { linksForEpisode.add(it) }

                    episodeUrl = linksForEpisode.joinToString(" ; ")

                    episodesData.add(
                        Episode(
                            episodeUrl,
                            "Episode ${index + 1}",
                            1,
                            index + 1
                        )
                    )
                }
                newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodesData) {
                    this.posterUrl = image

                    this.year = year
                    this.plot = plot
                }
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean = withContext(Dispatchers.IO){
        data.split(" ; ").forEach { link ->
            if (link.contains("savelinks")) {
                val doc = app.get(link, cacheTime = 60, timeout = 30).document
                doc.select("div.col-sm-8:nth-child(4) > a").forEach {
                    val url = it.attr("href")
                    if (url.contains("gdflix")) {
                        loadExtractor(url, subtitleCallback, callback)
                    }
                }
            }
        }
        true
    }
}
