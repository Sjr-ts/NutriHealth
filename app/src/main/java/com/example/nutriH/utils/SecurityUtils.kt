// app/src/main/java/com/example/nutrih/utils/SecurityUtils.kt

package com.example.nutrih.utils

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object SecurityUtils {
    private const val SECRET_KEY = "NutriHealthAppSecretKey2024Build" // 32 caracteres
    private const val INIT_VECTOR = "NutriHealthIV123" // 16 caracteres (IV)

    private val iv = IvParameterSpec(INIT_VECTOR.toByteArray(Charsets.UTF_8))
    private val secretKeySpec = SecretKeySpec(SECRET_KEY.toByteArray(Charsets.UTF_8), "AES")

    fun encrypt(value: String): String {
        return try {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, iv)
            val encrypted = cipher.doFinal(value.toByteArray())
            Base64.encodeToString(encrypted, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            value
        }
    }

    fun decrypt(encrypted: String): String {
        return try {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, iv)
            val original = cipher.doFinal(Base64.decode(encrypted, Base64.DEFAULT))
            String(original)
        } catch (e: Exception) {
            encrypted
        }
    }
}