package com.wallettest.wallet

class WalletException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
