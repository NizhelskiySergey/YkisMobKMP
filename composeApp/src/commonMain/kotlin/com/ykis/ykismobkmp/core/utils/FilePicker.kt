package com.ykis.ykismobkmp.core.utils

import androidx.compose.runtime.Composable

/**
 * [FilePicker] — Кроссплатформенный интерфейс для выбора файлов.
 * ИСПРАВЛЕНО: Теперь возвращает путь, имя, а также размеры (для фикса растягивания на Web).
 */
interface FilePicker {
    fun pickFile(onFilePicked: (String, String?, Int, Int) -> Unit)
}

/**
 * [rememberFilePicker] — Фабрика создания платформенного пикера файлов.
 */
@Composable
expect fun rememberFilePicker(): FilePicker
