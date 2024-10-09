package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.network.CloudflareKiller
import android.util.Log
import android.util.Base64
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.APIHolder.unixTime
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.fasterxml.jackson.annotation.JsonProperty

object CineStreamExtractors : CineStreamProvider() {

    suspend fun invokePrimeVideo(
        title: String,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ) {
        val cookie = getNFCookies() ?: return
        val cookies = mapOf(
            "t_hash_t" to cookie,
            "ott" to "pv",
            "hd" to "on"
        )
        val headers = mapOf("X-Requested-With" to "XMLHttpRequest")
        val url = "$netflixAPI/pv/search.php?s=$title&t=${APIHolder.unixTime}"
        val data = app.get(url, headers = headers, cookies = cookies).parsedSafe<NfSearchData>()
        val netflixId = data ?.searchResult ?.firstOrNull { it.t.equals(title.trim(), ignoreCase = true) }?.id

        val (nfTitle, id) = app.get(
            "$netflixAPI/pv/post.php?id=${netflixId ?: return}&t=${APIHolder.unixTime}",
            headers = headers,
            cookies = cookies,
            referer = "$netflixAPI/"
        ).parsedSafe<NetflixResponse>().let { media ->
            if (season == null && year.toString() == media?.year.toString()) {
                media?.title to netflixId
            } else if(year.toString() == media?.year.toString()) {
                val seasonId = media?.season?.find { it.s == "$season" }?.id
                val episodeId =
                    app.get(
                        "$netflixAPI/pv/episodes.php?s=${seasonId}&series=$netflixId&t=${APIHolder.unixTime}",
                        headers = headers,
                        cookies = cookies,
                        referer = "$netflixAPI/"
                    ).parsedSafe<NetflixResponse>()?.episodes?.find { it.ep == "E$episode" }?.id
                media?.title to episodeId
            }
            else {
                null to null
            }
        }

        app.get(
            "$netflixAPI/pv/playlist.php?id=${id ?: return}&t=${nfTitle ?: return}&tm=${APIHolder.unixTime}",
            headers = headers,
            cookies = cookies,
            referer = "$netflixAPI/"
        ).text.let {
            tryParseJson<ArrayList<NetflixResponse>>(it)
        }?.firstOrNull()?.sources?.map {
            callback.invoke(
                ExtractorLink(
                    "PrimeVideo",
                    "PrimeVideo",
                    "$netflixAPI/${it.file}",
                    "$netflixAPI/",
                    getQualityFromName(it.file?.substringAfter("q=")?.substringBefore("&in")),
                    INFER_TYPE,
                    headers = mapOf("Cookie" to "hd=on")
                )
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
        val cookie = getNFCookies() ?: return
        val cookies = mapOf(
            "t_hash_t" to cookie,
            "hd" to "on"
        )
        val headers = mapOf("X-Requested-With" to "XMLHttpRequest")
        val url = "$netflixAPI/search.php?s=$title&t=${APIHolder.unixTime}"
        val data = app.get(url, headers = headers, cookies = cookies).parsedSafe<NfSearchData>()
        val netflixId = data ?.searchResult ?.firstOrNull { it.t.equals(title.trim(), ignoreCase = true) }?.id

        val (nfTitle, id) = app.get(
            "$netflixAPI/post.php?id=${netflixId ?: return}&t=${APIHolder.unixTime}",
            headers = headers,
            cookies = cookies,
            referer = "$netflixAPI/"
        ).parsedSafe<NetflixResponse>().let { media ->
            if (season == null && year.toString() == media?.year.toString()) {
                media?.title to netflixId
            } else if(year.toString() == media?.year.toString()) {
                val seasonId = media?.season?.find { it.s == "$season" }?.id
                val episodeId =
                    app.get(
                        "$netflixAPI/episodes.php?s=${seasonId}&series=$netflixId&t=${APIHolder.unixTime}",
                        headers = headers,
                        cookies = cookies,
                        referer = "$netflixAPI/"
                    ).parsedSafe<NetflixResponse>()?.episodes?.find { it.ep == "E$episode" }?.id
                media?.title to episodeId
            }
            else {
                null to null
            }
        }

        app.get(
            "$netflixAPI/playlist.php?id=${id ?: return}&t=${nfTitle ?: return}&tm=${APIHolder.unixTime}",
            headers = headers,
            cookies = cookies,
            referer = "$netflixAPI/"
        ).text.let {
            tryParseJson<ArrayList<NetflixResponse>>(it)
        }?.firstOrNull()?.sources?.map {
            callback.invoke(
                ExtractorLink(
                    "Netflix",
                    "Netflix",
                    "$netflixAPI/${it.file}",
                    "$netflixAPI/",
                    getQualityFromName(it.file?.substringAfter("q=")?.substringBefore("&in")),
                    INFER_TYPE,
                    headers = mapOf("Cookie" to "hd=on")
                )
            )
        }
    }

    suspend fun getNFCookies(): String? {
        val json = app.get("https://raw.githubusercontent.com/SaurabhKaperwan/Utils/main/NF_Cookie.json").text
        val data = parseJson<NfCookie>(json)
        return data.cookie
    }

    data class NfSearchData(
        val head: String,
        val searchResult: List<NfSearchResult>,
        val type: Int
    )
    data class NfSearchResult(
        val id: String,
        val t: String
    )

    data class NfCookie(
        @JsonProperty("cookie") val cookie: String
    )

    data class NetflixSources(
        @JsonProperty("file") val file: String? = null,
        @JsonProperty("label") val label: String? = null,
    )
    data class NetflixEpisodes(
        @JsonProperty("id") val id: String? = null,
        @JsonProperty("t") val t: String? = null,
        @JsonProperty("s") val s: String? = null,
        @JsonProperty("ep") val ep: String? = null,
    )
    data class NetflixSeason(
        @JsonProperty("s") val s: String? = null,
        @JsonProperty("id") val id: String? = null,
    )
    data class NetflixResponse(
        @JsonProperty("title") val title: String? = null,
        @JsonProperty("year") val year : String? = null,
        @JsonProperty("season") val season: ArrayList<NetflixSeason>? = arrayListOf(),
        @JsonProperty("episodes") val episodes: ArrayList<NetflixEpisodes>? = arrayListOf(),
        @JsonProperty("sources") val sources: ArrayList<NetflixSources>? = arrayListOf(),
    )

    suspend fun invokeVadaPav(
        title: String,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val mirrors = listOf(
            "https://vadapav.mov",
            "https://dl1.vadapav.mov",
            "https://dl2.vadapav.mov",
            "https://dl3.vadapav.mov",
        )

        val url = if(season != null && episode != null) "$VadapavAPI/s/$title" else "$VadapavAPI/s/$title ($year)"
        val document = app.get(url).document
        val result = document.selectFirst("div.directory > ul > li > div > a")
        val text = result.text().trim()
        val href = VadapavAPI + result.attr("href")
        if(season != null && episode != null && title.equals(text, true)) {
            val doc = app.get(href).document
            val seasonLink = VadapavAPI + doc.selectFirst("div.directory > ul > li > div > a.directory-entry:matches((?i)(Season 0${season}|Season ${season}))").attr("href")
            val seasonDoc = app.get(seasonLink).document
            seasonDoc.select("div.directory > ul > li > div > a.file-entry:matches((?i)(Episode 0${episode}|Episode ${episode}|EP0${episode}|EP${episode}|EP 0${episode}|EP ${episode}|E0${episode}|E${episode}|E 0${episode}|E ${episode}))")
            .forEach {
                if(it.text().contains(".mkv", true) || it.text().contains(".mp4", true)) {
                    for((index, mirror) in mirrors.withIndex()) {
                        callback.invoke(
                            ExtractorLink(
                                "VadaPav" + " ${index+1}",
                                "VadaPav" + " ${index+1}",
                                mirror + it.attr("href"),
                                referer = "",
                                quality = Qualities.P1080.value,
                            )
                        )
                    }
                }
            }
        }
        else {
            val doc = app.get(href).document
            doc.select("div.directory > ul > li > div > a.file-entry:matches((?i)(.mkv|.mp4))").forEach {
                for((index, mirror) in mirrors.withIndex()) {
                    callback.invoke(
                        ExtractorLink(
                            "VadaPav" + " ${index+1}",
                            "VadaPav" + " ${index+1}",
                            mirror + it.attr("href"),
                            referer = "",
                            quality = Qualities.P1080.value,
                        )
                    )
                }
            }
        }
    }


    suspend fun invokeFull4Movies(
        title: String,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val url = if(season == null) "$Full4MoviesAPI/?s=$title $year" else "$Full4MoviesAPI/?s=$title season $season $year"
        val document = app.get(url).document
        val text = document.selectFirst("div.content > h2 > a")?.text().toString()
        val href = document.selectFirst("div.content > h2 > a")?.attr("href").toString()
        if (
            text.contains(title, true) &&
            year.let { text.contains("$it") } == true &&
            (season == null || season.let { text.contains("Season $it", true) } == true)
        ) {
            val doc2 = app.get(href).document
            val link = if(season == null) {
                Regex("""<a\s+class="myButton"\s+href="([^"]+)".*?>Watch Online 1<\/a>""").find(doc2.html())?.groupValues?.get(1) ?: ""

            } else {
                val urls = Regex("""<a[^>]*href="([^"]*)"[^>]*>(?:WCH|Watch)<\/a>""").findAll(doc2.html())
                urls.elementAtOrNull(episode?.minus(1) ?: 0)?.groupValues?.get(1) ?: ""
            }
            val doc = app.get(fixUrl(link)).document
            val source = doc.selectFirst("iframe").attr("src") ?: ""
            loadAddSourceExtractor("Full4Movies",source, referer = link, subtitleCallback, callback)
        }

    }

    suspend fun invokeRogmovies(
        title: String? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        invokeWpredis(
            title,
            year,
            season,
            episode,
            subtitleCallback,
            callback,
            rogMoviesAPI
        )
    }
    suspend fun invokeVegamovies(
        title: String? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        invokeWpredis(
            title,
            year,
            season,
            episode,
            subtitleCallback,
            callback,
            vegaMoviesAPI
        )
    }

    private suspend fun invokeWpredis(
        title: String? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
        api: String
    ) {
        val (seasonSlug, episodeSlug) = getEpisodeSlug(season, episode)
        val cfInterceptor = CloudflareKiller()
        val fixtitle = title?.substringBefore("-")?.substringBefore(":")?.replace("&", " ")
        val url = if (season == null) {
            "$api/search/$fixtitle $year"
        } else {
            "$api/search/$fixtitle season $season $year"
        }
        val domain= api.substringAfter("//").substringBefore(".")
        app.get(url, interceptor = cfInterceptor).document.select("#main-content article")
            .filter { element ->
                element.text().contains(
                    fixtitle.toString(), true
                )
            }
            .amap {
                val hrefpattern =
                    Regex("""(?i)<a\s+href="([^"]+)"[^>]*?>[^<]*?\b($fixtitle)\b[^<]*?""").find(
                        it.toString()
                    )?.groupValues?.get(1)
                if (hrefpattern!=null) {
                    val res = hrefpattern.let { app.get(it).document }
                    val hTag = if (season == null) "h5" else "h3,h5"
                    val aTag =
                        if (season == null) "Download Now" else "V-Cloud,Download Now,G-Direct"
                    val sTag = if (season == null) "" else "(Season $season|S$seasonSlug)"
                    val entries =
                        res.select("div.entry-content > $hTag:matches((?i)$sTag.*(720p|1080p|2160p))")
                            .filter { element ->
                                !element.text().contains("Series", true) &&
                                        !element.text().contains("Zip", true) &&
                                        !element.text().contains("[Complete]", true) &&
                                        !element.text().contains("480p, 720p, 1080p", true) &&
                                        !element.text().contains(domain, true) &&
                                        element.text().matches("(?i).*($sTag).*".toRegex())
                            }
                    entries.amap { it ->
                        val tags =
                            """(?:480p|720p|1080p|2160p)(.*)""".toRegex().find(it.text())?.groupValues?.get(1)
                                ?.trim()
                        val tagList = aTag.split(",")
                        val href = it.nextElementSibling()?.select("a")?.filter { anchor ->
                            tagList.any { tag ->
                                anchor.text().contains(tag.trim(), true)
                            }
                        }?.map { anchor ->
                            anchor.attr("href")
                        } ?: emptyList()
                        val selector =
                            if (season == null) "p a:matches(V-Cloud|G-Direct)" else "h4:matches(0?$episode) ~ p a:matches(V-Cloud|G-Direct)"
                        if (href.isNotEmpty()) {
                            href.amap { url ->
                            if (season==null)
                            {
                                app.get(
                                    url, interceptor = wpRedisInterceptor
                                ).document.select("div.entry-content > $selector").map { sources ->
                                    val server = sources.attr("href")
                                    loadCustomTagExtractor(
                                        tags,
                                        server,
                                        "$api/",
                                        subtitleCallback,
                                        callback,
                                        getIndexQuality(it.text())
                                    )
                                }
                            }
                            else
                            {
                                app.get(
                                    url, interceptor = wpRedisInterceptor
                                ).document.select("div.entry-content > $selector").first()?.let { sources ->
                                    val server = sources.attr("href")
                                    loadCustomTagExtractor(
                                        tags,
                                        server,
                                        "$api/",
                                        subtitleCallback,
                                        callback,
                                        getIndexQuality(it.text())
                                    )
                                }
                             }
                            }
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
            Regex("""(?i)<a\s+href="([^"]*\b$searchtitle\b[^"]*)"""").find(res1)?.groupValues?.get(1)
                ?: ""
        val document = app.get(hrefpattern).document
        if (season == null) {
            document.select("h5 > a").amap {
                val href = it.attr("href")
                val server = extractMdrive(href)
                server.amap {
                    loadExtractor(it, referer = "", subtitleCallback, callback)
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
                    val fEp = doc.selectFirst("h5:matches((?i)$sep)")?.toString()
                    if (fEp.isNullOrEmpty()) {
                        val furl = doc.select("h5 a:contains(HubCloud)").attr("href")
                        loadExtractor(furl, referer = "", subtitleCallback, callback)
                    } else
                        doc.selectFirst("h5:matches((?i)$sep)")?.let { epElement ->
                            val linklist = mutableListOf<String>()
                            val firstHubCloudH5 = epElement.nextElementSibling()
                            val secondHubCloudH5 = firstHubCloudH5?.nextElementSibling()
                            val firstLink = secondHubCloudH5?.selectFirst("a")?.attr("href")
                            val secondLink = secondHubCloudH5?.selectFirst("a")?.attr("href")
                            if (firstLink != null) linklist.add(firstLink)
                            if (secondLink != null) linklist.add(secondLink)
                            linklist.forEach { url ->
                                loadAddSourceExtractor(
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
            Regex("""(?i)<article[^>]*>\s*<a\s+href="([^"]*$searchtitle[^"]*)"""").find(res1)?.groupValues?.get(
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
                        loadAddSourceExtractor("Topmovies", link, referer = "", subtitleCallback, callback)
                    }
            }
        }
    }

    suspend fun invokeMoviesmod(
        title: String? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        invokeModflix(
            title,
            year,
            season,
            episode,
            subtitleCallback,
            callback,
            MoviesmodAPI
        )
    }

    suspend fun invokeModflix(
        title: String? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
        api: String
    ) {
        val fixTitle = title?.replace("-", " ")?.replace(":", " ")?.replace("&", " ")
        var url = ""
        val searchtitle =
            title?.replace("-", " ")?.replace(":", " ")?.replace("&", " ").createSlug()
        if (season == null) {
            url = "$api/search/$fixTitle"
        } else {
            url = "$api/search/$fixTitle $season"
        }
        var res1 =
            app.get(url, interceptor = wpRedisInterceptor).document.select("#content_box article")
                .toString()
        val hrefpattern =
            Regex("""(?i)<article[^>]*>\s*<a\s+href="([^"]*$searchtitle[^"]*)"""").find(res1)?.groupValues?.get(1)
        val hTag = if (season == null) "h4" else "h3"
        val aTag = if (season == null) "Download" else "Episode"
        val sTag = if (season == null) "" else "(S0$season|Season $season)"
        val res = app.get(
            hrefpattern ?: return,
            headers = mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:125.0) Gecko/20100101 Firefox/125.0"),
            interceptor = wpRedisInterceptor
        ).document
        val entries =
            res.select("div.thecontent $hTag:matches((?i)$sTag.*(720p|1080p|2160p))")
                .filter { element ->
                    !element.text().contains("MoviesMod", true) && !element.text()
                        .contains("1080p", true) || !element.text().contains("720p", true)
                }
        entries.amap { it ->
            val tags =
                """(?:480p|720p|1080p|2160p)(.*)""".toRegex().find(it.text())?.groupValues?.get(1)
                    ?.trim()
            val quality =
                """480p|720p|1080p|2160p""".toRegex().find(it.text())?.groupValues?.get(0)
                    ?.trim()
            var href =
                it.nextElementSibling()?.select("a:contains($aTag)")?.attr("href")
                    ?.substringAfter("=") ?: ""
            href = base64Decode(href)
            val selector =
                if (season == null) "p a.maxbutton" else "h3 a:matches(Episode $episode)"
            if (href.isNotEmpty())
            app.get(
                href,
                headers = mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:125.0) Gecko/20100101 Firefox/125.0")
            ).document.selectFirst(selector)?.let {
                val link = it.attr("href")
                if (link.contains("driveseed.org"))
                {
                    val file=app.get(link).toString().substringAfter("replace(\"").substringBefore("\")")
                    val domain= getBaseUrl(link)
                    val server="$domain$file"
                    loadAddSourceExtractor("Moviesmod", server, "", subtitleCallback, callback)
                }
                val server = bypassHrefli(link) ?: ""
                if (server.isNotEmpty()) {
                    loadAddSourceExtractor("Moviesmod", server, "", subtitleCallback, callback)
                }
            }
        }
    }
}
