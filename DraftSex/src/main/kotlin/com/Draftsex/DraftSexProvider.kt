package com.coxju

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class DraftSexProvider : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(DraftSex())
    }
}
