package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.network.WebViewResolver
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.fasterxml.jackson.annotation.JsonProperty
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import com.lagradost.cloudstream3.utils.M3u8Helper
import com.lagradost.cloudstream3.utils.getAndUnpack
import java.net.URI
import com.lagradost.api.Log
import com.lagradost.cloudstream3.extractors.StreamWishExtractor
import com.lagradost.cloudstream3.extractors.VidhideExtractor
import com.lagradost.cloudstream3.extractors.MixDrop
import com.lagradost.cloudstream3.extractors.StreamTape
import com.lagradost.cloudstream3.extractors.Vidguardto
import com.lagradost.cloudstream3.extractors.Filesim
import com.lagradost.cloudstream3.extractors.VidHidePro
import com.lagradost.cloudstream3.extractors.Vidmoly
import com.lagradost.cloudstream3.extractors.DoodLaExtractor
import com.lagradost.cloudstream3.extractors.LuluStream

class Luluvdoo : LuluStream() {
    override var mainUrl = "https://luluvdoo.com"
}

class Doodspro : DoodLaExtractor() {
    override var mainUrl = "https://doods.pro"
}

class Dsvplay : DoodLaExtractor() {
    override var mainUrl = "https://dsvplay.com"
}

class Vidmolybiz : Vidmoly() {
    override var mainUrl = "https://vidmoly.biz"
}

class Vidmolynet : Vidmoly() {
    override var mainUrl = "https://vidmoly.net"
}

class Cybervynx : StreamWishExtractor() {
    override var mainUrl = "https://cybervynx.com"
}

class Mivalyo : VidHidePro() {
    override var mainUrl = "https://mivalyo.com"
}

class Multimoviesshg : Filesim() {
    override var mainUrl = "https://multimoviesshg.com"
}

class Watchadsontape : StreamTape() {
    override var mainUrl = "https://watchadsontape.com"
}

open class SuperVideo : ExtractorApi() {
    override val name = "SuperVideo"
    override val mainUrl = "https://supervideo.cc"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val res = app.get(url.replace("tv","cc"), referer = referer)
        val script =
            res.document.selectFirst("script:containsData(function(p,a,c,k,e,d))")?.data()
        val unpacked = getAndUnpack(script ?: return)
        val m3u8 = Regex("file:\"(.*?m3u8.*?)").find(unpacked)?.groupValues?.getOrNull(1) ?:""
        M3u8Helper.generateM3u8(
            this.name,
            m3u8,
            referer = "$mainUrl/",
        ).forEach(callback)
    }
}

class Ryderjet: VidhideExtractor() {
    override var mainUrl = "https://ryderjet.com"
}

class Smoothpre: VidhideExtractor() {
    override var mainUrl = "https://smoothpre.com"
}

class MultimoviesAIO: StreamWishExtractor() {
    override var name = "Multimovies Cloud AIO"
    override var mainUrl = "https://allinonedownloader.fun"
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


class Dlions : VidhideExtractor() {
    override var mainUrl = "https://dlions.pro"
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

class Strwishcom : StreamWishExtractor() {
    override val name = "Strwish"
    override val mainUrl = "https://strwish.com"
    override val requiresReferer = true
}

class Kwik : ExtractorApi() {
    override val name            = "Kwik"
    override val mainUrl         = "https://kwik.cx"
    override val requiresReferer = true

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        val res = app.get(url, referer = url)
        val script =
            res.document.selectFirst("script:containsData(function(p,a,c,k,e,d))")?.data()
        val unpacked = getAndUnpack(script ?: return)
        val m3u8 =Regex("source=\\s*'(.*?m3u8.*?)'").find(unpacked)?.groupValues?.getOrNull(1) ?:""
        callback.invoke(
            newExtractorLink(
                name,
                name,
                m3u8,
                type = ExtractorLinkType.M3U8
            ) {
                this.referer = url
            }
        )
    }
}

class Pahe : ExtractorApi() {
    override val name = "Pahe"
    override val mainUrl = "https://pahe.win"
    override val requiresReferer = true
    private val kwikParamsRegex = Regex("""\("(\w+)",\d+,"(\w+)",(\d+),(\d+),\d+\)""")
    private val kwikDUrl = Regex("action=\"([^\"]+)\"")
    private val kwikDToken = Regex("value=\"([^\"]+)\"")
    private val client = OkHttpClient()

