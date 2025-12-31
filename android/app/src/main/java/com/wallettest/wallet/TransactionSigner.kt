package com.wallettest.wallet

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.Signature
import org.json.JSONObject

class TransactionSigner(
    private val context: Context,
    private val keyManager: WalletKeyManager
) {
    
    companion object {
        private const val PREFS_NAME = "wallet_prefs"
        private const val LAST_NONCE_KEY = "last_used_nonce"
    }

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun signTransaction(transactionJson: String): String {
        try {
            val transaction = JSONObject(transactionJson)
            val amount = transaction.getString("amount")
            val currency = transaction.getString("currency")
            val nonce = transaction.getLong("nonce")

            validateNonce(nonce)

            val transactionData = createCanonicalTransactionData(amount, currency, nonce)
            
            val privateKeyEntry = keyManager.getPrivateKeyEntry()
            
            val signature = Signature.getInstance("SHA256withECDSA")
            signature.initSign(privateKeyEntry.privateKey)
            signature.update(transactionData.toByteArray(Charsets.UTF_8))
            val signatureBytes = signature.sign()

            saveLastNonce(nonce)

            return Base64.encodeToString(signatureBytes, Base64.NO_WRAP)
        } catch (e: WalletException) {
            throw e
        } catch (e: Exception) {
            throw WalletException("Failed to sign transaction: ${e.message}", e)
        }
    }

    private fun validateNonce(nonce: Long) {
        val lastNonce = prefs.getLong(LAST_NONCE_KEY, -1L)
        
        if (nonce <= lastNonce) {
            throw WalletException(
                "Invalid nonce: $nonce. Must be greater than last used nonce: $lastNonce"
            )
        }
    }

    private fun saveLastNonce(nonce: Long) {
        prefs.edit().putLong(LAST_NONCE_KEY, nonce).apply()
    }

    private fun createCanonicalTransactionData(
        amount: String,
        currency: String,
        nonce: Long
    ): String {
        return "$amount|$currency|$nonce"
    }

    fun getLastNonce(): Long {
        return prefs.getLong(LAST_NONCE_KEY, -1L)
    }

    fun getNextNonce(): Long {
        val lastNonce = getLastNonce()
        return lastNonce + 1
    }
}
