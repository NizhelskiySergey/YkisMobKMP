package com.ykis.ykismobkmp.core.utils

import android.content.Context
import android.net.Uri
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.io.FileOutputStream

@Composable
actual fun rememberFilePicker(): FilePicker {
    val context = LocalContext.current
    var lastCallback by remember { mutableStateOf<((String, String?, Int, Int) -> Unit)?>(null) }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = getFileName(context, it)
            val path = getFilePathFromUri(context, it, fileName)
            if (path != null) {
                // ОТРИМУЄМО РЕАЛЬНІ РОЗМІРИ ДЛЯ БД
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(path, options)
                
                // Перевіряємо EXIF орієнтацію, бо ширина/висота можуть бути переплутані
                val exifInterface = try { android.media.ExifInterface(path) } catch (e: Exception) { null }
                val orientation = exifInterface?.getAttributeInt(
                    android.media.ExifInterface.TAG_ORIENTATION,
                    android.media.ExifInterface.ORIENTATION_NORMAL
                ) ?: android.media.ExifInterface.ORIENTATION_NORMAL
                
                val isRotated = orientation == android.media.ExifInterface.ORIENTATION_ROTATE_90 || 
                                orientation == android.media.ExifInterface.ORIENTATION_ROTATE_270
                
                val finalW = if (isRotated) options.outHeight else options.outWidth
                val finalH = if (isRotated) options.outWidth else options.outHeight
                
                println("[FilePicker.android]: $fileName | Dimensions: ${finalW}x${finalH}")
                lastCallback?.invoke(path, fileName, finalW, finalH)
                lastCallback = null
            }
        }
    }
    
    return remember(launcher) { 
        object : FilePicker {
            override fun pickFile(onFilePicked: (String, String?, Int, Int) -> Unit) {
                lastCallback = onFilePicked
                launcher.launch("*/*")
            }
        }
    }
}

private fun getFilePathFromUri(context: Context, uri: Uri, fileName: String?): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val finalName = fileName ?: "temp_file_${System.currentTimeMillis()}"
        val tempFile = File(context.cacheDir, finalName)
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
