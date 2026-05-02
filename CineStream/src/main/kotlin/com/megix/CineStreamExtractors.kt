package com.megix

// Cloudstream Core & Utils
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.mvvm.safeApiCall
import com.lagradost.cloudstream3.network.CloudflareKiller
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.nicehttp.NiceResponse
import com.lagradost.api.Log

// Gson & Jackson
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken

// Org JSON & Jsoup
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup

// Java Security, IO, & Encoding
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.SecureRandom

// Java Net
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import android.net.Uri

import com.megix.settings.Settings

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object CineStreamExtractors {

    private val cfKiller by lazy { CloudflareKiller() }
    private val globalGson by lazy { Gson() }
    private val cfMutex = Mutex()

    suspend fun invokeAllSources(
        res: AllLoadLinksData,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val stremioMap = getDynamicStremioMap(res.imdbId, res.season, res.episode, subtitleCallback, callback)

        val executionList = Settings.activeProviderOrder.mapNotNull { key ->
            ProviderRegistry.builtInProviders.find { it.key == key }?.executeStandard?.let { action ->
                suspend { this.action(res, subtitleCallback, callback) }
            } ?: stremioMap[key]
        }

        runLimitedAsync(concurrency = Settings.getConcurrency(), *executionList.toTypedArray())
    }

    suspend fun invokeAllAnimeSources(
        res: AllLoadLinksData,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val stremioMap = getDynamicStremioMap(res.imdbId, res.imdbSeason, res.imdbEpisode, subtitleCallback, callback)

        val executionList = Settings.activeProviderOrder.mapNotNull { key ->
            ProviderRegistry.builtInProviders.find { it.key == key }?.executeAnime?.let { action ->
                suspend { this.action(res, subtitleCallback, callback) }
            } ?: stremioMap[key]
        }

        runLimitedAsync(concurrency = Settings.getConcurrency(), *executionList.toTypedArray())
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
        val mal_response = app.get("$malsyncAPI/mal/anime/${malId ?: return}").parsedSafe<MALSyncResponses>()

        Log.d("Malsync", "mal_response: $mal_response")

        val title = mal_response?.title
        val malsync = mal_response?.sites

        val animepaheUrl = malsync?.animepahe?.values?.firstNotNullOfOrNull {
            (it as? Map<*, *>)?.get("url") as? String
        }

        val animepaheTitle = malsync?.animepahe?.values?.firstNotNullOfOrNull {
            (it as? Map<*, *>)?.get("title") as? String
        }

        val animekaiUrl = malsync?.animekai?.values?.firstNotNullOfOrNull {
            (it as? Map<*, *>)?.get("url") as? String
        }

        // Package the API results for the registry
        val malData = MalSyncData(title, animepaheUrl, aniId, episode, year, origin, animepaheTitle, animekaiUrl)

        Log.d("Malsync", "malData: $malData")

        val executionList = Settings.activeProviderOrder.mapNotNull { key ->
            ProviderRegistry.builtInProviders.find { it.key == key }?.executeMalSync?.let { action ->
                suspend { this.action(malData, subtitleCallback, callback) }
            }
        }

        runLimitedAsync(concurrency = Settings.getConcurrency(), *executionList.toTypedArray())
    }

    private fun getDynamicStremioMap(
        imdbId: String?,
        season: Int?,
        episode: Int?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Map<String, suspend () -> Unit> {
        return Settings.getStremioAddons().associate { addon ->
            val key = Settings.stremioAddonKey(addon.name)
            key to suspend {
                when (addon.type) {
                    Settings.AddonType.SUBTITLE -> invokeStremioSubtitlesGlobal(addon.name, addon.url, imdbId, season, episode, subtitleCallback)
                    Settings.AddonType.TORRENT -> invokeStremioTorrentsGlobal(addon.name, addon.url, imdbId, season, episode, callback)
                    Settings.AddonType.HTTPS, Settings.AddonType.DEBRID -> invokeStreamioStreamsGlobal(addon.name, addon.url, imdbId, season, episode, subtitleCallback, callback)
                }
            }
        }
    }

    private fun isCloudflarePage(response: NiceResponse): Boolean {
        val server = response.headers["Server"] ?: ""
        return server.contains("cloudflare", true) && response.code in listOf(403, 503)
    }

    suspend fun cfGet(url: String, headers: Map<String, String> = emptyMap(), allowRedirects: Boolean = true): NiceResponse {
        val response = app.get(url, headers = headers, allowRedirects = allowRedirects)
        return if (isCloudflarePage(response)) {
            cfMutex.withLock {
                val retryResponse = app.get(url, headers = headers, interceptor = cfKiller, allowRedirects = allowRedirects)
                if (isCloudflarePage(retryResponse)) {
                    cfKiller.savedCookies.clear()
                    app.get(url, headers = headers, interceptor = cfKiller, allowRedirects = allowRedirects)
                } else {
                    retryResponse
                }
            }
        } else {
            response
        }
    }

    suspend fun invokeShowbox(
        tmdbId: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val url = if(season == null) {
            "$femBoxAPI/movie/$tmdbId?ui=${Settings.getShowboxToken() ?: return}"
        } else {
            "$femBoxAPI/tv/$tmdbId/$season/$episode?ui=${Settings.getShowboxToken() ?: return}"
        }

        val response = app.get(url).parsedSafe<ShowboxResponse>() ?: return

        response.sources.forEach { source ->
            val isM3u8 = source.url.contains(".m3u8")

            callback.invoke(
                newExtractorLink(
                    "Showbox",
                    "ShowBox" + (source.size?.let { " [$it]" } ?: ""),
                    source.url,
                    if(isM3u8) ExtractorLinkType.M3U8 else  ExtractorLinkType.VIDEO
                ) {
                    this.quality = when (source.quality.uppercase()) {
                        "4K", "ORG" -> Qualities.P2160.value
                        "1080P"     -> Qualities.P1080.value
                        "720P"      -> Qualities.P720.value
                        "480P"      -> Qualities.P480.value
                        "360P"      -> Qualities.P360.value
                        else        -> Qualities.Unknown.value
                    }
                }
            )

        }

        response.subtitles.forEach { sub ->
            subtitleCallback.invoke(
                newSubtitleFile(
                    sub.language,
                    sub.url
                )
            )
        }
    }

    suspend fun invokeAkwam(
        imdbId: String? = null,
        title: String? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {

        suspend fun getLink(url: String) : String? {
            val link = app.get(url, referer = "$akwamAPI/")
            .document
            .selectFirst("a.link-download")
            ?.attr("href")
            ?: return null

            val link2 = app.get(link, referer = "$akwamAPI/")
                .document
                .selectFirst("a.download-link")
                ?.attr("href")
                ?: return null

            val source = app.get(link2, referer = "$akwamAPI/")
                .document
                .selectFirst("a.link")
                ?.attr("href")
                ?: return null

            return source
        }

        if(imdbId == null || title == null || year == null) return

        val type = if(season == null) "movie" else "series"
        val searchUrl = "$akwamAPI/search?q=${URLEncoder.encode(title, "UTF-8")}&section=$type&year=$year&rating=0&formats=0&quality=0"
        val url = app.get(searchUrl, referer = "$akwamAPI/")
            .document
            .selectFirst("a.box")
            ?.attr("href")
            ?: return
        val document = app.get(url, referer = "$akwamAPI/").document
        val imdb = document.selectFirst("a[href*='imdb.com']")
            ?.attr("href")
            ?.substringAfter("title/")
            ?.substringBefore("/")
            ?: return

        if(imdbId != imdb) return

        val source = if(season == null) {
            getLink(url)
        } else {
            val episodeLinks = document.select("h2 > a.text-white")

            val match = episodeLinks.find { element ->
                val text = element.text()
                val regex = "(?:حلقة|Episode)\\s+$episode(?!\\d)".toRegex(RegexOption.IGNORE_CASE)
                regex.containsMatchIn(text)
            }

            if(match == null) return
            getLink(match.attr("href"))
        }

        if(source == null) return

        callback.invoke(
            newExtractorLink(
                "Akwam 🇸🇦",
                "Akwam 🇸🇦",
                source,
                ExtractorLinkType.VIDEO
            ) {
                this.quality = Qualities.P720.value
                this.referer = "$akwamAPI/"
                this.headers = mapOf(
                    "Connection" to "keep-alive",
                    "Referer" to "$akwamAPI/",
                    "User-Agent" to USER_AGENT,
                )
            }
        )
    }

    suspend fun invokeLevidia(
        title: String? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        if(title == null || year == null) return

        val safeTitle = URLEncoder.encode(title, "utf-8")

        val url = if(season == null) {
            "$levidiaAPI/search.php?q=$safeTitle+$year&v=movies"
        } else {
            "$levidiaAPI/search.php?q=$safeTitle+$year&v=episodes"
        }

        val res = app.get(url)
        val sessionId = res.cookies["PHPSESSID"] ?: return
        val regex = Regex("""_3chk\(['"]([^'"]+)['"]\s*,\s*['"]([^'"]+)['"]\)""")
        val match = regex.find(res.text)

        if(match == null) return

        val value1 = match.groupValues[1]
        val value2 = match.groupValues[2]
        val document = res.document

        val headers = mapOf(
            "User-Agent" to USER_AGENT,
            "Referer" to "$levidiaAPI/",
            "Cookie" to "PHPSESSID=$sessionId;$value1=$value2"
        )

        val href = document.select("li.mlist div.mainlink a").firstNotNullOfOrNull { aTag ->
            val parsedTitle = aTag.selectFirst("strong")?.text()?.trim()
            ?: return@firstNotNullOfOrNull null
            val parsedYear = aTag.ownText().replace(Regex("""[^\d]"""), "").toIntOrNull()

            if (parsedTitle.equals(title, ignoreCase = true) && parsedYear == year) {
                aTag.attr("href")
            } else {
                null
            }
        } ?: return

        val doc = app.get(href, headers = headers).document

        if(season == null) {
            doc.select("a.xxx").safeAmap {
                val embedUrl = app.get(
                    it.attr("href"),
                    headers = headers,
                    allowRedirects = false
                ).headers["Location"] ?: return@safeAmap

                loadSourceNameExtractor("Levidia", embedUrl, "$levidiaAPI/", subtitleCallback, callback)
            }
        } else {
            val epRegex = Regex("""(?i)[^a-z]s0?${season}e0?${episode}[^0-9]""")

            val episodePath = doc.select("li.mlist.links b a").firstNotNullOfOrNull { aTag ->
                val href = aTag.attr("href")
                if (epRegex.containsMatchIn(href)) {
                    href
                } else {
                    null
                }
            } ?: return

            val doc2 = app.get("$levidiaAPI/" + episodePath, headers = headers).document

            doc2.select("a.xxx").safeAmap {
                val embedUrl = app.get(
                    it.attr("href"),
                    headers = headers,
                    allowRedirects = false
                ).headers["Location"] ?: return@safeAmap

                loadSourceNameExtractor("Levidia", embedUrl, "$levidiaAPI/", subtitleCallback, callback)
            }
        }
    }

    suspend fun invokeCinemacity(
        imdbId: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val url = "$cinemacityAPI/?do=search&subaction=search&search_start=0&full_search=0&story=$imdbId"

        val movieUrl = app.get(url).document
            .selectFirst("div.dar-short_item > a")
            ?.attr("href")
            ?: return

        val headers = mapOf(
            "Cookie" to CC_COOKIE
        )

        val scriptData = app.get(movieUrl, headers).document
            .select("script:containsData(atob)")
            .getOrNull(1)
            ?.data()
            ?: return

        val playerJson = JSONObject(
            base64Decode(
                scriptData.substringAfter("atob(\"").substringBefore("\")")
            ).substringAfter("new Playerjs(").substringBeforeLast(");")
        )

        val fileArray = JSONArray(playerJson.getString("file"))

        fun extractQuality(url: String): Int {
            return when {
                url.contains("2160p") -> Qualities.P2160.value
                url.contains("1440p") -> Qualities.P1440.value
                url.contains("1080p") -> Qualities.P1080.value
                url.contains("720p") -> Qualities.P720.value
                url.contains("480p") -> Qualities.P480.value
                url.contains("360p") -> Qualities.P360.value
                else -> Qualities.Unknown.value
            }
        }

        suspend fun emitExtractorLinks(files: String) {
            callback.invoke(
                newExtractorLink(
                    "CineCity",
                    "CineCity Multi Audio 🌐",
                    files,
                    INFER_TYPE
                ) {
                    referer = movieUrl
                    quality = extractQuality(files)
                }
            )
        }

        val first = fileArray.getJSONObject(0)

        // MOVIE
        if (!first.has("folder")) {
            emitExtractorLinks(
                files = first.getString("file")
            )
            return
        }

        // SERIES
        for (i in 0 until fileArray.length()) {
            val seasonJson = fileArray.getJSONObject(i)

            val seasonNumber = Regex("Season\\s*(\\d+)", RegexOption.IGNORE_CASE)
                .find(seasonJson.optString("title"))
                ?.groupValues
                ?.get(1)
                ?.toIntOrNull()
                ?: continue

            if (season != null && seasonNumber != season) continue

            val episodes = seasonJson.getJSONArray("folder")
            for (j in 0 until episodes.length()) {
                val epJson = episodes.getJSONObject(j)

                val episodeNumber = Regex("Episode\\s*(\\d+)", RegexOption.IGNORE_CASE)
                    .find(epJson.optString("title"))
                    ?.groupValues
                    ?.get(1)
                    ?.toIntOrNull()
                    ?: continue

                if (episode != null && episodeNumber != episode) continue

                emitExtractorLinks(
                    files = epJson.getString("file")
                )
            }
        }
    }

    suspend fun invokeVidstack(
        imdbId: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val json = app.get("$multiDecryptAPI/enc-vidstack").text

        Log.d("Vidstack", "enc response: $json"
        )
        val result = JSONObject(json).getJSONObject("result")
        val token = result.getString("token")
        val user_id = result.getString("user_id")

        Log.d("Vidstack", "token: $token, user_id: $user_id")

        if(token.isEmpty()) return

        val headers = mapOf(
            "User-Agent" to USER_AGENT,
            "Referer" to vidstackBaseAPI
        )

        // val servers = listOf(
        //     "videosmashyi", "smashystream",
        //     "short2embed", "videoophim", "videofsh"
        // )

        val servers = listOf(
            "videosmashyi"
        )

        servers.safeAmap { server ->

            val type = if(server == "videosmashyi" || server == "smashystream") {
                "1"
            } else {
                "2"
            }

            val url = if(season == null) {
                "$vidstackAPI/$server/$imdbId?token=$token&user_id=$user_id"
            } else {
                "$vidstackAPI/$server/$imdbId/$season/$episode?token=$token&user_id=$user_id"
            }

            val data_json = app.get(url, headers = headers).text

            Log.d("Vidstack", "data response for server $server: $data_json")

            if(type == "1") {
                val dataString = JSONObject(data_json).getString("data")

                if(dataString.isEmpty()) return@safeAmap

                val parts = dataString.split("/#")
                if (parts.size < 2) return@safeAmap
                val host = parts[0]
                val id = parts[1]
                val encrypted = app.get("$host/api/v1/video?id=$id", headers = headers).text

                Log.d("Vidstack", "encrypted data for server $server: $encrypted")

                val jsonBody = mapOf(
                    "text" to encrypted,
                    "type" to type
                )

                val decrypted = app.post("$multiDecryptAPI/dec-vidstack", json = jsonBody).text

                Log.d("Vidstack", "decrypted data for server $server: $decrypted")

                val resultObject = JSONObject(decrypted).getJSONObject("result")
                val m3u8 = resultObject.getString("source")

                callback.invoke(
                    newExtractorLink(
                        "Vidstack",
                        "Vidstack",
                        m3u8,
                        ExtractorLinkType.M3U8
                    ) {
                        this.headers = headers
                        this.quality = Qualities.P1080.value
                    }
                )

                if (resultObject.has("subtitle")) {
                    val subtitleObject = resultObject.getJSONObject("subtitle")
                    val keysIterator = subtitleObject.keys()

                    while (keysIterator.hasNext()) {
                        val languageName = keysIterator.next()
                        val relativePath = subtitleObject.getString(languageName)
                        val fullUrl = "$vidstackBaseAPI$relativePath"

                        subtitleCallback.invoke(
                            newSubtitleFile(
                                getLanguage(languageName) ?: languageName,
                                fullUrl
                            )
                        )
                    }
                }
            }
        }
    }

    suspend fun invokePlaysrc(
        tmdbId: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit
    ) {

        data class VideoResponse(
            @param:JsonProperty("file") val file: String?,
            @param:JsonProperty("headers") val headers: Map<String, String>?
        )

        val url = if(season == null) {
            "https://api.madplay.site/api/playsrc?id=$tmdbId&token=direct"
        } else {
            "https://madplay.site/api/movies/holly?id=${tmdbId}&season=${season}&episode=${episode}&token=direct"
        }

        val jsonText = app.get(url).text

        val parsedList = try {
            parseJson<List<VideoResponse>>(jsonText)
        } catch (e: Exception) {
            println("Failed to parse JSON: ${e.message}")
            return
        }

        val firstItem = parsedList.firstOrNull() ?: return

        val videoUrl = firstItem.file ?: return
        val headerMap = firstItem.headers ?: emptyMap()

        callback.invoke(
            newExtractorLink(
                "Playsrc",
                "Playsrc",
                videoUrl,
                ExtractorLinkType.M3U8
            ) {
                headers = headerMap
            }
        )
    }

    suspend fun invokeMadplayCDN(
        tmdbId: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit
    ) {
        val m3u8 = if(season == null) {
            "https://cdn.madplay.site/api/hls/unknown/${tmdbId}/master.m3u8"
        } else {
            "https://cdn.madplay.site/api/hls/unknown/${tmdbId}/season_${season}/episode_${episode}/master.m3u8"
        }

        M3u8Helper.generateM3u8(
            "Madplay[CDN]",
            m3u8,
            "",
        ).forEach(callback)
    }

    suspend fun invokeVidflix(
        tmdbId: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit
    ) {
        val url = if(season == null) {
            "https://madplay.site/api/movies/holly?id=${tmdbId}&token=direct"
        } else {
            "https://madplay.site/api/movies/holly?id=${tmdbId}&season=${season}&episode=${episode}&token=direct"
        }

        val jsonString = app.get(url).text
        val jsonArray = JSONArray(jsonString)

        for (i in 0 until jsonArray.length()) {
            val item: JSONObject = jsonArray.getJSONObject(i)
            val file = item.getString("file")
            var referer = ""
            var origin = ""

            if (item.has("headers")) {
                val headers: JSONObject = item.getJSONObject("headers")
                referer = headers.optString("Referer", "")
                origin = headers.optString("Origin", "")
            }

            callback.invoke(
                newExtractorLink(
                    "Vidflix",
                    "Vidflix",
                    file,
                    ExtractorLinkType.M3U8
                ) {
                    this.headers = mapOf(
                        "Referer" to referer,
                        "Origin" to origin
                    )
                }
            )
        }
    }

    suspend fun invokeXpass(
        tmdbId: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val embedUrl = if(season == null) "$xpassAPI/e/movie/$tmdbId" else "$xpassAPI/e/tv/$tmdbId/$season/$episode"
        val html = app.get(embedUrl, referer = "$xpassAPI/").text
        val backups = extractXpassBackups(html)

        Log.d("Xpass", "backups: $backups")

        backups.safeAmap { (name, url) ->
            val fullUrl  = if (url.startsWith("http")) url else xpassAPI + url

            Log.d("Xpass", "fullUrl: $fullUrl")

            val json     = app.get(fullUrl).text
            val sources  = JSONObject(json)
                .optJSONArray("playlist")
                ?.optJSONObject(0)
                ?.optJSONArray("sources") ?: return@safeAmap

            for (i in 0 until sources.length()) {
                val source = sources.getJSONObject(i)
                val file   = source.optString("file").takeIf {
                    it.isNotBlank() && it.startsWith("http")
                } ?: continue
                val isM3u8 = source.optString("type").contains("hls", ignoreCase = true)
                        || file.contains(".m3u8")

                if(isM3u8) {
                    M3u8Helper.generateM3u8(
                        "Xpass [$name]",
                        file,
                        "$xpassAPI/",
                    ).forEach(callback)
                } else {
                    callback.invoke(
                        newExtractorLink(
                            "Xpass [$name]",
                            "Xpass [$name]",
                            file
                        ) {
                            this.referer = "$xpassAPI/"
                        }
                    )
                }
            }

        }
    }

    suspend fun invokeRtally(
        title: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {

        fun getStreamUrl(
            id: String,
            service: String
        ): String? {
            if(service == "vidhide") return "https://vidhideplus.com/v/$id"
            else if(service == "lulustream") return "https://lulustream.com/e/$id"
            else if(service == "filemoon") return "https://filemoon.sx/e/$id"
            else if(service == "streamwish") return "https://playerwish.com/e/$id"
            else if(service == "strmup") return "https://strmup.cc/$id"
            else return null
        }

        if(season != null) return

        val slugTitle = title.createSlug()
        val url = "$rtallyAPI/post/$slugTitle"
        val doc = app.get(url).document

        val linkPattern = Regex("""\\"(small|medium|large|extraLarge)\\":\\"(https?://[^\\"]+)""")

        val sourceList = mutableListOf<String>()

        linkPattern.findAll(doc.toString()).forEach { match ->
            // val quality = match.groupValues[1]
            val durl = match.groupValues[2]
            if (durl.isNotEmpty()) sourceList.add(durl)
        }

        val streamPattern = Regex("""\\"(lulustream|strmup|filemoon|turbo|vidhide|doodStream|streamwish)Url\\":\\"?([^\\"]+)""")

        streamPattern.findAll(doc.toString()).forEach { match ->
            val service = match.groupValues[1]
            val id = match.groupValues[2]

            if (id != "null") {
                val eurl = getStreamUrl(id, service) ?: return@forEach
                if (eurl.isNotEmpty()) sourceList.add(eurl)
            }
        }

        sourceList.safeAmap { loadSourceNameExtractor("Rtally", it, "", subtitleCallback, callback) }
    }

    suspend fun invokeYflix(
        tmdbId: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        data class ServerItem(val name: String, val lid: String)

        suspend fun encrypt(id: String): String {
            val body = app.get("$multiDecryptAPI/enc-movies-flix?text=$id").text
            val json = JSONObject(body)
            return json.getString("result")
        }

        suspend fun decrypt(text: String): String {
            val jsonBody = mapOf("text" to text)
            val text = app.post(
                "$multiDecryptAPI/dec-movies-flix",
                json = jsonBody
            ).text

            val json = JSONObject(text)
            return json.getJSONObject("result").getString("url")
        }

        fun extractAllServers(root: JSONObject): List<ServerItem> {
            val list = mutableListOf<ServerItem>()

            try {
                val result = root.optJSONObject("result")
                val defaultObj = result?.optJSONObject("default")

                if (defaultObj != null) {
                    val keys = defaultObj.keys()

                    while (keys.hasNext()) {
                        val key = keys.next()
                        val serverNode = defaultObj.optJSONObject(key)

                        val lid = serverNode?.optString("lid")
                        val name = serverNode?.optString("name")

                        if (!lid.isNullOrEmpty() && !name.isNullOrEmpty()) {
                            list.add(ServerItem(name, lid))
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return list
        }

        suspend fun parseHtml(html: String): JSONObject {
            val resp = app.post(
                "$multiDecryptAPI/parse-html",
                json = mapOf("text" to html)
            ).text

            return try {
                JSONObject(resp)
            } catch (e: Exception) {
                val clean = resp.trim('"')
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")

                try {
                    JSONObject(clean)
                } catch (fatal: Exception) {
                    JSONObject()
                }
            }
        }

        val YflixAPI = returnWorkingUrl(multipleYflixAPI) ?: return

        Log.d("Yflix", "Using API: $YflixAPI")

        val findUrl = "https://enc-dec.app/db/flix/find?tmdb_id=$tmdbId"
        val findResp = app.get(findUrl).text
        val findJson = JSONArray(findResp)
        if (findJson.length() == 0) return

        val contentId = findJson.getJSONObject(0).optJSONObject("info")?.optString("flix_id")
            ?: return
        val encId = encrypt(contentId)
        val seasonText = if(season == null) "1" else "$season"
        val episodeText = if(episode == null) "1" else "$episode"
        val episodesUrl = "$YflixAPI/ajax/episodes/list?id=$contentId&_=$encId"
        val episodesResp = app.get(episodesUrl).text
        val episodesHtml = JSONObject(episodesResp).getString("result")
        val episodesObj = parseHtml(episodesHtml)
        val result = episodesObj.optJSONObject("result")
        val seasonObj = result?.optJSONObject(seasonText)
        val episodeObj = seasonObj?.optJSONObject(episodeText)
        val eid = episodeObj?.optString("eid")

        if (eid.isNullOrEmpty()) return

        val encTargetId = encrypt(eid)
        val serversUrl = "$YflixAPI/ajax/links/list?eid=$eid&_=$encTargetId"
        val serversResp = app.get(serversUrl).text
        val serversHtml = JSONObject(serversResp).getString("result")
        val serversObj = parseHtml(serversHtml)
        val servers = extractAllServers(serversObj)

        servers.safeAmap { server ->
            val lid = server.lid
            val encLid = encrypt(lid)
            val serverName = server.name
            val embedUrlReq = "$YflixAPI/ajax/links/view?id=$lid&_=$encLid"
            val embedRespStr = app.get(embedUrlReq).text
            val encryptedEmbed = JSONObject(embedRespStr).getString("result")
            if (encryptedEmbed.isEmpty()) return@safeAmap
            var embed_url = decrypt(encryptedEmbed)

            Log.d("Yflix", "embed_url: $embed_url")

            if(embed_url.contains(YflixAPI)) {
                embed_url = cfGet(embed_url).document.selectFirst("iframe")?.attr("src") ?: return@safeAmap
            }

            Log.d("Yflix", "embed_url: $embed_url")

            MegaUp().getUrl(embed_url, "Yflix", subtitleCallback, callback)
        }
    }

    suspend fun invokeStremioStreams(
        sourceName: String, api: String, imdbId: String? = null,
        season: Int? = null, episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit
    ) {
        val isMovie = season == null
        val url = if (sourceName == "Hdmovielover") "$api/${if (isMovie) "movie?imdbid=$imdbId" else "series?imdbid=$imdbId&s=$season&e=$episode"}"
                else "$api/stream/${if (isMovie) "movie/$imdbId" else "series/$imdbId:$season:$episode"}.json"

        globalGson.fromJson(app.get(url, timeout = 50000L).text, StreamifyResponse::class.java).streams.forEach { s ->
            val title = s.title ?: ""
            val name = s.name ?: title

            // Block filters
            if (s.url.contains("github.com") || s.url.contains("googleusercontent") ||
                listOf("4KHDHub", "Instant Download", "IOSMIRROR", "XDM").any { name.contains(it) } ||
                title.contains("redirecting")) return@forEach

            val type = if (sourceName.contains("Castle") || listOf("hls", "m3u8", "Vixsrc").any { (title + name).contains(it, true) }) ExtractorLinkType.M3U8 else INFER_TYPE
            val req = s.behaviorHints?.proxyHeaders?.request
            val streamUrl = if (sourceName.contains("Castle")) {
                URI(s.url).query
                ?.split("&")
                ?.firstOrNull { it.startsWith("url=") }
                ?.removePrefix("url=")
                ?: return@forEach
            } else {
                s.url
            }

            val proxyReq = s.behaviorHints?.proxyHeaders?.request
            val stdHeaders = s.behaviorHints?.headers

            val extractedReferer = proxyReq?.Referer ?: stdHeaders?.get("Referer") ?: stdHeaders?.get("referer") ?: ""
            val extractedOrigin = proxyReq?.Origin ?: stdHeaders?.get("Origin") ?: stdHeaders?.get("origin") ?: ""
            val extractedUserAgent = proxyReq?.userAgent ?: stdHeaders?.get("User-Agent") ?: stdHeaders?.get("user-agent") ?: USER_AGENT

            // Quality fallback
            val quality = getIndexQuality(title + name).takeIf { it != Qualities.Unknown.value }
                ?: if (sourceName.contains("Castle")) Qualities.P1080.value else Qualities.Unknown.value

            callback(newExtractorLink(sourceName, "[$sourceName]".toSansSerifBold() + " ${if (sourceName == "Hdmovielover") getSimplifiedTitle(title) else title}", streamUrl, type) {
                this.quality = quality
                this.headers = mapOf(
                    "User-Agent" to extractedUserAgent,
                    "Referer" to extractedReferer,
                    "Origin" to extractedOrigin
                ).filterValues { it.isNotBlank() }
            })

            s.subtitles?.forEach { subtitleCallback(newSubtitleFile(getLanguage(it.lang) ?: it.lang, it.url)) }
        }
    }

    suspend fun invokeDahmerMovies(
        title: String? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit,
    ) {
        val url = if (season == null) {
            "$dahmerMoviesAPI/movies/${title?.replace(":", "")} ($year)/"
        } else {
            "$dahmerMoviesAPI/tvs/${title?.replace(":", " -")}/Season $season/"
        }
        val request = app.get(url, timeout = 60L)
        if (!request.isSuccessful) return
        val paths = request.document.select("a").map {
            it.text() to it.attr("href")
        }.filter {
            if (season == null) {
                it.first.contains(Regex("(?i)(720p|1080p|2160p)"))
            } else {
                val (seasonSlug, episodeSlug) = getEpisodeSlug(season, episode)
                it.first.contains(Regex("(?i)S${seasonSlug}E${episodeSlug}"))
            }
        }.ifEmpty { return }

        paths.safeAmap {
            val quality = getIndexQuality(it.first)
            val tags = getIndexQualityTags(it.first)
            val href = if (it.second.contains(dahmerMoviesAPI)) it.second else (dahmerMoviesAPI + it.second)
            val videoLink = resolveFinalUrl(href) ?: return@safeAmap

            callback.invoke(
                newExtractorLink(
                    "DahmerMovies",
                    "[DahmerMovies]".toSansSerifBold() + " $tags",
                    videoLink,
                    ExtractorLinkType.VIDEO
                ) {
                    this.quality = quality
                    this.referer = dahmerMoviesAPI
                }
            )
        }
    }

    suspend fun invokeVideasy(
        title: String? = null,
        tmdbId: Int? = null,
        imdbId: String? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {

        fun quote(text: String): String {
            return URLEncoder.encode(text, StandardCharsets.UTF_8.toString())
                .replace("+", "%20")
        }

        val headers = mapOf(
            "Accept" to "*/*",
            "User-Agent" to USER_AGENT,
            "Origin" to "https://www.cineby.sc",
            "Referer" to "https://www.cineby.sc/"
        )

        val servers = listOf(
            "myflixerzupcloud",
            "1movies",
            "moviebox",
            "primewire",
            "m4uhd",
            "hdmovie",
            "cdn",
            "primesrcme",
            "visioncine",
            "overflix",
            "superflix",
            "cuevana",
            "lamovie"
        )

        if(title == null) return

        val firstPass = quote(title)
        val encTitle = quote(firstPass)

        servers.safeAmap { server ->
            val url = if (season == null) {
                "$videasyAPI/$server/sources-with-title?title=$encTitle&mediaType=movie&year=$year&tmdbId=$tmdbId&imdbId=$imdbId"
            } else {
                "$videasyAPI/$server/sources-with-title?title=$encTitle&mediaType=tv&year=$year&tmdbId=$tmdbId&episodeId=$episode&seasonId=$season&imdbId=$imdbId"
            }

            val enc_data = app.get(url, headers = headers).text

            val jsonBody = mapOf("text" to enc_data, "id" to tmdbId)
            val response = app.post(
                "$multiDecryptAPI/dec-videasy",
                json = jsonBody
            )

            if(response.isSuccessful) {
                val json = response.text
                val result = JSONObject(json).getJSONObject("result")

                val sourcesArray = result.getJSONArray("sources")
                for (i in 0 until sourcesArray.length()) {
                    val obj = sourcesArray.getJSONObject(i)
                    val quality = obj.getString("quality")
                    val source = obj.getString("url")

                    val type = if(source.contains(".m3u8")) {
                        ExtractorLinkType.M3U8
                    } else if(source.contains(".mp4") || source.contains(".mkv")) {
                        ExtractorLinkType.VIDEO
                    } else {
                        INFER_TYPE
                    }

                    callback.invoke(
                        newExtractorLink(
                            "Videasy[${server.uppercase()}]",
                            "Videasy[${server.uppercase()}] $quality",
                            source,
                            type
                        ) {
                            this.quality = getIndexQuality(quality)
                            this.headers = headers
                        }
                    )
                }

                val subtitlesArray = result.getJSONArray("subtitles")
                for (i in 0 until subtitlesArray.length()) {
                    val obj = subtitlesArray.getJSONObject(i)
                    val source = obj.getString("url")
                    val language = obj.getString("language")

                    subtitleCallback.invoke(
                        newSubtitleFile(
                            getLanguage(language) ?: language,
                            source
                        )
                    )
                }
            }
        }
    }

    suspend fun invokeMapple(
        tmdbId: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit
    ) {
        if (tmdbId == null) return

        val base = mappleAPI.removeSuffix("/")

        val mediaType = if (season == null) "movie" else "tv"
        val tvSlug = if (season != null && episode != null) "$season-$episode" else ""

        val headers = mapOf(
            "User-Agent" to USER_AGENT,
            "Referer" to "$base/",
            "Origin" to base,
            "Accept" to "*/*",
            "Content-Type" to "application/json"
        )

        val watchUrl = if (mediaType == "movie") {
            "$base/watch/movie/$tmdbId"
        } else {
            "$base/watch/tv/$tmdbId/$tvSlug"
        }

        val page = app.get(watchUrl, headers = headers).text
        val tokenRegex = Regex("""window\.__REQUEST_TOKEN__\s*=\s*"([^"]+)\"""")
        val requestToken = tokenRegex.find(page)?.groupValues?.get(1) ?: return

        val tokenRes1 = JSONObject(
            app.post(
                "$base/api/stream-token",
                json = mapOf(
                    "mediaId"      to tmdbId,
                    "mediaType"    to mediaType,
                    "requestToken" to requestToken
                ),
                headers = headers
            ).text
        )

        if (!tokenRes1.optBoolean("success")) return

        val finalToken = if (tokenRes1.optBoolean("requiresPow")) {
            val pow = tokenRes1.getJSONObject("pow")

            val nonce = solvePowChallenge(
                pow.getString("challenge"),
                pow.getInt("difficulty")
            ) ?: return

            val tokenRes2 = JSONObject(
                app.post(
                    "$base/api/stream-token",
                    json = mapOf(
                        "mediaId"      to tmdbId,
                        "mediaType"    to mediaType,
                        "requestToken" to requestToken,
                        "pow"          to mapOf(
                            "challengeId" to pow.getString("challengeId"),
                            "nonce"       to nonce
                        )
                    ),
                    headers = headers
                ).text
            )

            if (!tokenRes2.optBoolean("success")) return
            tokenRes2.getString("token")
        } else {
            tokenRes1.getString("token")
        }

        val sources = listOf(
            "mapple",
            "willow",
            "cherry",
            "pines",
            "oak",
            "sequoia",
            "sakura",
            "magnolia"
        )

        sources.safeAmap { source ->
            val streamUrl =
                "$base/api/stream?mediaId=$tmdbId&mediaType=$mediaType&tv_slug=$tvSlug" +
                        "&source=$source&apikey=mptv_sk_a8f29c4e7b3d1f" +
                        "&requestToken=$requestToken&token=$finalToken"

            val streamRes = JSONObject(app.get(streamUrl, headers = headers).text)

            if (!streamRes.optBoolean("success")) return@safeAmap

            val m3u8 = streamRes
                .getJSONObject("data")
                .optString("stream_url")

            if (m3u8.isNotEmpty()) {
                M3u8Helper.generateM3u8(
                    "Mapple [${source.uppercase()}]",
                    m3u8,
                    "$base/",
                    headers = headers
                ).forEach(callback)
            }
        }
    }

    suspend fun invokeHexa(
        tmdbId: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit
    ) {
        val url = if(season == null) {
            "$hexaAPI/api/tmdb/movie/$tmdbId/images"
        } else {
            "$hexaAPI/api/tmdb/tv/$tmdbId/season/$season/episode/$episode/images"
        }

        val keyBytes = ByteArray(32)
        SecureRandom().nextBytes(keyBytes)
        val key = keyBytes.joinToString("") { "%02x".format(it) }

        val tokenResponseText = app.get("$multiDecryptAPI/enc-hexa").text
        val token = JSONObject(tokenResponseText).getJSONObject("result").getString("token")

        val headers = mapOf(
            "User-Agent" to USER_AGENT,
            "Accept" to "text/plain",
            "X-Api-Key" to key,
            "X-Fingerprint-Lite" to "e9136c41504646444",
            "Referer" to "https://hexa.su/",
            "X-Cap-Token" to token
        )

        val enc_data = app.get(url, headers = headers).text

        val jsonBody = mapOf("text" to enc_data, "key" to key)
        val response = app.post(
            "$multiDecryptAPI/dec-hexa",
            json = jsonBody,
            headers = mapOf("Content-Type" to "application/json")
        )

        if(response.isSuccessful) {
            val json = response.text
            val result = JSONObject(json).getJSONObject("result")
            val sourcesArray = result.getJSONArray("sources")

            for (i in 0 until sourcesArray.length()) {
                val src = sourcesArray.getJSONObject(i)
                val server = src.getString("server")
                val m3u8 = src.getString("url")

                M3u8Helper.generateM3u8(
                    "Hexa ${server.uppercase()}",
                    m3u8,
                    "https://hexa.su/",
                ).forEach(callback)
            }
        }
    }

    suspend fun invokeVidlink(
        tmdbId: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val url = "$multiDecryptAPI/enc-vidlink?text=$tmdbId"
        val json = app.get(url).text

        Log.d("Vidlink", "enc response: $json")

        val enc_data = JSONObject(json).getString("result")

        val headers = mapOf(
            "User-Agent" to USER_AGENT,
            "Connection" to "keep-alive",
            "Referer" to "$vidlinkAPI/",
            "Origin" to vidlinkAPI,
        )

        val epUrl = if(season == null) {
            "$vidlinkAPI/api/b/movie/$enc_data"
        } else {
            "$vidlinkAPI/api/b/tv/$enc_data/$season/$episode"
        }

        val epJson = app.get(epUrl, headers = headers).text

        Log.d("Vidlink", "ep response: $epJson")

        val data = globalGson.fromJson(epJson, VidlinkResponse::class.java)
        val m3u8 = data.stream.playlist

        M3u8Helper.generateM3u8(
            "Vidlink",
            m3u8,
            "$vidlinkAPI/",
            headers = headers
        ).forEach(callback)
    }

    suspend fun invokeToonstream(
        title: String? = null,
        season: Int? = null,
        episode: Int?  = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val url = if(season == null) {
            "$toonStreamAPI/movies/${title.createSlug()}/"
        } else {
            "$toonStreamAPI/episode/${title.createSlug()}-${season}x${episode}/"
        }

        app.get(url, referer = toonStreamAPI).document.select("div.video > iframe").safeAmap {
            val source = it.attr("data-src")
            val doc = app.get(source).document
            doc.select("div.Video > iframe").safeAmap { iframe ->
                loadSourceNameExtractor(
                    "ToonStream",
                    iframe.attr("src"),
                    "$toonStreamAPI/",
                    subtitleCallback,
                    callback
                )
            }
        }
    }

    suspend fun invokeStremioSubtitles(
        imdbId: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
    ) {
        val subsUrls = listOf(
            "https://opensubtitles.stremio.homes/en|hi|de|ar|tr|es|ta|te|ru|ko/ai-translated=true|from=all|auto-adjustment=true",
            """https://subsense.nepiraw.com/n0tcjfba-{"languages":["en","hi","ta","es","ar"],"maxSubtitles":10}"""
        )

        subsUrls.safeAmap { subUrl ->
            try {
                val url = if(season != null) {
                    subUrl + "/subtitles/series/$imdbId:$season:$episode.json"
                } else {
                    subUrl + "/subtitles/movie/$imdbId.json"
                }

                val json = app.get(url).text
                val subtitleResponse = globalGson.fromJson(json, StremioSubtitleResponse::class.java)

                subtitleResponse.subtitles.forEach {
                    val lang = it.lang ?: it.lang_code
                    val fileUrl = it.url
                    if(lang != null && fileUrl != null) {
                        subtitleCallback.invoke(
                            newSubtitleFile(
                                getLanguage(lang) ?: lang,
                                fileUrl,
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                println("Error fetching/parsing subtitle from: $subUrl - ${e.message}")
            }
        }
    }

    suspend fun invokeSudatchi(
        aniId: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val headers = mapOf(
            "Referer" to sudatchiAPI,
            "Origin" to sudatchiAPI,
        )

        val json = app.get("$sudatchiAPI/api/episode/$aniId/$episode", headers = headers).text
        val jsonObject = JSONObject(json)
        val episode = jsonObject.getJSONObject("episode")
        val epId = episode.getInt("id")
        val url = "$sudatchiAPI/api/streams?episodeId=$epId"
        callback.invoke(
            newExtractorLink(
                "Sudatchi",
                "Sudatchi Multi Audio 🌐",
                url,
                type = ExtractorLinkType.M3U8
            ) {
                this.quality = 1080
                this.headers = headers
            }
        )

        val subsJson = JSONArray(jsonObject.getString("subtitlesJson"))

        for (i in 0 until subsJson.length()) {
            val sub = subsJson.getJSONObject(i)
            val subUrl = sub.getString("url").replace("/ipfs", "").replace("\\", "")
            val file = "$sudatchiAPI/api/proxy$subUrl"
            val label = sub.getJSONObject("SubtitlesName").getString("name")
            subtitleCallback.invoke(
                newSubtitleFile(
                    getLanguage(label) ?: label,
                    file
                )
            )
        }
    }

    suspend fun invokeGojo(
        title: String? = null,
        aniId: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        if (title == null) return

        val episodeNumber = episode ?: 1
        val gojoAPI = "$gojoBaseAPI/v2"
        val headers = mapOf(
            "User-Agent" to "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36",
            "Referer" to "$gojoBaseAPI/",
            "Origin" to gojoBaseAPI
        )

        val searchJson = app.get("$gojoAPI/api/anime/search/?query=$title", headers = headers).text

        Log.d("Gojo", "searchJson: $searchJson")

        val id = getGojoId(searchJson, title) ?: return

        Log.d("Gojo", "id: $id")

        val epDetailsJson = app.get("$gojoAPI/api/anime/servers/$id/$episodeNumber", headers = headers).text
        val servers = getGojoServers(epDetailsJson)

        Log.d("Gojo", "servers: $servers")

        if (servers.isEmpty()) return

        servers.safeAmap { server ->
            runLimitedAsync ( concurrency = 2,
                {
                    try {
                        val json = app.get("$gojoAPI/api/anime/oppai/$id/$episodeNumber?server=$server&source_type=sub", headers = headers).text
                        getGojoStreams(json, "sub", server, gojoBaseAPI, subtitleCallback ,callback)
                    } catch (e: Exception) {
                        println("Error Gojo Sub: $e")
                    }
                },
                {
                    try {
                        val json = app.get("$gojoAPI/api/anime/oppai/$id/$episodeNumber?server=$server&source_type=dub", headers = headers).text
                        getGojoStreams(json, "dub", server, gojoBaseAPI, subtitleCallback ,callback)
                    } catch (e: Exception) {
                        println("Error Gojo Dub: $e")
                    }
                }
            )
        }
    }

    suspend fun invoke2embed(
        id: String? = null,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit
    ) {
        val headers = mapOf(
            "Referer" to twoembedAPI,
            "sec-fetch-dest" to "iframe"
        )

        val slug = if (season != null) {
            "embedtv/$id&s=$season&e=$episode"
            } else {
            "embed/$id"
        }

        val api = "$twoembedAPI/$slug"

        val text = app.get(api, headers = headers).text
        var sKey = "swish?id="
        var start = text.indexOf(sKey)

        if(start < 0) return

        start += sKey.length

        val eKey = "'"
        var end = text.indexOf(eKey, start)
        val strmId = text.substring(start, end)

        val uplUrl = "https://uqloads.xyz/e/$strmId"

        val res = app.get(uplUrl, headers = headers).text

        sKey = "eval(function"
        start = res.indexOf(sKey)

        if(start < 0) return

        val eKey2 = "split('|')))"
        end = res.indexOf(eKey2, start) + eKey2.length
        val data = res.substring(start, end)

        val strmData = JsUnpacker(data).unpack() ?: return

        sKey = "\"hls2\":\""
        start = strmData.indexOf(sKey)

        if(start < 0) return

        start += sKey.length
        end = strmData.indexOf("\"}", start)
        val streamUrl = strmData.substring(start, end)

        callback.invoke(
            newExtractorLink(
                "2Embed",
                "2Embed",
                streamUrl,
                type = ExtractorLinkType.M3U8
            ) {
                this.headers = headers
            }
        )
    }

    suspend fun invokeAsiaflix(
        title: String? = null,
        season: Int? = null,
        episode: Int? = null,
        year: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ) {
        if(title == null) return
        if(season != null && season != 1) return
        val searchUrl = "https://api.asiaflix.net/v1/drama/search?q=$title"
        val headers = mapOf(
            "Referer" to asiaflixAPI,
            "X-Access-Control" to "web"
        )
        val jsonString = cfGet(searchUrl, headers).text

        Log.d("Asiaflix", "search response: $jsonString")

        val jsonObject = JSONObject(jsonString)
        val bodyArray = jsonObject.getJSONArray("body")

        var matchedId: String? = null
        var matchedName: String? = null

        for (i in 0 until bodyArray.length()) {
            val item = bodyArray.getJSONObject(i)
            val name = item.getString("name")

            if (title in name) {
                matchedId = item.getString("_id")
                matchedName = name
                break
            }
        }

        Log.d("Asiaflix", "matchedId: $matchedId, matchedName: $matchedName")

        val sourceList = mutableListOf<String>()

        if(matchedId != null && matchedName != null) {
            val titleSlug = matchedName.replace(" ", "-")
            val episodeUrl = "$asiaflixAPI/play/$titleSlug-1/$matchedId/1"

            Log.d("Asiaflix", "episodeUrl: $episodeUrl")

            val scriptText = app.get(episodeUrl).document.selectFirst("script#ng-state")?.data() ?: return
            val fullRegex = Regex("""\"number\"\s*:\s*${episode ?: 1}\b[\s\S]*?\"streamUrls\"\s*:\s*(\[[\s\S]*?])""")
            val epJson = fullRegex.find(scriptText)?.groupValues?.get(1) ?: return

            Log.d("Asiaflix", "epJson: $epJson")

            val urlRegex = Regex("""\"url\"\s*:\s*\"(.*?)\"""")
            urlRegex.findAll(epJson).forEach { match ->
                val source =  httpsify(match.groupValues[1])

                Log.d("Asiaflix", "found source: $source")

                if (source.isNotEmpty()) sourceList.add(source)
            }
        }

        sourceList.safeAmap {
            loadSourceNameExtractor("Asiaflix", it, "", subtitleCallback, callback)
        }
    }

    suspend fun invokeAllmovieland(
        id : String? = null,
        season : Int? = null,
        episode : Int? = null,
        callback: (ExtractorLink) -> Unit
    ) {
        val playerScript = app.get("https://allmovieland.link/player.js?v=60%20128").toString()
        val domainRegex = Regex("const AwsIndStreamDomain.*'(.*)';")
        val host = domainRegex.find(playerScript)?.groupValues?.getOrNull(1) ?: return
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

        servers?.safeAmap { (server, lang) ->
            val path =
                    app.post(
                        "${host}/playlist/${server ?: return@safeAmap}.txt",
                        headers = headers,
                        referer = referer
                    ).text

            callback.invoke(
                newExtractorLink(
                    "Allmovieland [$lang]",
                    "Allmovieland [$lang]",
                    path,
                    type = ExtractorLinkType.M3U8
                ) {
                    this.referer = referer
                    this.quality = Qualities.P1080.value
                }
            )
        }
    }

    suspend fun invokeHindmoviez(
        id: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        app.get("$hindMoviezAPI/?s=$id", timeout = 5000L).document.select("h2.entry-title > a").safeAmap {

            Log.d("HindMoviez", "matched link: ${it.attr("href")}")

            val doc = app.get(it.attr("href"), timeout = 5000L).document
            if(episode == null) {
                doc.select("a.maxbutton").safeAmap {

                    Log.d("HindMoviez", "link1: ${it.attr("href")}")

                    val res = app.get(it.attr("href"), timeout = 5000L).document

                    val link = res.selectFirst("a.get-link-btn")
                        ?.attr("href")
                        ?.takeIf { it.isNotBlank() }
                        ?.let { href ->
                            val baseurl=href.substringBefore("/?id=")
                            val rawId = href.substringAfter("id=")
                            hindmoviezsignHShare(rawId, baseurl)
                        }
                        ?: return@safeAmap

                    Log.d("HindMoviez", "link2: $link")

                    getHindMoviezLinks("HindMoviez", link, subtitleCallback, callback)
                }
            }
            else {
                doc.select("a.maxbutton").safeAmap {
                    val text = it.parent()?.parent()?.previousElementSibling()?.text() ?: ""
                    if(text.contains("Season $season")) {
                        val res = app.get(it.attr("href"), timeout = 5000L).document
                        val link = res.select("h3 > a")
                            .getOrNull(episode-1)
                            ?.attr("href")
                            ?.takeIf { it.isNotBlank() }
                            ?.let { href ->
                                val baseurl = href.substringBefore("/?id=")
                                val rawId = href.substringAfter("id=")
                                hindmoviezsignHShare(rawId, baseurl)

                            } ?: return@safeAmap

                        Log.d("HindMoviez", "link: $link")

                        getHindMoviezLinks("HindMoviez", link, subtitleCallback, callback)
                    }
                }
            }
        }
    }

    suspend fun invokeAnimekai(
        animekaiUrl: String? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val headers = mapOf(
            "User-Agent" to USER_AGENT,
            "Connection" to "keep-alive",
        )

        suspend fun encrypt(id: String): String {
            val body = app.get("$multiDecryptAPI/enc-kai?text=$id").text
            val json = JSONObject(body)
            return json.getString("result")
        }

        data class ServerInfo(
            val serverType: String,
            val lid: String?,
        )

        fun getEpisodeToken(html: String, episode: Int): String? {
            val doc = Jsoup.parse(html)
            val selector = "div.eplist ul.range li a[num=\"$episode\"]"
            val episodeElement = doc.selectFirst(selector)
            return episodeElement?.attr("token")
        }

        fun parseServersFromHtml(html: String): List<ServerInfo> {
            val doc = Jsoup.parse(html)
            val servers = mutableListOf<ServerInfo>()

            val groups = doc.select("div.server-items.lang-group")
            for (grp in groups) {
                val serverType = grp.attr("data-id").uppercase()
                for (span in grp.select("span.server")) {
                    val lid = span.attr("data-lid").ifBlank { null }
                    servers.add(ServerInfo(serverType, lid))
                }
            }
            return servers
        }

        suspend fun decrypt(text: String): String {
            val jsonBody = mapOf("text" to text)

            val text = app.post(
                "$multiDecryptAPI/dec-kai",
                json = jsonBody
            ).text

            val json = JSONObject(text)
            return json.getJSONObject("result").getString("url")
        }

        val animekaiAPI = returnWorkingUrl(multipleAnimekaiAPI) ?: return
        val oldBaseUrl = getBaseUrl(animekaiUrl ?: return)
        val postUrl = animekaiUrl.replace(oldBaseUrl, animekaiAPI)

        Log.d("Animekai", "postUrl: $postUrl")

        val id = app.get(postUrl)
            .document
            .selectFirst("div.rate-box")?.attr("data-id") ?: return

        Log.d("Animekai", "id: $id")

        val enc_id = encrypt(id)
        val json = app.get("$animekaiAPI/ajax/episodes/list?ani_id=$id&_=$enc_id", headers = headers).text

        Log.d("Animekai", "json: $json")

        val html = JSONObject(json).getString("result")
        val token = getEpisodeToken(html, episode ?: 1) ?: return
        val enc_token = encrypt(token)
        val servers_resp = app.get("$animekaiAPI/ajax/links/list?token=$token&_=$enc_token", headers = headers).text
        val servers = JSONObject(servers_resp).getString("result")
        val all = parseServersFromHtml(servers)

        all.safeAmap {
            val lid = it.lid ?: return@safeAmap
            val enc_lid = encrypt(lid)
            val type = it.serverType
            val embed_resp = app.get("$animekaiAPI/ajax/links/view?id=$lid&_=$enc_lid", headers = headers).text
            val encrypted = JSONObject(embed_resp).getString("result")
            var embed_url = decrypt(encrypted)

            Log.d("Animekai", "embed_url: $embed_url")

            if(embed_url.contains(animekaiAPI)) {
                embed_url = cfGet(embed_url).document.selectFirst("iframe")?.attr("src") ?: return@safeAmap
            }

            Log.d("Animekai", "embed_url: $embed_url")

            MegaUp().getUrl(embed_url, "Animekai[$type]", subtitleCallback, callback)
        }
    }

    suspend fun invokeOnetouchtv(
        title: String? = null,
        airedYear: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        if(title == null || airedYear == null) return

        var query = title

        if(season != null && season != 1) {
            query += " Season $season ($airedYear)"
        } else {
            query += " ($airedYear)"
        }

        val encrypt = app.get("$onetouchtvAPI/vod/search?page=1&keyword=$query").text

        val decrypt = app.post(
            "$multiDecryptAPI/dec-onetouchtv",
            json = mapOf("text" to encrypt)
        ).text

        //get result
        val result = JSONObject(decrypt).getJSONArray("result").toString()

        val listType = object : TypeToken<List<OneMediaItem>>() {}.type
        val mediaItems: List<OneMediaItem> = globalGson.fromJson(result, listType)

        Log.d("Onetouchtv", "mediaItems: $mediaItems")

        val matchedId = mediaItems.firstOrNull { it.title.equals(query, ignoreCase = true) }?.id ?: return

        Log.d("Onetouchtv", "matchedId: $matchedId")

        val encodeSource = app.get("$onetouchtvAPI/web/vod/$matchedId/episode/${episode ?: 0}").text

        val decryptSource = app.post(
            "$multiDecryptAPI/dec-onetouchtv",
            json = mapOf("text" to encodeSource)
        ).text

        Log.d("Onetouchtv", "decryptSource: $decryptSource")

        val sourceResult = JSONObject(decryptSource).getJSONObject("result").toString()

        val playbackData = globalGson.fromJson(sourceResult, OnePlaybackData::class.java)

        playbackData.sources.forEach { source ->

            val type = if(source.type == "hls") ExtractorLinkType.M3U8 else INFER_TYPE
            val quality = getIndexQuality(source.quality)

            callback.invoke(
                newExtractorLink(
                    "Onetouchtv",
                    "Onetouchtv",
                    source.url,
                    type
                ) {
                    this.headers = source.headers ?: emptyMap()
                    this.quality = quality
                }
            )
        }

        playbackData.track.forEach { subtitle ->
            subtitleCallback.invoke(
                newSubtitleFile(
                    subtitle.name,
                    subtitle.file
                )
            )
        }

    }

    suspend fun invokeKisskh(
        title: String? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ) {
        val slug = title.createSlug() ?: return
        val type = if (season == null) "2" else "1"
        val searchResponse = app.get(
            "$kissKhAPI/api/DramaList/Search?q=$title&type=$type",
            referer = "$kissKhAPI/"
        )
        if (searchResponse.code != 200) return
        val res = tryParseJson<ArrayList<KisskhResults>>(searchResponse.text) ?: return

        Log.d("Kisskh", "res: $res")

        val (id, contentTitle) = if (res.size == 1) {
            res.first().id to res.first().title
        } else {
            val data = res.find {
                val slugTitle = it.title.createSlug() ?: return@find false
                val tSlug = it.title?.createSlug() ?: return@find false
                val tActual = it.title
                when (season) {
                    null -> tSlug == slug
                    1 -> tSlug == slug || (tSlug.contains(slug) && (tActual.contains("$year") || tActual.contains("Season 1", true)))
                    else -> tSlug.contains(slug) && tActual.contains("Season $season", true)
                }
            } ?: res.find { it.title.equals(title, true) }
            data?.id to data?.title
        }

        Log.d("Kisskh", "res: $res")

        val detailResponse = app.get(
            "$kissKhAPI/api/DramaList/Drama/$id?isq=false",
            referer = "$kissKhAPI/Drama/${getKisskhTitle(contentTitle)}?id=$id"
        )
        if (detailResponse.code != 200) return
        val resDetail = detailResponse.parsedSafe<KisskhDetail>() ?: return

        Log.d("Kisskh", "resDetail: $resDetail")

        val epsId =
            if (season == null) resDetail.episodes?.first()?.id else resDetail.episodes?.find { it.number == episode }?.id
                ?: return

        Log.d("Kisskh", "epsId: $epsId")

        val epJson = app.get("$multiDecryptAPI/enc-kisskh?text=$epsId&type=vid", referer = kissKhAPI).text

        Log.d("Kisskh", "epJson: $epJson")

        val vid_key = JSONObject(epJson).getString("result")
        val sourcesResponse = app.get(
            "$kissKhAPI/api/DramaList/Episode/$epsId.png?err=false&ts=&time=&kkey=$vid_key",
            referer = kissKhAPI
        )

        if (sourcesResponse.code != 200) return

        Log.d("Kisskh", "sourcesResponse: ${sourcesResponse.text}")

        sourcesResponse.parsedSafe<KisskhSources>()?.let { source ->
            listOf(source.video, source.thirdParty).safeAmap { link ->
                val safeLink = link ?: return@safeAmap null
                when {
                    safeLink.contains(".m3u8") || safeLink.contains(".mp4") -> {
                        callback.invoke(
                            newExtractorLink(
                                "Kisskh",
                                "Kisskh",
                                fixUrl(safeLink, kissKhAPI),
                                INFER_TYPE
                            ) {
                                referer = kissKhAPI
                                quality = Qualities.P720.value
                                headers = mapOf("Origin" to kissKhAPI)
                            }
                        )
                    }

                    else -> {
                        val cleanedLink = safeLink.substringBefore("?").takeIf { it.isNotBlank() }
                            ?: return@safeAmap null
                        loadSourceNameExtractor(
                            "Kisskh",
                            fixUrl(cleanedLink, kissKhAPI),
                            "$kissKhAPI/",
                            subtitleCallback,
                            callback,
                            Qualities.P720.value
                        )
                    }
                }
            }
        }

        val subJson = app.get("$multiDecryptAPI/enc-kisskh?text=$epsId&type=sub").text

        Log.d("Kisskh", "subJson: $subJson")

        val sub_key = JSONObject(subJson).getString("result")

        val subResponse = app.get("$kissKhAPI/api/Sub/$epsId?kkey=$sub_key", referer = kissKhAPI)

        Log.d("Kisskh", "subResponse: ${subResponse.text}")

        if (subResponse.code != 200) return

        tryParseJson<List<KisskhSubtitle>>(subResponse.text)?.forEach { sub ->
            subtitleCallback.invoke(newSubtitleFile(getLanguage(sub.label) ?: return@forEach, sub.src ?: return@forEach))
        }
    }

    suspend fun invokeTokyoInsider(
        title: String? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ) {
        val tvtype = if(episode == null) "_(Movie)" else "_(TV)"
        val firstChar = getFirstCharacterOrZero("$title").uppercase()
        val newTitle = title?.replace(" ","_")
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

    suspend fun invokeNetmirror(
        serviceName: String,
        ottCode: String,
        title: String? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ) {
        val headers = mapOf(
            "ott" to ottCode,
            "user-agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:136.0) Gecko/20100101 Firefox/136.0 /OS.GatuNewTV v1.0",
            "x-requested-with" to "NetmirrorNewTV v1.0"
        )

        val searchUrl = "$nfmirrorAPI/search.php?s=$title"
        val searchData = app.get(searchUrl, headers = headers).parsedSafe<NfSearchData>()

        Log.d("Netmirror", "$serviceName searchData: $searchData")

        val netId = searchData?.searchResult?.firstOrNull { it.t.equals("${title?.trim()}", true) }?.id ?: return

        Log.d("Netmirror", "$serviceName netId: $netId")

        val finalId = app.get("$nfmirrorAPI/post.php?id=$netId", headers = headers)
            .parsedSafe<NetflixResponse>().let { media ->
                if (season == null) {
                    netId
                } else {
                    val seasonId = media?.season?.find { it.s.toString().contains("Season $season") }?.id
                    var episodeId: String? = null
                    var page = 1

                    // Loop for episodes
                    while (episodeId == null && page < 10) {
                        val epUrl = "$nfmirrorAPI/episodes.php?id=$seasonId&page=$page"
                        val data = app.get(epUrl, headers = headers).parsedSafe<NetflixResponse>()

                        Log.d("Netmirror", "$serviceName data: $data")

                        episodeId = data?.episodes?.find { it.ep == "$episode" }?.id
                        if ((data?.nextPageShow ?: 0) != 1) break
                        page++
                    }
                    episodeId
                }
        }

        if (finalId == null) return

        Log.d("Netmirror", "$serviceName finalId: $finalId")

        val playlistUrl = "$nfmirrorAPI/player.php?id=$finalId"

        val playlist = app.get(
            playlistUrl,
            headers = headers,
        ).parsed<NfPlaylist>()

        Log.d("Netmirror", "$serviceName playlist: $playlist")

        callback.invoke(
            newExtractorLink(
                serviceName,
                serviceName,
                playlist.video_link,
                ExtractorLinkType.M3U8
            ) {
                this.referer = playlist.referer
                this.quality = Qualities.P1080.value
            }
        )
    }

    suspend fun invokeHdmovie2(
        title: String? = null,
        year: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ) {
        val headers = mapOf(
            "User-Agent" to USER_AGENT
        )

        val document = app.get("$hdmovie2API/movies/${title.createSlug()}-$year", headers = headers, allowRedirects = true).document
        val ajaxUrl = "$hdmovie2API/wp-admin/admin-ajax.php"
        val commonHeaders = mapOf(
            "Accept" to "*/*",
            "X-Requested-With" to "XMLHttpRequest"
        )

        suspend fun String.getIframe(): String = Jsoup.parse(this).select("iframe").attr("src")

        suspend fun fetchSource(post: String, nume: String, type: String): String {
            val response = app.post(
                url = ajaxUrl,
                data = mapOf(
                "action" to "doo_player_ajax",
                "post" to post,
                "nume" to nume,
                "type" to type
            ),
            referer = hdmovie2API,
            headers = commonHeaders
            ).parsed<ResponseHash>()
            return response.embed_url.getIframe()
        }

        var link = ""

        if (episode != null) {
            document.select("ul#playeroptionsul > li").getOrNull(1)?.let { ep ->
                val post = ep.attr("data-post")
                val nume = (episode + 1).toString()
                link = fetchSource(post, nume, "movie")
        }
        } else {
            document.select("ul#playeroptionsul > li")
                .firstOrNull { it.text().contains("v2", ignoreCase = true) }
                ?.let { mv ->
                    val post = mv.attr("data-post")
                    val nume = mv.attr("data-nume")
                    link = fetchSource(post, nume, "movie")
                }
        }

        val (sSlug, eSlug) = getEpisodeSlug(1, episode)

        if (link.isEmpty()) {
            document.select("a[href*=dwo]").safeAmap { anchor ->
                val anchorText = anchor.text()

                val type = if (episode != null && !anchorText.contains("ep", ignoreCase = true)) {
                    " (Combined)"
                } else {
                    ""
                }

                if (episode != null && type == "" && !anchorText.contains("ep$eSlug", ignoreCase = true)) {
                    return@safeAmap
                }

                val innerDoc = app.get(anchor.attr("href")).document
                innerDoc.select("div > p > a").safeAmap {
                    loadSourceNameExtractor(
                        "Hdmovie2$type",
                        it.attr("href"),
                        "",
                        subtitleCallback,
                        callback
                    )
                }
            }
        }

        if (link.isNotEmpty()) {
            loadSourceNameExtractor(
                "Hdmovie2",
                link,
                hdmovie2API,
                subtitleCallback,
                callback,
            )
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
        val (sSlug, eSlug) = getEpisodeSlug(1, episode)
        app.get(url).document.select("div.L a").safeAmap {
            if(!it.text().trim().startsWith("$title ($year)")) return@safeAmap
            val regex = Regex("""S\d{2}E\d{2}""", RegexOption.IGNORE_CASE)
            var singleEpEntry = false

            if (episode != null && regex.containsMatchIn(it.text())) {
                val currentEpRegex = Regex(
                    """E$eSlug""",
                    RegexOption.IGNORE_CASE
                )

                if (!currentEpRegex.containsMatchIn(it.text())) {
                    return@safeAmap
                } else {
                    singleEpEntry = true
                }
            }

            app.get(skymoviesAPI + it.attr("href")).document.select("div.Bolly > a").safeAmap {
                val text = it.text()
                if(episode == null || singleEpEntry) {
                  loadSourceNameExtractor(
                        "Skymovies",
                        it.attr("href"),
                        "",
                        subtitleCallback,
                        callback,
                    )
                }
                else if(text.contains("Episode")) {
                    if(text.contains("Episode $eSlug")) {
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

    suspend fun invokeMovies4u(
        id: String? = null,
        title: String? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ) {
        val searchQuery = if(season == null) "${title?.replace(" ", "+")}+${year}" else "${title?.replace(" ", "+")}+season+${season}"
        val searchUrl = "$movies4uAPI/?s=$searchQuery"
        val headers = mapOf(
            "Cookie" to "xla=s4t",
            "User-Agent" to "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36",
            "Referer" to "$movies4uAPI/"
        )

        val searchDoc = app.get(searchUrl, headers = headers).document
        val links = searchDoc.select("article h3 a")

        Log.d("Movies4u", "links: $links")

        links.safeAmap { element ->
            val postUrl = element.attr("href")
            val postDoc = app.get(postUrl, headers = headers).document
            val imdbId = postDoc.select("p a:contains(IMDb Rating)").attr("href")
                            .substringAfter("title/").substringBefore("/")

            Log.d("Movies4u", "imdbId: $imdbId | id: $id")

            if(imdbId != id.toString()) { return@safeAmap }

            if (season == null) {
                val innerUrl = postDoc.select("div.download-links-div a.btn").attr("href")
                val innerDoc = app.get(innerUrl, headers = headers).document
                val sourceButtons = innerDoc.select("div.downloads-btns-div a.btn")
                sourceButtons.safeAmap { sourceButton ->
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
                seasonBlocks.safeAmap { block ->
                    val headerText = block.previousElementSibling()?.text().orEmpty()
                    if (headerText.contains("Season $season", ignoreCase = true)) {
                        val seasonLink = block.selectFirst("a.btn")?.attr("href") ?: return@safeAmap

                        val episodeDoc = app.get(seasonLink, headers = headers).document
                        val episodeBlocks = episodeDoc.select("div.downloads-btns-div")

                        if (episode != null && episode in 1..episodeBlocks.size) {
                            val episodeBlock = episodeBlocks[episode - 1]
                            val episodeLinks = episodeBlock.select("a.btn")

                            episodeLinks.safeAmap { epLink ->
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

    suspend fun invokeStremioTorrents(
        sourceName: String,
        api: String,
        id: String? = null,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit,
    ) {
        val url = if(season == null) {
            "$api/stream/movie/$id.json"
        } else if(id?.contains("kitsu") == true) {
            "$api/stream/series/$id:$episode.json"
        } else {
            "$api/stream/series/$id:$season:$episode.json"
        }

        val res = app.get(url, timeout = 200L).parsedSafe<TorrentioResponse>()

        res?.streams?.forEach { stream ->

            val title = stream.title ?: stream.description ?: stream.name ?: ""
            val seedersRegex = """[👤👥]\s*(\d+)""".toRegex()
            val seeders = seedersRegex.find(title)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            val sizeRegex = """💾\s*([0-9.]+\s*[A-Za-z]+)""".toRegex()
            val fileSize = sizeRegex.find(title)?.groupValues?.get(1) ?: ""

            if (seeders < 25) return@forEach

            val magnet = buildMagnetString(stream)
            callback.invoke(
                newExtractorLink(
                    "$sourceName🧲",
                    sourceName.toSansSerifBold() + " 🧲 | 👤 $seeders ⬆️ | " + getSimplifiedTitle(title + fileSize),
                    magnet,
                    ExtractorLinkType.MAGNET,
                ) {
                    this.quality = getIndexQuality(stream.name)
                }
            )
        }
    }

    suspend fun invokeAnimetosho(
        kitsuId: String? = null,
        malId: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit,
    ) {
        val id = malId ?: kitsuId?.toIntOrNull() ?: return
        val type = if(malId == null) "kitsu_id" else "mal_id"
        val json = app.get("$anizipAPI/mappings?$type=$id").text
        val epId = getEpAnizipId(json, episode ?: 1) ?: return
        val json2 = app.get("$animetoshoAPI/json?eid=$epId").text
        val listType = object : TypeToken<List<Animetosho>>() {}.type
        val items: List<Animetosho> = globalGson.fromJson(json2, listType)
        // val filtered = items.filter { (it.seeders ?: 0) > 25 }
        // val sorted = filtered.sortedByDescending { it.seeders ?: -1 }

        val sorted = items
        .filter { (it.seeders ?: 0) >= 25 && !it.magnetUri.isNullOrBlank() }
        .sortedBy { it.totalSize?.toLongOrNull() ?: Long.MAX_VALUE }

         for (it in sorted) {
            val title = it.title ?: ""
            val s = it.seeders ?: 0
            val l = it.leechers ?: 0
            val magnet = it.magnetUri ?: continue
            val size = it.totalSize?.toLongOrNull() ?: 0L
            val sizeStr = formatSize(size)
            val type = if(
                title.contains("Dual", ignoreCase = true)
                || title.contains("DUB", ignoreCase = true)
            ) {
                "DUB"
            }
            else {
                "SUB"
            }

            val simplifiedTitle = getSimplifiedTitle(title + sizeStr)

            val displayTitle = "Animetosho [$type]".toSansSerifBold() + " 🧲 | ⬆️ $s | ⬇️ $l | $simplifiedTitle"

            callback.invoke(
                newExtractorLink(
                    "Animetosho[$type]🧲",
                    displayTitle,
                    magnet,
                    ExtractorLinkType.MAGNET,
                ) {
                    this.quality = getIndexQuality(title)
                }
            )
        }
    }

    suspend fun invokeBollyflix(
        id: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ) {
        val res1 = cfGet("$bollyflixAPI/search/$id").document

        res1.select("div > article > a").safeAmap {
            val url = it.attr("href")
            val res = cfGet(url).document
            val hTag = if (season == null) "h5" else "h4"
            val sTag = if (season == null) "" else "Season $season"
            val entries =
                res.select("div.thecontent.clearfix > $hTag:matches((?i)$sTag.*(480p|720p|1080p|2160p))")
                    .filter { element -> !element.text().contains("Download", true) }.takeLast(4)

            entries.safeAmap {
                var href = it.nextElementSibling()?.select("a")?.attr("href") ?: return@safeAmap

                if(href.contains("id=")) {
                    val token = href.substringAfter("id=")
                    val encodedurl =
                        app.get("https://web.sidexfee.com/?id=$token").text.substringAfter("link\":\"")
                            .substringBefore("\"};")
                    href = base64Decode(encodedurl)
                }

                if (season == null) {
                    loadSourceNameExtractor("Bollyflix", href , "", subtitleCallback, callback)
                } else {
                    val episodeText = "Episode " + episode.toString().padStart(2, '0')
                    val link =
                        app.get(href).document.selectFirst("article h3 a:contains($episodeText)")!!
                            .attr("href")
                    loadSourceNameExtractor("Bollyflix", link , "", subtitleCallback, callback)
                }
            }
        }
    }

    suspend fun invokeMultimovies(
        title: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val fixTitle = title.createSlug()
        val url = if (season == null) {
            "$multimoviesAPI/movies/$fixTitle"
        } else {
            "$multimoviesAPI/episodes/$fixTitle-${season}x${episode}"
        }

        val req = app.get(url).document
        req.select("ul#playeroptionsul li").map {
            Triple(
                it.attr("data-post"),
                it.attr("data-nume"),
                it.attr("data-type")
            )
        }.safeAmap { (id, nume, type) ->
            if (!nume.contains("trailer")) {
                val source = app.post(
                    url = "$multimoviesAPI/wp-admin/admin-ajax.php",
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
                        loadSourceNameExtractor("Multimovies", link, referer = multimoviesAPI, subtitleCallback, callback)
                    }
                    else -> ""
                }
            }
        }
    }

    suspend fun invokeAnimepahe(
        url: String? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val headers = mapOf(
            "User-Agent" to USER_AGENT,
            "Cookie" to "__ddg2_=1234567890"
        )

        val id = app.get(url ?: return, headers).document.selectFirst("meta[property=og:url]")
            ?.attr("content").toString().substringAfterLast("/")

        val animeData =
            app.get("$animepaheAPI/api?m=release&id=$id&sort=episode_asc&page=1", headers)
                .parsedSafe<animepahe>()?.data
        val session = if(episode == null) {
            animeData?.firstOrNull()?.session ?: return
        } else {
            animeData?.getOrNull(episode-1)?.session ?: return
        }
        val doc = app.get("$animepaheAPI/play/$id/$session", headers).document

        runLimitedAsync( concurrency = 2,
            {
                doc.select("div#pickDownload > a").safeAmap {
                    val href = it.attr("href")
                    var type = "SUB"
                    if(it.select("span").text().contains("eng")) type = "DUB"

                    Log.d("Animepahe", "href: $href")

                    loadCustomExtractor(
                        "Animepahe [$type]",
                        href,
                        "",
                        subtitleCallback,
                        callback,
                        getIndexQuality(it.text())
                    )
                }
            },
            {
                doc.select("div#resolutionMenu > button").safeAmap {
                    var type = "SUB"
                    if(it.select("span").text().contains("eng")) type = "DUB"
                    val quality = it.attr("data-resolution")
                    val href = it.attr("data-src")
                    if (href.contains("kwik.cx")) {
                        loadCustomExtractor(
                            "Animepahe(VLC) [$type]",
                            href,
                            "",
                            subtitleCallback,
                            callback,
                            getQualityFromName(quality)
                        )
                    }
                }
            },
        )
    }

    suspend fun invoke4khdhub(
        title: String? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ) {
        val document = app.get("$fourkhdhubAPI/?s=$title").document
        val link = document.select("div.card-grid > a").firstOrNull { element ->
            val content = element.selectFirst("div.movie-card-content")?.text()?.lowercase() ?: return@firstOrNull false
            val matchTitle = title?.lowercase()?.let { it in content } ?: true
            val matchYear = year?.toString()?.lowercase()?.let { it in content } ?: true
            matchTitle && matchYear
        }?.attr("href") ?: return

        val doc = app.get("$fourkhdhubAPI$link").document

        if(season == null) {
            doc.select("div.download-item a").safeAmap {
               val source = getRedirectLinks(it.attr("href"))
               loadSourceNameExtractor(
                    "4Khdhub",
                    source,
                    "",
                    subtitleCallback,
                    callback
                )
            }
        } else {
            val (seasonText, episodeText) = getEpisodeSlug(season, episode)

            doc.select("div.episode-download-item:has(div.episode-file-title:contains(S${seasonText}E${episodeText}))").safeAmap {
                it.select("div.episode-links > a").safeAmap {
                    val source = getRedirectLinks(it.attr("href"))
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
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ) {
        val url = "$MostraguardaAPI/movie/$id"
        val doc = app.get(
            url,
            headers = mapOf(
                "User-Agent" to "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
            )
        ).document

        doc.select("ul > li").safeAmap {
            if(it.text().contains("supervideo")) {
                val source = "https:" + it.attr("data-link")
                SuperVideo().getUrl(source, "", subtitleCallback, callback)
            }
        }
    }

    suspend fun invokeWYZIESubs(
        id: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
    ) {
        val url = if(season != null) "$WYZIESubsAPI/search?id=$id&season=$season&episode=$episode&source=all&key=${Settings.getWyzieSubsKey() ?: return}" else "$WYZIESubsAPI/search?id=$id&source=all&key=${Settings.getWyzieSubsKey() ?: return}"
        val json = app.get(url, timeout = 10000).text
        Log.d("WyzieSubs", "Received subtitle response: $json")
        val data = parseJson<ArrayList<WYZIESubtitle>>(json)

        data.forEach {
            val lang = it.display ?: it.language
            subtitleCallback.invoke(
                newSubtitleFile(
                    getLanguage(lang) ?: return@forEach,
                    it.url
                )
            )
        }
    }

    suspend fun invokeVegamovies(
        sourceName: String,
        id: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        if(id == null) return
        val api = if (sourceName == "VegaMovies") vegamoviesAPI else rogmoviesAPI
        val searchUrl = "$api/search.php?q=$id&page=1"
        val json = app.get(searchUrl).text
        val movieUrls = tryParseJson<VegaSearchResponse>(json)?.hits?.map { hit ->
            val permalink = hit.document.permalink
            fixUrl(permalink, api)
        } ?: emptyList()

        movieUrls.safeAmap { pageUrl ->
            val res = app.get(pageUrl).document
            val currentId = res.select("a[href*=\"imdb\"]").attr("href").substringAfter("title/").substringBefore("/")
            if(currentId != id) return@safeAmap

            if(season == null) {
                res.select("button.dwd-button").safeAmap {
                    val link = it.parent()?.attr("href") ?: return@safeAmap
                    val doc = app.get(link).document
                    doc.select("p > a").safeAmap { source ->
                        loadSourceNameExtractor(sourceName, source.attr("href"), referer = "", subtitleCallback, callback)
                    }
                }
            }
            else {
                res.select("h4:matches((?i)(Season $season)), h3:matches((?i)(Season $season))").safeAmap { h4 ->
                    h4.nextElementSibling()?.select("a:matches((?i)(V-Cloud|Single|Episode|G-Direct))")?.safeAmap {
                        val doc = app.get(it.attr("href")).document
                        val epLink = doc.selectFirst("h4:contains(Episode):contains($episode)")
                            ?.nextElementSibling()
                            ?.selectFirst("a:matches((?i)(V-Cloud))")
                            ?.attr("href")
                            ?: return@safeAmap
                        loadSourceNameExtractor(sourceName, epLink, referer = "", subtitleCallback, callback)
                    }
                }
            }
        }
    }

    suspend fun invokeMoviesdrive(
        title: String? = null,
        imdbId: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val url = "$moviesdriveAPI/searchapi.php?q=$imdbId"
        val jsonString = app.get(url).text
        val root = JSONObject(jsonString)
        if (!root.has("hits")) return
        val hits = root.getJSONArray("hits")

        for (i in 0 until hits.length()) {
            val hit = hits.getJSONObject(i)
            val doc = hit.getJSONObject("document")
            val currentImdbId = doc.optString("imdb_id")
            if(imdbId == currentImdbId) {
                val document = app.get(moviesdriveAPI + doc.optString("permalink")).document
                if (season == null) {
                    document.select("h5 > a").safeAmap {
                        val href = it.attr("href")
                        val server = extractMdrive(href)
                        server.safeAmap {
                            loadSourceNameExtractor("MoviesDrive",it, "", subtitleCallback, callback)
                        }
                    }
                } else {
                    val (sSlug, eSlug) = getEpisodeSlug(season, episode)
                    val stag = "Season $season|S$sSlug"
                    val sep = "Ep$eSlug|Ep$episode"
                    val entries = document.select("h5:matches((?i)$stag)")
                    entries.safeAmap { entry ->
                        val href = entry.nextElementSibling()?.selectFirst("a")?.attr("href") ?: ""

                        if (href.isNotBlank()) {
                            val doc = app.get(href).document
                            val fEp = doc.selectFirst("h5:matches((?i)$sep)")
                            val linklist = mutableListOf<String>()
                            val source1 = fEp?.nextElementSibling()?.selectFirst("a")?.attr("href")
                            val source2 = fEp?.nextElementSibling()?.nextElementSibling()?.selectFirst("a")?.attr("href")
                            if (source1 != null) linklist.add(source1)
                            if (source2 != null) linklist.add(source2)

                            linklist.safeAmap { url ->
                                loadSourceNameExtractor(
                                    "MoviesDrive",
                                    url,
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

    suspend fun invokeUhdmovies(
        title: String? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit,
        subtitleCallback: (SubtitleFile) -> Unit
    ) {
        val url = cfGet("$uhdmoviesAPI/search/$title $year").document
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

        links.safeAmap {
            if(!it.isNullOrEmpty()) {
                val driveLink = if(it.contains("driveleech") || it.contains("driveseed")) {
                    val baseUrl = getBaseUrl(it)
                    val text = app.get(it).text
                    val regex = Regex("""window\.location\.replace\(["'](.*?)["']\)""")
                    val fileId = regex.find(text)?.groupValues?.get(1) ?: return@safeAmap
                    baseUrl + fileId
                } else {
                    bypassHrefli(it) ?: return@safeAmap
                }
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
        imdbId: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val hTag = if (season == null) "h3" else "div.single_post h3"
        val aTag = if (season == null) "Download" else "G-Drive"
        val sTag = if (season == null) "" else "(Season $season)"

        app.get("$topmoviesAPI/search/$imdbId").document.select("#content_box article > a").safeAmap { element ->
            val res = app.get(
                element.attr("href"),
                headers = mapOf("User-Agent" to USER_AGENT)
            ).document

            val entries = if (season == null) {
                res.select("$hTag:matches((?i)$sTag.*(480p|720p|1080p|2160p|4K))")
                    .filter { element -> !element.text().contains("Batch/Zip", true) && !element.text().contains("Info:", true) }.reversed()
            } else {
                res.select("$hTag:matches((?i)$sTag.*(480p|720p|1080p|2160p|4K))")
                    .filter { element -> !element.text().contains("Batch/Zip", true) || !element.text().contains("720p & 480p", true) || !element.text().contains("Series Info", true)}
            }

            entries.safeAmap {
                val source =
                    it.nextElementSibling()?.select("a.maxbutton:contains($aTag)")?.attr("href")
                val selector =
                    if (season == null) "a.maxbutton-5:contains(Server)" else "h3 a:matches(Episode $episode)"
                if (!source.isNullOrEmpty()) {
                    app.get(
                        source,
                        headers = mapOf("User-Agent" to USER_AGENT)
                    ).document.selectFirst(selector)
                        ?.attr("href")?.let {
                            val link = bypassHrefli(it).toString()
                            loadSourceNameExtractor("Topmovies", link, referer = "", subtitleCallback, callback)
                        }
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
            moviesmodAPI
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
        var href = cfGet(url).document.selectFirst("#content_box article > a")?.attr("href")

        Log.d("Moviesmod", "$href")

        val hTag = if (season == null) "h4" else "h3"
        val aTag = if (season == null) "Download" else "Episode"
        val sTag = if (season == null) "" else "(S0$season|Season $season)"
        val res = app.get(
            href ?: return,
        ).document

        val entries = res.select("div.thecontent $hTag:matches((?i)$sTag.*(480p|720p|1080p|2160p))")
        .filter { element ->
            val text = element.text()
            !text.contains("MoviesMod", true)
        }

        Log.d("Moviesmod", "$entries")

        entries.safeAmap { it ->
            var link =
                it.nextElementSibling()?.select("a:contains($aTag)")?.attr("href")
                    ?.substringAfter("=") ?: ""

            Log.d("Moviesmod", "$link")
            //link = base64Decode(href)

            val selector =
                if (season == null) "p a.maxbutton" else "h3 a:matches(Episode $episode)"

            if (link.isNotEmpty()) {
                val source = app.get(link).document.selectFirst(selector)?.attr("href") ?: return@safeAmap
                val bypassedLink = bypassHrefli(source).toString()
                loadSourceNameExtractor("Moviesmod", bypassedLink, "", subtitleCallback, callback)
            }
        }
    }

    suspend fun invokeAnizone(
        title: String? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val url = "$anizoneAPI/anime?search=$title"

        val link = app.get(url).document.select("div.truncate > a").firstOrNull {
            it.text().contains(title.toString(), ignoreCase = true)
        }?.attr("href") ?: return

        val document = app.get("$link/$episode").document

        val subtitles = document.select("track").map {
            subtitleCallback.invoke(
                newSubtitleFile(
                    getLanguage(it.attr("label")) ?: it.attr("label"),
                    it.attr("src")
                )
            )
        }

        val source = document.select("media-player").attr("src")
        callback.invoke(
            newExtractorLink(
                "Anizone",
                "Anizone Multi Audio 🌐",
                source,
                type = ExtractorLinkType.M3U8,
            ) {
                this.quality = Qualities.P1080.value
            }
        )
    }

    suspend fun invokeAllanime(
        name: String? = null,
        year: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val privatereferer = "https://allmanga.to"
        val ephash = "d405d0edd690624b66baba3068e0edc3ac90f1597d898a1ec8db4e5c43c00fec"
        val queryhash = "a24c500a1b765c68ae1d8dd85174931f661c71369c89b92b88b75a725afc471c"

        var type = ""
        if (episode == null) {
            type = "Movie"
        } else {
            type = "TV"
        }

        val query = """$AllanimeAPI?variables={"search":{"types":["$type"],"query":"$name"},"limit":26,"page":1,"translationType":"sub","countryOrigin":"ALL"}&extensions={"persistedQuery":{"version":1,"sha256Hash":"$queryhash"}}"""
        val res = app.get(query, referer = privatereferer)
        val response = res.parsedSafe<Anichi>()?.data?.shows?.edges

        val headers =
            mapOf(
                "app-version" to "android_c-253",
                "platformstr" to "android_c",
                "Referer" to privatereferer,
                "from-app" to base64Decode("YW5pbWVjaGlja2Vu")
            )

        if (response != null) {
            val id = response.firstOrNull()?.id ?: return
            val langType = listOf("sub", "dub")
            for (i in langType) {
                val epData =
                    """$AllanimeAPI?variables={"showId":"$id","translationType":"$i","episodeString":"${episode ?: 1}"}&extensions={"persistedQuery":{"version":1,"sha256Hash":"$ephash"}}"""

                Log.d("Allanime", "Episode Data URL: $epData")

                val eplinks = app.get(epData, headers = headers)
                    .parsedSafe<AnichiEP>()?.data?.episode?.sourceUrls

                Log.d("Allanime", "Episode Links: $eplinks")

                eplinks?.safeAmap { source ->
                    safeApiCall {
                        val sourceUrl = source.sourceUrl
                        if (sourceUrl.startsWith("http")) {
                            val sourcename = sourceUrl.getHost()
                            loadCustomExtractor(
                                "Allanime [${i.uppercase()}] [$sourcename]",
                                sourceUrl,
                                "",
                                subtitleCallback,
                                callback,
                            )
                        }
                        else if (URI(sourceUrl).isAbsolute || sourceUrl.startsWith("//")) {
                            val fixedLink = if (sourceUrl.startsWith("//")) "https:$sourceUrl" else sourceUrl
                            val host = fixedLink.getHost()

                            loadCustomExtractor(
                                "Allanime [$host]  [${i.uppercase()}]",
                                fixedLink,
                                "",
                                subtitleCallback,
                                callback
                            )

                            return@safeApiCall
                        }

                        else {
                            val decodedlink = if (sourceUrl.startsWith("--"))
                            {
                                decrypthex(sourceUrl)
                            }
                            else sourceUrl
                            val fixedLink = decodedlink.fixUrlPath()
                            val links = try {
                                app.get(fixedLink, headers = headers).parsedSafe<AnichiVideoApiResponse>()?.links ?: emptyList()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                return@safeApiCall
                            }
                            links.forEach { server ->
                                val host = server.link.getHost()
                                when {
                                    source.sourceName.contains("Default") && (server.resolutionStr == "SUB" || server.resolutionStr == "Alt vo_SUB") -> {
                                        getM3u8Qualities(
                                            server.link,
                                            "https://static.crunchyroll.com/",
                                            "Allanime [SUB] $host"
                                        ).forEach(callback)
                                    }

                                    server.hls == null -> {
                                        callback.invoke(
                                            newExtractorLink(
                                                "Allanime [${i.uppercase()}] ${host.replaceFirstChar { it.uppercase() }}",
                                                "Allanime [${i.uppercase()}] ${host.replaceFirstChar { it.uppercase() }}",
                                                server.link,
                                                INFER_TYPE
                                            )
                                            {
                                                this.quality = Qualities.P1080.value
                                            }
                                        )
                                    }

                                    server.hls == true -> {
                                        val endpoint = "https://allanime.day/player?uri=" +
                                                (if (URI(server.link).host.isNotEmpty())
                                                    server.link
                                                else "https://allanime.day" + URI(server.link).path)

                                        getM3u8Qualities(server.link, server.headers?.referer ?: endpoint, "Allanime [SUB] $host").forEach(callback)
                                    }

                                    else -> {
                                        server.subtitles?.forEach { sub ->
                                            val lang = SubtitleHelper.fromTagToEnglishLanguageName(sub.lang ?: "") ?: sub.lang.orEmpty()
                                            val src = sub.src ?: return@forEach
                                            subtitleCallback(newSubtitleFile(getLanguage(lang) ?: "", httpsify(src)))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun invokePrimeSrc(
        imdbId: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {

        val headers = mapOf(
            "Referer" to "$PrimeSrcApi/",
            "User-Agent" to USER_AGENT
        )
        val url = if (season == null) {
            "$PrimeSrcApi/api/v1/s?imdb=$imdbId&type=movie"
        } else {
            "$PrimeSrcApi/api/v1/s?imdb=$imdbId&season=$season&episode=$episode&type=tv"
        }

        val serverJson = app.get(url, timeout = 30, headers = headers).text

        val serverList = tryParseJson<PrimeSrcServerList>(serverJson) ?: return

        serverList.servers?.safeAmap {
            Log.d("Primesrc", "it: $it")
            val rawServerJson = cfGet("$PrimeSrcApi/api/v1/l?key=${it.key}", headers).text
            //val rawServerJson = app.get("$PrimeSrcApi/api/v1/l?key=${it.key}", timeout = 30, headers = headers).text
            val jsonObject = JSONObject(rawServerJson)
            loadSourceNameExtractor("PrimeWire", jsonObject.optString("link",""), PrimeSrcApi, subtitleCallback, callback)
        }

    }

    suspend fun invokeProjectfreetv(
        title: String? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val query = if(season == null) {
            "$title".replace(" ", "+")
        } else {
            "${title?.replace(" ", "+")}+-+season+$season"
        }

        val seacrhUrl = "$projectfreetvAPI/data/browse/?lang=3&keyword=$query&year=$year&networks=&rating=&votes=&genre=&country=&cast=&directors=&type=&order_by=&page=1&limit=1"
        val searchJson = app.get(seacrhUrl, referer = projectfreetvAPI, timeout = 60L).text
        val searchObject = JSONObject(searchJson)
        val moviesArray = searchObject.getJSONArray("movies")
        if (moviesArray.length() == 0) return
        val id = moviesArray.getJSONObject(0).getString("_id")
        if(id.isEmpty()) return
        val jsonString = app.get("$projectfreetvAPI/data/watch/?_id=$id", referer = projectfreetvAPI, timeout = 60L).text

        val rootObject = JSONObject(jsonString)

        val sourceList = mutableListOf<String>()

        if (rootObject.has("streams")) {
            val streamsArray = rootObject.getJSONArray("streams")

            for (i in 0 until streamsArray.length()) {
                val item = streamsArray.getJSONObject(i)
                val currentEpisode = item.optString("e").toIntOrNull() ?: -1
                if (episode == null || currentEpisode == episode) {
                    val source = item.optString("stream")
                    if (source.isNotEmpty()) {
                        sourceList.add(source)
                    }
                }
            }
        }

        sourceList.safeAmap {
            loadSourceNameExtractor("ProjectFreeTV", it, "", subtitleCallback, callback)
        }
    }

    suspend fun invokeVicSrcWtf(
        tmdbId: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit,
        subtitleCallback: (SubtitleFile) -> Unit,
    ) {
        val referer = "https://www.vidsrc.wtf"
        val headers = mapOf(
            "Origin" to referer,
            "Referer" to "$referer/",
            "User-Agent" to USER_AGENT,
        )

        try {
            val mainServer = if(season == null) "$vidSrcApi/main/movie/$tmdbId" else "$vidSrcApi/main/tv/$tmdbId/$season/$episode"
            val mainResponse = app.get(mainServer, timeout = 30, headers = headers).text
            val jsonObject = JSONObject(mainResponse)
            val streamUrl = jsonObject.getJSONObject("stream").getString("url")
            M3u8Helper.generateM3u8(
                "VidSrc [Main]",
                streamUrl,
                referer,
            ).forEach(callback)
        } catch (e: Exception) {}

        try {
            val hindiServer =
                if (season == null) "$vidSrcHindiApi/movie/$tmdbId" else "$vidSrcHindiApi/tv/$tmdbId/$season/$episode"
            val hindiResponse = app.get(hindiServer, timeout = 30, headers = headers).text
            val hindiJsonObject = JSONObject(hindiResponse)
            val streamsArray = hindiJsonObject.getJSONArray("streams")
            for (i in 0 until streamsArray.length()) {
                val streamObj = streamsArray.getJSONObject(i)
                val language = streamObj.getString("language")
                val url = streamObj.getString("url")
                // Convert headers JSONObject to Map<String, String>
                val headersJson = streamObj.getJSONObject("headers")
                val headersMap = mutableMapOf<String, String>()
                headersJson.keys().forEach { key ->
                    headersMap[key] = headersJson.getString(key)
                }
                M3u8Helper.generateM3u8(
                    "VidSrc [$language]",
                    url,
                    headersJson.getString("Referer"),
                    headers = headersMap
                ).forEach(callback)

            }
        } catch (e: Exception) { }

        try {
            val embededServer = if(season == null) "$vidSrcApi/premium_embeds/movie/$tmdbId" else "$vidSrcApi/premium_embeds/tv/$tmdbId/$season/$episode"
            val embededResponse = app.get(embededServer, timeout = 30, headers = headers).text
            val embededJsonObject = JSONObject(embededResponse)
            val linksArray = embededJsonObject.getJSONArray("links")
            for (i in 0 until linksArray.length()) {
                val streamObj = linksArray.getJSONObject(i)
                val url = streamObj.getString("url")
                loadSourceNameExtractor("VidSrc", url, "", subtitleCallback, callback)
            }
        } catch (e: Exception) { }
    }

    suspend fun invokeVidzee(
        id: Int?,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val secret = base64Decode("cGxlYXNlZG9udHNjcmFwZW1lc2F5d2FsbGFoaQ==")
        val defaultReferer = "https://player.vidzee.wtf/"

        (1..13).toList().safeAmap { sr ->
            try {
                val apiUrl = if (season == null) {
                    "$vidzeeApi/api/server?id=$id&sr=$sr"
                } else {
                    "$vidzeeApi/api/server?id=$id&sr=$sr&ss=$season&ep=$episode"
                }

                val response = app.get(apiUrl).text
                val json = JSONObject(response)

                val globalHeaders = mutableMapOf<String, String>()
                json.optJSONObject("headers")?.let { headersObj ->
                    headersObj.keys().forEach { key ->
                        globalHeaders[key] = headersObj.getString(key)
                    }
                }

                val urls = json.optJSONArray("url") ?: JSONArray()
                for (i in 0 until urls.length()) {
                    val obj = urls.getJSONObject(i)
                    val encryptedLink = obj.optString("link")
                    val name = obj.optString("name", "")
                    val type = obj.optString("type", "hls")
                    val lang = obj.optString("lang", "Unknown")
                    val flag = obj.optString("flag", "")

                    if (encryptedLink.isNotBlank()) {
                        val finalUrl = decryptVidzeeUrl(encryptedLink, secret) ?: continue
                        if(!finalUrl.contains("https:")) continue
                        val headersMap = mutableMapOf<String, String>()
                        headersMap.putAll(globalHeaders)
                        val referer = headersMap["referer"] ?: defaultReferer
                        val displayName =
                            if (flag.isNotBlank()) "VidZee $name ($lang - $flag)" else " VidZee$name ($lang)"

                        callback.invoke(
                            newExtractorLink(
                                "VidZee",
                                displayName,
                                finalUrl,
                                if (type.equals("hls", ignoreCase = true))
                                    ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO
                            ) {
                                this.referer = referer
                                this.headers = headersMap
                                this.quality = Qualities.P1080.value
                            }
                        )
                    }
                }

                val subs = json.optJSONArray("tracks") ?: JSONArray()
                for (i in 0 until subs.length()) {
                    val sub = subs.getJSONObject(i)
                    val subLang = sub.optString("lang", "Unknown")
                    val subUrl = sub.optString("url")
                    if (subUrl.isNotBlank()) subtitleCallback(newSubtitleFile(subLang, subUrl))
                }

            } catch (e: Exception) {
                Log.e("VidzeeApi", "Failed sr=$sr: $e")
            }
        }
    }

    suspend fun invokeBollywood(
        title: String? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit
    ) {
        val (seasonSlug, episodeSlug) = getEpisodeSlug(season, episode)
        val titleSlug = title?.replace(" ", ".")
        val headers = mapOf(
            "Origin" to bollywoodBaseAPI,
            "Referer" to "$bollywoodBaseAPI/",
            "User-Agent" to USER_AGENT,
            "Authorization" to "Bearer ${Settings.getGramCinemaToken() ?: return}"
        )

        Log.d("Bollywood", "Headers: $headers")

        val url = if (season == null) {
            "$bollywoodAPI/files/search?q=${titleSlug}.${year}&page=1"
        } else {
            "$bollywoodAPI/files/search?q=${titleSlug}.S${seasonSlug}E${episodeSlug}&page=1"
        }

        val response = app.get(
            url,
            headers = headers,
            timeout = 300000
        ).text

        Log.d("Bollywood", "Response: $response")

        val jsonObject = JsonParser.parseString(response).asJsonObject

        if (jsonObject.has("files")) {
            val filesArray = jsonObject.getAsJsonArray("files")

            filesArray.forEach { element ->
                val item = element.asJsonObject
                val fileName = item.get("file_name").asString
                if(fileName.contains(".$titleSlug")) return@forEach
                val fileId = item.get("id").asString
                Log.d("Bollywood", "Processing file ID: $fileId")
                val size = formatSize(item.get("file_size").asString.toLong())
                val res = app.get(
                    "$bollywoodAPI/genLink?type=files&id=$fileId",
                    headers = headers
                ).text
                Log.d("Bollywood", "Link response for file ID $fileId: $res")

                val linkJson = JsonParser.parseString(res).asJsonObject
                if (linkJson.has("url")) {
                    val streamUrl = linkJson.get("url").asString
                    val simplifiedTitle = getSimplifiedTitle("$fileName $size")

                    callback.invoke(
                        newExtractorLink(
                            "GramCinema",
                            "[GramCinema]".toSansSerifBold() + " ${simplifiedTitle}",
                            streamUrl,
                            ExtractorLinkType.VIDEO
                        ) {
                            this.quality = getIndexQuality(fileName)
                            this.referer = bollywoodBaseAPI
                        }
                    )
                }
            }
        }
    }

    suspend fun invokeMoviebox(
        title: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        fun unwrapData(json: JSONObject): JSONObject {
            val data = json.optJSONObject("data") ?: return json
            return data.optJSONObject("data") ?: data
        }

        val HOST = "h5.aoneroom.com"
        val BASE_URL = "https://$HOST"
        val SEASON_SUFFIX_REGEX = """\sS\d+(?:-S?\d+)*$""".toRegex(RegexOption.IGNORE_CASE)

        val baseHeaders = mapOf(
            "X-Client-Info"   to "{\"timezone\":\"Africa/Nairobi\"}",
            "Accept-Language" to "en-US,en;q=0.5",
            "Accept"          to "application/json",
            "Referer"         to BASE_URL,
            "Host"            to HOST,
            "Connection"      to "keep-alive"
        )

        app.get("$BASE_URL/wefeed-h5-bff/app/get-latest-app-pkgs?app_name=moviebox", headers = baseHeaders)

        val subjectType = if (season != null) 2 else 1
        val searchObj = try {
            JSONObject(
                app.post(
                    "$BASE_URL/wefeed-h5-bff/web/subject/search",
                    headers = baseHeaders,
                    json = mapOf(
                        "keyword"     to title,
                        "page"        to 1,
                        "perPage"     to 24,
                        "subjectType" to subjectType
                    )
                ).text
            )
        } catch (e: Exception) { return }

        val items = unwrapData(searchObj).optJSONArray("items") ?: return

        val titleMatchRegex = """^${Regex.escape(title ?: "")}(?: \[([^\]]+)\])?$""".toRegex(RegexOption.IGNORE_CASE)
        val uniqueIdsWithLang = mutableMapOf<String, String>()

        for (i in 0 until items.length()) {
            val item = items.optJSONObject(i) ?: continue
            val id = item.optString("subjectId")
            if (id.isEmpty()) continue
            val cleanTitle = item.optString("title", "").replace(SEASON_SUFFIX_REGEX, "")
            val matchResult = titleMatchRegex.find(cleanTitle) ?: continue
            val language = matchResult.groups[1]?.value ?: "Original"
            uniqueIdsWithLang.putIfAbsent(id, language)
        }

        if (uniqueIdsWithLang.isEmpty()) return

        uniqueIdsWithLang.forEach { (subjectId, language) ->
            val detailObj = try {
                JSONObject(
                    app.get(
                        "$BASE_URL/wefeed-h5-bff/web/subject/detail?subjectId=$subjectId",
                        headers = baseHeaders
                    ).text
                )
            } catch (e: Exception) { return@forEach }

            val detailPath = unwrapData(detailObj).optJSONObject("subject")?.optString("detailPath") ?: ""

            val params = buildString {
                append("subjectId=$subjectId")
                if (season != null) append("&se=$season&ep=$episode")
            }

            val downloadHeaders = baseHeaders + mapOf(
                "Referer" to "https://fmoviesunblocked.net/spa/videoPlayPage/movies/$detailPath?id=$subjectId&type=/movie/detail",
                "Origin"  to "https://fmoviesunblocked.net"
            )

            val sourceObj = try {
                JSONObject(
                    app.get(
                        "$BASE_URL/wefeed-h5-bff/web/subject/download?$params",
                        headers = downloadHeaders
                    ).text
                )
            } catch (e: Exception) { return@forEach }

            val sourceData = unwrapData(sourceObj)

            val downloads = sourceData.optJSONArray("downloads")
            if (downloads != null) {
                for (i in 0 until downloads.length()) {
                    val d = downloads.optJSONObject(i) ?: continue
                    val dlink = d.optString("url")
                    if (dlink.isNotEmpty()) {
                        callback.invoke(
                            newExtractorLink(
                                "MovieBox [$language]",
                                "MovieBox [$language]",
                                dlink,
                            ) {
                                this.headers = mapOf(
                                    "Referer" to "https://fmoviesunblocked.net/",
                                    "Origin"  to "https://fmoviesunblocked.net"
                                )
                                this.quality = d.optInt("resolution")
                            }
                        )
                    }
                }
            }

            val subtitles = sourceData.optJSONArray("captions")
            if (subtitles != null) {
                for (i in 0 until subtitles.length()) {
                    val s = subtitles.optJSONObject(i) ?: continue
                    val slink = s.optString("url")
                    if (slink.isNotEmpty()) {
                        val lan = s.optString("lan")
                        subtitleCallback.invoke(
                            newSubtitleFile(getLanguage(lan) ?: lan, slink)
                        )
                    }
                }
            }
        }
    }

    suspend fun invokeStreamioStreamsGlobal(
        sourceName: String,
        api: String,
        imdbId: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val url = if(season == null) {
            "$api/stream/movie/$imdbId.json"
        } else {
            "$api/stream/series/$imdbId:$season:$episode.json"
        }

        globalGson.fromJson(app.get(url, timeout = 100000L).text, StreamifyResponse::class.java).streams.forEach { s ->
            val title = s.description ?: s.title ?: s.name ?: ""

            val type = if(s.url.contains(".m3u8") || s.url.contains("hls")) {
                ExtractorLinkType.M3U8
            } else {
                INFER_TYPE
            }

            if(s.url.contains("video-downloads.googleusercontent") && Settings.allowDownloadLinks == false) return@forEach

            val proxyReq = s.behaviorHints?.proxyHeaders?.request
            val stdHeaders = s.behaviorHints?.headers

            val extractedReferer = proxyReq?.Referer ?: stdHeaders?.get("Referer") ?: stdHeaders?.get("referer") ?: ""
            val extractedOrigin = proxyReq?.Origin ?: stdHeaders?.get("Origin") ?: stdHeaders?.get("origin") ?: ""
            val extractedUserAgent = proxyReq?.userAgent ?: stdHeaders?.get("User-Agent") ?: stdHeaders?.get("user-agent") ?: USER_AGENT

            val quality = getIndexQuality(title)

            callback.invoke(
                newExtractorLink(
                    sourceName,
                    "[$sourceName] $title",
                    s.url,
                    type
                ) {
                    this.quality = quality
                    this.headers = mapOf(
                        "User-Agent" to extractedUserAgent,
                        "Referer" to extractedReferer,
                        "Origin" to extractedOrigin
                    ).filterValues { it.isNotBlank() }
                }
            )
        }
    }

    suspend fun invokeStremioSubtitlesGlobal(
        sourceName: String,
        api: String,
        imdbId: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
    ) {
        val url = if(season != null) {
            "$api/subtitles/series/$imdbId:$season:$episode.json"
        } else {
            "$api/subtitles/movie/$imdbId.json"
        }

        val json = app.get(url, timeout = 100000L).text
        val subtitleResponse = globalGson.fromJson(json, StremioSubtitleResponse::class.java)

        subtitleResponse.subtitles.forEach {
            val lang = it.lang ?: it.lang_code
            val fileUrl = it.url
            if(lang != null && fileUrl != null) {
                subtitleCallback.invoke(
                    newSubtitleFile(
                        getLanguage(lang) ?: lang,
                        fileUrl,
                    )
                )
            }
        }
    }

    suspend fun invokeStremioTorrentsGlobal(
        sourceName: String,
        api: String,
        imdbId: String? = null,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit,
    ) {
        val url = if(season == null) {
            "$api/stream/movie/$imdbId.json"
        } else {
            "$api/stream/series/$imdbId:$season:$episode.json"
        }

        val res = app.get(url, timeout = 100000L).parsedSafe<TorrentioResponse>()

        res?.streams?.forEach { stream ->

            val title = stream.description ?: stream.title ?: stream.name ?: ""
            val magnet = buildMagnetString(stream)

            callback.invoke(
                newExtractorLink(
                    "$sourceName🧲",
                    "[$sourceName] 🧲 $title",
                    magnet,
                    ExtractorLinkType.MAGNET,
                ) {
                    this.quality = getIndexQuality(title)
                }
            )
        }
    }

    suspend fun invokeVidsrcCC(
        imdbId: String? = null,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit
    ) {
        val headers = mapOf(
            "User-Agent" to USER_AGENT,
            "Referer"    to "$vidsrcCCAPI/"
        )

        val type = if (season == null) "movie" else "tv"

        val embedUrl = if (season != null && episode != null)
            "$vidsrcCCAPI/v2/embed/$type/$imdbId/$season/$episode"
        else
            "$vidsrcCCAPI/v2/embed/$type/$imdbId"

        val html = app.get(embedUrl, headers = headers).text

        val v       = Regex("""var v = "(.*?)";""").find(html)?.groupValues?.get(1) ?: return
        val userId  = Regex("""var userId = "(.*?)";""").find(html)?.groupValues?.get(1) ?: return
        val movieId = Regex("""var movieId = "(.*?)";""").find(html)?.groupValues?.get(1) ?: return

        Log.d("VidsrcCC", "Extracted v: $v, userId: $userId, movieId: $movieId")

        val encrypted = JSONObject(
            app.get("$multiDecryptAPI/enc-vidsrc?user_id=$userId&movie_id=$movieId").text
        ).optString("result").ifEmpty { return }

        Log.d("VidsrcCC", "Extracted encrypted: $encrypted")

        val serversUrl = buildString {
            append("$vidsrcCCAPI/api/$movieId/servers")
            append("?id=$movieId&type=$type&v=$v&vrf=$encrypted&imdbId=$imdbId")
            if (season  != null) append("&season=$season")
            if (episode != null) append("&episode=$episode")
        }

        val serversData  = JSONObject(app.get(serversUrl, headers = headers).text)

        Log.d("VidsrcCC", "serversData: $serversData")

        val serversArray = serversData.optJSONArray("data") ?: return

        val servers = (0 until serversArray.length()).associate {
            val obj = serversArray.getJSONObject(it)
            obj.optString("name") to obj.optString("hash")
        }

        try {
            servers["VidPlay"]?.let { hash ->
                val jsonResponse = JSONObject(app.get("$vidsrcCCAPI/api/source/$hash", headers = headers).text)
                val dataObject = jsonResponse.optJSONObject("data") ?: return@let
                val streamUrl = dataObject.optString("source")
                if(streamUrl.isNullOrBlank()) return@let

                callback.invoke(
                    newExtractorLink(
                        "VidsrcCC [VidPlay]",
                        "VidsrcCC [VidPlay]",
                        streamUrl,
                        ExtractorLinkType.M3U8
                    ) {
                        this.headers = headers
                        this.quality = 1080
                    }
                )
            }
        } catch (e: Exception) {
            Log.w("Vidsrc", "Failed to extract server: VidPlay")
        }

        try {
            servers["UpCloud"]?.let { hash ->
                val data = JSONObject(app.get("$vidsrcCCAPI/api/source/$hash", headers = headers).text)

                Log.d("VidsrcCC", "UpCloud data: $data")

                val dataObject = data.optJSONObject("data") ?: return@let
                val iframeUrl = dataObject.optString("source")

                Log.d("VidsrcCC", "UpCloud iframeUrl: $iframeUrl")

                getUpcloud(iframeUrl, vidsrcCCAPI, callback)
            }
        } catch (e: Exception) {
            Log.w("Vidsrc", "Failed to extract server: UpCloud")
        }

    }

    suspend fun invokeAutoembed(
        imdbId: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val url = if(season == null) {
            "$autoembedAPI/embed/movie/$imdbId"
        } else {
            "$autoembedAPI/embed/tv/$imdbId/$season/$episode"
        }

        val text = app.get(url).text
        val regex = Regex("""var\s+embedUrlValue\s*=\s*"([^"]+)";""")
        val embedUrl = regex.find(text)?.groupValues?.get(1) ?: return

        Log.d("Autoembed", "Extracted embed URL: $embedUrl")

        loadCustomExtractor("Autoembed", embedUrl, autoembedAPI, subtitleCallback, callback)
    }

    suspend fun invokeKuudere(
        title: String? = null,
        year: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val json = app.get(
            "$kuudereAPI/search/__data.json?keyword=${title?.replace(" ", "+")}&year=$year&x-sveltekit-invalidated=01",
            referer = "$kuudereAPI/"
        ).text

        val nodes = JSONObject(json).getJSONArray("nodes")
        var id: String? = null

        for (i in 0 until nodes.length()) {
            val node = nodes.getJSONObject(i)
            if (node.optString("type") != "data") continue
            val data = node.getJSONArray("data")
            val schema = (0 until data.length()).map { data.opt(it) }
                .filterIsInstance<JSONObject>().firstOrNull { it.has("id") } ?: continue
            id = data.optString(schema.getInt("id")).takeIf { it.isNotBlank() }
            break
        }

        if(id == null) return

        val epJson = app.get("$kuudereAPI/api/watch/$id/${episode ?: 1}", referer = "$kuudereAPI/").text
        val episodeLinks = JSONObject(epJson).getJSONArray("episode_links")
        (0 until episodeLinks.length()).forEach { i ->
            val embedUrl = episodeLinks.getJSONObject(i).optString("dataLink")
            val dataType = episodeLinks.getJSONObject(i).optString("dataType")
            val serverName = episodeLinks.getJSONObject(i).optString("serverName")
            loadCustomExtractor("Kuudere[${dataType.uppercase()}] $serverName", embedUrl, "$kuudereAPI/", subtitleCallback, callback, null, serverName)
        }
    }

    suspend fun invokeAnimekizz(
        title: String? = null,
        aniId: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        if (aniId == null || title == null) return

        // val encodedTitle = URLEncoder.encode(title ?: "", StandardCharsets.UTF_8.toString())
        val encodedTitle = title.replace(" ", "-")
        val query = "${encodedTitle}-${aniId}:${episode ?: 1}"

        val serversJson = try {
            app.get(
                "$animekizzAPI/api/v1/video/servers/$query",
                referer = "$animekizzAPI/"
            ).text
        } catch (e: Exception) {
            Log.e("Animekizz", "Failed to fetch servers for query=$query")
            return
        }

        Log.d("Animekizz", "Servers JSON: $serversJson")

        val serversArray = try {
            JSONObject(serversJson).optJSONArray("servers") ?: return
        } catch (e: Exception) {
            Log.e("Animekizz", "Unable to parse servers response")
            return
        }

        for (i in 0 until serversArray.length()) {
            val serverObj = serversArray.optJSONObject(i) ?: continue
            val id = serverObj.optString("id").takeIf { it.isNotBlank() } ?: continue
            val name = serverObj.optString("name").uppercase()
            val serverType = serverObj.optString("server_type").uppercase()

            Log.d("Animekizz", "Processing server: id=$id, name=$name, type=$serverType")

            val resolveJson = try {
                app.post(
                    "$animekizzAPI/api/v1/video/resolve",
                    json = mapOf(
                        "episode_id" to query,
                        "server_id" to id,
                    ),
                    referer = "$animekizzAPI/"
                ).text
            } catch (e: Exception) {
                Log.e("Animekizz", "Failed to resolve server $name ($id)")
                continue
            }

            Log.d("Animekizz", "Resolve response for server $name: $resolveJson")

            val sourcesArray = try {
                JSONObject(resolveJson).optJSONArray("sources") ?: continue
            } catch (e: Exception) {
                Log.e("Animekizz", "Unable to parse resolve response for server $name")
                continue
            }

            for (j in 0 until sourcesArray.length()) {
                val sourceObj = sourcesArray.optJSONObject(j) ?: continue
                var streamUrl = sourceObj.optString("url").takeIf { it.isNotBlank() } ?: continue
                if(streamUrl.startsWith("/api/")) streamUrl = animekizzAPI + streamUrl
                val quality = sourceObj.optString("quality", "Unknown")
                val format = sourceObj.optString("format", "Unknown")

                Log.d("Animekizz", "Adding link from server $name: url=$streamUrl, quality=$quality, format=$format")

                callback.invoke(
                    newExtractorLink(
                        "Animekizz [$name] [$serverType]",
                        "Animekizz [$name] [$serverType]",
                        streamUrl,
                        if (format.equals("hls", ignoreCase = true)) ExtractorLinkType.M3U8 else INFER_TYPE
                    ) {
                        this.quality = if(quality == "auto") Qualities.P1080.value else getIndexQuality(quality)
                        this.headers = mapOf(
                            "Referer" to "$animekizzAPI/",
                            "Origin" to animekizzAPI
                        )
                    }
                )
            }
        }
    }

    suspend fun invokeVidrock(
        tmdbId: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit
    ) {
        if(tmdbId == null) return
        val type = if (season == null) "movie" else "tv"
        val query = if (type == "movie") "$tmdbId" else "${tmdbId}_${season}_${episode}"
        val urlEncoded = getVidrockUrlEncode(query)
        val apiUrl = "$vidrockAPI/api/$type/$urlEncoded"
        val headers = mapOf(
            "Origin" to vidrockAPI,
            "Referer" to "$vidrockAPI/",
            "User-Agent" to USER_AGENT
        )

        val responseText = app.get(apiUrl, headers = headers).text
        val jsonObject = JSONObject(responseText)

        jsonObject.keys().forEach { serverName ->
            val serverData = jsonObject.optJSONObject(serverName) ?: return@forEach

            val url = serverData.optString("url", "")

            if (url.isNotEmpty() || url != "error" || url != "null") {

                Log.d("Vidrock", "$serverName url: $url")

                if(serverName == "Astra" || serverName == "Atlas") {
                    return@forEach
                } else {
                    val type = if(url.contains("m3u8")) ExtractorLinkType.M3U8 else INFER_TYPE

                    callback.invoke(
                        newExtractorLink(
                            "Vidrock[$serverName]",
                            "Vidrock[$serverName]",
                            url,
                            type
                        ) {
                            this.headers = headers
                        }
                    )
                }
            }
        }
    }

    suspend fun invokePulp(
        tmdbId: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val url = if (season == null) {
            "$pulpAPI/movies/$tmdbId?provider=02moviedownloader"
        } else {
            "$pulpAPI/tv/$tmdbId/seasons/$season/episodes/$episode?provider=02moviedownloader"
        }

        val json = app.get(url, timeout = 30000L, referer = "https://tv.pulp.watch/").text
        Log.d("Pulp", "Response JSON: $json")
        val response = tryParseJson<PulpResponse>(json) ?: return
        Log.d("Pulp", "Parsed Response: $response")

        response.sources?.forEach { source ->
            val sourceUrl = source.url?.takeIf { it.isNotBlank() } ?: return@forEach
            val qualityTag = source.quality.orEmpty().ifEmpty { "Unknown" }
            val providerName = source.provider?.name.takeIf { !it.isNullOrBlank() }
                ?: source.provider?.id.takeIf { !it.isNullOrBlank() }
                ?: "Pulp"

            val extractorLinkType = when {
                sourceUrl.contains(".m3u8", true) -> ExtractorLinkType.M3U8
                source.type.equals("hls", true) -> ExtractorLinkType.M3U8
                source.type.equals("dash", true) -> ExtractorLinkType.DASH
                source.type.equals("mp4", true) || source.type.equals("mkv", true) -> ExtractorLinkType.VIDEO
                else -> INFER_TYPE
            }

            Log.d("Pulp", "Adding source: provider=$providerName, quality=$qualityTag, url=$sourceUrl, type=$extractorLinkType")

            callback.invoke(
                newExtractorLink(
                    "Pulp [$providerName]",
                    "Pulp [$providerName]",
                    sourceUrl,
                    type = extractorLinkType
                ) {
                    this.headers = source.headers.orEmpty().filterValues { it.isNotBlank() }
                    this.quality = qualityTag.toIntOrNull() ?: Qualities.Unknown.value
                }
            )
        }

        response.subtitles?.forEach { subtitle ->
            val subtitleUrl = subtitle.url?.takeIf { it.isNotBlank() } ?: return@forEach
            val label = subtitle.label.takeIf { !it.isNullOrBlank() }
                ?: subtitle.language.takeIf { !it.isNullOrBlank() }
                ?: "Unknown"

            Log.d("Pulp", "Adding subtitle: label=$label, url=$subtitleUrl")

            subtitleCallback.invoke(
                newSubtitleFile(
                    getLanguage(label) ?: label,
                    subtitleUrl
                )
            )
        }
    }

    suspend fun invokeDudefilms(
        imdbId: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        if(imdbId == null) return
        val urls = app.get("$dudefilmsAPI/?s=$imdbId").document.select("a.simple-grid-grid-post-thumbnail-link")

        urls.safeAmap {
            val url = it.attr("href")
            Log.d("Dudefilms", "Found URL: $url")
            val doc = app.get(url).document

            if(season == null && episode == null) {
                doc.select("a.maxbutton").safeAmap { link ->
                    val href = link.attr("href")
                    val document = app.get(href).document
                    document.select("a.maxbutton").safeAmap { source ->
                        Log.d("Dudefilms", "source: $source")
                        loadSourceNameExtractor("Dudefilms", source.attr("href"), "", subtitleCallback, callback)
                    }
                }
            } else {
                val matchingH4Tags = doc.select("h4").filter {
                    Regex("""Season\s*0*$season\b""", RegexOption.IGNORE_CASE).containsMatchIn(it.text())
                }

                if(matchingH4Tags.isEmpty()) return@safeAmap

                Log.d("Dudefilms", "matchingH4Tags: $matchingH4Tags")

                matchingH4Tags.safeAmap { h4Tag ->
                    var currentSibling = h4Tag.nextElementSibling()
                    while (currentSibling != null) {
                        val tagName = currentSibling.tagName()

                        if(tagName != "p") return@safeAmap

                        if (tagName == "p") {
                            currentSibling.select("a").safeAmap{ aTag ->
                                val source = aTag.attr("href")
                                Log.d("Dudefilms", "source: $source")
                                val epSource = app.get(source).document
                                    .select("a.maxbutton")
                                    .find { Regex("""(?:Episode|Ep|E)\s*(\d+)""", RegexOption.IGNORE_CASE).find(it.text())?.groupValues?.getOrNull(1)?.toIntOrNull() == episode }
                                    ?.attr("href") ?: return@safeAmap
                                Log.d("Dudefilms", "epSource: $epSource")
                                loadSourceNameExtractor("Dudefilms", epSource, "", subtitleCallback, callback)
                            }
                        }

                        currentSibling = currentSibling.nextElementSibling()

                    }
                }
            }
        }
    }

    suspend fun invokeVidFastPro(
        tmdbId: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ) {
        val url = if (season == null) "$vidfastProApi/movie/$tmdbId/" else "$vidfastProApi/tv/$tmdbId/$season/$episode/"

        val headers = mutableMapOf(
            "User-Agent" to USER_AGENT,
            "Referer" to "$vidfastProApi/",
            "X-Requested-With" to "XMLHttpRequest",
        )

        val response = app.get(url, headers = headers).text
        val encodedText = Regex("""\\"en\\":\\"(.*?)\\""").find(response)?.groupValues?.get(1) ?: return
        val decApiUrl = "$multiDecryptAPI/enc-vidfast?text=$encodedText&version=1"
        val decodedDataJson = app.get(decApiUrl).text
        val decodedData = tryParseJson<EncDecResponse>(decodedDataJson)?.result ?: return

        val serversUrl = decodedData.servers ?: return
        val streamBaseUrl = decodedData.stream ?: return
        val token = decodedData.token ?: return

        headers["X-CSRF-Token"] = token

        val serversEncrypted = app.post(serversUrl, headers = headers).text
        val serversListJson = app.post(
            "$multiDecryptAPI/dec-vidfast",
            json = mapOf("text" to serversEncrypted, "version" to "1")
        ).text

        val serversList = tryParseJson<VidfastStreamResponse>(serversListJson)?.result ?: return

        serversList.forEach { server ->
            try {
                val serverHash = server.data ?: return@forEach
                val finalStreamUrl = "$streamBaseUrl/$serverHash"

                val streamDataEncrypted = app.post(finalStreamUrl, headers = headers).text
                if(streamDataEncrypted.isNullOrBlank()) return@forEach
                val streamDataJson = app.post(
                    "$multiDecryptAPI/dec-vidfast",
                    json = mapOf("text" to streamDataEncrypted , "version" to "1")
                ).text

                Log.d("VidFastPro", "Stream data JSON for server ${server.name}: $streamDataJson")

                val streamData = tryParseJson<VidfastServersStreamRoot>(streamDataJson)?.result ?: return@forEach

                streamData.tracks?.forEach { track ->
                    if (track.file != null && track.label != null) {
                        subtitleCallback.invoke(
                            newSubtitleFile(
                                getLanguage(track.label) ?: track.label,
                                track.file
                            )
                        )
                    }
                }

                val fileUrl = streamData.url ?: return@forEach
                val type = if (fileUrl.contains(".m3u8")) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO

                val is4k = streamData.is4kAvailable == true || server.description?.contains("4K", true) == true
                val quality = if (is4k) Qualities.P2160.value else Qualities.P1080.value

                callback.invoke(
                    newExtractorLink(
                        "Vidfast[${server.name}]",
                        "Vidfast[${server.name}] ${server.description ?: ""}",
                        fileUrl,
                        type
                    ) {
                        this.headers = headers
                        this.quality = quality
                    }
                )
            } catch (e: Exception) {
                Log.w("VidFastPro", "Failed to extract server: ${server.name}")
            }
        }
    }

    suspend fun invokeM4ufree(
        title: String? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        if(title == null || year == null) return
        val searchQuery = if(season == null) {
            "${getUrlTitle(title)}-${year}?type=movie"
        } else {
            "${getUrlTitle(title)}-${year}?type=tvs"
        }

        Log.d("M4ufree", "url: $m4ufreeAPI/search/$searchQuery")

        val searchDoc = app.get("$m4ufreeAPI/search/$searchQuery").document

        val matchedHref = searchDoc.select(".item > a").firstOrNull { element ->
            val name = element.attr("title").ifEmpty { element.text() }
            name.contains("$title ($year", ignoreCase = true) || name.contains("$title $year", ignoreCase = true)
        }?.attr("href") ?: return

        val link = fixUrl(matchedHref, m4ufreeAPI)

        Log.d("M4ufree", "link: $link")

        val request = app.get(link)
        val doc = request.document
        val cookies = request.cookies

        Log.d("M4ufree", "cookies: $cookies")

        val token = doc
            .selectFirst("meta[name=csrf-token]")
            ?.attr("content")

        if (token.isNullOrBlank()) return

        Log.d("M4ufree", "token: $token")

        val m4uData = if (season == null && episode == null) {
            doc.selectFirst("span.singlemv.active, span#fem")
                ?.attr("data")
        } else {
            val epCode = "S%02d-E%02d".format(season, episode)
            val episodeBtn = doc.select("button.episode")
                .firstOrNull {
                    it.text().trim().equals(epCode, true)
                } ?: return

            val idepisode = episodeBtn.attr("idepisode")

            if (idepisode.isBlank()) return

            val embed = app.post(
                "$m4ufreeAPI/ajaxtv",
                data = mapOf(
                    "idepisode" to idepisode,
                    "_token" to token
                ),
                referer = link,
                headers = mapOf(
                    "X-Requested-With" to "XMLHttpRequest"
                ),
                cookies = cookies
            ).document

            embed.selectFirst("span.singlemv.active, span#fem")
                    ?.attr("data")

        }

        if (m4uData.isNullOrBlank()) return

        Log.d("M4ufree", "m4uData: $m4uData")

        val iframe = app.post(
            "$m4ufreeAPI/ajax",
            data = mapOf(
                "m4u" to m4uData,
                "_token" to token
            ),
            referer = link,
            headers = mapOf(
                "X-Requested-With" to "XMLHttpRequest"
            ),
            cookies = cookies
        ).document
            .selectFirst("iframe")
            ?.attr("src")

        if (iframe.isNullOrBlank()) return

        Log.d("M4ufree", "iframe: $iframe")

        loadSourceNameExtractor(
            "M4uhd",
            fixUrl(iframe, link),
            m4ufreeAPI,
            subtitleCallback,
            callback
        )

    }

    suspend fun invokePlayImdb(
        imdbId: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val url = if(season == null) {
            "$playImdbAPI/embed/$imdbId"
        } else {
            "$playImdbAPI/embed/tv?imdb=$imdbId&season=$season&episode=$episode"
        }

        var iframe = app.get(url).document.selectFirst("#player_iframe")?.attr("src") ?: return

        if(!iframe.contains("https:")) iframe = "https:" + iframe

        Log.d("Playimdb", "iframe: $iframe")

        loadCustomExtractor("Playimdb", iframe, playImdbAPI, subtitleCallback, callback)

    }

    suspend fun invokeAv1encodes(
        title: String? = null,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit
    ) {
        if(title == null) return

        val slug = title.lowercase().trim().replace(Regex("\\s+"), "-")

        val url = if (season == null) {
            "$av1encodesAPI/episodes/$slug/movie/1920%20x%201080"
        } else {
            "$av1encodesAPI/episodes/$slug/$season/1920%20x%201080"
        }

        val document = app.get(
            url,
            headers = mapOf(
                "User-Agent" to USER_AGENT,
                "Referer" to "$av1encodesAPI/",
                "Accept-Language" to "en-US,en;q=0.9",
                "Sec-Ch-Ua" to "\"Chromium\";v=\"124\", \"Google Chrome\";v=\"124\"",
                "Sec-Ch-Ua-Mobile" to "?0",
                "Sec-Ch-Ua-Platform" to "\"Windows\""
            )
        ).document

        var targetPath: String? = null

        if (season != null && episode != null) {
            val episodeLinks = document.select("div.episode-item a")

            for (link in episodeLinks) {
                val labelText = link.selectFirst("span.episode-label")?.text() ?: ""

                val parsedEpisodeNum = labelText.filter { it.isDigit() }.toIntOrNull()

                if (parsedEpisodeNum == episode) {
                    targetPath = link.attr("href")
                    break
                }
            }
        } else {
            targetPath = document.selectFirst("div.episode-item a")?.attr("href")
        }

        if(targetPath == null) return

        val fileName = targetPath.substringAfterLast("/").substringBefore("?")

        val json = app.get("$av1encodesAPI/get_ddl/$fileName", referer = "$av1encodesAPI/").text
        val jsonObject = org.json.JSONObject(json)

        if (!jsonObject.optBoolean("success", false)) return

        val streamLink = jsonObject.optString("stream_link", "")
        if (streamLink.isBlank()) return
        // val downloadLink = jsonObject.optString("download_link", "")
        // val torrentLink = jsonObject.optString("torrent_link", "")
        val fileSize = jsonObject.optString("file_size", "")
        // val fileName = jsonObject.optString("file_name", "")

        var isDual = false
        val audioDetails = jsonObject.optJSONObject("audio_details")
        val audioArray = audioDetails?.optJSONArray("audio")

        if (audioArray != null) {
            for (i in 0 until audioArray.length()) {
                val audioObj = audioArray.optJSONObject(i)
                val language = audioObj?.optString("language") ?: ""

                if (language.equals("English", ignoreCase = true)) {
                    isDual = true
                    break
                }
            }
        }

        val audioType = if (isDual) "[DUAL]" else "[SUB]"

        callback.invoke(
            newExtractorLink(
                "Av1encodes $audioType",
                "Av1encodes $audioType $fileSize",
                streamLink
            ) {
                this.quality = Qualities.P1080.value
                this.referer = "$av1encodesAPI/"
            }
        )

    }

    suspend fun invokeVadapav(
        title: String? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit
    ) {

        fun getJsonObject(htmlString: String): JSONObject? {
            val regex = """window\.__INITIAL_DATA__\s*=\s*(\{.*?\});\s*window\.__INITIAL_META__""".toRegex(RegexOption.DOT_MATCHES_ALL)
            val matchResult = regex.find(htmlString) ?: return null
            val jsonString = matchResult.groupValues[1]
            
            return try {
                JSONObject(jsonString)
            } catch (e: Exception) {
                Log.e("Vadapav", "Failed to parse JSON from HTML")
                null
            }
        }

        if(title == null || year == null) return

        val query = "$title ($year)"

        val htmlString = app.get("$vadapavAPI/s/$query", referer = "$vadapavAPI/").text

        Log.d("Vadapav", "htmlString: $htmlString")

        val jsonObject = getJsonObject(htmlString) ?: return
        val items = jsonObject.optJSONArray("items") ?: return

        var targetId: String? = null

        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)
            val name = item.optString("name").trim()
            val type = item.optString("type")

            if (name.contains(query) && type == "folder") {
                targetId = item.optString("id")
                break
            }
        }

        if(targetId == null) return

        val folderUrl = fixUrl("/$targetId", vadapavAPI)

        Log.d("Vadapav", "Folder URL: $folderUrl")

        val folderDoc = app.get(folderUrl, referer = "$vadapavAPI/").text
        val folderJsonObject = getJsonObject(folderDoc) ?: return
        val folderItems = folderJsonObject.optJSONArray("items") ?: return

        if(season != null && episode != null) {
            val (seasonSlug, episodeSlug) = getEpisodeSlug(season, episode)
            var targetSeasonId: String? = null

            for (i in 0 until folderItems.length()) {
                val item = folderItems.getJSONObject(i)
                val name = item.optString("name").trim()
                val type = item.optString("type")

                if (name == "Season $seasonSlug" && type == "folder") {
                    targetSeasonId = item.optString("id")
                    break
                }
            }

            if (targetSeasonId == null) return

            val seasonUrl = fixUrl("/$targetSeasonId", vadapavAPI)

            Log.d("Vadapav", "Season URL: $seasonUrl")

            val seasonDoc = app.get(seasonUrl, referer = "$vadapavAPI/").text
            val seasonJsonObject = getJsonObject(seasonDoc) ?: return
            val seasonItems = seasonJsonObject.optJSONArray("items") ?: return

            var targetEpisodeId: String? = null
            var targetEpisodeName: String? = null

            for (i in 0 until seasonItems.length()) {
                val item = seasonItems.getJSONObject(i)
                val name = item.optString("name").trim()
                val category = item.optString("category")

                if (name.contains("E$episodeSlug") && category == "video") {
                    targetEpisodeId = item.optString("id")
                    targetEpisodeName = item.optString("name")
                    break
                }
            }

            if (targetEpisodeId == null || targetEpisodeName == null) return

            val entryUrl = fixUrl("/f/$targetEpisodeId", vadapavAPI)

            Log.d("Vadapav", "Episode URL: $entryUrl")

            val simplifiedTitle = getSimplifiedTitle(targetEpisodeName)

            callback.invoke(
                newExtractorLink(
                    "Vadapav",
                    "Vadapav $simplifiedTitle",
                    entryUrl,
                    ExtractorLinkType.VIDEO
                ) {
                    this.quality = getIndexQuality(targetEpisodeName)
                }
            )
        } else {
            for (i in 0 until folderItems.length()) {
                val item = folderItems.getJSONObject(i)
                val name = item.optString("name").trim()
                val category = item.optString("category")

                if (category == "video") {
                    val targetEpisodeId = item.optString("id")
                    val entryUrl = fixUrl("/f/$targetEpisodeId", vadapavAPI)

                    Log.d("Vadapav", "Video URL: $entryUrl")

                    val simplifiedTitle = getSimplifiedTitle(name)
                    
                    callback.invoke(
                        newExtractorLink(
                            "Vadapav",
                            "Vadapav $simplifiedTitle",
                            entryUrl,
                            ExtractorLinkType.VIDEO
                        ) {
                            this.quality = getIndexQuality(name)
                        }
                    )
                    
                }
            }
        }
    }

    suspend fun invokePeachify(
        tmdbId: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit
    ) {
        val headers = mapOf(
            "Accept"          to "*/*",
            "Accept-Language" to "en-US,en;q=0.5",
            "Origin"          to "$peachifyBaseAPI",
            "Referer"         to "$peachifyBaseAPI/",
            "Sec-Fetch-Dest"  to "empty",
            "Sec-Fetch-Mode"  to "cors",
            "Sec-Fetch-Site"  to "cross-site",
            "User-Agent"      to "Mozilla/5.0 (X11; Linux x86_64; rv:139.0) Gecko/20100101 Firefox/139.0"
        )

        val servers = listOf(
            "https://usa.eat-peach.sbs/holly",
            "https://usa.eat-peach.sbs/multi",
            "https://usa.eat-peach.sbs/ice",
            "https://usa.eat-peach.sbs/air",
            "https://uwu.peachify.top/moviebox"
        )

        servers.safeAmap { server ->
            val url = if(season == null) "$server/movie/$tmdbId" else "$server/tv/$tmdbId/$season/$episode"
            val text = app.get(url, headers = headers).text
            val encrypt = JSONObject(text).optString("data").ifEmpty { return@safeAmap }
            val decrypted = peachifyDecrypt(encrypt) ?: return@safeAmap
            val json      = JSONObject(decrypted)
            val provider  = json.optString("providerName", "Peachify")
            val sources   = json.optJSONArray("sources") ?: return@safeAmap

             for (i in 0 until sources.length()) {
                val src     = sources.getJSONObject(i)
                val rawUrl  = src.optString("url").ifEmpty { continue }
                val dub     = src.optString("dub", "")
                val srcType = src.optString("type", "hls")
                val quality = src.optInt("quality", 0)
                val srcHeaders  = src.optJSONObject("headers")

                val isProxy = rawUrl.contains("/m3u8-proxy") || rawUrl.contains("/mp4-proxy")
                val (finalUrl, proxyHeaders) = if (isProxy) {
                    val query      = java.net.URI(rawUrl).query?.queryParams() ?: emptyMap()
                    val realUrl    = query["url"] ?: rawUrl
                    val headersObj = query["headers"]
                        ?.let { runCatching { JSONObject(it) }.getOrNull() }
                    realUrl to headersObj.toStringMap()
                } else {
                    rawUrl to srcHeaders.toStringMap()
                }

                val finalReferer = proxyHeaders["referer"] ?: srcHeaders?.optString("referer") ?: "$peachifyBaseAPI/"
                val finalOrigin  = proxyHeaders["origin"]  ?: srcHeaders?.optString("origin")  ?: peachifyBaseAPI
                val finalUA      = proxyHeaders["user-agent"] ?: srcHeaders?.optString("user-agent") ?: USER_AGENT

                val name = buildString {
                    append("Peachify[$provider]")
                    if (dub.isNotEmpty()) append(" • $dub")
                }

                val type = if (srcType == "hls") ExtractorLinkType.M3U8 else INFER_TYPE

                Log.d("Peachify", "finalUrl: $finalUrl")

                callback.invoke(
                    newExtractorLink("Peachify", name, finalUrl, type) {
                        this.headers = mapOf(
                            "Origin"     to finalOrigin,
                            "Referer"    to finalReferer,
                            "User-Agent" to finalUA
                        )
                        this.quality = quality
                    }
                )
            }
        }
    }

}
