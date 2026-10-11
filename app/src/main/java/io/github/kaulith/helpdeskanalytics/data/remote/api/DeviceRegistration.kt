package io.github.kaulith.helpdeskanalytics.data.remote.api

/** This device's registration for push with the site the app is signed in to. */
interface DeviceRegistration {

    /** Only works while the session can still authenticate, so before signing out clears it. */
    suspend fun unregisterCurrentDevice()

    /** Registers with the site now in use, as soon as there is a network. */
    fun registerNow()
}
