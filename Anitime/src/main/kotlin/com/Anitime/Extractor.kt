package com.Anitime
import com.lagradost.cloudstream3.extractors.Chillx
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.google.gson.Gson
import okhttp3.FormBody
import okhttp3.Request
import okhttp3.MultipartBody

class Boosterx : Chillx() {
    override val name = "Boosterx"
    override val mainUrl = "https://boosterx.stream"
}

class AbyssCdn : ExtractorApi() {
    override val name: String = "AbyssCdn"
    override val mainUrl: String = "https://abysscdn.com"
    override val requiresReferer = true
    data class Source(
        val label: String,
        val type: String
    )

    data class ResponseData(
        val sources: List<Source>,
        val id: String,
        val domain: String,
    )

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val data2 = app.get(url, referer = mainUrl).document.select("script").findLast { it.data().contains("━┻") }?.data() ?: ""
        val reqBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("abyss", data2)
            .build()

        val jsonDataString = app.post("https://abyss-api-two.vercel.app/decode", requestBody = reqBody).text
        val responseData = Gson().fromJson(jsonDataString, ResponseData::class.java)

        responseData.sources.forEach { source: Source ->
            val label = source.label
            val domain = "https://${responseData.domain}"
            val id = responseData.id
            var url = ""
            when (label) {
                "360p" -> url = "$domain/$id"
                "720p" -> url = "$domain/www$id"
                "1080p" -> url = "$domain/whw$id"
            }

            val headers = mapOf(
                "Sec-Fetch-Mode" to "cors",
            )

            callback.invoke (
                ExtractorLink (
                    this.name,
                    this.name,
                    url,
                    referer = mainUrl,
                    getQualityFromName(label),
                    headers = headers
                )
            )
        }
    }
}
