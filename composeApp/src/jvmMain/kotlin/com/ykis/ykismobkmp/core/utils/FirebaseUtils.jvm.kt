package com.ykis.ykismobkmp.core.utils

import dev.gitlive.firebase.storage.Data

actual fun ByteArray.wrapForFirebase(): Data = Data(this)

