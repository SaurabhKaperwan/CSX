package com.hexated

import com.lagradost.cloudstream3.HomePageList
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.fixUrlNull
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.network.CloudflareKiller
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newMovieLoadResponse
import com.lagradost.cloudstream3.newMovieSearchResponse
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import kotlinx.coroutines.delay
import org.jsoup.nodes.Element

class Perverzija : MainAPI() {
    override var name = "Perverzija"
    override var mainUrl = "https://tube.perverzija.com/"
    override val supportedTypes = setOf(TvType.NSFW)

    override val hasDownloadSupport = true
    override val hasMainPage = true

    private val cfInterceptor = CloudflareKiller()

    override val mainPage = mainPageOf(
        "$mainUrl/page/%d/" to "Home",
        "$mainUrl/studio/page/%d/?orderby=view" to "Most Viewed",
        "$mainUrl/studio/page/%d/?orderby=like" to "Most Liked",
        "$mainUrl/featured-scenes/page/%d/?orderby=date" to "Featured",
        "$mainUrl/featured-scenes/page/%d/?orderby=view" to "Featured Most Viewed",
        "$mainUrl/featured-scenes/page/%d/?orderby=like" to "Featured Most Liked",
        "$mainUrl/full-movie/page/%d/?orderby=date" to "Latest Movies",
        "$mainUrl/full-movie/page/%d/?orderby=view" to "Most Viewed Movies",
        "$mainUrl/full-movie/page/%d/?orderby=like" to "Most Liked Movies",
        "$mainUrl/tag/big-ass/page/%d/" to "Big Ass",
        "$mainUrl/tag/squirt/page/%d/" to "Squirting",
        "$mainUrl/tag/yoga/page/%d/" to "Yoga",
        "$mainUrl/tag/wife/page/%d/" to "Wife",
        "$mainUrl/tag/massage/page/%d/" to "Massage",
        "$mainUrl/tag/double-penetration/page/%d/" to "DP",
        "$mainUrl/studio/teamskeet/page/%d/" to "Team Skeet",
        "$mainUrl/studio/private/page/%d/" to "Private",
        "$mainUrl/studio/brazzers/page/%d/" to "Brazzers",
        "$mainUrl/studio/bangbros/page/%d/" to "Bang Bros",
        "$mainUrl/studio/realitykings/page/%d/" to "Reality Kings",
        "$mainUrl/studio/naughtyamerica/page/%d/" to "NaughtyAmerica",
        "$mainUrl/studio/nubiles/page/%d/" to "Nubiles",
        "$mainUrl/studio/adulttime/page/%d/" to "Adult Time",
        "$mainUrl/studio/vxn/tushy/page/%d/" to "Tushy",
        "$mainUrl/studio/dorcelclub/page/%d/" to "DorcelClub",
        "$mainUrl/studio/pornworld/ddfnetwork/page/%d/" to "DDFNetwork",
        "$mainUrl/studio/mylf/page/%d/" to "Mylf",
        "$mainUrl/studio/pornpros/page/%d/" to "PornPros",
        "$mainUrl/studio/evilangel/page/%d/" to "EvilAngel",
        "$mainUrl/studio/digitalplayground/page/%d/" to "Digital Playground",
        "$mainUrl/studio/kbproductions/page/%d/" to "KBProductions",
        "$mainUrl/studio/fakehub/page/%d/" to "FakeHub",
        "$mainUrl/studio/sexyhub/page/%d/" to "SexyHub",
        "$mainUrl/studio/milehighmedia/page/%d/" to "MileHighMedia",
        "$mainUrl/studio/mofos/page/%d/" to "Mofos",
        "$mainUrl/studio/adulttime/21sextury/page/%d/" to "21Sextury",
        "$mainUrl/studio/fullpornnetwork/page/%d/" to "FullPornNetwork",
        "$mainUrl/studio/vxn/blacked/page/%d/" to "Blacked",
        "$mainUrl/studio/bang/page/%d/" to "Bang",
        "$mainUrl/studio/newsensations/page/%d/" to "NewSensations",
        "$mainUrl/studio/letsdoeit/page/%d/" to "LetsDoeIt",
        "$mainUrl/studio/julesjordan/page/%d/" to "JulesJordan",
        "$mainUrl/studio/xempire/page/%d/" to "XEmpire",
        "$mainUrl/studio/sexmex/page/%d/" to "SexMex",
        "$mainUrl/studio/pornworld/page/%d/" to "PornWorld",
        "$mainUrl/studio/hustler/page/%d/" to "Hustler",
        "$mainUrl/studio/penthousegold/page/%d/" to "PenthouseGold",
        "$mainUrl/studio/babesnetwork/page/%d/" to "BabesNetwork",
        "$mainUrl/studio/pornfidelity/page/%d/" to "PornFidelity",
        "$mainUrl/studio/mypervyfamily/page/%d/" to "MyPervyFamily",
        "$mainUrl/studio/adulttime/puretaboo/page/%d/" to "PureTaboo",
        "$mainUrl/studio/teamskeet/familystrokes/page/%d/" to "FamilyStrokes",
        "$mainUrl/studio/teamskeet/daughterswap/page/%d/" to "DaughterSwap",
        "$mainUrl/studio/adulttime/21sextury/dpfanatics/page/%d/" to "DPFanatics",
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val document = app.get(request.data.format(page), interceptor = cfInterceptor).document
        val home = document.select("div.row div div.post").mapNotNull {
            it.toSearchResult()
        }

        return newHomePageResponse(
            list = HomePageList(
                name = request.name, list = home, isHorizontalImages = true
            ), hasNext = true
        )
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val posterUrl = fixUrlNull(this.select("dt a img").attr("src"))
        val title = this.select("dd a").text() ?: return null
        val href = fixUrlNull(this.select("dt a").attr("href")) ?: return null

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }

    }

    private fun Element.toSearchResult(): SearchResponse? {
        val posterUrl = fixUrlNull(this.select("div.item-thumbnail img").attr("src"))
        val title = this.select("div.item-head a").text() ?: return null
        val href = fixUrlNull(this.select("div.item-head a").attr("href")) ?: return null

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }

    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()
        val maxPages = if (query.contains(" ")) 6 else 20
        for (i in 1..maxPages) {
            val url = if (query.contains(" ")) {
            "$mainUrl/page/$i/?s=${query.replace(" ", "+")}&orderby=date"
            } else {
                "$mainUrl/tag/$query/page/$i/"
            }

            val results = app.get(url, interceptor = cfInterceptor).document
                .select("div.row div div.post").mapNotNull {
                    it.toSearchResult()
                }.distinctBy { it.url }
            if (results.isEmpty()) break
            else delay((100L..500L).random())
            searchResponse.addAll(results)
        }
        return searchResponse.distinctBy { it.url }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url, interceptor = cfInterceptor).document

        val poster = document.select("div#featured-img-id img").attr("src")
        val title = document.select("div.title-info h1.light-title.entry-title").text()
        val description =
            document.select("div.item-content div.bigta-container div.bialty-container p").text()

        val tags = document.select("div.item-tax-list div a").map { it.text() }

        val recommendations =
            document.select("div.related-gallery dl.gallery-item").mapNotNull {
                it.toRecommendationResult()
            }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = description
            this.tags = tags
            this.recommendations = recommendations
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val response = app.get(data)
        val document = response.document

        val iframeUrl = document.select("div#player-embed iframe").attr("src")

        return loadExtractor(iframeUrl, subtitleCallback, callback)
    }
}
