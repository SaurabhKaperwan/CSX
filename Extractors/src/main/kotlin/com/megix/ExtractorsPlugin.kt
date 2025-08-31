package com.megix

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class ExtractorsPlugin: Plugin() {
    override fun load(context: Context) {
        // All providers should be added in this manner. Please don't edit the providers list directly.
        registerExtractorAPI(Luxdrive())
        registerExtractorAPI(Ziddiflix())
        registerExtractorAPI(Linkstore())
        registerExtractorAPI(LinkstoreDrive())
        registerExtractorAPI(Vifix())
        registerExtractorAPI(VCloud())
        registerExtractorAPI(GDFlix())
        registerExtractorAPI(GDFlix3())
        registerExtractorAPI(GDFlix7())
        registerExtractorAPI(GDFlix2())
        registerExtractorAPI(GDLink())
        registerExtractorAPI(GDFlixXYZ())
        registerExtractorAPI(GDFlixDev())
        registerExtractorAPI(Hubdrive())
        registerExtractorAPI(HubCloud())
        registerExtractorAPI(HubCloudBz())
        registerExtractorAPI(HubCloudInk())
        registerExtractorAPI(HubCloudArt())
        registerExtractorAPI(HubCloudDad())
        registerExtractorAPI(fastdlserver())
        registerExtractorAPI(fastdlserver2())
        registerExtractorAPI(Driveleech())
        registerExtractorAPI(Driveseed())
        registerExtractorAPI(WLinkFast())
        registerExtractorAPI(FastLinks())
        registerExtractorAPI(Photolinx())
        registerExtractorAPI(Watchadsontape())
        registerExtractorAPI(Howblogs())
        registerExtractorAPI(Smoothpre())
        registerExtractorAPI(Gofile())
    }
}
