package com.phisher98

import com.google.gson.JsonParser
import com.lagradost.api.Log
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.base64Decode
import com.lagradost.cloudstream3.extractors.StreamWishExtractor
import com.lagradost.cloudstream3.extractors.VidHidePro
import com.lagradost.cloudstream3.network.WebViewResolver
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.JsUnpacker
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.newExtractorLink
import java.net.URI
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class MultimoviesAIO: StreamWishExtractor() {
    override var name = "Multimovies Cloud AIO"
    override var mainUrl = "https://allinonedownloader.fun"
    override var requiresReferer = true
}

class Dhcplay: VidHidePro() {
    override var name = "DHC Play"
    override var mainUrl = "https://dhcplay.com"
    override var requiresReferer = true
}

class Multimovies: StreamWishExtractor() {
    override var name = "Multimovies Cloud"
    override var mainUrl = "https://multimovies.cloud"
    override var requiresReferer = true
}

class Animezia : VidhideExtractor() {
    override var name = "Animezia"
    override var mainUrl = "https://animezia.cloud"
    override var requiresReferer = true
}

class server2 : VidhideExtractor() {
    override var name = "Multimovies Vidhide"
    override var mainUrl = "https://server2.shop"
    override var requiresReferer = true
}

class Asnwish : StreamWishExtractor() {
    override val name = "Streanwish Asn"
    override val mainUrl = "https://asnwish.com"
    override val requiresReferer = true
}

class CdnwishCom : StreamWishExtractor() {
    override val name = "Cdnwish"
    override val mainUrl = "https://cdnwish.com"
    override val requiresReferer = true
}

