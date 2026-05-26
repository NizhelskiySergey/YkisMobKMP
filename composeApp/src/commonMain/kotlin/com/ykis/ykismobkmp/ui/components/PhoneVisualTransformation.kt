package com.ykis.ykismobkmp.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import kotlin.math.min

class PhoneVisualTransformation : VisualTransformation {
  override fun filter(text: AnnotatedString): TransformedText {
    // Пропускаем только чистые цифры, игнорируя случайные +380 на старте
    var trimmed = text.text.filter { it.isDigit() }
    if (trimmed.startsWith("380")) {
      trimmed = trimmed.substring(3)
    } else if (trimmed.startsWith("80")) {
      trimmed = trimmed.substring(2)
    } else if (trimmed.startsWith("0")) {
      trimmed = trimmed.substring(1)
    }

    val out = StringBuilder("+380 ")
    val transformedIndices = IntArray(trimmed.length + 1)
    transformedIndices[0] = out.length

    for (i in trimmed.indices) {
      if (i == 0) out.append("(")
      out.append(trimmed[i])
      if (i == 1) out.append(") ")
      if (i == 4 || i == 6) out.append("-")
      transformedIndices[i + 1] = out.length
    }

    val formattedText = out.toString()

    val phoneOffsetMapping = object : OffsetMapping {
      override fun originalToTransformed(offset: Int): Int {
        val safeOffset = min(offset, trimmed.length)
        if (safeOffset < 0) return 5
        return min(transformedIndices[safeOffset], formattedText.length)
      }

      override fun transformedToOriginal(offset: Int): Int {
        if (offset <= 5) return 0
        for (i in transformedIndices.indices) {
          if (transformedIndices[i] >= offset) return i
        }
        return trimmed.length
      }
    }

    return TransformedText(AnnotatedString(formattedText), phoneOffsetMapping)
  }
}
