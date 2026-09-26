package com.example.kinetiq.utils

import java.security.MessageDigest

object SecurityUtils {
    const val SESSION_TIMEOUT_MS = 15 * 60 * 1000L // 15 minutes

    fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verifyPin(inputPin: String, storedHash: String): Boolean {
        return hashPin(inputPin) == storedHash
    }

    fun isSessionExpired(lastActiveTimestamp: Long, expiryTimestamp: Long): Boolean {
        val now = System.currentTimeMillis()
        return now >= expiryTimestamp || (now - lastActiveTimestamp) >= SESSION_TIMEOUT_MS
    }
}