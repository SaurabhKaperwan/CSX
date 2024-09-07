package com.megix

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class Cinemaluxe: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(CinemaluxeProvider())
        registerExtractorAPI(GDFlix())
        registerExtractorAPI(GDFlix1())
        registerExtractorAPI(GDFlix2())
        registerExtractorAPI(Sharepoint())
        registerExtractorAPI(HubCloud())
    }
}
