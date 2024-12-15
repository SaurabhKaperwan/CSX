package com.megix

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context
import com.lagradost.cloudstream3.extractors.Chillx
import com.lagradost.cloudstream3.extractors.Vectorx
import com.lagradost.cloudstream3.extractors.Bestx
import com.lagradost.cloudstream3.extractors.Boltx
import com.lagradost.cloudstream3.extractors.Boosterx

@CloudstreamPlugin
class Full4Movies: Plugin() {
    override fun load(context: Context) {
        // All providers should be added in this manner. Please don't edit the providers list directly.
        registerMainAPI(Full4MoviesProvider())
        registerExtractorAPI(Chillx())
        registerExtractorAPI(Vectorx())
        registerExtractorAPI(Bestx())
        registerExtractorAPI(Boltx())
        registerExtractorAPI(Boosterx())
    }
}
