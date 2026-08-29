package io.github.kaulith.helpdeskanalytics.data.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateVersionTest {

    @Test
    fun `a higher patch is an update`() {
        assertTrue(isNewerVersion("1.4.1", "1.4.0"))
    }

    @Test
    fun `parts compare numerically, not as text`() {
        assertTrue(isNewerVersion("1.10.0", "1.9.0"))
    }

    @Test
    fun `the same version is not an update`() {
        assertFalse(isNewerVersion("1.4.0", "1.4.0"))
    }

    @Test
    fun `an older release is not an update`() {
        assertFalse(isNewerVersion("1.3.9", "1.4.0"))
    }

    @Test
    fun `a build suffix does not make the installed version older`() {
        assertFalse(isNewerVersion("1.4.0", "1.4.0-debug"))
    }

    @Test
    fun `a shorter version fills the missing parts with zero`() {
        assertTrue(isNewerVersion("2", "1.9.9"))
        assertFalse(isNewerVersion("1.4", "1.4.0"))
    }

    @Test
    fun `an unparseable tag is never an update`() {
        assertFalse(isNewerVersion("nightly", "0.0.0"))
    }
}
