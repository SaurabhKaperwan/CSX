package com.megix

import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.getQualityFromName
import okhttp3.FormBody

class GDFlix1 : GDFlix() {
    override val mainUrl: String = "https://new3.gdflix.cfd"
}

class GDFlix2 : GDFlix() {
    override val mainUrl: String = "https://new2.gdflix.cfd"
}

open class GDFlix : ExtractorApi() {
    override val name: String = "GDFlix"
    override val mainUrl: String = "https://new4.gdflix.cfd"
    override val requiresReferer = false

    private suspend fun extractbollytag(url: String): String = withContext(Dispatchers.IO) {
        app.get(url).text.let { tagdoc ->
            """\b\d{3,4}p\b""".toRegex().find(tagdoc)?.value?.trim() ?: ""
        }
    }

    private suspend fun extractbollytag2(url: String): String = withContext(Dispatchers.IO) {
        app.get(url).text.let { tagdoc ->
            """\b\d{3,4}p\b\s(.*?)\[""".toRegex().find(tagdoc)?.groupValues?.get(1)?.trim() ?: ""
        }
    }

    private suspend fun extractFastCloud(element: Element, tagquality: String, tags: String, callback: (ExtractorLink) -> Unit) {
        val link = element.attr("href")
        val trueurl = app.get("$mainUrl$link", timeout = 30L).document
