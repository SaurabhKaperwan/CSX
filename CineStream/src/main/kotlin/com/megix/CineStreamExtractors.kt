package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.network.CloudflareKiller
import com.lagradost.api.Log
import android.util.Base64
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.APIHolder.unixTime
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import java.net.URLEncoder
import okhttp3.FormBody
import java.nio.charset.StandardCharsets
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.json.JSONObject
import org.json.JSONArray
import com.lagradost.cloudstream3.runAllAsync
import com.lagradost.cloudstream3.extractors.helper.GogoHelper
import com.lagradost.cloudstream3.mvvm.safeApiCall
import com.lagradost.nicehttp.RequestBodyTypes
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import com.lagradost.cloudstream3.utils.AppUtils.toJson

object CineStreamExtractors : CineStreamProvider() {

    suspend fun invokeTvStream(
        id: String? = null,
        api: String,
        tvtype: String,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val url = "$api/stream/$tvtype/$id.json"
        val json = app.get(url).text
        val data = parseJson<TvStreamsResponse>(json)
        data.streams.forEach {
            callback.invoke(
                newExtractorLink(
                    it.name ?: it.title ?: "TV",
                    it.description ?: it.title ?: "TV",
                    it.url,
                ) {
                    this.headers = it.behaviorHints.proxyHeaders.request ?: mapOf()
                }
            )
        }
    }

    suspend fun invokeAllmovieland(
        id : String? = null,
        season : Int? = null,
        episode : Int? = null,
        callback: (ExtractorLink) -> Unit
    ) {
        val host = "https://zativertz295huk.com"
        val referer = "$allmovielandAPI/"
        val res =
                app.get("$host/play/$id", referer = referer)
                        .document
                        .selectFirst("script:containsData(playlist)")
                        ?.data()
                        ?.substringAfter("{")
                        ?.substringBefore(";")
                        ?.substringBefore(")")
        val json = tryParseJson<AllMovielandPlaylist>("{${res ?: return}")
        val headers = mapOf("X-CSRF-TOKEN" to "${json?.key}")

        val serverRes =
                app.get(fixUrl(json?.file ?: return, host), headers = headers, referer = referer)
                        .text
                        .replace(Regex(""",\s*\[]"""), "")
        val servers =
                tryParseJson<ArrayList<AllMovielandServer>>(serverRes).let { server ->
                    if (season == null) {
                        server?.map { it.file to it.title }
                    } else {
                        server
                                ?.find { it.id.equals("$season") }
                                ?.folder
                                ?.find { it.episode.equals("$episode") }
                                ?.folder
                                ?.map { it.file to it.title }
                    }
                }

        servers?.amap { (server, lang) ->
            val path =
                    app.post(
                        "${host}/playlist/${server ?: return@amap}.txt",
                        headers = headers,
                        referer = referer
                    ).text
            M3u8Helper.generateM3u8("Allmovieland [$lang]", path, referer).forEach(callback)
        }
    }

    suspend fun invokeHindmoviez(
        source: String,
        api: String,
        id: String? = null,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit
    ) {
        app.get("$api/?s=$id", timeout = 50L).document.select("h2.entry-title > a").amap {
            val doc = app.get(it.attr("href"), timeout = 50L).document
            if(episode == null) {
                doc.select("a.maxbutton").amap {
                    val res = app.get(it.attr("href"), timeout = 50L).document
                    val link = res.select("h3 > a").attr("href")
                    getHindMoviezLinks(source, link, callback)
                }
            }
            else {
                doc.select("a.maxbutton").amap {
                    val text = it.parent()?.parent()?.previousElementSibling()?.text() ?: ""
                    if(text.contains("Season $season")) {
                        val res = app.get(it.attr("href"), timeout = 50L).document
                        res.select("h3 > a").getOrNull(episode-1)?.let { link ->
                            getHindMoviezLinks(source, link.attr("href"), callback)
                        }
                    }
                }
            }
        }
    }

    suspend fun invokeDramacool(
        title: String,
        provider: String,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ) {
        val titleSlug = title.replace(" ", "-")
        val s = if(season != 1) "-season-$season" else ""
        val url = "$stremio_Dramacool/stream/series/$provider-${titleSlug}${s}::$titleSlug${s}-ep-$episode.json"
        val json = app.get(url).text
        val data = tryParseJson<Dramacool>(json) ?: return
        data.streams.forEach {

            callback.invoke(
                newExtractorLink(
                    it.title,
                    it.title,
                    it.url,
                )
            )

            it.subtitles.forEach {
                subtitleCallback.invoke(
                    SubtitleFile(
                        it.lang,
                        it.url
                    )
                )
            }
        }
    }

    suspend fun invokeTokyoInsider(
        title: String,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ) {
        val tvtype = if(episode == null) "_(Movie)" else "_(TV)"
        val firstChar = getFirstCharacterOrZero(title).uppercase()
        val newTitle = title.replace(" ","_")
        val doc = app.get("$tokyoInsiderAPI/anime/$firstChar/$newTitle$tvtype", timeout = 500L).document
        val selector = if(episode != null) "a.download-link:matches((?i)(episode $episode\\b))" else "a.download-link"
        val aTag = doc.selectFirst(selector)
        val epUrl = aTag?.attr("href") ?: return
        val res = app.get(tokyoInsiderAPI + epUrl, timeout = 500L).document
        res.select("div.c_h2 > div > a").map {
            val name = it.text()
            val url = it.attr("href")
            callback.invoke(
                newExtractorLink(
                    "TokyoInsider",
                    "[TokyoInsider] - $name",
                    url,
                ) {
                    this.quality = getIndexQuality(name)
                }
            )
        }
    }

    suspend fun invokePrimeVideo(
        title: String,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ) {
        NfCookie = NFBypass(netflixAPI)
        val cookies = mapOf(
            "t_hash_t" to NfCookie,
            "ott" to "pv",
            "hd" to "on"
        )
        val headers = mapOf("X-Requested-With" to "XMLHttpRequest")
        val url = "$netflixAPI/mobile/pv/search.php?s=$title&t=${APIHolder.unixTime}"
        val data = app.get(url, headers = headers, cookies = cookies).parsedSafe<NfSearchData>()
        val netflixId = data ?.searchResult ?.firstOrNull { it.t.equals(title.trim(), ignoreCase = true) }?.id

        val (nfTitle, id) = app.get(
            "$netflixAPI/mobile/pv/post.php?id=${netflixId ?: return}&t=${APIHolder.unixTime}",
            headers = headers,
            cookies = cookies,
            referer = "$netflixAPI/"
        ).parsedSafe<NetflixResponse>().let { media ->
            if (season == null && year.toString() == media?.year.toString()) {
                media?.title to netflixId
            } else if(year.toString() == media?.year.toString()) {
                val seasonId = media?.season?.find { it.s == "$season" }?.id
                var episodeId : String? = null
                var page = 1

                while(episodeId == null && page < 10) {
                    val data = app.get(
                        "$netflixAPI/mobile/pv/episodes.php?s=${seasonId}&series=$netflixId&t=${APIHolder.unixTime}&page=$page",
                        headers = headers,
                        cookies = cookies,
                        referer = "$netflixAPI/"
                    ).parsedSafe<NetflixResponse>()
                    episodeId = data?.episodes?.find { it.ep == "E$episode" }?.id
                    if(data?.nextPageShow != 1) { break }
                    page++
                }

                media?.title to episodeId
            }
            else {
                null to null
            }
        }

        app.get(
            "$netflixAPI/mobile/pv/playlist.php?id=${id ?: return}&t=${nfTitle ?: return}&tm=${APIHolder.unixTime}",
            headers = headers,
            cookies = cookies,
            referer = "$netflixAPI/"
        ).text.let {
            tryParseJson<ArrayList<NetflixResponse>>(it)
        }?.firstOrNull()?.sources?.map {
            callback.invoke(
                newExtractorLink(
                    "PrimeVideo",
                    "PrimeVideo",
                    "$netflixAPI/${it.file}",
                ) {
                    this.referer = "$netflixAPI/"
                    this.quality = getQualityFromName(it.file?.substringAfter("q=")?.substringBefore("&in"))
                    this.headers = mapOf("Cookie" to "hd=on")
                }
            )
        }
    }

