package com.megix

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context
import com.lagradost.cloudstream3.extractors.VidSrcTo

@CloudstreamPlugin
class MovierulzhdPlugin: Plugin() {
    override fun load(context: Context) {
        // All providers should be added in this manner. Please don't edit the providers list directly.
        registerMainAPI(Movierulzhd())
//        registerMainAPI(Hdmovie2())
//        registerMainAPI(Hdmovie6())
        registerExtractorAPI(FMHD())
        registerExtractorAPI(VidSrcTo())
        registerExtractorAPI(Akamaicdn())
        registerExtractorAPI(Mocdn())
        registerExtractorAPI(Luluvdo())
        registerExtractorAPI(FMX())
        registerExtractorAPI(Lulust())
        registerExtractorAPI(Playonion())
        registerExtractorAPI(FilemoonV2())
    }
}