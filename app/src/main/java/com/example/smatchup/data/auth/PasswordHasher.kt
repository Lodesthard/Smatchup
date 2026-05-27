package com.example.smatchup.data.auth

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordHasher {

    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16

    private val factory: SecretKeyFactory = SecretKeyFactory.getInstance(ALGORITHM)
    private val rng = SecureRandom()
    private val encoder: Base64.Encoder = Base64.getEncoder()
    private val decoder: Base64.Decoder = Base64.getDecoder()

    fun newSalt(): String {
        val bytes = ByteArray(SALT_BYTES)
        rng.nextBytes(bytes)
        return encoder.encodeToString(bytes)
    }

    fun hash(password: String, saltBase64: String): String {
        val saltBytes = decoder.decode(saltBase64)
        val spec = PBEKeySpec(password.toCharArray(), saltBytes, ITERATIONS, KEY_LENGTH_BITS)
        val key = factory.generateSecret(spec)
        return encoder.encodeToString(key.encoded)
    }

    fun verify(password: String, saltBase64: String, expectedHashBase64: String): Boolean {
        val computed = hash(password, saltBase64).toByteArray()
        val expected = expectedHashBase64.toByteArray()
        if (computed.size != expected.size) return false
        var diff = 0
        for (i in computed.indices) diff = diff or (computed[i].toInt() xor expected[i].toInt())
        return diff == 0
    }
}
