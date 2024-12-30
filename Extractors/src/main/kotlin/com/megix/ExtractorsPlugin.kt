package com.megix

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class ExtractorsPlugin: Plugin() {
    override fun load(context: Context) {
        // All providers should be added in this manner. Please don't edit the providers list directly.
        registerExtractorAPI(VCloud())
        registerExtractorAPI(GDFlix())
        registerExtractorAPI(GDFlix5())
        registerExtractorAPI(GDFlix4())
        registerExtractorAPI(PixelDrain())
        registerExtractorAPI(HubCloud())
        registerExtractorAPI(HubCloudArt())
        registerExtractorAPI(HubCloudInk())
        registerExtractorAPI(fastdlserver())
        registerExtractorAPI(Driveleech())
        registerExtractorAPI(Driveseed())
        registerExtractorAPI(WLinkFast())
        registerExtractorAPI(FastLinks())
        registerExtractorAPI(Sendcm())
        registerExtractorAPI(Photolinx())
    }
}
