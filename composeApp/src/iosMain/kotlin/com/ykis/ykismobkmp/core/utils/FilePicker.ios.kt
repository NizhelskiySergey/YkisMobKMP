package com.ykis.ykismobkmp.core.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UniformTypeIdentifiers.UTType
import platform.UniformTypeIdentifiers.UTTypeData
import platform.UniformTypeIdentifiers.UTTypeImage
import platform.UniformTypeIdentifiers.UTTypePDF
import platform.UniformTypeIdentifiers.UTTypePlainText
import platform.darwin.NSObject

/**
 * [IosFilePicker] — Реалізація для iOS.
 */
class IosFilePicker : FilePicker {
    
    // Хранимо посилання на делегат, щоб ARC (пам'ять) не видалила його під час вибору файлу
    private var currentDelegate: PickerDelegate? = null

    private class PickerDelegate(private val onFilePicked: (String, String?, Int, Int) -> Unit) : NSObject(), UIDocumentPickerDelegateProtocol {
        override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
            val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
            val path = url?.path
            if (path != null) {
                val fileName = url.lastPathComponent
                println("[YkisLogKMP.FilePicker]: Файл успішно отримано: $fileName")
                onFilePicked(path, fileName, 0, 0)
            }
        }

        override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
            println("[YkisLogKMP.FilePicker]: Вибір скасовано користувачем")
        }
    }

    override fun pickFile(onFilePicked: (String, String?, Int, Int) -> Unit) {
        val contentTypes = listOf(
            UTTypeImage,
            UTTypePDF,
            UTTypePlainText,
            UTTypeData
        )

        val picker = UIDocumentPickerViewController(forOpeningContentTypes = contentTypes, asCopy = true)
        
        // Створюємо та зберігаємо делегат
        val delegate = PickerDelegate(onFilePicked)
        this.currentDelegate = delegate
        picker.delegate = delegate
        
        val window = UIApplication.sharedApplication.keyWindow
        val rootViewController = window?.rootViewController
        rootViewController?.presentViewController(picker, animated = true, completion = null)
    }
}

@Composable
actual fun rememberFilePicker(): FilePicker = remember { IosFilePicker() }
