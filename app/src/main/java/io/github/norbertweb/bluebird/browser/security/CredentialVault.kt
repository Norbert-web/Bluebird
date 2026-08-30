package com.io.github.norbertweb.bluebird.browser.security

import android.content.Context
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Small local credential vault. Passwords are encrypted with an AES/GCM key
 * held in Android Keystore; plaintext passwords are never written to the
 * SharedPreferences file.
 */
class CredentialVault(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance("AES", "AndroidKeyStore")
        generator.init(android.security.keystore.KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT
        ).setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(false)
            .build())
        return generator.generateKey()
    }

    fun load(): List<StoredCredential> {
        val raw = prefs.getString(KEY_DATA, null) ?: return emptyList()
        return runCatching {
            val payload = decrypt(raw)
            val arr = JSONArray(payload)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                StoredCredential(
                    id = o.optString("id", UUID.randomUUID().toString()),
                    origin = o.optString("origin", ""),
                    username = o.optString("username", ""),
                    password = o.optString("password", ""),
                    nickname = o.optString("nickname", ""),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
                )
            }
        }.getOrDefault(emptyList())
    }

    @Synchronized fun save(items: List<StoredCredential>) {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(JSONObject().apply {
                put("id", item.id)
                put("origin", item.origin)
                put("username", item.username)
                put("password", item.password)
                put("nickname", item.nickname)
                put("createdAt", item.createdAt)
                put("updatedAt", item.updatedAt)
            })
        }
        prefs.edit().putString(KEY_DATA, encrypt(arr.toString())).commit()
    }

    fun clear() { prefs.edit().remove(KEY_DATA).commit() }

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val ciphertext = cipher.doFinal(plain.toByteArray(StandardCharsets.UTF_8))
        return Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" + Base64.encodeToString(ciphertext, Base64.NO_WRAP)
    }

    private fun decrypt(value: String): String {
        val parts = value.split(":", limit = 2)
        require(parts.size == 2)
        val iv = Base64.decode(parts[0], Base64.NO_WRAP)
        val ciphertext = Base64.decode(parts[1], Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8)
    }

    companion object {
        private const val PREFS = "bluebird_credentials"
        private const val KEY_DATA = "encrypted_credentials"
        private const val KEY_ALIAS = "BluebirdCredentialVaultKey"
    }
}

data class StoredCredential(
    val id: String = UUID.randomUUID().toString(),
    val origin: String,
    val username: String,
    val password: String,
    val nickname: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
