package com.ykis.ykismobkmp.ui.screens.chat

import dev.gitlive.firebase.storage.Data

actual fun ByteArray.toFirebaseData(): Data = Data(this)
