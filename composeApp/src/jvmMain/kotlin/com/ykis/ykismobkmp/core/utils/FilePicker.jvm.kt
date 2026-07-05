package com.ykis.ykismobkmp.core.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberFilePicker(): FilePicker = remember {
    object : FilePicker {
        override fun pickFile(onFilePicked: (String, String?, Int, Int) -> Unit) {
            println("[YkisLogKMP.FilePicker]: Вибір файлів на Desktop поки не реалізований")
        }
    }
}