    private fun decrypt(fullString: String, key: String, v1: Int, v2: Int): String {
        val keyIndexMap = key.withIndex().associate { it.value to it.index }
        val sb = StringBuilder()
        var i = 0
        val toFind = key[v2]

        while (i < fullString.length) {
            val nextIndex = fullString.indexOf(toFind, i)
            val decodedCharStr = buildString {
                for (j in i until nextIndex) {
                    append(keyIndexMap[fullString[j]] ?: -1)
                }
            }

            i = nextIndex + 1

            val decodedChar = (decodedCharStr.toInt(v2) - v1).toChar()
            sb.append(decodedChar)
        }

        return sb.toString()
    }

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        val noRedirects = OkHttpClient.Builder()
            .followRedirects(false)
            .followSslRedirects(false)
            .build()

        val initialRequest = Request.Builder()
            .url("$url/i")
            .get()
            .build()

        val kwikUrl = "https://" + noRedirects.newCall(initialRequest).execute()
                        .header("location")!!.substringAfterLast("https://")

        val fContentRequest = Request.Builder()
            .url(kwikUrl)
            .header("referer", "https://kwik.cx/")
            .header("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36")
            .get()
            .build()

        val fContent = client.newCall(fContentRequest).execute()
        val fContentString = fContent.body.string()

        val (fullString, key, v1, v2) = kwikParamsRegex.find(fContentString)!!.destructured
        val decrypted = decrypt(fullString, key, v1.toInt(), v2.toInt())

        val uri = kwikDUrl.find(decrypted)!!.destructured.component1()
        val tok = kwikDToken.find(decrypted)!!.destructured.component1()

        val noRedirectClient = OkHttpClient().newBuilder()
            .followRedirects(false)
            .followSslRedirects(false)
            .cookieJar(client.cookieJar)
            .build()

        var code = 419
        var tries = 0
        var content: Response? = null

        while (code != 302 && tries < 20) {
            val formBody = FormBody.Builder()
                .add("_token", tok)
                .build()

            val postRequest = Request.Builder()
                .url(uri)
                .header("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36")
                .header("referer", fContent.request.url.toString())
                .header("cookie",  fContent.headers("set-cookie").firstOrNull().toString())
                .post(formBody)
                .build()

            content = noRedirectClient.newCall(postRequest).execute()
            code = content.code
            tries++
        }

        val location = content?.header("location").toString()
        content?.close()

        callback.invoke(
            newExtractorLink(
                name,
                name,
                location,
            ) {
                this.referer = "https://kwik.cx/"
            }
        )
    }
}

class Dhcplay: VidhideExtractor() {
    override var name = "DHC Play"
    override var mainUrl = "https://dhcplay.com"
    override var requiresReferer = true
}

class MixDropPs : MixDrop() {
    override var mainUrl = "https://mixdrop.ps"
}

class Mdy : MixDrop() {
    override var mainUrl = "https://mdy48tn97.com"
}

class MixDropTo : MixDrop() {
    override var mainUrl = "https://mxdrop.to"
}

class MixDropSi : MixDrop() {
    override var mainUrl = "https://mixdrop.si"
}

class Vembed : Vidguardto() {
    override var mainUrl = "https://vembed.net"
}

class VidHideHub : VidHidePro() {
    override var mainUrl = "https://vidhidehub.com"
}

class Akamaicdn : ExtractorApi() {
    override val name = "Akamaicdn"
    override val mainUrl = "https://molop.art"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val headers= mapOf("user-agent" to "okhttp/4.12.0")
        val res = app.get(url, referer = referer, headers = headers).document
        val sniffScript = res.selectFirst("script:containsData(sniff\\()")
            ?.data()
            ?.substringAfter("sniff(")
            ?.substringBefore(");") ?: return

