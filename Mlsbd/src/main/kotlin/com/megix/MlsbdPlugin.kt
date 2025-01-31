package com.redowan

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context
import com.lagradost.cloudstream3.extractors.StreamTape

@CloudstreamPlugin
class MlsbdPlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(MlsbdProvider())
        registerExtractorAPI(StreamTape())
        registerExtractorAPI(GDFlix())
        registerExtractorAPI(GDFlix1())
        registerExtractorAPI(GDFlix2())
    }
}