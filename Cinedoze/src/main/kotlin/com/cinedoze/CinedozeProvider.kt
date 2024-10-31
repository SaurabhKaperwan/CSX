package com.cinedoze

import android.content.Context
import com.lagradost.cloudstream3.extractors.VidHidePro3
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class CinedozeProvider: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(Cinedoze())
    }
}
