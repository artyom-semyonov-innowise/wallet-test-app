package com.wallettest.wallet

import android.content.Context
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.UserNotAuthenticatedException
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
        private const val MAX_AMOUNT_LENGTH = 50
        private const val MAX_DECIMAL_PLACES = 18
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

    private val nonceLock = Any()

    fun signTransaction(transactionJson: String): String {
        try {
            val transaction = JSONObject(transactionJson)
            
            if (!transaction.has("amount")) {
                throw WalletException("Missing 'amount' field")
            }
            if (!transaction.has("currency")) {
                throw WalletException("Missing 'currency' field")
            }
            if (!transaction.has("nonce")) {
                throw WalletException("Missing 'nonce' field")
            }
            
            val amount: String
            val currency: String
            val nonce: Long
            
            try {
                amount = transaction.getString("amount")
                currency = transaction.getString("currency")
                nonce = transaction.getLong("nonce")
            } catch (e: org.json.JSONException) {
                throw WalletException("Invalid transaction data format: ${e.message}", e)
            }

            validateAmount(amount)

            synchronized(nonceLock) {
                validateAndSaveNonce(nonce)
            }

            val transactionData = createCanonicalTransactionData(amount, currency, nonce)
            
            val privateKeyEntry = keyManager.getPrivateKeyEntry()
            
            val signature = Signature.getInstance("SHA256withECDSA")
            signature.initSign(privateKeyEntry.privateKey)
            signature.update(transactionData.toByteArray(Charsets.UTF_8))
            val signatureBytes = signature.sign()

            return Base64.encodeToString(signatureBytes, Base64.NO_WRAP)
        } catch (e: WalletException) {
            throw e
        } catch (e: UserNotAuthenticatedException) {
            throw WalletException("Authentication required. Please unlock your device with PIN/pattern/password, then try signing again. The system will prompt for authentication automatically.", e)
        } catch (e: KeyPermanentlyInvalidatedException) {
            throw WalletException("Key has been permanently invalidated (e.g., biometrics changed). Please generate a new key pair.", e)
        } catch (e: Exception) {
            throw WalletException("Failed to sign transaction: ${e.message}", e)
        }
    }

    private fun validateAmount(amount: String) {
        if (amount.isEmpty()) {
            throw WalletException("Amount cannot be empty")
        }

        if (amount.length > MAX_AMOUNT_LENGTH) {
            throw WalletException("Amount exceeds maximum length")
        }

        val amountValue = amount.toDoubleOrNull()
            ?: throw WalletException("Invalid amount format")

        if (amountValue <= 0) {
            throw WalletException("Amount must be positive")
        }

        val decimalIndex = amount.indexOf('.')
        if (decimalIndex >= 0) {
            val decimalPlaces = amount.length - decimalIndex - 1
            if (decimalPlaces > MAX_DECIMAL_PLACES) {
                throw WalletException("Amount cannot have more than $MAX_DECIMAL_PLACES decimal places")
            }
        }
    }

    private fun validateAndSaveNonce(nonce: Long) {
        val lastNonce = prefs.getLong(LAST_NONCE_KEY, -1L)
        
        if (nonce <= lastNonce) {
            throw WalletException(
                "Invalid nonce: $nonce. Must be greater than last used nonce: $lastNonce"
            )
        }

        prefs.edit().putLong(LAST_NONCE_KEY, nonce).commit()
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
