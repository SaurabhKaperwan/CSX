package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class RogmoviesProvider : VegaMoviesProvider() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://rogmovies.lol"
    override var name = "Rogmovies"
    override val hasMainPage = true
    override var lang = "hi"
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries
    )

    val headers = mapOf(
        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
        "Cache-Control" to "no-store",
        "Accept-Language" to "en-US,en;q=0.9",
        "DNT" to "1",
        "sec-ch-ua" to "\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"120\", \"Microsoft Edge\";v=\"120\"",
        "sec-ch-ua-mobile" to "?0",
        "sec-ch-ua-platform" to "\"Windows\"",
        "Sec-Fetch-Dest" to "document",
        "Sec-Fetch-Mode" to "navigate",
        "Sec-Fetch-Site" to "none",
        "Sec-Fetch-User" to "?1",
        "Cookie" to "_lscache_vary=62abf8b96599676eb8ec211cffaeb8ff; ext_name=ojplmecpdpgccookcobabopnaifgidhf; cf_clearance=n4Y1XTKZ5TfIMBNQuAXzerwKpx0U35KoOm3imfT0GpU-1732097818-1.2.1.1-ZeAnEu.8D9TSZHYDoj7vwo1A1rpdKl304ZpaBn_QbAQOr211JFAb7.JRQU3EL2eIy1Dfl8HhYvH7_259.22lUz8gbchHcQ8hvfuQXMtFMCbqDBLzjNUZa9stuk.39l28IcPhH9Z2szsf3SGtNI1sAfo66Djt7sOReLK3lHw9UkJp7BdGqt6a2X9qAc8EsAI3lE480Tmt0fkHv14Oc30LSbPB_WwFmiqAki2W.Gv9hV7TN_QBFESleTDlXd.6KGflfd4.KwWF7rpSRo_cgoc9ALLLIafpxHVbe7_g5r7zvpml_Pj8fEL75fw.1GBuy16bciHBuB8s_kahuJYUnhtQFFgfTQl8_Gn6KeovBWx.PJ7nFv5sklHUfAyBVq3t30xKe8ZDydsQ_G.yipfj_In5GmmWcXGb6E4.bioDOwW_sKLtxwdTQt7Nu.RkILX_mKvXNpyLqflIVj8G7X5E8I.unw",
        "Upgrade-Insecure-Requests" to "1",
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0"
    )

    override val mainPage = mainPageOf(
        "$mainUrl/page/%d/" to "Home",
        "$mainUrl/category/web-series/netflix/page/%d/" to "Netflix",
        "$mainUrl/category/web-series/disney-plus-hotstar/page/%d/" to "Disney Plus Hotstar",
        "$mainUrl/category/web-series/amazon-prime-video/page/%d/" to "Amazon Prime",
        "$mainUrl/category/web-series/mx-original/page/%d/" to "MX Original",
        "$mainUrl/category/web-series/jio-studios/page/%d/" to "Jio Cinema",
        "$mainUrl/category/web-series/sonyliv/page/%d/" to "Sony Liv",
        "$mainUrl/category/web-series/zee5-originals/page/%d/" to "Zee5",
        "$mainUrl/category/web-series/alt-balaji-web-series/page/%d/" to "ALT Balaji",
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val document = app.get(
            request.data.format(page),
            referer = mainUrl,
            headers = headers
        ).document
        val home = document.select("a.blog-img").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.attr("title").replace("Download ", "")
        val href = this.attr("href")
        val posterUrl = this.select("img").attr("data-src")
        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }
    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()
        for (i in 1..7) {
            val document = app.get(
                "$mainUrl/page/$i/?s=$query",
                referer = mainUrl,
                headers = headers
            ).document
            val results = document.select("a.blog-img").mapNotNull { it.toSearchResult() }
            if (results.isEmpty()) {
                break
            }
            searchResponse.addAll(results)
        }
        return searchResponse
    }

}
