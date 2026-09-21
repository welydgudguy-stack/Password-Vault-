package com.example.security

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class PasswordStrength(
    val score: Int, // 0 to 100
    val label: String, // Very Weak, Weak, Moderate, Strong, Very Strong
    val progress: Float, // 0.0 to 1.0
    val hasMinLength: Boolean,
    val hasUpper: Boolean,
    val hasLower: Boolean,
    val hasNumber: Boolean,
    val hasSymbol: Boolean
)

object CryptoManager {
    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 12000
    private const val KEY_LENGTH = 256
    private const val GCM_TAG_LENGTH = 128
    private const val IV_LENGTH = 12

    private val secureRandom = SecureRandom()

    // Character sets for generator
    private const val UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val LOWERCASE = "abcdefghijklmnopqrstuvwxyz"
    private const val NUMBERS = "0123456789"
    private const val SYMBOLS = "!@#$%^&*()-_=+[]{}|;:,.<>?"

    fun generateSalt(): ByteArray {
        val salt = ByteArray(16)
        secureRandom.nextBytes(salt)
        return salt
    }

    fun deriveKey(password: String, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val secretKeyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(secretKeyBytes, "AES")
    }

    fun hashPassword(password: String, salt: ByteArray): String {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val hash = factory.generateSecret(spec).encoded
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    fun verifyPassword(password: String, storedHash: String, salt: ByteArray): Boolean {
        val computedHash = hashPassword(password, salt)
        return computedHash == storedHash
    }

    fun encrypt(plainText: String, secretKey: SecretKey): String {
        if (plainText.isEmpty()) return ""
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(IV_LENGTH)
        secureRandom.nextBytes(iv)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        // Combine IV and cipherText
        val combined = ByteArray(iv.size + cipherText.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)

        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decrypt(encryptedBase64: String, secretKey: SecretKey): String {
        if (encryptedBase64.isEmpty()) return ""
        try {
            val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            if (combined.size <= IV_LENGTH) return ""
            val iv = ByteArray(IV_LENGTH)
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH)
            val cipherText = ByteArray(combined.size - IV_LENGTH)
            System.arraycopy(combined, IV_LENGTH, cipherText, 0, cipherText.size)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            val plainBytes = cipher.doFinal(cipherText)
            return String(plainBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    fun generatePassword(
        length: Int = 16,
        includeUpper: Boolean = true,
        includeLower: Boolean = true,
        includeNumbers: Boolean = true,
        includeSymbols: Boolean = true
    ): String {
        val pool = StringBuilder()
        val guaranteedChars = mutableListOf<Char>()

        if (includeUpper) {
            pool.append(UPPERCASE)
            guaranteedChars.add(UPPERCASE[secureRandom.nextInt(UPPERCASE.length)])
        }
        if (includeLower) {
            pool.append(LOWERCASE)
            guaranteedChars.add(LOWERCASE[secureRandom.nextInt(LOWERCASE.length)])
        }
        if (includeNumbers) {
            pool.append(NUMBERS)
            guaranteedChars.add(NUMBERS[secureRandom.nextInt(NUMBERS.length)])
        }
        if (includeSymbols) {
            pool.append(SYMBOLS)
            guaranteedChars.add(SYMBOLS[secureRandom.nextInt(SYMBOLS.length)])
        }

        if (pool.isEmpty()) {
            pool.append(LOWERCASE).append(NUMBERS)
        }

        val passwordChars = ArrayList<Char>(length)
        passwordChars.addAll(guaranteedChars)

        while (passwordChars.size < length) {
            val randomIndex = secureRandom.nextInt(pool.length)
            passwordChars.add(pool[randomIndex])
        }

        // Shuffle to avoid predictable positions for guaranteed characters
        passwordChars.shuffle(secureRandom)
        return passwordChars.joinToString("")
    }

    fun evaluateStrength(password: String): PasswordStrength {
        val hasMinLength = password.length >= 12
        val hasUpper = password.any { it.isUpperCase() }
        val hasLower = password.any { it.isLowerCase() }
        val hasNumber = password.any { it.isDigit() }
        val hasSymbol = password.any { !it.isLetterOrDigit() }

        var score = 0
        if (password.length >= 8) score += 15
        if (password.length >= 12) score += 20
        if (password.length >= 16) score += 15
        if (hasUpper) score += 15
        if (hasLower) score += 10
        if (hasNumber) score += 12
        if (hasSymbol) score += 13

        // Diversity bonus
        val variety = listOf(hasUpper, hasLower, hasNumber, hasSymbol).count { it }
        if (variety == 4 && password.length >= 12) score = (score + 10).coerceAtMost(100)

        val progress = (score / 100f).coerceIn(0f, 1f)
        val label = when {
            score < 30 -> "Weak"
            score < 60 -> "Moderate"
            score < 85 -> "Strong"
            else -> "Very Strong"
        }

        return PasswordStrength(
            score = score,
            label = label,
            progress = progress,
            hasMinLength = hasMinLength,
            hasUpper = hasUpper,
            hasLower = hasLower,
            hasNumber = hasNumber,
            hasSymbol = hasSymbol
        )
    }
}
