package com.ykis.ykismobkmp.core.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

class JvmSmsRetriever : SmsRetriever {
    override fun startRetriever(onCodeReceived: (String) -> Unit) {
        // SMS Retriever API не підтримується на JVM Desktop
    }
    override fun stopRetriever() {
        // Заглушка для JVM
    }
}

@Composable
actual fun rememberSmsRetriever(): SmsRetriever = remember { JvmSmsRetriever() }
