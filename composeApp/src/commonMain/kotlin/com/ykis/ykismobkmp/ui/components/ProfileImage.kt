/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ykis.mob.core.composable


import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ykis.ykismobkmp.core.utils.Log
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private const val className = "ProfileImageKt"

@Composable
fun ProfileImage(
  drawableResource: DrawableResource, // Заменили Int на DrawableResource
  description: String,
  modifier: Modifier = Modifier
) {
  // Логирование согласно правилу [Класс.Метод]
  Log.d("YkisLog", "[$className.ProfileImage]: Rendering image. Description: $description")

  Image(
    modifier = modifier
      .size(40.dp)
      .clip(CircleShape),
    painter = painterResource(drawableResource),
    contentDescription = description,
  )
}

