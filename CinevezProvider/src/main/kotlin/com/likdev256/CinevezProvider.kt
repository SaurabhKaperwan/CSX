package com.likdev256

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.extractors.StreamTape
import com.lagradost.cloudstream3.extractors.MixDrop
import com.lagradost.cloudstream3.extractors.StreamWishExtractor
import com.lagradost.cloudstream3.extractors.VidhideExtractor
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Element

class CinevezProvider : MainAPI() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://www.cinevez.io"
    override var name = "Cinevez"
    override val hasMainPage = true
    override var lang = "hi"
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries
    )

    override val mainPage = mainPageOf(
        "$mainUrl/featured/page/" to "Popular Movies",
        "$mainUrl/language/english/page/" to "English",
        "$mainUrl/language/hindi/page/" to "Hindi",
        "$mainUrl/language/tamil/page/" to "Tamil",
        "$mainUrl/language/telugu/page/" to "Telugu",
        "$mainUrl/language/malayalam/page/" to "Malayalam",
        "$mainUrl/language/kannada/page/" to "Kannada"
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {

        val document = app.get(request.data + page).document
        val home = document.select(".post-item").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h2")?.text()?.trim() ?: return null
        val href = fixUrl(this.selectFirst("a")?.attr("href").toString())
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))
        val quality = getQualityFromString(this.select("span").text().substringBefore("-"))

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
            this.quality = quality
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/?s=$query").document

        return document.select(".post-item").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst(".box-title h1")?.text()?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst(".post-image img")?.attr("src"))
        val tags = document.select("div:nth-child(3) > p.text-sm a").map { it.text() }
        val year = document.select("div:nth-child(2) > p.text-sm a").text().toIntOrNull()
        val description = document.selectFirst("div.text-left > div:nth-child(8)")?.text()
            ?.replace("Story Plot:", "")?.trim()
        val actors =
            document.select("div:nth-child(5) > p.text-sm a").map { it.text() }
        val recommendations = document.select(".box-content .post-item").mapNotNull {
            it.toSearchResult()
        }
        val sourceUrl =
            if (document.select("div:nth-child(3) > div.box-title.border-dark").text().trim()
                    .contains("Seasons")
            ) document.select("div:nth-child(3) > div > div > div > a.rounded")
                .attr("href") else url
        return newMovieLoadResponse(title, url, TvType.Movie, sourceUrl) {
            this.posterUrl = poster
            this.year = year
            this.plot = description
            this.tags = tags
            this.recommendations = recommendations
            addActors(actors)
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {

        app.get(data).document.select(".list-episodes a.bg-button")
            .mapNotNull {
                var href = it.attr("href")
                if(!href.contains("gofile")) {
                    href = href.replace("/([a-z])/".toRegex(),"/e/")
                }
                loadExtractor(
                    href,
                    "$mainUrl/",
                    subtitleCallback,
                    callback
                )
            }
        return true
    }
}

class ShaveTape : StreamTape() {
    override var mainUrl = "https://shavetape.cash"
}

class JodWish : StreamWishExtractor() {
    override var mainUrl = "https://jodwish.com"
}

class Swdyu : StreamWishExtractor() {
    override var mainUrl = "https://swdyu.com"
}


class MDrop : MixDrop() {
    override var mainUrl = "https://mixdrop.nu"
}

class VidHidePre : VidhideExtractor() {
    override var mainUrl = "https://vidhidepre.com"
}


class VidHidePro : VidhideExtractor() {
    override var mainUrl = "https://vidhidepro.com"
}
