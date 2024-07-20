package com.likdev256

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context
//import com.lagradost.cloudstream3.extractors.Gofile


@CloudstreamPlugin
class CinevezPlugin: Plugin() {
    override fun load(context: Context) {
        // All providers should be added in this manner. Please don't edit the providers list directly.
        registerMainAPI(CinevezProvider())
        registerExtractorAPI(ShaveTape())
        registerExtractorAPI(MDrop())
        registerExtractorAPI(JodWish())
        //registerExtractorAPI(Gofile())
    }
}
