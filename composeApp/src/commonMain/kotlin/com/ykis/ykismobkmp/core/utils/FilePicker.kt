package com.ykis.ykismobkmp.core.utils

import androidx.compose.runtime.Composable

/**
 * [FilePicker] — Кроссплатформенный интерфейс для выбора файлов.
 * ИСПРАВЛЕНО: Теперь возвращает и содержимое (путь/base64), и имя файла.
 */
interface FilePicker {
    fun pickFile(onFilePicked: (String, String?) -> Unit)
}

/**
 * [rememberFilePicker] — Фабрика создания платформенного пикера файлов.
 */
@Composable
expect fun rememberFilePicker(): FilePicker
