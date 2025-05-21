package com.phisher98

import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.extractors.Filesim
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.extractors.StreamWishExtractor
import com.lagradost.cloudstream3.utils.JsUnpacker

class FMHD : Filesim() {
    override val name = "FMHD"
    override var mainUrl = "https://fmhd.bar/"
    override val requiresReferer = true
}

class Playonion : Filesim() {
    override val mainUrl = "https://playonion.sbs"
}


class Luluvdo : StreamWishExtractor() {
    override val mainUrl = "https://luluvdo.com"
}

class Lulust : StreamWishExtractor() {
    override val mainUrl = "https://lulu.st"
}

class FilemoonV2 : ExtractorApi() {
    override var name = "Filemoon"
    override var mainUrl = "https://movierulz2025.bar"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val href=app.get(url).document.selectFirst("iframe")?.attr("src") ?:""
        val res= app.get(href, headers = mapOf("Accept-Language" to "en-US,en;q=0.5","sec-fetch-dest" to "iframe")).document.selectFirst("script:containsData(function(p,a,c,k,e,d))")?.data().toString()
        val m3u8=JsUnpacker(res).unpack()?.let { unPacked ->
            Regex("sources:\\[\\{file:\"(.*?)\"").find(unPacked)?.groupValues?.get(1)
        }
        callback.invoke(
            ExtractorLink(
                this.name,
                this.name,
                m3u8 ?:"",
                url,
                Qualities.P1080.value,
                type = ExtractorLinkType.M3U8,
            )
        )
    }
}

open class FMX : ExtractorApi() {
    override var name = "FMX"
    override var mainUrl = "https://fmx.lol"
    override val requiresReferer = true

    override suspend fun getUrl(url: String, referer: String?): List<ExtractorLink>? {
        val response = app.get(url,referer=mainUrl).document
        val extractedpack =response.selectFirst("script:containsData(function(p,a,c,k,e,d))")?.data().toString()
        JsUnpacker(extractedpack).unpack()?.let { unPacked ->
            Regex("sources:\\[\\{file:\"(.*?)\"").find(unPacked)?.groupValues?.get(1)?.let { link ->
                return listOf(
                    newExtractorLink(
                        this.name,
                        this.name,
                        url = link,
                        INFER_TYPE
                    ) {
                        this.referer = referer ?: ""
                        this.quality = Qualities.Unknown.value
                    }
                )
            }
        }
        return null
    }
}

open class Akamaicdn : ExtractorApi() {
    override val name = "Akamaicdn"
    override val mainUrl = "https://akamaicdn.life"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val headers= mapOf("user-agent" to "okhttp/4.12.0")
        val res = app.get(url, referer = referer, headers = headers).document
        val mappers = res.selectFirst("script:containsData(sniff\\()")?.data()?.substringAfter("sniff(")
            ?.substringBefore(");") ?: return
        val ids = mappers.split(",").map { it.replace("\"", "") }
        val m3u8="$mainUrl/m3u8/${ids[1]}/${ids[2]}/master.txt?s=1&cache=1"
        callback.invoke(
            newExtractorLink(
                name,
                name,
                m3u8,
                ExtractorLinkType.M3U8
            )
            {
                this.referer=url
                this.quality=Qualities.P1080.value
                this.headers=headers

            }
        )
    }
}

