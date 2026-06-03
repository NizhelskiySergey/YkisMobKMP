package com.ykis.ykismobkmp.core.utils

import androidx.compose.runtime.Composable

/**
 * [SmsRetriever] — Кроссплатформенный интерфейс автоматического перехвата SMS-кодов авторизации.
 */
interface SmsRetriever {
    /**
     * [startRetriever] — Запуск системного слушателя входящих сообщений.
     * @param onCodeReceived — Коллбек, вызываемый при успешном парсинге 6-значного кода.
     */
    fun startRetriever(onCodeReceived: (String) -> Unit)

    /**
     * [stopRetriever] — Остановка слушателя и освобождение ресурсов.
     */
    fun stopRetriever()
}

/**
 * [rememberSmsRetriever] — Expect-фабрика для получения платформенной реализации ретривера.
 */
@Composable
expect fun rememberSmsRetriever(): SmsRetriever
