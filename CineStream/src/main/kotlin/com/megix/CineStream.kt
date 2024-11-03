package com.megix

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context
import com.lagradost.cloudstream3.extractors.Chillx
import com.lagradost.cloudstream3.extractors.Moviesapi
import com.lagradost.cloudstream3.extractors.Rabbitstream
import com.lagradost.cloudstream3.extractors.DoodLaExtractor
import com.lagradost.cloudstream3.extractors.MixDrop
import com.lagradost.cloudstream3.extractors.XStreamCdn
import com.lagradost.cloudstream3.extractors.VidHidePro5
import com.lagradost.cloudstream3.extractors.VidHidePro6
import com.lagradost.cloudstream3.extractors.Filesim
import com.lagradost.cloudstream3.extractors.StreamWishExtractor
import com.lagradost.cloudstream3.extractors.VidhideExtractor

@CloudstreamPlugin
open class CineStream: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(CineStreamProvider())
        registerExtractorAPI(Chillx())
        registerExtractorAPI(Moviesapi())
        registerExtractorAPI(Kwik())
        registerExtractorAPI(Rabbitstream())
        registerExtractorAPI(VidHidePro5())
        registerExtractorAPI(MixDrop())
        registerExtractorAPI(XStreamCdn())
        registerExtractorAPI(DoodLaExtractor())
        registerExtractorAPI(Animezia())
        registerExtractorAPI(server2())
        registerExtractorAPI(MultimoviesAIO())
        registerExtractorAPI(VidHidePro6())
        registerExtractorAPI(VidhideExtractor())
        registerExtractorAPI(StreamWishExtractor())
        registerExtractorAPI(Strwishcom())
        registerExtractorAPI(CdnwishCom())
        registerExtractorAPI(Asnwish())
        registerExtractorAPI(Multimovies())
    }
}
