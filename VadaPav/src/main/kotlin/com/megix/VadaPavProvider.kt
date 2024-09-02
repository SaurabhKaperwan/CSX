package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.jsoup.Jsoup

class VadaPavProvider : MainAPI() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://vadapav.mov"
    override var name = "VadaPav"
    override val hasMainPage = true
    override var lang = "en"
    override val hasDownloadSupport = true
    private val mirrors = listOf(
        "https://vadapav.mov",
        "https://dl1.vadapav.mov",
        "https://dl2.vadapav.mov",
        "https://dl3.vadapav.mov",
    )
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
        TvType.Anime,
    )

    override val mainPage = mainPageOf(
        "f36be06f-8edd-4173-99df-77bc4c7c2626" to "Movies",
        "28dc7aeb-902b-4824-8be2-fa1e4f20383c" to "TV",
        "acb8953f-8a6a-480e-938f-2796213aa261" to "Bollywood Movies",
        "53e89fa0-5c79-47d9-a50c-ed7fd4b623c8" to "Bollywood TV",
        "accac8e8-f794-47fd-b40b-8486bc8ab531" to "Hollywood Movies Hindi Dubbed",
        "72be5227-4a91-4939-96b3-dc77a9563f55" to "Hollywood Series Hindi Dubbed",
        "716da8ac-ed44-4fd4-aedc-eacefd00eeec" to "Recent",
        "60ac9f3f-9a3b-417a-abe7-dac7d20e38f4" to "Airing Anime",
        "54bc9bc8-3496-4091-8174-544de130ce21" to "South Hindi Dubbed"
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
        val title = this.text() ?: ""
        val link = fixUrl(this.attr("href"))

        return newMovieSearchResponse(title, link, TvType.Movie) {
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()
        val document = app.get("$mainUrl/s/$query").document

        val results = document.select("div.directory > ul > li > div > a").filter { element -> !element.text().contains("Parent Directory", true) }.mapNotNull { it.toSearchResult() }

        searchResponse.addAll(results)

        return searchResponse
    }

    data class MutableInt(var value: Int)

    private suspend fun traverse(dTags: List<Element> ,tvSeriesEpisodes: MutableList<Episode>, seasonList: MutableList<Pair<String, Int>>, mutableSeasonNum: MutableInt) {
        for(dTag in dTags) {
            val document = app.get(fixUrl(dTag.attr("href"))).document
            val innerDTags = document.select("div.directory > ul > li > div > a.directory-entry").filter { element -> !element.text().contains("Parent Directory", true) }
            val innerFTags = document.select("div.directory > ul > li > div > a.file-entry")

            if(innerFTags.isNotEmpty()) {
                val span = document.select("div > span")
                val lastSpan = span.takeIf { it.isNotEmpty() } ?. lastOrNull()
                val title = lastSpan ?. text() ?: ""
                seasonList.add("$title" to mutableSeasonNum.value)
                val episodes = innerFTags.amap { tag ->
                    newEpisode(tag.attr("href")){
                        name = tag.text()
                        season = mutableSeasonNum.value
                        episode = innerFTags.indexOf(tag) + 1
                    }
                }
                tvSeriesEpisodes.addAll(episodes)
                mutableSeasonNum.value++
            }

            if(innerDTags.isNotEmpty()) {
                traverse(innerDTags, tvSeriesEpisodes, seasonList, mutableSeasonNum)
            }
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val span = document.select("div > span")
        val lastSpan = span.takeIf { it.isNotEmpty() } ?. lastOrNull()
        val title = lastSpan ?. text() ?: ""
        var seasonNum = 1
        val tvSeriesEpisodes = mutableListOf<Episode>()
        val dTags = document.select("div.directory > ul > li > div > a.directory-entry").filter { element -> !element.text().contains("Parent Directory", true) }
        val fTags = document.select("div.directory > ul > li > div > a.file-entry")
        val seasonList = mutableListOf<Pair<String, Int>>()
        val mutableSeasonNum = MutableInt(seasonNum)

        if(fTags.isNotEmpty()) {
            val innerSpan = document.select("div > span")
            val lastInnerSpan = innerSpan.takeIf { it.isNotEmpty() } ?. lastOrNull()
            val titleText = lastInnerSpan ?. text() ?: ""
            seasonList.add("$titleText" to mutableSeasonNum.value)
            val episodes = fTags.amap { tag ->
                newEpisode(tag.attr("href")){
                    name = tag.text()
                    season = mutableSeasonNum.value
                    episode = fTags.indexOf(tag) + 1
                }
            }
            tvSeriesEpisodes.addAll(episodes)
            mutableSeasonNum.value++
        }

        if(dTags.isNotEmpty()) {
            traverse(dTags, tvSeriesEpisodes, seasonList, mutableSeasonNum)
        }
       
        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, tvSeriesEpisodes) {
            this.seasonNames = seasonList.map {(name, int) -> SeasonData(int, name)}
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        var link = data.replace("$mainUrl", "")
        for((index, mirror) in mirrors.withIndex()) {
            callback.invoke(
                ExtractorLink(
                    name+ " ${index+1}",
                    name+" ${index+1}",
                    mirror + link,
                    referer = "",
                    quality = Qualities.Unknown.value,
                )
            )
        }
        return true
    }

}