    suspend fun invokeNetflix(
        title: String,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ) {
        NfCookie = NFBypass(netflixAPI)
        val cookies = mapOf(
            "t_hash_t" to NfCookie,
            "hd" to "on"
        )
        val headers = mapOf("X-Requested-With" to "XMLHttpRequest")
        val url = "$netflixAPI/mobile/search.php?s=$title&t=${APIHolder.unixTime}"
        val data = app.get(url, headers = headers, cookies = cookies).parsedSafe<NfSearchData>()
        val netflixId = data ?.searchResult ?.firstOrNull { it.t.equals(title.trim(), ignoreCase = true) }?.id

        val (nfTitle, id) = app.get(
            "$netflixAPI/mobile/post.php?id=${netflixId ?: return}&t=${APIHolder.unixTime}",
            headers = headers,
            cookies = cookies,
            referer = "$netflixAPI/"
        ).parsedSafe<NetflixResponse>().let { media ->
            if (season == null && year.toString() == media?.year.toString()) {
                media?.title to netflixId
            } else if(year.toString() == media?.year.toString()) {
                val seasonId = media?.season?.find { it.s == "$season" }?.id
                var episodeId : String? = null
                var page = 1
                while(episodeId == null && page < 10) {
                    val data = app.get(
                        "$netflixAPI/mobile/episodes.php?s=${seasonId}&series=$netflixId&t=${APIHolder.unixTime}&page=$page",
                        headers = headers,
                        cookies = cookies,
                        referer = "$netflixAPI/"
                    ).parsedSafe<NetflixResponse>()
                    episodeId = data?.episodes?.find { it.ep == "E$episode" }?.id
                    if(data?.nextPageShow != 1) { break }
                    page++
                }
                media?.title to episodeId
            }
            else {
                null to null
            }
        }

        app.get(
            "$netflixAPI/mobile/playlist.php?id=${id ?: return}&t=${nfTitle ?: return}&tm=${APIHolder.unixTime}",
            headers = headers,
            cookies = cookies,
            referer = "$netflixAPI/"
        ).text.let {
            tryParseJson<ArrayList<NetflixResponse>>(it)
        }?.firstOrNull()?.sources?.map {
            callback.invoke(
                newExtractorLink(
                    "Netflix",
                    "Netflix",
                    "$netflixAPI/${it.file}",
                ) {
                    this.referer = "$netflixAPI/"
                    this.quality = getQualityFromName(it.file?.substringAfter("q=")?.substringBefore("&in"))
                    this.headers = mapOf("Cookie" to "hd=on")
                }
            )
        }
    }

    suspend fun invokeHdmovie2(
        title: String? = null,
        year: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ) {
        val headers = mapOf("User-Agent" to "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
        val document = app.get("$hdmovie2API/movies/${title.createSlug()}-$year", headers = headers, allowRedirects = true).document
        document.select("div.wp-content p a").amap {
            if(episode != null && it.text().contains("EP")) {
                if(
                    it.text().contains("EP$episode")||
                    it.text().contains("EP0$episode")
                ) {
                    app.get(it.attr("href")).document.select("div > p > a").amap {
                        loadSourceNameExtractor(
                            "Hdmovie2",
                            it.attr("href"),
                            "",
                            subtitleCallback,
                            callback,
                        )
                    }
                } else {
                    return@amap;
                }
            }
            else {
                val type = if(episode != null) "(Combined)" else ""
                app.get(it.attr("href")).document.select("div > p > a").amap {
                    loadSourceNameExtractor(
                        "Hdmovie2$type",
                        it.attr("href"),
                        "",
                        subtitleCallback,
                        callback,
                    )
                }
            }
        }
    }

    suspend fun invokeSkymovies(
        title: String? = null,
        year: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ) {
        val url = "$skymoviesAPI/search.php?search=$title ($year)&cat=All"
        app.get(url).document.select("div.L a").amap {
            app.get(skymoviesAPI + it.attr("href")).document.select("div.Bolly > a").amap {
                val text = it.text()
                if(episode == null) {
                  loadSourceNameExtractor(
                        "Skymovies",
                        it.attr("href"),
                        "",
                        subtitleCallback,
                        callback,
                    )
                }
                else if(text.contains("Episode")) {
                    if(text.contains("Episode $episode") || text.contains("Episode 0$episode")) {
                        loadSourceNameExtractor(
                            "Skymovies",
                            it.attr("href"),
                            "",
                            subtitleCallback,
                            callback,
                        )
                    }
                }
                else {
                    loadSourceNameExtractor(
                        "Skymovies(Combined)",
                        it.attr("href"),
                        "",
                        subtitleCallback,
                        callback,
                    )
                }
            }
        }
    }

    suspend fun invokeTom(
        id: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit,
        subtitleCallback: (SubtitleFile) -> Unit
    ) {
        val url = if(season == null) "$TomAPI/api/getVideoSource?type=movie&id=$id" else "$TomAPI/api/getVideoSource?type=tv&id=$id/$season/$episode"
        val headers = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:101.0) Gecko/20100101 Firefox/101.0",
            "Referer" to "https://autoembed.cc"
        )
        val json = app.get(url, headers = headers).text
        val data = tryParseJson<TomResponse>(json) ?: return

        callback.invoke(
            newExtractorLink(
                "Tom",
                "Tom",
                data.videoSource,
                ExtractorLinkType.M3U8
            )
        )

