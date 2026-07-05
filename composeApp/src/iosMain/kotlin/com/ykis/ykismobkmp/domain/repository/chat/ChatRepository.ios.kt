package com.ykis.ykismobkmp.domain.repository.chat

import platform.Foundation.NSData
import platform.Foundation.create
import platform.posix.memcpy
import kotlinx.cinterop.*
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIGraphicsBeginImageContext

@OptIn(ExperimentalForeignApi::class)
actual suspend fun platformReadFileAsBytes(path: String): ByteArray {
  val data = NSData.create(contentsOfFile = path) ?: return byteArrayOf()
  val bytes = ByteArray(data.length.toInt())

  bytes.usePinned { pinned ->
    memcpy(pinned.addressOf(0), data.bytes, data.length)
  }

  return bytes
}

@OptIn(ExperimentalForeignApi::class)
actual suspend fun platformCompressImage(path: String): ByteArray {
    val image = UIImage.imageWithContentsOfFile(path) ?: return platformReadFileAsBytes(path)
    
    val maxSide = 1600.0
    val width = image.size.useContents { width }
    val height = image.size.useContents { height }
    
    var newWidth = width
    var newHeight = height
    
    if (width > height && width > maxSide) {
        newHeight = height * (maxSide / width)
        newWidth = maxSide
    } else if (height > maxSide) {
        newWidth = width * (maxSide / height)
        newHeight = maxSide
    }
    
    UIGraphicsBeginImageContextWithOptions(CGSizeMake(newWidth, newHeight), false, 1.0)
    image.drawInRect(platform.CoreGraphics.CGRectMake(0.0, 0.0, newWidth, newHeight))
    val resizedImage = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()
    
    if (resizedImage == null) return platformReadFileAsBytes(path)
    
    val data = UIImageJPEGRepresentation(resizedImage, 0.85) ?: return platformReadFileAsBytes(path)
    val bytes = ByteArray(data.length.toInt())
    bytes.usePinned { pinned ->
        memcpy(pinned.addressOf(0), data.bytes, data.length)
    }
    
    println("[ChatRepository.ios]: Фото стиснено. Розмір: ${bytes.size / 1024} КБ. Розміри: ${newWidth.toInt()}x${newHeight.toInt()}")
    return bytes
}
