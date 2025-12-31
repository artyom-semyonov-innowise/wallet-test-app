package com.wallettest.wallet

import android.util.Base64
import com.facebook.react.bridge.*
import java.security.PublicKey

class WalletModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    private val keyManager: WalletKeyManager by lazy {
        WalletKeyManager(reactApplicationContext)
    }
    
    private val transactionSigner: TransactionSigner by lazy {
        TransactionSigner(reactApplicationContext, keyManager)
    }

    override fun getName(): String {
        return "WalletModule"
    }

    @ReactMethod
    fun generateKeyPair(promise: Promise) {
        try {
            val publicKey = keyManager.generateKeyPair()
            val publicKeyBytes = publicKey.encoded
            val publicKeyBase64 = Base64.encodeToString(publicKeyBytes, Base64.NO_WRAP)
            
            val result = WritableNativeMap().apply {
                putString("publicKey", publicKeyBase64)
            }
            
            promise.resolve(result)
        } catch (e: WalletException) {
            promise.reject("WALLET_ERROR", e.message, e)
        } catch (e: Exception) {
            promise.reject("UNKNOWN_ERROR", "Failed to generate key pair: ${e.message}", e)
        }
    }

    @ReactMethod
    fun getPublicKey(promise: Promise) {
        try {
            val publicKey = keyManager.getPublicKey()
            val publicKeyBytes = publicKey.encoded
            val publicKeyBase64 = Base64.encodeToString(publicKeyBytes, Base64.NO_WRAP)
            
            promise.resolve(publicKeyBase64)
        } catch (e: WalletException) {
            promise.reject("WALLET_ERROR", e.message, e)
        } catch (e: Exception) {
            promise.reject("UNKNOWN_ERROR", "Failed to get public key: ${e.message}", e)
        }
    }

    @ReactMethod
    fun signTransaction(transaction: ReadableMap, promise: Promise) {
        try {
            val amount = transaction.getString("amount")
                ?: throw IllegalArgumentException("Transaction must have 'amount' field")
            val currency = transaction.getString("currency")
                ?: throw IllegalArgumentException("Transaction must have 'currency' field")
            
            val nonceValue = transaction.getDouble("nonce")
            if (nonceValue.isNaN() || nonceValue < 0) {
                throw IllegalArgumentException("Invalid nonce value")
            }
            val nonce = nonceValue.toLong()
            
            if (amount.isEmpty()) {
                throw IllegalArgumentException("Amount cannot be empty")
            }
            if (currency.isEmpty()) {
                throw IllegalArgumentException("Currency cannot be empty")
            }

            val transactionJson = org.json.JSONObject().apply {
                put("amount", amount)
                put("currency", currency)
                put("nonce", nonce)
            }.toString()

            val signature = transactionSigner.signTransaction(transactionJson)
            
            promise.resolve(signature)
        } catch (e: WalletException) {
            promise.reject("WALLET_ERROR", e.message, e)
        } catch (e: IllegalArgumentException) {
            promise.reject("INVALID_INPUT", e.message, e)
        } catch (e: Exception) {
            promise.reject("UNKNOWN_ERROR", "Failed to sign transaction: ${e.message}", e)
        }
    }

    @ReactMethod
    fun keyExists(promise: Promise) {
        try {
            val exists = keyManager.keyExists()
            promise.resolve(exists)
        } catch (e: Exception) {
            promise.reject("UNKNOWN_ERROR", "Failed to check key existence: ${e.message}", e)
        }
    }

    @ReactMethod
    fun getNextNonce(promise: Promise) {
        try {
            val nextNonce = transactionSigner.getNextNonce()
            promise.resolve(nextNonce.toDouble())
        } catch (e: WalletException) {
            promise.reject("WALLET_ERROR", e.message, e)
        } catch (e: Exception) {
            promise.reject("UNKNOWN_ERROR", "Failed to get next nonce: ${e.message}", e)
        }
    }
}
