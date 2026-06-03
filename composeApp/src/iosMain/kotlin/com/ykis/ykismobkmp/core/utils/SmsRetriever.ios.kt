package com.ykis.ykismobkmp.core.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * [IosSmsRetriever] — Заглушка для iOS. 
 * На iOS автоподстановка кода работает нативно через textContentType.
 */
class IosSmsRetriever : SmsRetriever {
    override fun startRetriever(onCodeReceived: (String) -> Unit) { /* Не требуется */ }
    override fun stopRetriever() { /* Не требуется */ }
}

@Composable
actual fun rememberSmsRetriever(): SmsRetriever {
    return remember { IosSmsRetriever() }
}
