package com.megix

// import com.lagradost.cloudstream3.utils.M3u8Helper
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.extractors.DoodLaExtractor
import com.lagradost.cloudstream3.extractors.StreamTape
import com.lagradost.cloudstream3.extractors.Voe

class MyDood : DoodLaExtractor() {
    override var mainUrl = "https://d000d.com"
}

class MyStreamTape : StreamTape() {
    override var mainUrl = "https://streamtape.to"
}

class MyVoe : Voe() {
    override var mainUrl = "https://michaelapplysome.com"
}


// open class Streamvid : ExtractorApi() {
//     override val name = "Streamvid"
//     override val mainUrl = "https://streamvid.net"
//     override val requiresReferer = true

//     override suspend fun getUrl(
//         url: String,
//         referer: String?,
//         subtitleCallback: (SubtitleFile) -> Unit,
//         callback: (ExtractorLink) -> Unit
//     ) {
//         val response = app.get(url, referer = referer)
//         val script = if (!getPacked(response.text).isNullOrEmpty()) {
//             getAndUnpack(response.text)
//         } else {
//             response.document.selectFirst("script:containsData(sources:)")?.data()
//         }
//         val m3u8 =
//             Regex("src:\\s*\"(.*?m3u8.*?)\"").find(script ?: return)?.groupValues?.getOrNull(1)
//         M3u8Helper.generateM3u8(
//             name,
//             m3u8 ?: return,
//             mainUrl
//         ).forEach(callback)
//     }

// }
