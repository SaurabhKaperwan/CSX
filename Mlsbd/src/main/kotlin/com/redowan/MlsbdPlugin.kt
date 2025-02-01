package com.megix

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost
.cloudstream3.plugins.Plugin
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@CloudstreamPlugin
class MlsbdPlugin : Plugin() {
    override fun load(context: Context) {
        // Launch API registration in background threads for faster startup
        registerInParallel()
    }

    private fun registerInParallel() {
        // Use coroutines to run registration concurrently
        // Dispatchers.Default is used for CPU-bound tasks
        // withContext will ensure the task runs in the specified dispatcher
        kotlinx.coroutines.launch {
            withContext(Dispatchers.Default) {
                registerMainAPI(MlsbdProvider())
            }
        }

        kotlinx.coroutines.launch {
            withContext(Dispatchers.Default) {
                registerExtractorAPI(GDFlix())
            }
        }

        kotlinx.coroutines.launch {
            withContext(Dispatchers.Default) {
                registerExtractorAPI(GDFlix1())
            }
        }

        kotlinx.coroutines.launch {
            withContext(Dispatchers.Default) {
                registerExtractorAPI(GDFlix2())
            }
        }
    }
}
