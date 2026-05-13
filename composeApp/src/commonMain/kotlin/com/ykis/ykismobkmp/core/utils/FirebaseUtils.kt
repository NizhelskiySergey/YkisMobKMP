package com.ykis.ykismobkmp.core.utils
import dev.gitlive.firebase.storage.Data

/**
 * Кроссплатформенное расширение для конвертации байтов в формат Firebase Storage.
 */
expect fun ByteArray.wrapForFirebase(): Data
