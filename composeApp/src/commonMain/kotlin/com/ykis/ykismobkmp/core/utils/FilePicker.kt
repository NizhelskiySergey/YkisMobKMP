package com.ykis.ykismobkmp.core.utils

import androidx.compose.runtime.Composable

/**
 * [FilePicker] — Кроссплатформенный интерфейс для выбора файлов (изображения, документы).
 */
interface FilePicker {
    fun pickFile(onFilePicked: (String) -> Unit)
}

/**
 * [rememberFilePicker] — Фабрика создания платформенного пикера файлов.
 */
@Composable
expect fun rememberFilePicker(): FilePicker
