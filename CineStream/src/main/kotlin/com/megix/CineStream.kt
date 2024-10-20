package com.megix

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context
import com.lagradost.cloudstream3.extractors.Chillx
import com.lagradost.cloudstream3.extractors.Moviesapi

@CloudstreamPlugin
class CineStream: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(CineStreamProvider())
        registerExtractorAPI(Chillx())
        registerExtractorAPI(Moviesapi())
    }
}
