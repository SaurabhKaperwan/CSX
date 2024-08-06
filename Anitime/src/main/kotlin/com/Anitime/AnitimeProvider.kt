package com.Anitime

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class AnitimeProvider: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(Anitime())
        registerExtractorAPI(Boosterx())
        registerExtractorAPI(AbyssCdn())
    }
}