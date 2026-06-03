package com.ykis.ykismobkmp.core.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

class JsSmsRetriever : SmsRetriever {
    override fun startRetriever(onCodeReceived: (String) -> Unit) {}
    override fun stopRetriever() {}
}

@Composable
actual fun rememberSmsRetriever(): SmsRetriever = remember { JsSmsRetriever() }
