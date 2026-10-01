package io.github.kaulith.helpdeskanalytics.testing

import android.content.Context
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import io.github.kaulith.helpdeskanalytics.data.local.credentials.CredentialsManager

/** Robolectric has no Android Keystore, so the keyset stays in memory. */
fun credentialsManager(context: Context): CredentialsManager {
    AeadConfig.register()
    val aead = KeysetHandle.generateNew(KeyTemplates.get("AES256_GCM"))
        .getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    return CredentialsManager(context, aead)
}
