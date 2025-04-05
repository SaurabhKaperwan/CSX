package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class Sharepoint : ExtractorApi() {
    override val name: String = "Sharepoint"
    override val mainUrl: String = "https://indjatin-my.sharepoint.com"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        callback.invoke(
            newExtractorLink(
                this.name,
                this.name,
                url,
            )
        )
    }
}

