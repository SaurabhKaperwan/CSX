package com.megix

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class MlsbdPlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(MlsbdProvider())
        registerExtractorAPI(GDFlix())
        registerExtractorAPI(GDFlix1())
        registerExtractorAPI(GDFlix2())
    }
}
