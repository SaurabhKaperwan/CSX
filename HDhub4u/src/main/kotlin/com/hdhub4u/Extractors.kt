package com.hdhub4u

import com.lagradost.api.Log
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.base64Decode
import com.lagradost.cloudstream3.extractors.VidHidePro
import com.lagradost.cloudstream3.extractors.VidStack
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.newExtractorLink

class HdStream4u : VidHidePro() {
    override var mainUrl = "https://hdstream4u.com"
}

class Hubstream : VidStack() {
    override var mainUrl = "https://hubstream.art"
}

class Hblinks : ExtractorApi() {
    override val name = "Hblinks"
    override val mainUrl = "https://hblinks.pro"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        app.get(url).document.select("h3 a,div.entry-content p a").map {
            val href=it.attr("href")
            loadExtractor(href,"HDHUB4U",subtitleCallback, callback)
        }

    }
}

class Hubcdn : ExtractorApi() {
    override val name = "Hubcdn"
    override val mainUrl = "https://hubcdn.cloud"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        app.get(url).document.toString().let {
            val encoded = Regex("r=([A-Za-z0-9+/=]+)").find(it)?.groups?.get(1)?.value
            if (!encoded.isNullOrEmpty()) {
                val m3u8 = base64Decode(encoded).substringAfterLast("link=")
                callback.invoke(
                    newExtractorLink(
                        this.name,
                        this.name,
                        url = m3u8,
                        ExtractorLinkType.M3U8
                    ) {
                        this.referer = url
                        this.quality = Qualities.Unknown.value
                    }
                )
            } else {
                Log.e("Error", "Encoded URL not found")
            }


        }
    }
}

class Hubdrive : ExtractorApi() {
    override val name = "Hubdrive"
    override val mainUrl = "https://hubdrive.fit"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val href=app.get(url).document.select(".btn.btn-primary.btn-user.btn-success1.m-1").attr("href")
        loadExtractor(href,"HubDrive",subtitleCallback, callback)
    }
}


@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
open class HubCloud : ExtractorApi() {
    override val name = "Hub-Cloud"
    override val mainUrl = "https://hubcloud.ink"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        source: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val realUrl = replaceHubclouddomain(url)
        val href = if (realUrl.contains("hubcloud.php")) {
            realUrl
        } else {
            val regex = "var url = '([^']*)'".toRegex()
            val regexdata=app.get(realUrl).document.selectFirst("script:containsData(url)")?.toString() ?: ""
            regex.find(regexdata)?.groupValues?.get(1).orEmpty()
        }
        if (href.isEmpty()) {
            Log.d("Error", "Not Found")
            return
        }

        val document = app.get(href).document
        val size = document.selectFirst("i#size")?.text()
        val header = document.selectFirst("div.card-header")?.text()

        document.select("div.card-body a.btn").forEach { linkElement ->
            val link = linkElement.attr("href")
            val quality = getIndexQuality(header)

            when {
                link.contains("www-google-com") -> Log.d("Error:", "Not Found")

                link.contains("technorozen.workers.dev") -> {
                    callback(
                        newExtractorLink(
                            "$source 10GB Server",
                            "$source 10GB Server $size",
                            url = getGBurl(link)
                        ) {
                            this.quality = quality
                        }
                    )
                }

                link.contains("pixeldra.in") || link.contains("pixeldrain") -> {
                    callback(
                        newExtractorLink(
                            "$source Pixeldrain",
                            "$source Pixeldrain $size",
                            url = link
                        ) {
                            this.quality = quality
                        }
                    )
                }

                link.contains("buzzheavier") -> {
                    callback(
                        newExtractorLink(
                            "$source Buzzheavier",
                            "$source Buzzheavier $size",
                            url = "$link/download"
                        ) {
                            this.quality = quality
                        }
                    )
                }

                link.contains(".dev") -> {
                    callback(
                        newExtractorLink(
                            "$source Hub-Cloud",
                            "$source Hub-Cloud $size",
                            url = link
                        ) {
                            this.quality = quality
                        }
                    )
                }

                link.contains("fastdl.lol") -> {
                    callback(
                        newExtractorLink(
                            "$source [FSL] Hub-Cloud",
                            "$source [FSL] Hub-Cloud $size",
                            url = link
                        ) {
                            this.quality = quality
                        }
                    )
                }

                link.contains("hubcdn.xyz") -> {
                    callback(
                        newExtractorLink(
                            "$source [File] Hub-Cloud",
                            "$source [File] Hub-Cloud $size",
                            url = link
                        ) {
                            this.quality = quality
                        }
                    )
                }

                link.contains("gofile.io") -> {
                    loadCustomExtractor(source.orEmpty(), link, "Pixeldrain", subtitleCallback, callback)
                }

                else -> Log.d("Error:", "No Server Match Found")
            }
        }
    }

    private fun getIndexQuality(str: String?) =
        Regex("(\\d{3,4})[pP]").find(str.orEmpty())?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: Qualities.P2160.value

    private suspend fun getGBurl(url: String): String =
        app.get(url).document.selectFirst("#vd")?.attr("href").orEmpty()
}