        val cleaned = sniffScript.replace(Regex("\\[.*?\\]"), "")
        val regex = Regex("\"(.*?)\"")
        val args = regex.findAll(cleaned).map { it.groupValues[1].trim() }.toList()
        val token = args.lastOrNull().orEmpty()
        val m3u8 = "$mainUrl/m3u8/${args[1]}/${args[2]}/master.txt?s=1&cache=1&plt=$token"
        M3u8Helper.generateM3u8(name, m3u8, mainUrl, headers = headers).forEach(callback)
    }
}

class Fourspromax : MegaUp() {
    override var mainUrl = "https://4spromax.site"
    override val requiresReferer = true
}

class MegaUpTwoTwo : MegaUp() {
    override var mainUrl = "https://megaup22.online"
    override val requiresReferer = true
}

class Rapidairmax : MegaUp() {
    override var mainUrl = "https://rapidairmax.site"
    override val requiresReferer = true
}
//Thanks to https://github.com/AzartX47/EncDecEndpoints
open class MegaUp : ExtractorApi() {
    override var name = "MegaUp"
    override var mainUrl = "https://megaup.live"
    override val requiresReferer = true

    companion object {
        private val HEADERS = mapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:134.0) Gecko/20100101 Firefox/134.0",
                "Accept" to "text/html, *//*; q=0.01",
                "Accept-Language" to "en-US,en;q=0.5",
                "Sec-GPC" to "1",
                "Sec-Fetch-Dest" to "empty",
                "Sec-Fetch-Mode" to "cors",
                "Sec-Fetch-Site" to "same-origin",
                "Priority" to "u=0",
                "Pragma" to "no-cache",
                "Cache-Control" to "no-cache",
                "referer" to "https://animekai.to/",
        )
    }

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val mediaUrl = url.replace("/e/", "/media/").replace("/e2/", "/media/")
        val displayName = referer ?: this.name

        val encodedResult = app.get(mediaUrl, headers = HEADERS)
        .parsedSafe<AnimeKaiResponse>()
        ?.result

        if (encodedResult == null) return

        val body = """
        {
        "text": "$encodedResult",
        "agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:134.0) Gecko/20100101 Firefox/134.0"
        }
        """.trimIndent()
            .trim()
            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val m3u8Data=app.post("https://enc-dec.app/api/dec-mega", requestBody = body).text
        if (m3u8Data.isBlank()) {
            Log.d("Phisher", "Encoded result is null or empty")
            return
        }

        try {
            val root = JSONObject(m3u8Data)
            val result = root.optJSONObject("result")
            if (result == null) {
                Log.d("Error:", "No 'result' object in M3U8 JSON")
                return
            }

            val sources = result.optJSONArray("sources") ?: JSONArray()
            if (sources.length() > 0) {
                val firstSourceObj = sources.optJSONObject(0)
                val m3u8File = when {
                    firstSourceObj != null -> firstSourceObj.optString("file").takeIf { it.isNotBlank() }
                    else -> {
                        val maybeString = sources.optString(0)
                        maybeString.takeIf { it.isNotBlank() }
                    }
                }
                if (m3u8File != null) {
                    M3u8Helper.generateM3u8(displayName, m3u8File, mainUrl).forEach(callback)
                } else {
                    Log.d("Error:", "No 'file' found in first source")
                }
            } else {
                Log.d("Error:", "No sources found in M3U8 data")
            }

            val tracks = result.optJSONArray("tracks") ?: JSONArray()
            for (i in 0 until tracks.length()) {
                val trackObj = tracks.optJSONObject(i) ?: continue
                val label = trackObj.optString("label").trim().takeIf { it.isNotEmpty() }
                val file = trackObj.optString("file").takeIf { it.isNotBlank() }
                if (label != null && file != null) {
                    subtitleCallback(newSubtitleFile(getLanguage(label) ?: label, file))
                }
            }
        } catch (_: JSONException) {
            Log.e("Error", "Failed to parse M3U8 JSON")
        }
      }

    data class AnimeKaiResponse(
        @param:JsonProperty("status") val status: Int,
        @param:JsonProperty("result") val result: String
    )

}
