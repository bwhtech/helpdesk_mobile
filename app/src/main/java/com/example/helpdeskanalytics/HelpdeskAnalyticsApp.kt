package com.example.helpdeskanalytics

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.example.helpdeskanalytics.data.local.credentials.CredentialsManager
import com.example.helpdeskanalytics.data.remote.api.AgentSessionManager
import com.example.helpdeskanalytics.data.remote.interceptor.AuthInterceptor
import com.example.helpdeskanalytics.data.sync.SyncManager
import com.example.helpdeskanalytics.di.appModule
import com.example.helpdeskanalytics.di.databaseModule
import com.example.helpdeskanalytics.di.networkModule
import com.example.helpdeskanalytics.di.repositoryModule
import com.example.helpdeskanalytics.di.viewModelModule
import com.example.helpdeskanalytics.notifications.DeviceTokenManager
import com.example.helpdeskanalytics.notifications.NotificationHelper
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
