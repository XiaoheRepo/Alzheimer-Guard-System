package com.xiaohelab.guard.android

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.xiaohelab.guard.android.core.logging.PiiRedactingTree
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class GuardApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        Timber.plant(PiiRedactingTree(debug = BuildConfig.DEBUG_TOOLS_ENABLED))
        // Startup smoke-test: if you don't see this line, Logcat is not connected properly.
        Timber.tag("GUARD/APP").i(
            "GuardApplication started | debugTools=%b | pid=%d",
            BuildConfig.DEBUG_TOOLS_ENABLED,
            android.os.Process.myPid(),
        )
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