class GDMirrorbot : ExtractorApi() {
    override var name = "GDMirrorbot"
    override var mainUrl = "https://gdmirrorbot.nl"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        try {
            val host = getBaseUrl(app.get(url).url)
            val embedId = url.substringAfterLast("/")

            val data = mapOf("sid" to embedId)
            val response = app.post("$host/embedhelper.php", data = data).toString()
            val jsonElement = JsonParser.parseString(response)

            if (!jsonElement.isJsonObject) {
                Log.e("GDMirrorbot", "Unexpected JSON format: not a JSON object")
                return
            }

            val jsonObject = jsonElement.asJsonObject
            val siteUrls = jsonObject["siteUrls"]?.asJsonObject
            val siteFriendlyNames = jsonObject["siteFriendlyNames"]?.asJsonObject
            val mresultEncoded = jsonObject["mresult"]?.asString

            if (siteUrls == null || siteFriendlyNames == null || mresultEncoded.isNullOrBlank()) {
                Log.e("GDMirrorbot", "Missing siteUrls, siteFriendlyNames, or mresult")
                return
            }

            val mresult = try {
                val decoded = base64Decode(mresultEncoded)
                JsonParser.parseString(decoded).asJsonObject
            } catch (e: Exception) {
                Log.e("GDMirrorbot", "Failed to decode mresult: ${e.message}")
                return
            }

            val commonKeys = siteUrls.keySet().intersect(mresult.keySet())

            for (key in commonKeys) {
                try {
                    val siteName = siteFriendlyNames[key]?.asString.orEmpty()
                    val siteUrl = siteUrls[key]?.asString.orEmpty()
                    val resultUrl = mresult[key]?.asString.orEmpty()

                    if (siteName.isBlank() || siteUrl.isBlank() || resultUrl.isBlank()) {
                        Log.w("GDMirrorbot", "Skipping key '$key' due to blank values")
                        continue
                    }

                    // Ensure final URL has the correct scheme
                    val finalUrl = if (resultUrl.startsWith("http://") || resultUrl.startsWith("https://")) {
                        siteUrl + resultUrl
                    } else {
                        // If no scheme is present, prepend the scheme from siteUrl
                        val finalUrlWithScheme = if (siteUrl.startsWith("https://") || siteUrl.startsWith("http://")) {
                            siteUrl + resultUrl
                        } else {
                            "https://$siteUrl$resultUrl"
                        }
                        finalUrlWithScheme
                    }

                    when {
                        siteName.contains("EarnVids", ignoreCase = true) -> {
                            runCatching {
                                VidHidePro().getUrl(finalUrl, referer, subtitleCallback, callback)
                            }.onFailure {
                                Log.e("GDMirrorbot", "VidHidePro failed: ${it.message}")
                            }
                        }

                        siteName.contains("RpmShare", ignoreCase = true) -> {
                            runCatching {
                                MultimoviesVidstack().getUrl(finalUrl, referer, subtitleCallback, callback)
                            }.onFailure {
                                Log.e("GDMirrorbot", "VidStack failed: ${it.message}")
                            }
                        }

                        siteName.contains("StreamHG", ignoreCase = true) -> {
                            runCatching {
                                VidHidePro().getUrl(finalUrl, referer, subtitleCallback, callback)
                            }.onFailure {
                                Log.e("GDMirrorbot", "StreamHG failed: ${it.message}")
                            }
                        }

                        else -> {
                            runCatching {
                                loadExtractor(finalUrl, subtitleCallback, callback)
                            }.onFailure {
                                Log.e("GDMirrorbot", "Generic extractor failed: ${it.message}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("GDMirrorbot", "Exception in processing key '$key': ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("GDMirrorbot", "Fatal error in getUrl: ${e.message}")
        }
    }

    private fun getBaseUrl(url: String): String {
        return try {
            URI(url).let { "${it.scheme}://${it.host}" }
        } catch (e: Exception) {
            Log.e("GDMirrorbot", "getBaseUrl fallback: ${e.message}")
            mainUrl
        }
    }
}




class MultimoviesVidstack : ExtractorApi() {
    override var name = "Vidstack"
    override var mainUrl = "https://multimovies.rpmhub.site"
    override val requiresReferer = true

    override suspend fun getUrl(url: String, referer: String?): List<ExtractorLink> {
        val headers= mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:134.0) Gecko/20100101 Firefox/134.0")
        val hash=url.substringAfterLast("#")
        val baseurl=getBaseUrl(url)
        val encoded= app.get("$baseurl/api/v1/video?id=$hash",headers=headers).text.trim()
        val decryptedText = AesHelper.decryptAES(encoded, "kiemtienmua911ca", "0123456789abcdef")
        val m3u8=Regex("\"source\":\"(.*?)\"").find(decryptedText)?.groupValues?.get(1)?.replace("\\/","/") ?:""
        return listOf(
            newExtractorLink(
                this.name,
                this.name,
                url = m3u8,
                ExtractorLinkType.M3U8
            ) {
                this.referer = url
                this.quality = Qualities.P1080.value
            }
        )
    }

    private fun getBaseUrl(url: String): String {
        return try {
            URI(url).let { "${it.scheme}://${it.host}" }
        } catch (e: Exception) {
            Log.e("GDMirrorbot", "getBaseUrl fallback: ${e.message}")
            mainUrl
        }
    }
}

object AesHelper {
    private const val TRANSFORMATION = "AES/CBC/PKCS5PADDING"

    fun decryptAES(inputHex: String, key: String, iv: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val secretKey = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES")
        val ivSpec = IvParameterSpec(iv.toByteArray(Charsets.UTF_8))

        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
        val decryptedBytes = cipher.doFinal(inputHex.hexToByteArray())
        return String(decryptedBytes, Charsets.UTF_8)
    }

    private fun String.hexToByteArray(): ByteArray {
        check(length % 2 == 0) { "Hex string must have an even length" }
        return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
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
        val m3u8= JsUnpacker(res).unpack()?.let { unPacked ->
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

class Streamcasthub : ExtractorApi() {
    override var name = "Streamcasthub"
    override var mainUrl = "https://multimovies.streamcasthub.store"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val id=url.substringAfterLast("/#")
        val m3u8= "https://ss1.rackcloudservice.cyou/ic/$id/master.txt"
        callback.invoke(
            ExtractorLink(
                this.name,
                this.name,
                m3u8,
                url,
                Qualities.Unknown.value,
                type = ExtractorLinkType.M3U8,
            )
        )
    }
}



class Strwishcom : StreamWishExtractor() {
    override val name = "Strwish"
    override val mainUrl = "https://strwish.com"
    override val requiresReferer = true
}



open class VidhideExtractor : ExtractorApi() {
    override var name = "VidHide"
    override var mainUrl = "https://vidhide.com"
    override val requiresReferer = false

    override suspend fun getUrl(url: String, referer: String?): List<ExtractorLink>? {
        val response = app.get(
            url, referer = referer ?: "$mainUrl/", interceptor = WebViewResolver(
                Regex("""master\.m3u8""")
            )
        )
        val sources = mutableListOf<ExtractorLink>()
        if (response.url.contains("m3u8"))
            sources.add(
                newExtractorLink(
                    source = name,
                    name = name,
                    url = response.url,
                    ExtractorLinkType.M3U8
                ) {
                    this.referer = referer ?: "$mainUrl/"
                    this.quality = Qualities.Unknown.value
                }

            )
        return sources
    }
}
