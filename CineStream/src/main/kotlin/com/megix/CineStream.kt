package com.megix

import android.content.Context
import com.lagradost.cloudstream3.MainActivity
import com.lagradost.cloudstream3.CloudStreamApp.Companion.getKey
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import com.megix.settings.Settings
import kotlinx.coroutines.runBlocking

@CloudstreamPlugin
open class CineStream: Plugin() {
    override fun load(context: Context) {

        // Load dynamic API URLs once
        runBlocking { init() }

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
        registerExtractorAPI(Videostr())
        registerExtractorAPI(Streameeeeee())
        registerExtractorAPI(PpzjYoutube())
        registerExtractorAPI(Asianload())
        registerExtractorAPI(Embedload())
        registerExtractorAPI(Cloudnestra())
        registerExtractorAPI(Cloudorchestranova())
        registerExtractorAPI(FlixCloud())
        registerExtractorAPI(VibePlayer())
        registerExtractorAPI(Playmogo())
        registerExtractorAPI(Otakuvid())
        registerExtractorAPI(Otakuhg())
        registerExtractorAPI(Allanimeups())
        registerExtractorAPI(Bysekoze())

        this.openSettings = { ctx: Context ->
            Settings.showSettingsDialog(ctx) {
                MainActivity.reloadHomeEvent.invoke(true)
            }
        }
    }
}
