package com.andrewnguyen.bowpress

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Implements [Configuration.Provider] so WorkManager picks up our
 * [HiltWorkerFactory] and can construct `@HiltWorker`s
 * ([com.andrewnguyen.bowpress.core.data.sync.BowPressSyncWorker] in particular)
 * with their injected dependencies.
 */
@HiltAndroidApp
class BowPressApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
