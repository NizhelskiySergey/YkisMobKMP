package com.ykis.ykismobkmp.ui.screens.appartment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ykis.ykismobkmp.ui.components.DefaultAppBar
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.add
import ykismobkmp.composeapp.generated.resources.add_appartment
import ykismobkmp.composeapp.generated.resources.secret_сode
import ykismobkmp.composeapp.generated.resources.tooltip_code


private const val className = "AddApartmentScreen"

class AddApartmentScreen(
  private val onDrawerClicked: () -> Unit = {},
  private val closeContentDetail: () -> Unit = {}
) : Screen {

  @Composable
  override fun Content() {
    val navigator = LocalNavigator.currentOrThrow
    val keyboard = LocalSoftwareKeyboardController.current
    val screenModel = koinInject<ApartmentScreenModel>()

    // Приведено к каноническому КМР-синтаксису collectAsState
    val secretCode by screenModel.secretCode.collectAsState()

    Scaffold(
      modifier = Modifier.fillMaxSize(),
    ) { padding ->
      AddApartmentScreenStateless(
        modifier = Modifier.padding(padding),
        isButtonEnabled = secretCode.trim().isNotEmpty(),
        onDrawerClicked = onDrawerClicked,
        secretCode = secretCode,
        onCodeChanged = { newValue ->
          println("[YkisLogKMP.$className.onCodeChanged]: Користувач вводить інфо-код ГІОЦ: $newValue")
          screenModel.onSecretCodeChanged(newValue)
        },
        onAddClick = {
          println("[YkisLogKMP.$className.onAddClick]: Клік по кнопці відправки коду БТІ: $secretCode")
          keyboard?.hide()
          screenModel.addApartment()
        }
      )
    }
  }
}

@Composable
fun AddApartmentScreenStateless(
  modifier: Modifier = Modifier,
  isButtonEnabled: Boolean,
  onDrawerClicked: () -> Unit,
  onAddClick: () -> Unit,
  secretCode: String,
  onCodeChanged: (String) -> Unit
) {
  Column(modifier = modifier.fillMaxSize()) {
    DefaultAppBar(
      onDrawerClick = onDrawerClicked,
      title = stringResource(Res.string.add_appartment),
      canNavigateBack = false
    )
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Spacer(modifier = Modifier.height(16.dp))

      Card(
        modifier = Modifier.widthIn(max = 500.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
      ) {
        Column(modifier = Modifier.padding(24.dp)) {
          Text(
            text = stringResource(Res.string.tooltip_code),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            lineHeight = 20.sp
          )
          Spacer(modifier = Modifier.height(20.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            OutlinedTextField(
              value = secretCode,
              onValueChange = onCodeChanged,
              modifier = Modifier.weight(1f),
              label = { Text(stringResource(Res.string.secret_сode)) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
              ),
              shape = RoundedCornerShape(12.dp)
            )
            Button(
              onClick = onAddClick,
              enabled = isButtonEnabled,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.height(54.dp)
            ) {
              Text(
                text = stringResource(Res.string.add),
                style = MaterialTheme.typography.labelLarge
              )
            }
          }
        }
      }
    }
  }
}

