package com.ykis.ykismobkmp.core.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLImageElement
import org.w3c.files.FileReader

@Composable
actual fun rememberFilePicker(): FilePicker = remember {
    object : FilePicker {
        override fun pickFile(onFilePicked: (String, String?, Int, Int) -> Unit) {
            val input = document.createElement("input") as HTMLInputElement
            input.type = "file"
            input.style.display = "none"
            input.accept = "image/*,application/pdf,.doc,.docx,.xls,.xlsx"
            
            document.body?.appendChild(input)
            
            input.onchange = {
                val files = input.files
                if (files != null && files.length > 0) {
                    val file = files.item(0)
                    if (file != null) {
                        val fileName = file.name
                        val reader = FileReader()
                        reader.onload = { loadEvent ->
                            val result = loadEvent.target.asDynamic().result as String
                            
                            val img = document.createElement("img") as HTMLImageElement
                            img.onload = {
                                val w = img.naturalWidth
                                val h = img.naturalHeight
                                println("[FilePicker.js]: $fileName | Width: ${w}px | Height: ${h}px")
                                (window.asDynamic()).lastSelectedFile = file
                                onFilePicked(result, fileName, w, h)
                            }
                            img.src = result
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
