package com.ykis.ykismobkmp.core.utils

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.io.FileOutputStream

@Composable
actual fun rememberFilePicker(): FilePicker {
    val context = LocalContext.current
    var lastCallback by remember { mutableStateOf<((String) -> Unit)?>(null) }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val path = getFilePathFromUri(context, it)
            if (path != null) {
                lastCallback?.invoke(path)
                lastCallback = null
            }
        }
    }
    
    return remember(launcher) { 
        object : FilePicker {
            override fun pickFile(onFilePicked: (String) -> Unit) {
                lastCallback = onFilePicked
                launcher.launch("*/*")
            }
        }
    }
}

/**
 * [getFilePathFromUri] — Вспомогательный метод для копирования Uri во временный файл.
 * Это необходимо, так как современные Android не дают прямого пути к файлу.
 */
private fun getFilePathFromUri(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val fileName = getFileName(context, uri) ?: "temp_file_${System.currentTimeMillis()}"
        val tempFile = File(context.cacheDir, fileName)
        val outputStream = FileOutputStream(tempFile)
        
        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        tempFile.absolutePath
    } catch (e: Exception) {
        println("[YkisLogKMP.FilePicker]: Ошибка копирования файла: ${e.message}")
        null
    }
}

private fun getFileName(context: Context, uri: Uri): String? {
    var name: String? = null
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (index != -1) {
                name = it.getString(index)
            }
        }
    }
    return name
}
