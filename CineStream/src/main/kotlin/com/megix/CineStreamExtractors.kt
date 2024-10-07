package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.network.CloudflareKiller
import android.util.Log
import android.util.Base64

object CineStreamExtractors : CineStreamProvider() {

    suspend fun invokeFull4Movies(
        title: String? = null,
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
            text.contains(title.toString()) == true &&
            year.let { text.contains("$it") } == true &&
            (season == null || season.let { text.contains("Season $it") } == true)
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
        val domain = api.substringAfter("//").substringBefore(".")
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
                    res.select("div.entry-content > $hTag:matches((?i)$sTag.*(480p|720p|1080p|2160p))")
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
                    val href = it.nextElementSibling()?.select("a")?.find { anchor ->
                        tagList.any { tag ->
                            anchor.text().contains(tag.trim(), true)
                        }
                    }?.attr("href") ?: ""
                    val selector =
                        if (season == null) "p a:matches(V-Cloud|G-Direct)" else "h4:matches(0?$episode) ~ p a:matches(V-Cloud|G-Direct)"
                    if (href.isNotEmpty()) {
                        app.get(
                            href, interceptor = wpRedisInterceptor
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
            Regex("""(?i)<article[^>]*>\s*<a\s+href="([^"]*$searchtitle[^"]*)"""").find(res1)?.groupValues?.get(
                1
            )
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
