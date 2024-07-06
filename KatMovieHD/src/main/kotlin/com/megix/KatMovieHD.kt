package com.megix

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context


@CloudstreamPlugin
class KatMovieHD: Plugin() {
    override fun load(context: Context) {
        // All providers should be added in this manner. Please don't edit the providers list directly.
        registerMainAPI(KatMovieHDProvider())
        registerMainAPI(PikaHDProvider())
        registerExtractorAPI(HubCloud())
        registerExtractorAPI(Sendcm())
        registerExtractorAPI(KMHD())
        registerExtractorAPI(GDFlix())
        //registerExtractorAPI(KatDrive())
        registerExtractorAPI(KMHTNet())
    }
}
