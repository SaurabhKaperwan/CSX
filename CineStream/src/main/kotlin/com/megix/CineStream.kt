package com.megix

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
open class CineStream: Plugin() {
    override fun load(context: Context) {
        CineStreamStorage.init(context.applicationContext)
        registerMainAPI(CineStreamProvider())
        registerMainAPI(CineSimklProvider())
        registerMainAPI(CineTmdbProvider())
        registerExtractorAPI(Kwik())
        registerExtractorAPI(Pahe())
        registerExtractorAPI(SuperVideo())
        registerExtractorAPI(Akamaicdn())
        registerExtractorAPI(MegaUp())
        registerExtractorAPI(MegaUpTwoTwo())
        registerExtractorAPI(Fourspromax())
        registerExtractorAPI(Rapidairmax())
    }
}
