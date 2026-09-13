package io.github.kaulith.helpdeskanalytics.data.local.database

import android.app.Application
import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AppDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun `every exported schema migrates to the current version`() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val versions = context.assets.list(AppDatabase::class.java.name).orEmpty()
            .map { it.removeSuffix(".json").toInt() }
            .sorted()
        assertTrue("no exported schemas in the test assets", versions.isNotEmpty())

        val currentVersion = versions.last()
        versions.forEach { version ->
            val name = "migration-from-$version"
            helper.createDatabase(name, version).close()
            helper.runMigrationsAndValidate(name, currentVersion, true, *ALL_MIGRATIONS).close()
        }
    }
}
