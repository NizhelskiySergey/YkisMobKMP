package com.ykis.ykismobkmp.ui.components

import androidx.compose.runtime.Composable

@Composable
expect fun GoogleAuthButton(
  buttonTextRes: Int,
  isLoading: Boolean,
  onTokenReceived: (String) -> Unit
)
