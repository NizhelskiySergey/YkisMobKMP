package com.ykis.ykismobkmp.core.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
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
 * [IosFilePicker] — Реалізація вибору файлів для iOS.
 * Використовує стандартний UIDocumentPickerViewController.
 */
class IosFilePicker : NSObject(), FilePicker, UIDocumentPickerDelegateProtocol {
    private var onFilePickedCallback: ((String) -> Unit)? = null

    override fun pickFile(onFilePicked: (String) -> Unit) {
        this.onFilePickedCallback = onFilePicked

        // Визначаємо типи файлів, які дозволено вибирати
        val contentTypes = listOf(
            UTTypeImage,
            UTTypePDF,
            UTTypePlainText,
            UTTypeData
        )

        val picker = UIDocumentPickerViewController(forOpeningContentTypes = contentTypes, asCopy = true)
        picker.delegate = this

        // Отримуємо активне вікно та контролер для відображення
        val window = UIApplication.sharedApplication.keyWindow
        val rootViewController = window?.rootViewController
        
        rootViewController?.presentViewController(picker, animated = true, completion = null)
    }

    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
        val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
        url?.path?.let { path ->
            println("[YkisLogKMP.FilePicker]: Обрано файл: $path")
            onFilePickedCallback?.invoke(path)
        }
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        println("[YkisLogKMP.FilePicker]: Користувач скасував вибір файлу.")
    }
}

@Composable
actual fun rememberFilePicker(): FilePicker = remember { IosFilePicker() }
