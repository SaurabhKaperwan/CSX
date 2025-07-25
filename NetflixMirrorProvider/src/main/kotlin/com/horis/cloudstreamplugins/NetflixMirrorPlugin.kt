package com.horis.cloudstreamplugins

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
open class NetflixMirrorPlugin: Plugin() {
    override fun load(context: Context) {
        // All providers should be added in this manner. Please don't edit the providers list directly.
        NetflixMirrorStorage.init(context.applicationContext)
        registerMainAPI(NetflixProvider())
        registerMainAPI(PrimeVideoProvider())
        registerMainAPI(JioHotstarProvider())
    }

}
