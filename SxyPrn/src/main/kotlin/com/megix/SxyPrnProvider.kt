package com.megix

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import com.lagradost.cloudstream3.extractors.StreamTape
import com.lagradost.cloudstream3.extractors.DoodLaExtractor
import com.lagradost.cloudstream3.extractors.Vidguardto
import com.lagradost.cloudstream3.extractors.Vtbe


@CloudstreamPlugin
class SxyPrnProvider : Plugin() {
    override fun load(context: Context) {
        // All providers should be added in this manner. Please don't edit the providers list directly.
        registerMainAPI(SxyPrn())
        registerExtractorAPI(StreamTape())
        registerExtractorAPI(DoodLaExtractor())
        registerExtractorAPI(Vidguardto())
        registerExtractorAPI(Vtbe())
        registerExtractorAPI(MyDood())
    }
}
