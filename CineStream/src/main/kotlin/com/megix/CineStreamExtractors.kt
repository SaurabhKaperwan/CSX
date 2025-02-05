package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.network.CloudflareKiller
import android.util.Log
import android.util.Base64
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.APIHolder.unixTime
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import java.net.URLEncoder
import okhttp3.FormBody
import java.nio.charset.StandardCharsets
import org.jsoup.Jsoup
import com.lagradost.cloudstream3.argamap
import com.lagradost.cloudstream3.extractors.helper.GogoHelper

object CineStreamExtractors : CineStreamProvider() {

    suspend fun invokeTvStream(
        id: String,
        api: String,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val url = "$api/stream/tv/$id.json"
        val json = app.get(url).text
        val data = parseJson<TvStreamsResponse>(json)
        data.streams.forEach {
            callback.invoke(
                ExtractorLink(
                    it.name ?: it.title ?: "TV",
                    it.description ?: it.title ?: "TV",
                    it.url,
                    "",
                    Qualities.Unknown.value,
                    INFER_TYPE,
                    headers = it.behaviorHints.proxyHeaders.request ?: mapOf(),
                )
            )
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
                ExtractorLink(
                    "TokyoInsider",
                    "[TokyoInsider] - $name",
                    url,
                    referer = "",
                    getIndexQuality(name),
                    INFER_TYPE,
                )
            )
        }
    }

    suspend fun invokeAnimia(
        id: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val json = app.get("$animiaAPI/api/episode/$id/${episode ?: 1}", timeout = 500L).text
        val data = parseJson<AnimiaResponse>(json)
        data.server1embedLink?.let {
            loadSourceNameExtractor(
                "Animia[SUB]",
                it,
                "",
                subtitleCallback,
                callback,
            )
        }
        data.server2embedLink?.let {
            loadSourceNameExtractor(
                "Animia[SUB]",
                it,
                "",
                subtitleCallback,
                callback,
            )
        }
        data.server3embedLink?.let {
            loadSourceNameExtractor(
                "Animia[SUB]",
                it,
                "",
                subtitleCallback,
                callback,
            )
        }
    }

    suspend fun invokeDramaCool(
        title: String,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ) {
        val json = if(season == null) {
            var episodeSlug = "$title episode 1".createSlug()
            val url = "${CONSUMET_API}/movies/dramacool/watch?episodeId=${episodeSlug}"
            val res = app.get(url).text
            if(res.contains("Media Not found")) {
                val newEpisodeSlug = "$title $year episode 1".createSlug()
                val newUrl = "$CONSUMET_API/movies/dramacool/watch?episodeId=${newEpisodeSlug}"
                app.get(newUrl).text
            }
            else {
                res
            }
        }
        else {
            val seasonText = if(season == 1) "" else "season $season"
            val episodeSlug = "$title $seasonText episode $episode".createSlug()
            val url =  "${CONSUMET_API}/movies/dramacool/watch?episodeId=${episodeSlug}"
            val res = app.get(url).text
            if(res.contains("Media Not found")) {
                val newEpisodeSlug = "$title $seasonText $year episode $episode".createSlug()
                val newUrl = "$CONSUMET_API/movies/dramacool/watch?episodeId=${newEpisodeSlug}"
                app.get(newUrl).text
            }
            else {
                res
            }
        }
        val data = parseJson<ConsumetSources>(json)
        data.sources?.forEach {
            callback.invoke(
                ExtractorLink(
                    "DramaCool",
                    "DramaCool",
                    it.url,
                    referer = "",
                    quality = Qualities.P1080.value,
                    isM3u8 = true
                )
            )
        }

        data.subtitles?.forEach {
            subtitleCallback.invoke(
                SubtitleFile(
                    it.lang,
                    it.url
                )
            )
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
                ExtractorLink(
                    "Torrentio",
                    stream.title ?: stream.name ?: "",
                    magnet,
                    "",
                    getIndexQuality(stream.name),
                    ExtractorLinkType.MAGNET,
                )
            )
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
            "Referer" to "$AutoembedAPI/"
        )
        val json = app.get(url, headers = headers).text
        val data = tryParseJson<TomResponse>(json) ?: return

        callback.invoke(
            ExtractorLink(
                "Tom",
                "Tom",
                data.videoSource,
                "",
                Qualities.Unknown.value,
                true
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

    suspend fun invokeCinemaluxe(
        title: String? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit,
        subtitleCallback: (SubtitleFile) -> Unit
    ) {
        val searchDocument = app.get("$cinemaluxeAPI/?s=$title $year").document
        val url = searchDocument.select("div.title > a:matches((?i)($title $year))").attr("href")
        val document = app.get(url).document
        if(season == null) {
            document.select("a.maxbutton").amap {
                var link = it.attr("href")
                if(link.contains("luxedailyupdates.xyz")) {
                    link = cinemaluxeBypass(link)
                }
                val selector = if(link.contains("luxedrive")) "div > a" else "a.maxbutton"
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

            val season = document.select("a.maxbutton-5:matches((?i)(Season 0?$season))")
            season.amap { div ->
                var link = div.select("a").attr("href")
                if(link.contains("luxedailyupdates.xyz")) {
                    link = cinemaluxeBypass(link)
                }
                 app.get(link).document.select("""a.maxbutton:matches((?i)(?:episode\s*[-]?\s*)(0?$episode))""").amap {
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
        id: String,
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
                app.get("https://blog.finzoox.com/?id=$token").text.substringAfter("link\":\"")
                    .substringBefore("\"};")
            val decodedurl = base64Decode(encodedurl)
            if (season == null) {
                loadSourceNameExtractor("Bollyflix", decodedurl , "", subtitleCallback, callback)
            } else {
                val link =
                    app.get(decodedurl).document.selectFirst("article h3 a:contains(Episode 0$episode)")!!
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
                ExtractorLink(
                    it.name,
                    "[${it.name}] ${it.title}",
                    it.url,
                    "",
                    Qualities.Unknown.value,
                    INFER_TYPE,
                )
            )
        }
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

    suspend fun invokeVite(
        id: String,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit,
    ) {
        val url = if(season != null) "$viteAPI/tv/$id/$season/$episode" else "$viteAPI/movie/$id"
        val str = app.get(url).text
        val link = Regex("""file:\s*"([^"]+)\"""").find(str)?.groupValues?.get(1) ?: return
        callback.invoke(
            ExtractorLink(
                "Vite",
                "Vite",
                link,
                "",
                Qualities.P1080.value,
                true
            )
        )
    }

    suspend fun invokeAnimes(
        malId: Int? = null,
        aniId: Int? = null,
        episode: Int? = null,
        year: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val malsync = app.get("$malsyncAPI/mal/anime/${malId ?: return}")
            .parsedSafe<MALSyncResponses>()?.sites
        val zoroIds = malsync?.zoro?.keys?.map { it }
        val zorotitle = malsync?.zoro?.firstNotNullOf { it.value["title"] }?.replace(":"," ")
        val animepahe = malsync?.animepahe?.firstNotNullOf { it.value["url"] }
        val animepahetitle = malsync?.animepahe?.firstNotNullOf { it.value["title"] }

        argamap(
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
                val Gogourl = malsync?.Gogoanime?.firstNotNullOfOrNull { it.value["url"] }
                if (Gogourl != null)
                    invokeAnitaku(Gogourl, episode, subtitleCallback, callback)
            }
        )

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
        val session = animeData?.find { it.episode == episode }?.session ?: ""
        app.get("$animepaheAPI/play/$id/$session", headers).document.select("div.dropup button")
            .map {
                var lang=""
                val dub=it.select("span").text()
                if (dub.contains("eng")) lang="DUB" else lang="SUB"
                val quality = it.attr("data-resolution")
                val href = it.attr("data-src")
                if (href.contains("kwik.si")) {
                    loadCustomExtractor(
                        "Animepahe [$lang]",
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

    suspend fun invoke2embed(
        id:  String,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit,
        subtitleCallback: (SubtitleFile) -> Unit,
    ) {
        val url = if(season != null && episode != null) "${TwoEmbedAPI}/scrape?id=${id}&s=${season}&e=${episode}" else "${TwoEmbedAPI}/scrape?id=${id}"
        val json = app.get(url).text
        val data = parseJson<TwoEmbedQuery>(json)
        data.stream.forEach {
            callback.invoke(
                ExtractorLink(
                    "2embed[${it.type}]",
                    "2embed[${it.type}]",
                    it.playlist,
                    referer = "",
                    quality = Qualities.Unknown.value,
                    INFER_TYPE,
                )
            )
        }
    }

    suspend fun invokeMovies(
        tmdbId: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val url = if (season == null) {
            "$moviesAPI/movie/$tmdbId"
        } else {
            "$moviesAPI/tv/$tmdbId-$season-$episode"
        }

        val iframe =
            app.get(
                url,
                referer = "https://pressplay.top/"
            ).document.selectFirst("iframe")
                ?.attr("src")

        loadExtractor(iframe ?: return, "$moviesAPI/", subtitleCallback, callback)
    }

    suspend fun invokeVidSrcNL(
        id: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit,
    ) {
        val sources = listOf("vidcloud", "upstream", "hindi", "english")
        sources.forEach { source ->
            val url = if(season != null) "https://${source}.vidsrc.nl/stream/tv/${id}/${season}/{episode}" else "https://${source}.vidsrc.nl/stream/movie/${id}"
            val doc = app.get(url).document
            val link = doc.selectFirst("div#player-container > media-player")?.attr("src")
            if (!link.isNullOrEmpty()) {
                callback.invoke(
                    ExtractorLink(
                        "VidSrcNL[${source}]",
                        "VidSrcNL[${source}]",
                        link,
                        referer = "",
                        quality = Qualities.Unknown.value,
                        isM3u8 = true,
                    )
                )
            }
        }
    }

    suspend fun invokeVidbinge(
        title: String,
        imdb_id: String,
        tmdb_id: Int? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit,
        subtitleCallback: (SubtitleFile) -> Unit,
    ) {
        val providers = mutableListOf("astra", "nova", "orion")
        val type = if (season == null) "movie" else "tv"
        val s = season ?: ""
        val e = episode ?: ""
        val query = """{"title":"$title","imdbId":"$imdb_id","tmdbId":"$tmdb_id","type":"$type","season":"$s","episode":"$e","releaseYear":"$year"}"""
        val headers = mapOf(
            "accept" to "*/*",
            "origin" to "https://www.vidbinge.app",
            "user-agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
        )

        val tokenJson = app.get(WHVX_TOKEN).text
        val token = parseJson<WHVXToken>(tokenJson).token
        val encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8.toString())
        val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
        providers.map { provider ->
            val json = app.get("${WHVXAPI}/search?query=${encodedQuery}&provider=${provider}&token=${encodedToken}", headers = headers).text
            val data = tryParseJson<WHVX>(json) ?: return@map
            val encodedUrl = URLEncoder.encode(data.url, StandardCharsets.UTF_8.toString())
            val json2 = app.get("${WHVXAPI}/source?resourceId=${encodedUrl}&provider=${provider}", headers = headers).text
            if(provider == "astra") {
                val data2 = tryParseJson<AstraQuery>(json2) ?: return@map
                data2.stream.forEach {
                    callback.invoke(
                        ExtractorLink(
                            "Astra",
                            "Astra",
                            it.playlist,
                            "",
                            Qualities.Unknown.value,
                            INFER_TYPE
                        )
                    )
                }
            }
            else if(provider == "nova") {
                val data2 = tryParseJson<NovaVideoData>(json2) ?: return@map
                for (stream in data2.stream) {
                    for ((quality, details) in stream.qualities) {
                        callback.invoke(
                            ExtractorLink(
                                "Nova",
                                "Nova",
                                details.url,
                                "",
                                getQualityFromName(quality),
                                INFER_TYPE,
                            )
                        )
                    }
                }

                for (stream in data2.stream) {
                    for (caption in stream.captions) {
                        subtitleCallback.invoke(
                            SubtitleFile(
                                caption.language,
                                caption.url
                            )
                        )
                    }
                }
            }
            else {
                val data2 = tryParseJson<OrionStreamData>(json2) ?: return@map
                for(stream in data2.stream) {
                    callback.invoke(
                        ExtractorLink(
                            "Orion",
                            "Orion",
                            stream.playlist,
                            "",
                            Qualities.Unknown.value,
                            INFER_TYPE
                        )
                    )

                    for (caption in stream.captions) {
                        subtitleCallback.invoke(
                            SubtitleFile(
                                caption.language,
                                caption.url
                            )
                        )
                    }
                }
            }
        }
    }

    suspend fun invokeAutoembed(
        id: Int?,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit,
    ) {
        val url = if(season != null && episode != null) "${AutoembedAPI}/embed/player.php?id=${id}&s=${season}&e=${episode}" else "${AutoembedAPI}/embed/player.php?id=${id}"
        val document = app.get(url).document
        val regex = Regex("""(?:"title":\s*"([^"]*)",\s*"file":\s*"([^"]*)")""")
        val matches = regex.findAll(document.toString())

        matches.forEach { match ->
            val title = match.groups?.get(1)?.value ?: ""
            val file = match.groups?.get(2)?.value ?: ""
            callback.invoke(
                ExtractorLink(
                    "Autoembed[${title}]",
                    "Autoembed[${title}]",
                    file,
                    referer = "",
                    quality = Qualities.Unknown.value,
                    true,
                )
            )
        }
    }

    suspend fun invokeWYZIESubs(
        id: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
    ) {
        val url = if(season != null) "${WYZIESubsAPI}/search?id=${id}&season=${season}&episode=${episode}" else "${WYZIESubsAPI}/search?id=${id}"
        val json = app.get(url).text
        val data = parseJson<ArrayList<WYZIESubtitle>>(json)
        data.forEach {
            subtitleCallback.invoke(
                SubtitleFile(
                    it.display,
                    it.url
                )
            )
        }
    }

    suspend fun invokeWHVXSubs(
        id: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
    ) {
        val url = if(season != null) "${WHVXSubsAPI}/search?id=${id}&season=${season}&episode=${episode}" else "${WHVXSubsAPI}/search?id=${id}"
        val json = app.get(url).text
        val data = parseJson<ArrayList<WHVXSubtitle>>(json)
        data.forEach {
            subtitleCallback.invoke(
                SubtitleFile(
                    it.languageName,
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

    // suspend fun invokeFull4Movies(
    //     title: String,
    //     year: Int? = null,
    //     season: Int? = null,
    //     episode: Int? = null,
    //     subtitleCallback: (SubtitleFile) -> Unit,
    //     callback: (ExtractorLink) -> Unit
    // ) {
    //     val url = if(season == null) "$Full4MoviesAPI/?s=$title $year" else "$Full4MoviesAPI/?s=$title season $season $year"
    //     val document = app.get(url).document
    //     val text = document.selectFirst("div.content > h2 > a")?.text().toString()
    //     val href = document.selectFirst("div.content > h2 > a")?.attr("href").toString()
    //     if (
    //         text.contains(title, true) &&
    //         year.let { text.contains("$it") } == true &&
    //         (season == null || season.let { text.contains("Season $it", true) } == true)
    //     ) {
    //         val doc2 = app.get(href).document
    //         val link = if(season == null) {
    //             Regex("""<a[^>]*href="([^"]*)"[^>]*>(?:WCH|Watch|Watch Online)<\/a>""").find(doc2.html())?.groupValues?.get(1) ?: ""

    //         } else {
    //             val urls = Regex("""<a[^>]*href="([^"]*)"[^>]*>(?:WCH|Watch|Watch Online)<\/a>""").findAll(doc2.html())
    //             urls.elementAtOrNull(episode?.minus(1) ?: return)?.groupValues?.get(1) ?: ""
    //         }
    //         if(link.contains("4links.")) {
    //             val doc = app.get(fixUrl(link)).document
    //             val source = doc.selectFirst("iframe")?.attr("src") ?: ""
    //             loadSourceNameExtractor("Full4Movies",source, referer = link, subtitleCallback, callback)
    //         }
    //         else {
    //             loadSourceNameExtractor("Full4Movies",link, referer = href, subtitleCallback, callback)
    //         }
    //     }

    // }

    suspend fun invokeRogmovies(
        id: String? = null,
        title: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        invokeWpredis(
            id,
            title,
            season,
            episode,
            subtitleCallback,
            callback,
            rogMoviesAPI
        )
    }
    suspend fun invokeVegamovies(
        title: String,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val (seasonSlug, episodeSlug) = getEpisodeSlug(season, episode)
        val cfInterceptor = CloudflareKiller()
        val fixtitle = title?.substringBefore("-")?.substringBefore(":")?.replace("&", " ")
        val url = if (season == null) {
            "$vegaMoviesAPI/?s=$fixtitle $year"
        } else {
            "$vegaMoviesAPI/?s=$fixtitle season $season"
        }
        app.get(url, interceptor = cfInterceptor).document.select("article h2").amap {
            val hrefpattern = Regex("""(?i)<a\s+href="([^"]+)"[^>]*?>[^<]*?\b($title)\b[^<]*?""").find( it.toString() )?.groupValues?.get(1)
            if(hrefpattern != null) {
                val res = hrefpattern.let { app.get(it).document }
                if(season == null) {
                    res.select("button.dwd-button").amap {
                        val link = it.parent()?.attr("href") ?: return@amap
                        val doc = app.get(link).document
                        val source = doc.selectFirst("button.btn:matches((?i)(V-Cloud))")
                            ?.parent()
                            ?.attr("href")
                            ?: return@amap
                        loadSourceNameExtractor("VegaMovies", source, referer = "", subtitleCallback, callback)
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
                            loadSourceNameExtractor("VegaMovies", epLink, referer = "", subtitleCallback, callback)
                        }
                    }
                }
            }
        }
    }

    private suspend fun invokeWpredis(
        id: String? = null,
        title: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
        api: String
    ) {
        val cfInterceptor = CloudflareKiller()
        val url = "$api/search/$id"
        app.get(url, interceptor = cfInterceptor).document.select("article h3 a")
            .amap {
                val hrefpattern=it.attr("href") ?: null
                if (hrefpattern != null) {
                    val res = hrefpattern.let { app.get(it).document }
                    if(season == null) {
                        res.select("button.dwd-button").amap {
                            val link = it.parent()?.attr("href") ?: return@amap
                            val doc = app.get(link).document
                            val source = doc.selectFirst("button.btn:matches((?i)(V-Cloud))")
                                ?.parent()
                                ?.attr("href")
                                ?: return@amap
                            loadSourceNameExtractor("LuxMovies", source, referer = "", subtitleCallback, callback)
                        }
                    }
                    else {
                        res.select("h3:matches((?i)(Season $season))").amap { h3 ->
                            val link = h3.nextElementSibling()?.selectFirst("a:matches((?i)(V-Cloud))")?.attr("href") ?: return@amap
                            val doc = app.get(link).document
                            val epLink = doc.selectFirst("h4:contains(Episodes):contains($episode)")
                                ?.nextElementSibling()
                                ?.selectFirst("a:matches((?i)(V-Cloud))")
                                ?.attr("href")
                                ?: return@amap

                            loadSourceNameExtractor("LuxMovies", epLink, referer = "", subtitleCallback, callback)
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
                    val fEp = doc.selectFirst("h5:matches((?i)$sep)")?.toString()
                    if (fEp.isNullOrEmpty()) {
                        val furl = doc.select("h5 a:contains(HubCloud)").attr("href")
                        loadSourceNameExtractor("MoviesDrive",furl, "", subtitleCallback, callback)
                    } else
                        doc.selectFirst("h5:matches((?i)$sep)")?.let { epElement ->
                            val linklist = mutableListOf<String>()
                            val firstHubCloudH5 = epElement.nextElementSibling()
                            val secondHubCloudH5 = firstHubCloudH5?.nextElementSibling()
                            val firstLink = firstHubCloudH5?.selectFirst("a")?.attr("href")
                            val secondLink = secondHubCloudH5?.selectFirst("a")?.attr("href")
                            if (firstLink != null) linklist.add(firstLink)
                            if (secondLink != null) linklist.add(secondLink)
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
    }

    suspend fun invokeUhdmovies(
        title: String? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit,
        subtitleCallback: (SubtitleFile) -> Unit
    ) {
        val fixTitle = title?.replace("-", " ")?.replace(":", " ")
        val searchtitle = fixTitle.createSlug()
        val (seasonSlug, episodeSlug) = getEpisodeSlug(season, episode)
        app.get("$uhdmoviesAPI/search/$fixTitle $year").document.select("#content article").map {
            val hrefpattern =
                Regex("""(?i)<a\s+href="([^"]*?\b$searchtitle\b[^"]*?\b$year\b[^"]*?)"[^>]*>""").find(
                    it.toString()
                )?.groupValues?.get(1)
            val detailDoc = hrefpattern?.let { app.get(it).document }
            val iSelector = if (season == null) {
                "div.entry-content p:has(:matches($year))"
            } else {
                "div.entry-content p:has(:matches((?i)(?:S\\s*$seasonSlug|Season\\s*$seasonSlug)))"
            }
            val iframeList = detailDoc!!.select(iSelector).mapNotNull {
                if (season == null) {
                    it.text() to it.nextElementSibling()?.select("a")?.attr("href")
                } else {
                    it.text() to it.nextElementSibling()?.select("a")?.find { child ->
                        child.select("span").text().equals("Episode $episode", true)
                    }?.attr("href")
                }
            }.filter { it.first.contains(Regex("(2160p)|(1080p)")) }
                .filter { element -> !element.toString().contains("Download", true) }
            iframeList.amap { (quality, link) ->
                val driveLink = bypassHrefli(link ?: "") ?: ""
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
        id: String,
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
        id: String,
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

    private suspend fun invokeHianime(
        url: String? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val id = url?.substringAfterLast("/") ?: return
        val json = app.get("$CONSUMET_API/anime/zoro/info?id=$id").text
        val data = tryParseJson<HiAnime>(json) ?: return
        val epId = data.episodes.find { it.number == episode }?.id ?: return
        val types =  mutableListOf("sub")
        if(data.subOrDub == "both") types.add("dub")
        val servers = mutableListOf("vidstreaming", "vidcloud")
        types.map { t ->
            servers.map { server ->
                val epJson = app.get("$CONSUMET_API/anime/zoro/watch?episodeId=${epId.replace("both", t)}&server=$server").text
                val epData = tryParseJson<HiAnimeMedia>(epJson) ?: return@map

                epData.sources.map {
                    callback.invoke(
                        ExtractorLink(
                            "HiAnime ${server.uppercase()} [${t.uppercase()}]",
                            "HiAnime ${server.uppercase()} [${t.uppercase()}]",
                            it.url,
                            "",
                            Qualities.Unknown.value,
                            if(it.type == "hls") true else false
                        )
                    )
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
}
