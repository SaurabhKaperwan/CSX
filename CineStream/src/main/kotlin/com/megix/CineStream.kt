package com.megix

import android.content.Context
import com.lagradost.cloudstream3.MainActivity
import com.lagradost.cloudstream3.AcraApplication.Companion.getKey
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
open class CineStream: Plugin() {
    override fun load(context: Context) {

        // Seed seen-providers on every load so reinstalls/updates
        // don't treat existing providers as new
        Settings.initSeenProviders()

        if (getKey<Boolean>(Settings.PROVIDER_CINESTREAM) ?: true) {
            registerMainAPI(CineStreamProvider())
        }

        if (getKey<Boolean>(Settings.PROVIDER_SIMKL) ?: true) {
            registerMainAPI(CineSimklProvider())
        }

        if (getKey<Boolean>(Settings.PROVIDER_TMDB) ?: true) {
            registerMainAPI(CineTmdbProvider())
        }

        registerExtractorAPI(Kwik())
        registerExtractorAPI(Pahe())
        registerExtractorAPI(SuperVideo())
        registerExtractorAPI(Akamaicdn())
        registerExtractorAPI(MegaUp())
        registerExtractorAPI(MegaUpTwoTwo())
        registerExtractorAPI(Fourspromax())
        registerExtractorAPI(Rapidairmax())
        registerExtractorAPI(HubCloud())
        registerExtractorAPI(VCloud())
        registerExtractorAPI(GDLink())
        registerExtractorAPI(GDFlixApp())
        registerExtractorAPI(GdFlix1())
        registerExtractorAPI(GdFlix2())
        registerExtractorAPI(GDFlixNet())
        registerExtractorAPI(GDFlix())
        registerExtractorAPI(fastdlserver())
        registerExtractorAPI(Hubdrive())
        registerExtractorAPI(Driveseed())
        registerExtractorAPI(Driveleech())
        registerExtractorAPI(Howblogs())
        registerExtractorAPI(Wootly())
        registerExtractorAPI(Gofile())

        this.openSettings = { ctx: Context ->
            Settings.showSettingsDialog(ctx) {
                MainActivity.reloadHomeEvent.invoke(true)
            }
        }
    }
}
