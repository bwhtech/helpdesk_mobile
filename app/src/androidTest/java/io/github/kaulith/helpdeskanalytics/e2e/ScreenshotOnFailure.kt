package io.github.kaulith.helpdeskanalytics.e2e

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.services.storage.TestStorage
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** Saves the whole screen, dialogs included, to the connected test's additional output. */
class ScreenshotOnFailure : TestWatcher() {

    override fun failed(e: Throwable, description: Description) {
        val screen = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot() ?: return
        TestStorage().openOutputFile("${description.testClass.simpleName}.${description.methodName}.png").use {
            screen.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
