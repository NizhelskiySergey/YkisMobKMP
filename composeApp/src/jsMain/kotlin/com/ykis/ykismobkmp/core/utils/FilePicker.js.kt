package com.ykis.ykismobkmp.core.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberFilePicker(): FilePicker = remember {
    object : FilePicker {
        override fun pickFile(onFilePicked: (String) -> Unit) {
            println("[YkisLogKMP.FilePicker]: Выбор файлов в Web пока не реализован")
        }
    }
}
