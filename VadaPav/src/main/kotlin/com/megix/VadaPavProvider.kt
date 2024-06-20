package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element


class VadaPavProvider : MainAPI() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://vadapav.mov"
    override var name = "VadaPav"
    override val hasMainPage = true
    override var lang = "en"
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
        TvType.Anime,
    )

    override val mainPage = mainPageOf(
        "716da8ac-ed44-4fd4-aedc-eacefd00eeec" to "Recent",
        "f36be06f-8edd-4173-99df-77bc4c7c2626" to "Movies",
        "28dc7aeb-902b-4824-8be2-fa1e4f20383c" to "TV",
        "acb8953f-8a6a-480e-938f-2796213aa261" to "Bollywood Movies",
        "53e89fa0-5c79-47d9-a50c-ed7fd4b623c8" to "Bollywood TV",
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val document = app.get("$mainUrl/${request.data}").document
        val home = document.select("div.directory > ul > li > div > a").filter { element -> !element.text().contains("Parent Directory", true) }.mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this ?. text() ?: ""
        val link = this ?. attr("href") ?: ""
        val href = "$mainUrl$link"

        return newMovieSearchResponse(title, href, TvType.Movie) {
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()
        val document = app.get("$mainUrl/s/$query").document

        val results = document.select("div.directory > ul > li > div > a").filter { element -> !element.text().contains("Parent Directory", true) }.mapNotNull { it.toSearchResult() }

        searchResponse.addAll(results)

        return searchResponse
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val span = document.select("div > span")
        val lastSpan = span.takeIf { it.isNotEmpty() }?.lastOrNull()
        val title = lastSpan ?. text()?: ""
        var seasonNum = 1
        val tvSeriesEpisodes = mutableListOf<Episode>()
        val aTags = document.select("div.directory > ul > li > div > a.directory-entry").filter { element -> !element.text().contains("Parent Directory", true) }
        if(aTags.isNotEmpty()) {
            aTags.mapNotNull { element ->
                val doc = app.get(mainUrl + element?.attr("href")).document
                val tags = doc.select("div.directory > ul > li > div > a.file-entry")
                val episodes = tags.mapNotNull { tag ->
                    Episode(
                        name = tag?.text()?: "",
                        data = mainUrl + tag?.attr("href"),
                        season = seasonNum,
                        episode = tags.indexOf(tag) + 1,
                    )
                }
                tvSeriesEpisodes.addAll(episodes)
                seasonNum++
            }
        }
        else {
            val tags = document.select("div.directory > ul > li > div > a.file-entry")
            val episodes = tags.mapNotNull { tag ->
                Episode(
                    name = tag?.text()?: "",
                    data = mainUrl + tag?.attr("href"),
                    season = seasonNum,
                    episode = tags.indexOf(tag) + 1,
                )
            }
            tvSeriesEpisodes.addAll(episodes)
            seasonNum++
        }
        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, tvSeriesEpisodes) {
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        callback.invoke(
            ExtractorLink(
                name,
                name,
                data,
                referer = "",
                quality = Qualities.Unknown.value,
            )
        )
        return true
    }

}
