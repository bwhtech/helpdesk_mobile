package io.github.kaulith.helpdeskanalytics.domain.model

import org.junit.Assert.assertFalse
import org.junit.Test

class LeaderboardPeriodTest {

    @Test
    fun `presets hold every period when a period object loads first`() {
        // Class initialisation runs once per JVM, so load the periods fresh to control the order.
        val loader = FreshPeriodClassLoader(javaClass.classLoader!!)
        Class.forName("$PERIOD\$AllTime", true, loader)
        val companion = Class.forName(PERIOD, true, loader).getField("Companion").get(null)
        val presets = companion.javaClass.getMethod("getPresets").invoke(companion) as List<*>

        assertFalse(presets.contains(null))
    }

    private class FreshPeriodClassLoader(parent: ClassLoader) : ClassLoader(parent) {
        override fun loadClass(name: String, resolve: Boolean): Class<*> {
            if (!name.startsWith(PERIOD)) return super.loadClass(name, resolve)
            return findLoadedClass(name) ?: parent.getResourceAsStream("${name.replace('.', '/')}.class")!!
                .use { it.readBytes() }
                .let { defineClass(name, it, 0, it.size) }
        }
    }

    private companion object {
        val PERIOD: String = LeaderboardPeriod::class.java.name
    }
}
