package com.weloop.weloop.utils

import android.util.Base64
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/* Created by *-----* Alexandre Thauvin *-----* */   object AES256Cryptor {
    /**
     * Encrypt
     * @param plaintext plain string
     * @param passphrase passphrase
     * @return
     */
    fun encrypt(plaintext: String, passphrase: String): String? {
        try {
            val keySize = 256
            val ivSize = 128

            // Create empty key and iv
            val key = ByteArray(keySize / 8)
            val iv = ByteArray(ivSize / 8)

            // Create random salt
            val saltBytes = generateSalt(8)

            // Derive key and iv from passphrase and salt
            EvpKDF(
                passphrase.toByteArray(charset("UTF-8")),
                keySize,
                ivSize,
                saltBytes,
                key,
                iv
            )

            // Actual encrypt
            val cipher =
                Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(
                Cipher.ENCRYPT_MODE,
                SecretKeySpec(key, "AES"),
                IvParameterSpec(iv)
            )
            val cipherBytes =
                cipher.doFinal(plaintext.toByteArray(charset("UTF-8")))

            /**
             * Create CryptoJS-like encrypted string from encrypted data
             * This is how CryptoJS do:
             * 1. Create new byte array to hold ecrypted string (b)
             * 2. Concatenate 8 bytes to b
             * 3. Concatenate salt to b
             * 4. Concatenate encrypted data to b
             * 5. Encode b using Base64
             */
            val sBytes = "Salted__".toByteArray(charset("UTF-8"))
            val b =
                ByteArray(sBytes.size + saltBytes.size + cipherBytes.size)
            System.arraycopy(sBytes, 0, b, 0, sBytes.size)
            System.arraycopy(saltBytes, 0, b, sBytes.size, saltBytes.size)
            System.arraycopy(
                cipherBytes,
                0,
                b,
                sBytes.size + saltBytes.size,
                cipherBytes.size
            )
            val base64b =
                Base64.encode(b, Base64.DEFAULT)
            return String(base64b)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * Decrypt
     * Thanks Artjom B. for this: http://stackoverflow.com/a/29152379/4405051
     * @param ciphertext encrypted string
     * @param passphrase passphrase
     */
    fun decrypt(ciphertext: String, passphrase: String): String? {
        try {
            val keySize = 256
            val ivSize = 128

            // Decode from base64 text
            val ctBytes = Base64.decode(
                ciphertext.toByteArray(charset("UTF-8")),
                Base64.DEFAULT
            )

            // Get salt
            val saltBytes = Arrays.copyOfRange(ctBytes, 8, 16)

            // Get ciphertext
            val ciphertextBytes =
                Arrays.copyOfRange(ctBytes, 16, ctBytes.size)

            // Get key and iv from passphrase and salt
            val key = ByteArray(keySize / 8)
            val iv = ByteArray(ivSize / 8)
            EvpKDF(
                passphrase.toByteArray(charset("UTF-8")),
                keySize,
                ivSize,
                saltBytes,
                key,
                iv
            )

            // Actual decrypt
            val cipher =
                Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(key, "AES"),
                IvParameterSpec(iv)
            )
            val recoveredPlaintextBytes = cipher.doFinal(ciphertextBytes)
            return String(recoveredPlaintextBytes)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4)
                    + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    /**
     * @return a new pseudorandom salt of the specified length
     */
    private fun generateSalt(length: Int): ByteArray {
        val r: Random = SecureRandom()
        val salt = ByteArray(length)
        r.nextBytes(salt)
        return salt
    }

    @Throws(NoSuchAlgorithmException::class)
    private fun EvpKDF(
        password: ByteArray,
        keySize: Int,
        ivSize: Int,
        salt: ByteArray,
        resultKey: ByteArray,
        resultIv: ByteArray
    ): ByteArray {
        return EvpKDF(password, keySize, ivSize, salt, 1, "MD5", resultKey, resultIv)
    }

    @Throws(NoSuchAlgorithmException::class)
    private fun EvpKDF(
        password: ByteArray,
        keySize: Int,
        ivSize: Int,
        salt: ByteArray,
        iterations: Int,
        hashAlgorithm: String,
        resultKey: ByteArray,
        resultIv: ByteArray
    ): ByteArray {
        var keySize = keySize
        var ivSize = ivSize
        keySize = keySize / 32
        ivSize = ivSize / 32
        val targetKeySize = keySize + ivSize
        val derivedBytes = ByteArray(targetKeySize * 4)
        var numberOfDerivedWords = 0
        var block: ByteArray? = null
        val hasher =
            MessageDigest.getInstance(hashAlgorithm)
        while (numberOfDerivedWords < targetKeySize) {
            if (block != null) {
                hasher.update(block)
            }
            hasher.update(password)
            block = hasher.digest(salt)
            hasher.reset()

            // Iterations
            for (i in 1 until iterations) {
                block = hasher.digest(block)
                hasher.reset()
            }
            System.arraycopy(
                block!!, 0, derivedBytes, numberOfDerivedWords * 4,
                Math.min(block.size, (targetKeySize - numberOfDerivedWords) * 4)
            )
            numberOfDerivedWords += block.size / 4
        }
        System.arraycopy(derivedBytes, 0, resultKey, 0, keySize * 4)
        System.arraycopy(derivedBytes, keySize * 4, resultIv, 0, ivSize * 4)
        return derivedBytes // key + iv
    }
}