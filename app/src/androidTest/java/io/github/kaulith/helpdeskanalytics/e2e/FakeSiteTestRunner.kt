package io.github.kaulith.helpdeskanalytics.e2e

import android.app.Application
import androidx.test.runner.AndroidJUnitRunner
import com.google.firebase.perf.FirebasePerformance
import okhttp3.OkHttpClient
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module

/** Points the app's HTTP client at [FakeSite], whose certificate no device trusts. */
class FakeSiteTestRunner : AndroidJUnitRunner() {

    override fun callApplicationOnCreate(app: Application) {
        // CI builds with the sample Firebase config, whose placeholder app id crashes the first trace upload.
        FirebasePerformance.getInstance().isPerformanceCollectionEnabled = false
        super.callApplicationOnCreate(app)
        loadKoinModules(
            module {
                single {
                    OkHttpClient.Builder()
                        .sslSocketFactory(
                            FakeSite.clientCertificates.sslSocketFactory(),
                            FakeSite.clientCertificates.trustManager
                        )
                        .build()
                }
            }
        )
    }
}
