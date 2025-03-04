package com.megix

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class ExtractorsPlugin: Plugin() {
    override fun load(context: Context) {
        // All providers should be added in this manner. Please don't edit the providers list directly.
        registerExtractorAPI(Luxdrive())
        registerExtractorAPI(Luxdrive2())
        registerExtractorAPI(VCloud())
        registerExtractorAPI(GDFlix())
        registerExtractorAPI(GDFlix3())
        registerExtractorAPI(GDFlix4())
        registerExtractorAPI(GDFlix2())
        registerExtractorAPI(GDLink())
        registerExtractorAPI(PixelDrain())
        registerExtractorAPI(HubCloud())
        registerExtractorAPI(HubCloudInk())
        registerExtractorAPI(HubCloudArt())
        registerExtractorAPI(fastdlserver())
        registerExtractorAPI(Driveleech())
        registerExtractorAPI(Driveseed())
        registerExtractorAPI(WLinkFast())
        registerExtractorAPI(FastLinks())
        registerExtractorAPI(Sendcm())
        registerExtractorAPI(Photolinx())
    }
}
