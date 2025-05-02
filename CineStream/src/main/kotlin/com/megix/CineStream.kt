package com.megix

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
open class CineStream: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(CineStreamProvider())
        registerExtractorAPI(Kwik())
        registerExtractorAPI(Animezia())
        registerExtractorAPI(server2())
        registerExtractorAPI(MultimoviesAIO())
        registerExtractorAPI(Strwishcom())
        registerExtractorAPI(CdnwishCom())
        registerExtractorAPI(Asnwish())
        registerExtractorAPI(Multimovies())
        registerExtractorAPI(Pahe())
        registerExtractorAPI(Smoothpre())
        registerExtractorAPI(Ryderjet())
        registerExtractorAPI(SuperVideo())
    }
}
