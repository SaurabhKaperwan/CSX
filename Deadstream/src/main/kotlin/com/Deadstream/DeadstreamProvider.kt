package com.Deadstream

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context
import com.lagradost.cloudstream3.extractors.VidHidePro
import com.lagradost.cloudstream3.extractors.Voe
import com.lagradost.cloudstream3.extractors.StreamWishExtractor
import com.lagradost.cloudstream3.extractors.Chillx

@CloudstreamPlugin
class DeadstreamProvider: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(Deadstream())
        registerExtractorAPI(Chillx())
        registerExtractorAPI(Voe())
        registerExtractorAPI(StreamWishExtractor())
        registerExtractorAPI(MyFileMoon())
        registerExtractorAPI(VidHidePro())
        //registerExtractorAPI(AbyssCdn())
    }
}
