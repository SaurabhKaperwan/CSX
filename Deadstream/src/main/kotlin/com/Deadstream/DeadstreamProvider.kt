package com.Deadstream

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context
import com.lagradost.cloudstream3.extractors.VidHidePro
import com.lagradost.cloudstream3.extractors.Chillx
import com.lagradost.cloudstream3.extractors.StreamWishExtractor
import com.lagradost.cloudstream3.extractors.Voe

@CloudstreamPlugin
class DeadstreamProvider: Plugin() {
    private fun classExist(className: String) : Boolean {
        try {
            Class.forName(className, false, ClassLoader.getSystemClassLoader())
        } catch (e: ClassNotFoundException) {
            return false
        }

        return true
    }

    override fun load(context: Context) {
        registerMainAPI(Deadstream())
        registerExtractorAPI(Chillx())
        registerExtractorAPI(Voe())
        registerExtractorAPI(StreamWishExtractor())
        registerExtractorAPI(MyFileMoon())
        /*
            Check if class exists before load
            v.4.4.0 released 20240725
            Class added on 20240819

            7bdf1461 (Added VidHidePro Extractor (#1286), 2024-08-19)
            library/src/commonMain/kotlin/com/lagradost/cloudstream3/extractors/VidHidePro.kt

         */
        if (classExist("com.lagradost.cloudstream3.extractors.VidHidePro")) {
            registerExtractorAPI(VidHidePro())
        }

        //registerExtractorAPI(AbyssCdn())
    }
}
