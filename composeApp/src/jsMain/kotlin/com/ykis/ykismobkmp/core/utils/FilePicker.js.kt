package com.ykis.ykismobkmp.core.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.browser.document
import org.w3c.dom.HTMLInputElement

@Composable
actual fun rememberFilePicker(): FilePicker = remember {
    object : FilePicker {
        override fun pickFile(onFilePicked: (String) -> Unit) {
            println("[YkisLogKMP.FilePicker]: Спроба відкрити діалог вибору файлу...")
            
            // Створюємо елемент приховано, але додаємо в DOM для кращої сумісності
            val input = document.createElement("input") as HTMLInputElement
            input.type = "file"
            input.style.display = "none"
            input.accept = "image/*,application/pdf"
            
            document.body?.appendChild(input)
            
            input.onchange = {
                val files = input.files
                if (files != null && files.length > 0) {
                    val file = files.item(0)
                    if (file != null) {
                        val reader = org.w3c.files.FileReader()
                        reader.onload = { loadEvent ->
                            val result = loadEvent.target.asDynamic().result as String
                            println("[YkisLogKMP.FilePicker]: Файл отримано. Розмір: ${file.size}")
                            onFilePicked(result)
                            document.body?.removeChild(input)
                        }
                        reader.readAsDataURL(file)
                    }
                } else {
                    document.body?.removeChild(input)
                }
            }
            
            input.click()
        }
    }
}
