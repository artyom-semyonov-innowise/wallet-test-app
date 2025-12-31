package com.wallettest.wallet

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.security.keystore.UserNotAuthenticatedException
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PublicKey

class WalletKeyManager(private val context: Context) {
    
    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "wallet_private_key"
        private const val KEY_ALGORITHM = KeyProperties.KEY_ALGORITHM_EC
        private const val SIGNATURE_ALGORITHM = "SHA256withECDSA"
        private const val AUTHENTICATION_VALIDITY_DURATION_SECONDS = 30
    }

    private val keyStore: KeyStore by lazy {
        try {
            KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
                load(null)
            }
        } catch (e: Exception) {
            throw WalletException("Failed to initialize KeyStore: ${e.message}", e)
        }
    }

    fun generateKeyPair(): PublicKey {
        try {
            if (keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.deleteEntry(KEY_ALIAS)
            }

            val keyPairGenerator = KeyPairGenerator.getInstance(KEY_ALGORITHM, KEYSTORE_PROVIDER)
            
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            )
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setAlgorithmParameterSpec(java.security.spec.ECGenParameterSpec("secp256r1"))
                .setUserAuthenticationRequired(true)
                .setUserAuthenticationValidityDurationSeconds(AUTHENTICATION_VALIDITY_DURATION_SECONDS)
                .build()

            keyPairGenerator.initialize(keyGenParameterSpec)
            val keyPair = keyPairGenerator.generateKeyPair()
            
            return keyPair.public
        } catch (e: IllegalStateException) {
            if (e.message?.contains("Secure lock screen") == true) {
                throw WalletException("Secure lock screen (PIN/Pattern/Password) must be enabled to create keys requiring authentication. Please enable lock screen in device settings.", e)
            }
            throw WalletException("Failed to generate key pair: ${e.message}", e)
        } catch (e: Exception) {
            throw WalletException("Failed to generate key pair: ${e.message}", e)
        }
    }

    fun getPublicKey(): PublicKey {
        try {
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                throw WalletException("Key pair does not exist. Generate it first.")
            }
            
            val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.PrivateKeyEntry
                ?: throw WalletException("Key entry is not a private key entry")
            
            return entry.certificate.publicKey
        } catch (e: WalletException) {
            throw e
        } catch (e: Exception) {
            throw WalletException("Failed to get public key: ${e.message}", e)
        }
    }

    fun keyExists(): Boolean {
        return try {
            keyStore.containsAlias(KEY_ALIAS)
        } catch (e: Exception) {
            false
        }
    }

    internal fun getPrivateKeyEntry(): KeyStore.PrivateKeyEntry {
        try {
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                throw WalletException("Key pair does not exist. Generate it first.")
            }
            
            val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.PrivateKeyEntry
                ?: throw WalletException("Key entry is not a private key entry or has been invalidated")
            
            return entry
        } catch (e: WalletException) {
            throw e
        } catch (e: UserNotAuthenticatedException) {
            throw WalletException("Authentication required. Please authenticate to sign the transaction.", e)
        } catch (e: KeyPermanentlyInvalidatedException) {
            throw WalletException("Key has been permanently invalidated (e.g., biometrics changed). Please generate a new key pair.", e)
        } catch (e: java.security.UnrecoverableKeyException) {
            throw WalletException("Key has been invalidated. Please generate a new key pair.", e)
        } catch (e: Exception) {
            throw WalletException("Failed to access private key: ${e.message}", e)
        }
    }
}
