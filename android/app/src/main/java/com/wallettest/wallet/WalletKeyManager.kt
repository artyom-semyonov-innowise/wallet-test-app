package com.wallettest.wallet

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PublicKey

class WalletKeyManager(private val context: Context) {
    
    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "wallet_private_key"
        private const val KEY_ALGORITHM = KeyProperties.KEY_ALGORITHM_EC
        private const val SIGNATURE_ALGORITHM = "SHA256withECDSA"
    }

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
            load(null)
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
                .setUserAuthenticationRequired(false)
                .build()

            keyPairGenerator.initialize(keyGenParameterSpec)
            val keyPair = keyPairGenerator.generateKeyPair()
            
            return keyPair.public
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
        } catch (e: java.security.UnrecoverableKeyException) {
            throw WalletException("Key has been invalidated. Please generate a new key pair.", e)
        } catch (e: Exception) {
            throw WalletException("Failed to access private key: ${e.message}", e)
        }
    }
}
