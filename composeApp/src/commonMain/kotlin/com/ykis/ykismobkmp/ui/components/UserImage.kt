package com.ykis.mob.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ykis.mob.R

@Composable
fun UserImage(
    modifier: Modifier = Modifier,
    photoUrl: String
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(photoUrl)
            .build(),
        contentDescription = null,
        error = painterResource(id = R.drawable.ic_account_circle),
        contentScale = ContentScale.Fit,
        placeholder = painterResource(id = R.drawable.ic_account_circle) ,
        modifier = modifier
            .clip(CircleShape)
            .width(48.dp)
            .height(48.dp)
    )
}