class MovieRulzUPN : ExtractorApi() {
    override val name = "MovieRulzUPN"
    override val mainUrl = "https://movierulz.upn.one"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val headers = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
            "Accept" to "*/*",
            "Accept-Language" to "en-US,en;q=0.5",
            "Referer" to (referer ?: mainUrl)
        )
        
        // Parse hash parameter from URL (e.g., https://movierulz.upn.one/#pkv3a6&dl=1)
        val hash = url.substringAfter("#", "").substringBefore("&", "")
        if (hash.isEmpty()) return
        
        // Get the player page
        val doc = app.get(url, headers = headers).document
        
        // Look for obfuscated JS that contains our video data
        val scripts = doc.select("script").map { it.data() }.filter { it.contains("eval(") }
        
        for (script in scripts) {
            try {
                // Try to unpack the JS
                val unpacked = JsUnpacker(script).unpack()
                if (unpacked == null || !unpacked.contains("sources")) continue

                // Extract video source URL
                val m3u8Regex = Regex("sources\\s*:\\s*\\[\\s*\\{\\s*file\\s*:\\s*[\"'](https?://[^\"']+\\.m3u8[^\"']*)[\"']")
                val mp4Regex = Regex("sources\\s*:\\s*\\[\\s*\\{\\s*file\\s*:\\s*[\"'](https?://[^\"']+\\.mp4[^\"']*)[\"']")
                
                val m3u8Match = m3u8Regex.find(unpacked)?.groupValues?.getOrNull(1)
                val mp4Match = mp4Regex.find(unpacked)?.groupValues?.getOrNull(1)
                
                // Try to get video URL from sources
                val videoUrl = m3u8Match ?: mp4Match
                if (videoUrl != null) {
                    val type = if (videoUrl.contains(".m3u8")) ExtractorLinkType.M3U8 else INFER_TYPE
                    callback.invoke(
                        newExtractorLink(
                            name,
                            name,
                            videoUrl,
                            type
                        ) {
                            this.referer = url
                            this.quality = Qualities.P1080.value
                            this.headers = headers
                        }
                    )
                    return
                }
                
                // Look for API calls that might contain video information
                val apiCallRegex = Regex("fetch\\([\"'](https?://[^\"']+)[\"']")
                val apiCall = apiCallRegex.find(unpacked)?.groupValues?.getOrNull(1)
                
                if (apiCall != null) {
                    val apiResponse = app.get(apiCall, headers = headers).text
                    val jsonVideoUrlRegex = Regex("[\"']file[\"']\\s*:\\s*[\"'](https?://[^\"']+)[\"']")
                    val jsonVideoUrl = jsonVideoUrlRegex.find(apiResponse)?.groupValues?.getOrNull(1)
                    
                    if (jsonVideoUrl != null) {
                        val type = if (jsonVideoUrl.contains(".m3u8")) ExtractorLinkType.M3U8 else INFER_TYPE
                        callback.invoke(
                            newExtractorLink(
                                name,
                                name,
                                jsonVideoUrl,
                                type
                            ) {
                                this.referer = url
                                this.quality = Qualities.P1080.value
                                this.headers = headers
                            }
                        )
                        return
                    }
                }
            } catch (e: Exception) {
                // Log exception and continue to next script
                println("Exception unpacking script: ${e.message}")
            }
        }
        
        // If no video found through scripts, look for iframes that might contain the video
        val iframes = doc.select("iframe")
        for (iframe in iframes) {
            val iframeSrc = iframe.attr("src")
            if (iframeSrc.isNotEmpty() && !iframeSrc.contains("javascript:")) {
                loadExtractor(iframeSrc, url, subtitleCallback, callback)
            }
        }
    }
}

class Streamsn : ExtractorApi() {
    override val name = "Streamsn"
    override val mainUrl = "https://streamsn.one"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val headers = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
            "Referer" to (referer ?: mainUrl)
        )
        
        val doc = app.get(url, headers = headers).document
        
        // Look for script that contains packed JS
        val packedScript = doc.selectFirst("script:containsData(function(p,a,c,k,e,d))")?.data()
        if (packedScript != null) {
            val unpacked = JsUnpacker(packedScript).unpack()
            if (unpacked != null) {
                val m3u8Link = Regex("file:\\s*\"(https?://[^\"]+\\.m3u8[^\"]*)\"").find(unpacked)?.groupValues?.get(1)
                if (m3u8Link != null) {
                    callback.invoke(
                        newExtractorLink(
                            name,
                            name,
                            m3u8Link,
                            ExtractorLinkType.M3U8
                        ) {
                            this.referer = url
                            this.quality = Qualities.P1080.value
                            this.headers = headers
                        }
                    )
                    return
                }
            }
        }
        
        // Fallback: Try to find iframe source
        val iframeSrc = doc.select("iframe").firstOrNull()?.attr("src")
        if (!iframeSrc.isNullOrEmpty() && !iframeSrc.contains("javascript:")) {
            loadExtractor(iframeSrc, url, subtitleCallback, callback)
        }
    }
}