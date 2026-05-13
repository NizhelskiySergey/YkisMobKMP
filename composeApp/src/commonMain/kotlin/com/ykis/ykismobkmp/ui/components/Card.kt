package com.ykis.ykismobkmp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ykis.ykismobkmp.core.utils.Log
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val className = "CardEditorsKt"

@Composable
fun DangerousCardEditor(
  title: StringResource,
  icon: DrawableResource,
  content: String,
  modifier: Modifier = Modifier,
  onEditClick: () -> Unit
) {
  Log.d("YkisLog", "[$className.DangerousCardEditor]: Clicked")
  CardEditor(title, icon, content, onEditClick, modifier)
}

@Composable
fun RegularCardEditor(
  title: StringResource,
  icon: DrawableResource,
  content: String,
  modifier: Modifier = Modifier,
  onEditClick: () -> Unit
) {
  Log.d("YkisLog", "[$className.RegularCardEditor]: Clicked")
  CardEditor(title, icon, content, onEditClick, modifier)
}

@Composable
private fun CardEditor(
  title: StringResource,
  icon: DrawableResource,
  content: String,
  onEditClick: () -> Unit,
  modifier: Modifier
) {
  Card(
    modifier = modifier
      .widthIn(0.dp, 480.dp)
      .padding(16.dp),
    elevation = CardDefaults.cardElevation(20.dp),
    shape = RoundedCornerShape(20.dp),
    onClick = onEditClick,
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(48.dp)
    ),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(16.dp)
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = stringResource(title),
          style = MaterialTheme.typography.bodyLarge
        )
      }

      if (content.isNotBlank()) {
        Text(
          text = content,
          modifier = Modifier.padding(16.dp, 0.dp),
          style = MaterialTheme.typography.bodyMedium
        )
      }

      Image(
        painter = painterResource(icon),
        contentDescription = "Icon",
        modifier = Modifier.size(24.dp)
      )
    }
  }
}

@Composable
fun CardEditorInfo(
  title: StringResource,
  icon: DrawableResource,
  content: String,
  onEditClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Log.d("YkisLog", "[$className.CardEditorInfo]: Clicked")
  Card(
    modifier = modifier
      .widthIn(0.dp, 480.dp)
      .padding(16.dp),
    elevation = CardDefaults.cardElevation(20.dp),
    shape = RoundedCornerShape(5.dp),
    onClick = onEditClick
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = stringResource(title),
          style = MaterialTheme.typography.bodyLarge
        )
      }

      if (content.isNotBlank()) {
        Text(
          text = content,
          modifier = Modifier.padding(16.dp, 0.dp),
          style = MaterialTheme.typography.bodyMedium
        )
      }

      Icon(
        painter = painterResource(icon),
        contentDescription = "Icon",
        modifier = Modifier.size(24.dp)
      )
    }
  }
}