        data.subtitles.map {
            subtitleCallback.invoke(
                SubtitleFile(
                    it.label,
                    it.file,
                )
            )
        }
    }

    suspend fun invokeProtonmovies(
        id: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ) {
        val headers = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0",
            "Referer" to protonmoviesAPI
        )
        val url = "$protonmoviesAPI/search/$id"
        val text = app.get(url, headers = headers).text
        val regex = Regex("""\[(?=.*?\"<div class\")(.*?)\]""")
        val htmlArray = regex.findAll(text).map { it.value }.toList()
        if (htmlArray.isNotEmpty()) {
            val lastJsonArray = JSONArray(htmlArray.last())
            val html = decodeHtml(Array(lastJsonArray.length()) { i -> lastJsonArray.getString(i) })
            val doc = Jsoup.parse(html)
            val link = doc.select(".col.mb-4 h5 a").attr("href")
            val document = app.get("${protonmoviesAPI}${link}", headers = headers).document
            val decodedDoc = decodeMeta(document)
            if (decodedDoc != null) {
                if(episode == null) {
                    getProtonStream(decodedDoc, protonmoviesAPI,subtitleCallback, callback)
                } else{
                    val episodeDiv = decodedDoc.select("div.episode-block:has(div.episode-number:matchesOwn(S${season}E${episode}))").firstOrNull()
                    episodeDiv?.selectFirst("a")?.attr("href")?.let {
                        val source = protonmoviesAPI + it
                        val doc2 = app.get(source, headers = headers).document

                        val decodedDoc = decodeMeta(doc2)
                        if(decodedDoc != null) {
                            getProtonStream(decodedDoc, protonmoviesAPI, subtitleCallback, callback)
                        }
                    }
                }
            }
        }
    }

    suspend fun invokeMoviesflix(
        sourceName: String,
        api: String,
        id: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ) {
        app.get("$api/?s=$id").document.select("a.post-image").amap {
            val doc = app.get(it.attr("href")).document
            if(episode == null) {
                doc.select("a.maxbutton").amap { button ->
                    val document = app.get(button.attr("href")).document
                    document.select("a.maxbutton").amap { source ->
                        val linkText = source.text()
                        if (!linkText.contains("Google Drive", ignoreCase = true) &&
                            !linkText.contains("Other Download Links", ignoreCase = true) &&
                            !linkText.contains("G-Direct", ignoreCase = true)
                        ) {
                            loadSourceNameExtractor(
                                sourceName,
                                source.attr("href"),
                                "",
                                subtitleCallback,
                                callback,
                            )
                        }
                    }
                }
            }
            else {
                doc.select("p:has(a.maxbutton)").amap { p ->
                    val possibleMatches = listOf(
                        "Season $season",
                        "Season 0$season",
                        "S$season",
                        "S0$season"
                    )
                    if(possibleMatches.any {
                        p.previousElementSibling()?.previousElementSibling()?.text()?.contains(it) == true
                    }) {
                        p.select("a.maxbutton").amap { button ->
                            val buttonText = button.text()
                            if(!buttonText.contains("G-Direct", ignoreCase = true) &&
                                !buttonText.contains("Drop Galaxy", ignoreCase = true) &&
                                !buttonText.contains("G-Drive", ignoreCase = true) &&
                                !buttonText.contains("Mega.nz", ignoreCase = true)
                            ) {
                                app.get(button.attr("href")).document.select("h3 > strong > a").getOrNull(episode-1)?.let { source ->
                                    loadSourceNameExtractor(
                                        sourceName,
                                        source.attr("href"),
                                        "",
                                        subtitleCallback,
                                        callback,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun invokeMovies4u(
        id: String? = null,
        title: String? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ) {
        val searchQuery = "$title $year".trim()
        val searchUrl = "$movies4uAPI/?s=$searchQuery"

        val searchDoc = app.get(searchUrl).document
        val links = searchDoc.select("article h3 a")

        links.amap { element ->
            val postUrl = element.attr("href")
            val postDoc = app.get(postUrl).document
            val imdbId = postDoc.select("p a:contains(IMDb Rating)").attr("href")
                            .substringAfter("title/").substringBefore("/")

            if(imdbId != id.toString()) { return@amap }

            if (season == null) {
                val innerUrl = postDoc.select("div.download-links-div a.btn").attr("href")
                val innerDoc = app.get(innerUrl).document
                val sourceButtons = innerDoc.select("div.downloads-btns-div a.btn")
                sourceButtons.amap { sourceButton ->
                    val sourceLink = sourceButton.attr("href")
                    loadSourceNameExtractor(
                        "Movies4u",
                        sourceLink,
                        "",
                        subtitleCallback,
                        callback
                    )
                }
            } else {
                val seasonBlocks = postDoc.select("div.downloads-btns-div")
                seasonBlocks.amap { block ->
                    val headerText = block.previousElementSibling()?.text().orEmpty()
                    if (headerText.contains("Season $season", ignoreCase = true)) {
                        val seasonLink = block.selectFirst("a.btn")?.attr("href") ?: return@amap

                        val episodeDoc = app.get(seasonLink).document
                        val episodeBlocks = episodeDoc.select("div.downloads-btns-div")

                        if (episode != null && episode in 1..episodeBlocks.size) {
                            val episodeBlock = episodeBlocks[episode - 1]
                            val episodeLinks = episodeBlock.select("a.btn")

                            episodeLinks.amap { epLink ->
                                val sourceLink = epLink.attr("href")
                                loadSourceNameExtractor(
                                    "Movies4u",
                                    sourceLink,
                                    "",
                                    subtitleCallback,
                                    callback
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun invokeTorrentio(
        id: String? = null,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit,
    ) {
        val url = if(season == null) {
            "$torrentioAPI/$torrentioCONFIG/stream/movie/$id.json"
        }
        else {
            "$torrentioAPI/$torrentioCONFIG/stream/series/$id:$season:$episode.json"
        }
        val headers = mapOf(
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8",
            "User-Agent" to "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36",
        )
        val res = app.get(url, headers = headers, timeout = 200L).parsedSafe<TorrentioResponse>()
        res?.streams?.forEach { stream ->
            val resp = app.get(TRACKER_LIST_URL).text
            val sourceTrackers = resp
                    .split("\n")
                    .filterIndexed { i, _ -> i % 2 == 0 }
                    .filter { s -> s.isNotEmpty() }.joinToString("") { "&tr=$it" }
            val magnet = "magnet:?xt=urn:btih:${stream.infoHash}&dn=${stream.infoHash}$sourceTrackers&index=${stream.fileIdx}"
            callback.invoke(
                newExtractorLink(
                    "Torrentio",
                    stream.title ?: stream.name ?: "",
                    magnet,
                    ExtractorLinkType.MAGNET,
                ) {
                    this.quality = getIndexQuality(stream.name)
                }
            )
        }
    }

    suspend fun invokeCinemaluxe(
        title: String? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit,
        subtitleCallback: (SubtitleFile) -> Unit
    ) {
        val newTitle = title?.replace(" ", "+")?.replace("’s", "") ?: return
        val url = "$cinemaluxeAPI/?s=$newTitle+$year"
        val link = app.get(url).document.selectFirst("div.title > a:matches((?i)($title $year))")?.attr("href") ?: return
        val document = app.get(link).document

        if(season == null) {
            document.select("div.ep-button-container > a").amap {
                var link = it.attr("href")
                link = cinemaluxeBypass(link)
                val selector = if(link.contains("linkstore")) "div.ep-button-container > a" else "div.mirror-buttons a"
                app.get(link).document.select(selector).amap {
                    loadSourceNameExtractor(
                        "Cinemaluxe",
                        it.attr("href"),
                        "",
                        subtitleCallback,
                        callback,
                    )
                }
            }
        }
        else {
            val season = document.select("div.ep-button-container > a:matches((?i)(Season 0?$season))")
            season.amap { div ->
                var link = div.select("a").attr("href")
                link = cinemaluxeBypass(link)
                 app.get(link).document.select("""div.ep-button-container > a:matches((?i)(?:episode\s*[-]?\s*)(0?$episode\b))""").amap {
                    loadSourceNameExtractor(
                        "Cinemaluxe",
                        it.attr("href"),
                        "",
                        subtitleCallback,
                        callback,
                    )
                }
            }
        }
    }

    suspend fun invokeBollyflix(
        id: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ) {
        var res1 = app.get("""$bollyflixAPI/search/$id ${season ?: ""}""", interceptor = wpRedisInterceptor).document
        val url = res1.select("div > article > a").attr("href") ?: return
        val res = app.get(url).document
        val hTag = if (season == null) "h5" else "h4"
        val sTag = if (season == null) "" else "Season $season"
        val entries =
            res.select("div.thecontent.clearfix > $hTag:matches((?i)$sTag.*(720p|1080p|2160p))")
                ?.filter { element -> !element.text().contains("Download", true) }?.takeLast(4)
        entries?.map {
            val href = it.nextElementSibling()?.select("a")?.attr("href")
            val token = href?.substringAfter("id=")
            val encodedurl =
                app.get("https://web.sidexfee.com/?id=$token").text.substringAfter("link\":\"")
                    .substringBefore("\"};")
            val decodedurl = base64Decode(encodedurl)

            if (season == null) {
                loadSourceNameExtractor("Bollyflix", decodedurl , "", subtitleCallback, callback)
            } else {
                val episodeText = "Episode " + episode.toString().padStart(2, '0')
                val link =
                    app.get(decodedurl).document.selectFirst("article h3 a:contains($episodeText)")!!
                        .attr("href")
                loadSourceNameExtractor("Bollyflix", link , "", subtitleCallback, callback)
            }
        }
    }

    suspend fun invokeStreamify(
        id: String,
        callback: (ExtractorLink) -> Unit
    ) {
        val url = "$stremifyAPI/movie/$id.json"
        val json = app.get(url).text
        val data = tryParseJson<StreamifyResponse>(json) ?: return
        data.streams.forEach {
            callback.invoke(
                newExtractorLink(
                    it.name,
                    "[${it.name}] ${it.title}",
                    it.url,
                )
            )
        }
    }

    suspend fun invokeMultimovies(
        apiUrl: String,
        title: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val fixTitle = title.createSlug()
        val url = if (season == null) {
            "$apiUrl/movies/$fixTitle"
        } else {
            "$apiUrl/episodes/$fixTitle-${season}x${episode}"
        }
        val req = app.get(url).document
        req.select("ul#playeroptionsul li").map {
            Triple(
                it.attr("data-post"),
                it.attr("data-nume"),
                it.attr("data-type")
            )
        }.amap { (id, nume, type) ->
            if (!nume.contains("trailer")) {
                val source = app.post(
                    url = "$apiUrl/wp-admin/admin-ajax.php",
                    data = mapOf(
                        "action" to "doo_player_ajax",
                        "post" to id,
                        "nume" to nume,
                        "type" to type
                    ),
                    referer = url,
                    headers = mapOf("X-Requested-With" to "XMLHttpRequest")
                ).parsed<ResponseHash>().embed_url
                val link = source.substringAfter("\"").substringBefore("\"")
                when {
                    !link.contains("youtube") -> {
                        loadSourceNameExtractor("Multimovies",link, referer = apiUrl, subtitleCallback, callback)
                    }
                    else -> ""
                }
            }
        }
    }

    suspend fun invokeAnimes(
        malId: Int? = null,
        aniId: Int? = null,
        episode: Int? = null,
        year: Int? = null,
        origin: String,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val malsync = app.get("$malsyncAPI/mal/anime/${malId ?: return}")
            .parsedSafe<MALSyncResponses>()?.sites
        val zoroIds = malsync?.zoro?.keys?.map { it }
        val zorotitle = malsync?.zoro?.firstNotNullOf { it.value["title"] }?.replace(":"," ")
        val animepahe = malsync?.animepahe?.firstNotNullOf { it.value["url"] }
        val animepahetitle = malsync?.animepahe?.firstNotNullOf { it.value["title"] }

        runAllAsync(
            {
                val hianimeurl=malsync?.zoro?.firstNotNullOf { it.value["url"] }
                invokeHianime(hianimeurl, episode, subtitleCallback, callback)
            },
            {
                val animepahe = malsync?.animepahe?.firstNotNullOfOrNull { it.value["url"] }
                if (animepahe!=null)
                    invokeAnimepahe(animepahe, episode, subtitleCallback, callback)
            },
            {
                malsync?.Gogoanime?.forEach { (_, entry) ->
                    entry["url"]?.let { url ->
                        invokeAnitaku(url, episode, subtitleCallback, callback)
                    }
                }
            },
            {
                if(origin == "imdb" && zorotitle != null) invokeTokyoInsider(
                    zorotitle,
                    episode,
                    subtitleCallback,
                    callback
                )
            },
            {
                if(origin == "imdb" && zorotitle != null) invokeAllanime(
                    zorotitle,
                    year,
                    episode,
                    subtitleCallback,
                    callback
                )
            },
            {
                if(origin == "imdb" && zorotitle != null) invokeAnizone(
                    zorotitle,
                    episode,
                    subtitleCallback,
                    callback
                )
            },
        )
    }

    suspend fun invokeAnitaku(
        url: String? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
            val subDub = if (url!!.contains("-dub")) "Dub" else "Sub"
            val epUrl = url.replace("category/", "").plus("-episode-${episode}")
            val epRes = app.get(epUrl).document
            epRes.select("div.anime_muti_link > ul > li").forEach {
                val sourcename = it.selectFirst("a")?.ownText() ?: return@forEach
                val iframe = it.selectFirst("a")?.attr("data-video") ?: return@forEach
                if(iframe.contains("s3taku"))
                {
                    val iv = "3134003223491201"
                    val secretKey = "37911490979715163134003223491201"
                    val secretDecryptKey = "54674138327930866480207815084989"
                    GogoHelper.extractVidstream(
                        iframe,
                        "Anitaku Vidstreaming [$subDub]",
                        callback,
                        iv,
                        secretKey,
                        secretDecryptKey,
                        isUsingAdaptiveKeys = false,
                        isUsingAdaptiveData = true
                    )
                }
                else
                loadCustomExtractor(
                    "Anitaku $sourcename [$subDub]",
                    iframe,
                    "",
                    subtitleCallback,
                    callback
                )
            }
    }

    private suspend fun invokeAnimepahe(
        url: String? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val headers = mapOf("Cookie" to "__ddg2_=1234567890")
        val id = app.get(url ?: "", headers).document.selectFirst("meta[property=og:url]")
            ?.attr("content").toString().substringAfterLast("/")
        val animeData =
            app.get("$animepaheAPI/api?m=release&id=$id&sort=episode_desc&page=1", headers)
                .parsedSafe<animepahe>()?.data
        val session = if(episode == null) {
            animeData?.firstOrNull()?.session ?: return
        } else {
            animeData?.find { it.episode == episode }?.session ?: return
        }
        val doc = app.get("$animepaheAPI/play/$id/$session", headers).document

        doc.select("div#pickDownload > a").amap {
            val href = it.attr("href")
            var type = "SUB"
            if(it.select("span").text().contains("eng")) type = "DUB"

            loadCustomExtractor(
                "Animepahe [$type]",
                href,
                "",
                subtitleCallback,
                callback,
                getIndexQuality(it.text())
            )
        }

        doc.select("div#resolutionMenu > button").amap {
            var type = "SUB"
            if(it.select("span").text().contains("eng")) type = "DUB"
            val quality = it.attr("data-resolution")
            val href = it.attr("data-src")
            if (href.contains("kwik.si")) {
                loadCustomExtractor(
                    "Animepahe(VLC) [$type]",
                    href,
                    mainUrl,
                    subtitleCallback,
                    callback,
                    getQualityFromName(quality)
                )
            }
        }
    }

    // suspend fun invokeRar(
    //     title: String,
    //     year: Int? = null,
    //     season: Int? = null,
    //     episode: Int? = null,
    //     callback: (ExtractorLink) -> Unit,
    // ) {
    //     val json = app.get("$RarAPI/ajax/posts?q=$title ($year)").text
    //     val responseData = parseJson<RarResponseData>(json)
    //     val id = responseData.data?.firstOrNull {
    //         it.second_name == title
    //     }?.id ?: return
    //     val slug = "$title $year $id".createSlug()
    //     val url = if(season != null) "$RarAPI/show/$slug/season/$season/episode/$episode" else "$RarAPI/movie/$slug"
    //     val embedId = app.get(url).document.selectFirst("a.btn-service")?.attr("data-embed") ?: return
    //     val body = FormBody.Builder().add("id", embedId).build()
    //     val document = app.post("$RarAPI/ajax/embed", requestBody = body).document
    //     val regex = Regex("""(https?:\/\/[^\"']+\.m3u8)""")
    //     val link = regex.find(document.toString())?.groupValues?.get(1) ?: return
    //     callback.invoke(
    //         ExtractorLink(
    //             "Rar",
    //             "Rar",
    //             link,
    //             referer = "",
    //             Qualities.P1080.value,
    //             true
    //         )
    //     )
    // }

    // suspend fun invokeVidbinge(
    //     title: String,
    //     imdb_id: String,
    //     tmdb_id: Int? = null,
    //     year: Int? = null,
    //     season: Int? = null,
    //     episode: Int? = null,
    //     callback: (ExtractorLink) -> Unit,
    //     subtitleCallback: (SubtitleFile) -> Unit,
    // ) {
    //     val providers = mutableListOf("orion", "astra", "nova")
    //     val type = if (season == null) "movie" else "tv"
    //     val s = season ?: ""
    //     val e = episode ?: ""
    //     val query = """{"title":"$title","imdbId":"$imdb_id","tmdbId":"$tmdb_id","type":"$type","season":"$s","episode":"$e","releaseYear":"$year"}"""
    //     val headers = mapOf(
    //         "accept" to "*/*",
    //         "origin" to "https://www.vidbinge.app",
    //         "user-agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    //     )

    //     val tokenJson = app.get("$BYPASS_API/whvxToken", timeout = 500L).text
    //     val token = parseJson<WHVXToken>(tokenJson).token
    //     val encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8.toString())
    //     val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
    //     providers.amap { provider ->
    //         val json = app.get("${WHVXAPI}/search?query=${encodedQuery}&provider=${provider}&token=${encodedToken}", headers = headers, timeout = 500L).text
    //         val data = tryParseJson<WHVX>(json) ?: return@amap
    //         val encodedUrl = URLEncoder.encode(data.url, StandardCharsets.UTF_8.toString())
    //         val json2 = app.get("${WHVXAPI}/source?resourceId=${encodedUrl}&provider=${provider}", headers = headers, timeout = 500L).text
    //         if(provider == "astra") {
    //             val data2 = tryParseJson<AstraQuery>(json2) ?: return@amap
    //             data2.stream.forEach {
    //                 callback.invoke(
    //                     ExtractorLink(
    //                         "Astra",
    //                         "Astra",
    //                         it.playlist,
    //                         "",
    //                         Qualities.Unknown.value,
    //                         INFER_TYPE
    //                     )
    //                 )
    //             }
    //         }
    //         else if(provider == "nova") {
    //             val data2 = tryParseJson<NovaVideoData>(json2) ?: return@amap
    //             for (stream in data2.stream) {
    //                 for ((quality, details) in stream.qualities) {
    //                     callback.invoke(
    //                         ExtractorLink(
    //                             "Nova",
    //                             "Nova",
    //                             details.url,
    //                             "",
    //                             getQualityFromName(quality),
    //                             INFER_TYPE,
    //                         )
    //                     )
    //                 }
    //             }

    //             for (stream in data2.stream) {
    //                 for (caption in stream.captions) {
    //                     subtitleCallback.invoke(
    //                         SubtitleFile(
    //                             caption.language,
    //                             caption.url
    //                         )
    //                     )
    //                 }
    //             }
    //         }
    //         else {
    //             val data2 = tryParseJson<OrionStreamData>(json2) ?: return@amap
    //             for(stream in data2.stream) {
    //                 callback.invoke(
    //                     ExtractorLink(
    //                         "Orion",
    //                         "Orion",
    //                         stream.playlist,
    //                         "",
    //                         Qualities.Unknown.value,
    //                         INFER_TYPE
    //                     )
    //                 )

    //                 for (caption in stream.captions) {
    //                     subtitleCallback.invoke(
    //                         SubtitleFile(
    //                             caption.language,
    //                             caption.url
    //                         )
    //                     )
    //                 }
    //             }
    //         }
    //     }
    // }

    suspend fun invokeMultiAutoembed(
        id: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ) {
        val url =  if(season != null) "$MultiembedAPI/api/getVideoSource?type=tv&id=$id/$season/$episode"
        else "$MultiembedAPI/api/getVideoSource?type=movie&id=$id"
        val jsonBody = app.get(url, headers = mapOf("Referer" to MultiembedAPI)).text.toJson()
            .toRequestBody(RequestBodyTypes.JSON.toMediaTypeOrNull())
        val headers = mapOf(
            "Content-Type" to "application/json",
            "Referer" to "$MultiembedAPI/",
        )
        val json = app.post("$MultiembedAPI/api/decryptVideoSource",
            requestBody = jsonBody, headers = headers
        ).text

        val data = tryParseJson<MultiAutoembedResponse>(json) ?: return
        data.audioTracks.forEach {
            callback.invoke(
                newExtractorLink(
                    "MultiAutoembed[${it.label}]",
                    "MultiAutoembed[${it.label}]",
                    it.file,
                    type = ExtractorLinkType.M3U8,
                )
            )
        }
    }

    suspend fun invoke4khdhub(
        title: String? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ) {
        if (title.isNullOrBlank()) return

        val link = app.get("$fourkhdhubAPI/?s=$title").document
            .selectFirst("div.card-grid > a:has(div.movie-card-content:contains(${year ?: ""}))")
            ?.attr("href") ?: return

        val doc = app.get("$fourkhdhubAPI$link").document
        if(season == null) {
            doc.select("div.download-item a").amap {
               val source = it.attr("href")
               loadSourceNameExtractor(
                    "4Khdhub",
                    source,
                    "",
                    subtitleCallback,
                    callback
                )
            }
        } else {
            val seasonText = "S" + season.toString().padStart(2, '0')
            val episodeText = "E" + episode.toString().padStart(2, '0')
            doc.select("div.episode-download-item:has(div.episode-file-title:contains(${seasonText}${episodeText}))").amap {
                it.select("div.episode-links > a").amap {
                    val source = it.attr("href")
                    loadSourceNameExtractor(
                        "4Khdhub",
                        source,
                        "",
                        subtitleCallback,
                        callback
                    )
                }
            }
        }
    }

    suspend fun invokeMostraguarda(
        id: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ) {
        val url = if(season == null) "$MostraguardaAPI/movie/$id" else "$MostraguardaAPI/serie/$id/$season/$episode"
        val doc = app.get(
            url,
            headers = mapOf(
                "User-Agent" to "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
            )
        ).document

        doc.select("ul > li").amap {
            if(it.text().contains("supervideo")) {
                val source = "https:" + it.attr("data-link")
                SuperVideo().getUrl(source, "", subtitleCallback, callback)
            }
        }
    }

    suspend fun invokeWHVXSubs(
        api: String,
        id: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
    ) {
        val url = if(season != null) "$api/search?id=$id&season=$season&episode=$episode" else "$api/search?id=$id"
        val json = app.get(url).text
        val data = parseJson<ArrayList<WHVXSubtitle>>(json)
        data.forEach {
            subtitleCallback.invoke(
                SubtitleFile(
                    it.languageName ?: it.display ?: "Unknown",
                    it.url
                )
            )
        }
    }

    suspend fun invokeW4U(
        title: String,
        year: Int? = null,
        id: String,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ) {
        val url = if(season == null) "$W4UAPI/?s=$title $year" else "$W4UAPI/?s=$title $season"
        val document = app.get(url).document
        val link = document.selectFirst("div.post-thumb > a")?.attr("href")
        val doc = app.get(link ?: return).document
        val imdbId = doc.selectFirst("div.imdb_left > a")?.attr("href")?.substringAfter("title/")?.substringBefore("/") ?: ""
        if(id == imdbId) {
            if(season != null && episode != null) {
                doc.select("a.my-button").mapNotNull {
                    val title = it.parent()?.parent()?.previousElementSibling()?.text()?: ""
                    val qualityRegex = """(1080p|720p|480p|2160p|4K|[0-9]*0p)""".toRegex(RegexOption.IGNORE_CASE)
                    val quality = qualityRegex.find(title) ?. groupValues ?. get(1) ?: ""
                    val realSeason = Regex("""(?:Season |S)(\d+)""").find(title) ?. groupValues ?. get(1) ?. toIntOrNull() ?: 300
                    if(season == realSeason) {
                        val doc2 = app.get(it.attr("href")).document
                        val h3 = doc2.select("h3:matches((?i)(episode))").get(episode-1)
                        var source = h3?.nextElementSibling()?.selectFirst("a")?.attr("href") ?: ""
                        loadSourceNameExtractor("W4U", source, "", subtitleCallback, callback, getIndexQuality(quality))
                    }
                    else {
                    }
                }
            }
            else {
                doc.select("a.my-button").mapNotNull {
                    val title = it.parent()?.parent()?.previousElementSibling()?.text()?: ""
                    val qualityRegex = """(1080p|720p|480p|2160p|4K|[0-9]*0p)""".toRegex(RegexOption.IGNORE_CASE)
                    val quality = qualityRegex.find(title) ?. groupValues ?. get(1) ?: ""
                    app.get(it.attr("href")).document.select("h4 > a").mapNotNull {
                        loadSourceNameExtractor("W4U", it.attr("href"), "", subtitleCallback, callback, getIndexQuality(quality))
                    }
                }
            }
        }
    }

    suspend fun invokeVegamovies(
        api: String,
        sourceName: String,
        id: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
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
        val url = "$api/?s=$id"
        app.get(
            url,
            referer = api,
            headers = headers
        ).document.select("article h2 a,article h3 a").amap {
            val res = app.get(
                it.attr("href"),
                referer = api,
                headers = headers
            ).document
            if(season == null) {
                res.select("button.dwd-button").amap {
                    val link = it.parent()?.attr("href") ?: return@amap
                    val doc = app.get(link).document
                    val source = doc.selectFirst("button.btn:matches((?i)(V-Cloud))")
                        ?.parent()
                        ?.attr("href")
                        ?: return@amap
                    loadSourceNameExtractor(sourceName, source, referer = "", subtitleCallback, callback)
                }
            }
            else {
                res.select("h4:matches((?i)(Season $season)), h3:matches((?i)(Season $season))").amap { h4 ->
                    h4.nextElementSibling()?.select("a:matches((?i)(V-Cloud|Single|Episode|G-Direct))")?.amap {
                        val doc = app.get(it.attr("href")).document
                        val epLink = doc.selectFirst("h4:contains(Episodes):contains($episode)")
                            ?.nextElementSibling()
                            ?.selectFirst("a:matches((?i)(V-Cloud))")
                            ?.attr("href")
                            ?: return@amap
                        loadSourceNameExtractor(sourceName, epLink, referer = "", subtitleCallback, callback)
                    }
                }
            }
        }
    }

    suspend fun invokeMoviesdrive(
        title: String? = null,
        season: Int? = null,
        episode: Int? = null,
        year: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val fixTitle = title?.substringBefore("-")?.replace(":", " ")?.replace("&", " ")
        val searchtitle = title?.substringBefore("-").createSlug()
        val url = if (season == null) {
            "$MovieDrive_API/search/$fixTitle $year"
        } else {
            "$MovieDrive_API/search/$fixTitle"
        }
        val res1 =
            app.get(url, interceptor = wpRedisInterceptor).document.select("figure")
                .toString()
        val hrefpattern =
            Regex("""(?i)<a\s+href="([^"]*\b$searchtitle\b[^"]*)\"""").find(res1)?.groupValues?.get(1)
                ?: ""
        val document = app.get(hrefpattern).document
        if (season == null) {
            document.select("h5 > a").amap {
                val href = it.attr("href")
                val server = extractMdrive(href)
                server.amap {
                    loadSourceNameExtractor("MoviesDrive",it, "", subtitleCallback, callback)
                }
            }
        } else {
            val stag = "Season $season|S0$season"
            val sep = "Ep0$episode|Ep$episode"
            val entries = document.select("h5:matches((?i)$stag)")
            entries.amap { entry ->
                val href = entry.nextElementSibling()?.selectFirst("a")?.attr("href") ?: ""
                if (href.isNotBlank()) {
                    val doc = app.get(href).document
                    val fEp = doc.selectFirst("h5:matches((?i)$sep)")
                    val linklist = mutableListOf<String>()
                    val source1 = fEp?.nextElementSibling()?.selectFirst("a")?.attr("href")
                    val source2 = fEp?.nextElementSibling()?.nextElementSibling()?.selectFirst("a")?.attr("href")
                    if (source1 != null) linklist.add(source1)
                    if (source2 != null) linklist.add(source2)
                    linklist.forEach { url ->
                        loadSourceNameExtractor(
                            "MoviesDrive",
                            url,
                            referer = "",
                            subtitleCallback,
                            callback
                        )
                    }
                }
            }
        }
    }

    suspend fun invokeUhdmovies(
        title: String? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit,
        subtitleCallback: (SubtitleFile) -> Unit
    ) {
        val url = app.get("$uhdmoviesAPI/search/$title $year").document
            .select("article div.entry-image a").attr("href")
        val doc = app.get(url).document

        val selector = if (season == null) {
            "div.entry-content p:matches($year)"
        } else {
            "div.entry-content p:matches((?i)(S0?$season|Season 0?$season))"
        }
        val epSelector = if (season == null) {
            "a:matches((?i)(Download))"
        } else {
            "a:matches((?i)(Episode $episode))"
        }

        val links = doc.select(selector).mapNotNull {
            val nextElementSibling = it.nextElementSibling()
            nextElementSibling?.select(epSelector)?.attr("href")
        }

        links.amap {
            if(!it.isNullOrEmpty()) {
                val driveLink = bypassHrefli(it) ?: ""
                loadSourceNameExtractor(
                    "UHDMovies",
                    driveLink,
                    "",
                    subtitleCallback,
                    callback,
                )
            }
        }
    }

    suspend fun invokeTopMovies(
        title: String? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val fixTitle = title?.replace("-", " ")?.substringBefore(":")
        var url = ""
        val searchtitle = title?.replace("-", " ")?.substringBefore(":").createSlug()
        if (season == null) {
            url = "$topmoviesAPI/search/$fixTitle $year"
        } else {
            url = "$topmoviesAPI/search/$fixTitle Season $season $year"
        }
        var res1 =
            app.get(url).document.select("#content_box article")
                .toString()
        val hrefpattern =
            Regex("""(?i)<article[^>]*>\s*<a\s+href="([^"]*$searchtitle[^"]*)\"""").find(res1)?.groupValues?.get(
                1
            )
        val hTag = if (season == null) "h3" else "div.single_post h3"
        val aTag = if (season == null) "Download" else "G-Drive"
        val sTag = if (season == null) "" else "(Season $season)"
        //val media =res.selectFirst("div.post-cards article:has(h2.title.front-view-title:matches((?i)$title.*$match)) a")?.attr("href")
        val res = app.get(
            hrefpattern ?: return,
            headers = mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:125.0) Gecko/20100101 Firefox/125.0"),
            interceptor = wpRedisInterceptor
        ).document
        val entries = if (season == null) {
            res.select("$hTag:matches((?i)$sTag.*(480p|720p|1080p|2160p|4K))")
                .filter { element -> !element.text().contains("Batch/Zip", true) && !element.text().contains("Info:", true) }.reversed()
        } else {
            res.select("$hTag:matches((?i)$sTag.*(480p|720p|1080p|2160p|4K))")
                .filter { element -> !element.text().contains("Batch/Zip", true) || !element.text().contains("720p & 480p", true) || !element.text().contains("Series Info", true)}
        }
        entries.amap {
            val href =
                it.nextElementSibling()?.select("a.maxbutton:contains($aTag)")?.attr("href")
            val selector =
                if (season == null) "a.maxbutton-5:contains(Server)" else "h3 a:matches(Episode $episode)"
            if (href!!.isNotEmpty()) {
                app.get(
                    href ?: "",
                    headers = mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:125.0) Gecko/20100101 Firefox/125.0"),
                    interceptor = wpRedisInterceptor, timeout = 10L
                ).document.selectFirst(selector)
                    ?.attr("href")?.let {
                        val link = bypassHrefli(it).toString()
                        loadSourceNameExtractor("Topmovies", link, referer = "", subtitleCallback, callback)
                    }
            }
        }
    }

    suspend fun invokeMoviesmod(
        id: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        invokeModflix(
            id,
            season,
            episode,
            subtitleCallback,
            callback,
            MoviesmodAPI
        )
    }

    suspend fun invokeModflix(
        id: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
        api: String
    ) {
        var url = ""
        if (season == null) {
            url = "$api/search/$id"
        } else {
            url = "$api/search/$id $season"
        }
        var href = app.get(url).document.selectFirst("#content_box article > a")?.attr("href")
        val hTag = if (season == null) "h4" else "h3"
        val aTag = if (season == null) "Download" else "Episode"
        val sTag = if (season == null) "" else "(S0$season|Season $season)"
        val res = app.get(
            href ?: return,
        ).document
        val entries =
            res.select("div.thecontent $hTag:matches((?i)$sTag.*(720p|1080p|2160p))")
                .filter { element ->
                    !element.text().contains("MoviesMod", true) && !element.text()
                        .contains("1080p", true) || !element.text().contains("720p", true)
                }
        entries.amap { it ->
            var href =
                it.nextElementSibling()?.select("a:contains($aTag)")?.attr("href")
                    ?.substringAfter("=") ?: ""
            href = base64Decode(href)
            val selector =
                if (season == null) "p a.maxbutton" else "h3 a:matches(Episode $episode)"
            if (href.isNotEmpty())
            app.get(
                href,
            ).document.selectFirst(selector)?.let {
                val link = it.attr("href")
                val bypassedLink = bypassHrefli(link).toString()
                loadSourceNameExtractor("Moviesmod", bypassedLink, "", subtitleCallback, callback)
            }
        }
    }

    suspend fun invokeAnizone(
        title: String,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val url = "$anizoneAPI/anime?search=$title"
        val link = app.get(url).document.select("div.truncate > a").firstOrNull {
            it.text() == title
        } ?.attr("href") ?: return
        val document = app.get("$link/$episode").document

        val subtitles = document.select("track").map {
            subtitleCallback.invoke(
                SubtitleFile(
                    it.attr("label"),
                    it.attr("src")
                )
            )
        }

        val source = document.select("media-player").attr("src")
        callback.invoke(
            newExtractorLink(
                "Anizone[Multi Lang]",
                "Anizone[Multi Lang]",
                source,
                type = ExtractorLinkType.M3U8,
            ) {
                this.quality = Qualities.P1080.value
            }
        )
    }

    suspend fun invokeFlixhq(
        title: String,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val type = if (season == null) "Movie" else "TV Series"
        val searchJson = app.get("$CONSUMET_API/movies/flixhq/$title").text
        val searchData = tryParseJson<ConsumetSearch>(searchJson) ?: return
        val id = searchData.results.firstOrNull {
            it.title == title && it.type == type
        }?.id ?: return
        val infoJson = app.get("$CONSUMET_API/movies/flixhq/info?id=$id").text
        val infoData = tryParseJson<ConsumetInfo>(infoJson) ?: return
        val epId = if(season == null) { infoData.episodes.firstOrNull()?.id ?: return }
        else {
            infoData.episodes.firstOrNull { it.number  == episode && it.season == season }?.id ?: return
        }

        val servers = listOf("upcloud", "vidcloud")
        servers.amap { server ->
            val epJson = app.get("$CONSUMET_API/movies/flixhq/watch?episodeId=$epId&mediaId=$id&server=$server").text
            val epData = tryParseJson<ConsumetWatch>(epJson) ?: return@amap
            val referer = epData.headers.Referer ?: ""

            epData.sources.map {
                callback.invoke(
                    newExtractorLink(
                        "Flixhq ${server.uppercase()}",
                        "Flixhq ${server.uppercase()}",
                        it.url,
                        type = if(it.isM3U8) ExtractorLinkType.M3U8 else INFER_TYPE,
                    ) {
                        this.referer = referer
                        this.quality = it.quality.toIntOrNull() ?: Qualities.Unknown.value
                    }
                )
            }

            epData.subtitles.map {
                subtitleCallback.invoke(
                    SubtitleFile(
                        it.lang,
                        it.url
                    )
                )
            }
        }
    }

    private suspend fun invokeHianime(
        url: String? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val id = url?.substringAfterLast("/") ?: return
        val json = app.get("$CONSUMET_API/anime/zoro/info?id=$id").text
        val data = tryParseJson<HiAnime>(json) ?: return
        val epId = if(episode == null) { data.episodes.firstOrNull()?.id ?: return }
        else {
            data.episodes.find { it.number == episode }?.id ?: return
        }
        val isDubbed = if(episode == null) { data.episodes.firstOrNull()?.isDubbed ?: false }
        else { data.episodes.find { it.number == episode }?.isDubbed ?: false }
        val types =  mutableListOf("sub")
        if(isDubbed == true) types.add("dub")
        val servers = mutableListOf("vidstreaming", "vidcloud")
        val headers = mapOf(
            "Referer" to "https://megacloud.club/",
            "Origin" to "https://megacloud.club/"
        )
        types.map { t ->
            servers.map { server ->
                val epJson = app.get("$CONSUMET_API/anime/zoro/watch?episodeId=$epId${'$'}$t&server=$server").text
                val epData = tryParseJson<HiAnimeMedia>(epJson) ?: return@map

                epData.sources.map {
                    M3u8Helper.generateM3u8(
                        "HiAnime ${server.uppercase()} [${t.uppercase()}]",
                        it.url,
                        "https://megacloud.club/",
                        headers = headers
                    ).forEach(callback)
                }

                epData.subtitles.map {
                    subtitleCallback.invoke(
                        SubtitleFile(
                            it.lang,
                            it.url,
                        )
                    )
                }
            }
        }
    }

    suspend fun invokeAllanime(
        name: String,
        year: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val privatereferer = "https://allmanga.to"
        val ephash = "5f1a64b73793cc2234a389cf3a8f93ad82de7043017dd551f38f65b89daa65e0"
        val queryhash = "06327bc10dd682e1ee7e07b6db9c16e9ad2fd56c1b769e47513128cd5c9fc77a"
        var type = ""
        if (episode == null) {
            type = "Movie"
        } else {
            type = "TV"
        }

        val query =
            """$AllanimeAPI?variables={"search":{"types":["$type"],"year":$year,"query":"$name"},"limit":26,"page":1,"translationType":"sub","countryOrigin":"ALL"}&extensions={"persistedQuery":{"version":1,"sha256Hash":"$queryhash"}}"""
        val response =
            app.get(query, referer = privatereferer).parsedSafe<Anichi>()?.data?.shows?.edges
        if (response != null) {
            val id = response.firstOrNull()?.id ?: return
            val langType = listOf("sub", "dub")
            for (i in langType) {
                val epData =
                    """$AllanimeAPI?variables={"showId":"$id","translationType":"$i","episodeString":"${episode ?: 1}"}&extensions={"persistedQuery":{"version":1,"sha256Hash":"$ephash"}}"""
                val eplinks = app.get(epData, referer = privatereferer)
                    .parsedSafe<AnichiEP>()?.data?.episode?.sourceUrls
                eplinks?.amap { source ->
                    safeApiCall {
                        val sourceUrl = source.sourceUrl
                        val downloadUrl = source.downloads?.downloadUrl ?: ""
                        if (downloadUrl.contains("blog.allanime.day")) {
                            if (downloadUrl.isNotEmpty()) {
                                val downloadid = downloadUrl.substringAfter("id=")
                                val sourcename = downloadUrl.getHost()
                                app.get("https://allanime.day/apivtwo/clock.json?id=$downloadid")
                                    .parsedSafe<AnichiDownload>()?.links?.amap {
                                    val href = it.link
                                    loadNameExtractor(
                                        "Allanime [${i.uppercase()}] [$sourcename]",
                                        href,
                                        "",
                                        subtitleCallback,
                                        callback,
                                        Qualities.P1080.value
                                    )
                                }
                            } else {
                                Log.d("Error:", "Not Found")
                            }
                        } else {
                            if (sourceUrl.startsWith("http")) {
                                if (sourceUrl.contains("embtaku.pro")) {
                                    val iv = "3134003223491201"
                                    val secretKey = "37911490979715163134003223491201"
                                    val secretDecryptKey = "54674138327930866480207815084989"
                                    GogoHelper.extractVidstream(
                                        sourceUrl,
                                        "Allanime [${i.uppercase()}] [Vidstreaming]",
                                        callback,
                                        iv,
                                        secretKey,
                                        secretDecryptKey,
                                        isUsingAdaptiveKeys = false,
                                        isUsingAdaptiveData = true
                                    )
                                }
                                val sourcename = sourceUrl.getHost()
                                loadCustomExtractor(
                                    "Allanime [${i.uppercase()}] [$sourcename]",
                                    sourceUrl
                                        ?: "",
                                    "",
                                    subtitleCallback,
                                    callback,
                                )
                            } else {
                                Log.d("Error:", "Not Found")
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun invokePlayer4U(
        title: String? = null,
        season: Int? = null,
        episode: Int? = null,
        year: Int? = null,
        callback: (ExtractorLink) -> Unit
    ) {
        val fixTitle = title?.createPlayerSlug().orEmpty()
        val fixQuery = (season?.let { "$fixTitle S${"%02d".format(it)}E${"%02d".format(episode)}" } ?: "$fixTitle $year").replace(" ","+") // It is necessary for query with year otherwise it will give wrong movie
        val allLinks = HashSet<Player4uLinkData>()

        var page = 0
        var nextPageExists: Boolean = true

        do {
            val url = if(page == 0) {"$Player4uApi/embed?key=$fixQuery"} else {"$Player4uApi/embed?key=$fixQuery&page=$page"}
            try {
                var document = app.get(url, timeout = 20).document
                allLinks.addAll(
                    document.select(".playbtnx").map {
                        Player4uLinkData(name = it.text(), url = it.attr("onclick"))
                    }
                )

                if(page == 0 && season == null && allLinks.size == 0)
                {
                    document = app.get("$Player4uApi/embed?key=${fixTitle.replace(" ","+")}", timeout = 20).document
                    allLinks.addAll(
                        document.select(".playbtnx").map {
                            Player4uLinkData(name = it.text(), url = it.attr("onclick"))
                        }
                    )
                    break
                }

                nextPageExists = document.select("div a").any { it.text().contains("Next", true) }
            } catch (e: Exception) {}
            page++
        } while (nextPageExists && page <= 4)

        allLinks.distinctBy { it.name }.forEach { link ->
            try {

                val splitName = link.name.split("|").reversed()
                val firstPart = splitName.getOrNull(0)?.trim().orEmpty()
                val nameFormatted = "Player4U ${if(firstPart.isNullOrEmpty()) { "" } else { "{$firstPart}" }}"

                val qualityFromName = Regex("""(\d{3,4}p|4K|CAM|HQ|HD|SD|WEBRip|DVDRip|BluRay|HDRip|TVRip|HDTC|PREDVD)""", RegexOption.IGNORE_CASE)
                    .find(nameFormatted)?.value?.uppercase() ?: "UNKNOWN"


                val selectedQuality = getPlayer4UQuality(qualityFromName)

                val subLink = "go\\('(.*)'\\)".toRegex().find(link.url)?.groups?.get(1)?.value ?: return@forEach
                val iframeSource = app.get("$Player4uApi$subLink", timeout = 10, referer = Player4uApi)
                    .document.select("iframe").attr("src")
                getPlayer4uUrl(
                    nameFormatted,
                    selectedQuality,
                    "https://uqloads.xyz/e/$iframeSource",
                    Player4uApi,
                    callback
                )

            } catch (_: Exception) { }
        }
    }

    suspend fun invokePrimeWire(
        imdbId: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val url = if (season == null) {
            "$Primewire/embed/movie?imdb=$imdbId"
        } else {
            "$Primewire/embed/tv?imdb=$imdbId&season=$season&episode=$episode"
        }
        val doc = app.get(url, timeout = 10).document
        val userData = doc.select("#user-data")
        var decryptedLinks = decryptLinks(userData.attr("v"))
        for (link in decryptedLinks) {
            val url = "$Primewire/links/go/$link"
            val oUrl = app.get(url,timeout = 10)
            loadSourceNameExtractor(
                "Primewire",
                oUrl.url,
                "",
                subtitleCallback,
                callback
            )
        }
    }

    suspend fun invokeThepiratebay(
        imdbId: String? =null,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit
    ) {
        try {
            val url = if(season == null) {
                "$ThePirateBayApi/stream/movie/$imdbId.json"
            }
            else {
                "$ThePirateBayApi/stream/series/$imdbId:$season:$episode.json"
            }
            val res = app.get(url, timeout = 10).parsedSafe<TBPResponse>()
            for(stream in res?.streams!!)
            {
                val magnetLink = generateMagnetLink(TRACKER_LIST_URL,stream.infoHash)
                callback.invoke(
                    newExtractorLink(
                        "ThePirateBay",
                        "ThePirateBay [${stream.title}]",
                        magnetLink,
                        ExtractorLinkType.MAGNET
                    ) {
                        this.quality = getIndexQuality(stream.title)
                    }
                )
            }
        } catch (_: Exception) { }
    }


    suspend fun invokeVidJoy(
        imdbId: Int? =null,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit
    ) {
        try {
            val listSrc = listOf("Camelot","Atlantis","Babylon","NYC")
            val link = if(season != null)
            {
                "$VidJoyApi/embed/api/fetch2/$imdbId/$season/$episode"
            }
            else
            {
                "$VidJoyApi/embed/api/fetch2/$imdbId"
            }
            listSrc.forEach { src ->
                if(src == "Camelot")
                {
                    (0..4).forEach { i ->
                        try {
                            val finalLink = "$link/?srName=Camelot&sr=$i"
                            val encrptedText = app.get(finalLink).text;
                            val decryptedJson = decryptOpenSSLAES(encrptedText,"b91eeba6b7c848828ba8b84d44fa38a88affef23ec270ea4cd904810280b34fa")
                            if (decryptedJson != "> - <") {
                                val vidjoyResponse = tryParseJson<VidjoyResponse>(decryptedJson)
                                if (vidjoyResponse != null) {
                                    for(url in vidjoyResponse.url)
                                    {
                                        callback.invoke(
                                            newExtractorLink(
                                                "Vidjoy ${url.lang}",
                                                "Vidjoy ${url.lang}",
                                                url = url.link,
                                                type =  if(url.link.contains(".mp4")) ExtractorLinkType.VIDEO else INFER_TYPE
                                            ) {
                                                this.quality = getQualityFromName(url.resulation)
                                            }
                                        )
                                    }
                                }
                            }
                        } catch (e: Exception) { }
                    }
                }
                else
                {
                    try {
                        val finalLink = "$link/?srName=$src"
                        val encrptedText = app.get(finalLink).text;
                        val decryptedJson = decryptOpenSSLAES(encrptedText,"b91eeba6b7c848828ba8b84d44fa38a88affef23ec270ea4cd904810280b34fa")
                        if (decryptedJson != "> - <") {
                            val vidjoyResponse = tryParseJson<VidjoyResponse>(decryptedJson)
                            if (vidjoyResponse != null) {
                                for(url in vidjoyResponse.url)
                                {

                                    callback.invoke(
                                        newExtractorLink(
                                            "Vidjoy ${url.lang}",
                                            "Vidjoy ${url.lang}",
                                            url = url.link,
                                            type =  if(url.link.contains(".m3u8")) ExtractorLinkType.M3U8 else INFER_TYPE
                                        ) {
                                            this.quality = getQualityFromName(url.resulation)
                                        }
                                    )
                                }
                            }
                        }
                    } catch (e: Exception) { }
                }
            }

        } catch (_: Exception) { }
    }
}
