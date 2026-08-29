package io.github.kaulith.helpdeskanalytics

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import io.github.kaulith.helpdeskanalytics.data.local.credentials.CredentialsManager
import io.github.kaulith.helpdeskanalytics.data.remote.api.AgentSessionManager
import io.github.kaulith.helpdeskanalytics.data.remote.interceptor.AuthInterceptor
import io.github.kaulith.helpdeskanalytics.data.sync.SyncManager
import io.github.kaulith.helpdeskanalytics.di.appModule
import io.github.kaulith.helpdeskanalytics.di.databaseModule
import io.github.kaulith.helpdeskanalytics.di.networkModule
import io.github.kaulith.helpdeskanalytics.di.repositoryModule
import io.github.kaulith.helpdeskanalytics.di.viewModelModule
import io.github.kaulith.helpdeskanalytics.notifications.DeviceTokenManager
import io.github.kaulith.helpdeskanalytics.notifications.NotificationHelper
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.android.ext.android.get
import org.koin.core.context.startKoin

class HelpdeskAnalyticsApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            if (BuildConfig.DEBUG) androidLogger()
            androidContext(this@HelpdeskAnalyticsApp)
            modules(
                appModule,
                networkModule,
                databaseModule,
                repositoryModule,
                viewModelModule
            )
        }
        get<NotificationHelper>().createChannel()
        SyncManager(this).schedulePeriodic()
        get<DeviceTokenManager>().start()
    }

    // Coil loader that carries the API key/secret, so private ticket attachments load.
    override fun newImageLoader(): ImageLoader {
        val authedClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(get<AgentSessionManager>(), get<CredentialsManager>()))
            .build()
        return ImageLoader.Builder(this)
            .okHttpClient(authedClient)
            .crossfade(true)
            .build()
    }
}
