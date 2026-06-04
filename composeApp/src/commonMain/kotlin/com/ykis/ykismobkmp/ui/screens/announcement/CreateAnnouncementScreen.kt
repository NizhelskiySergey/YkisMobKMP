package com.ykis.ykismobkmp.ui.screens.announcement

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ykis.ykismobkmp.ui.components.DefaultAppBar
import com.ykis.ykismobkmp.ui.screens.appartment.ApartmentScreenModel
import org.koin.compose.koinInject

class CreateAnnouncementScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val announcementModel = koinInject<AnnouncementScreenModel>()
        val apartmentModel = koinInject<ApartmentScreenModel>()
        val baseUIState by apartmentModel.uiState.collectAsState()

        var title by remember { mutableStateOf("") }
        var message by remember { mutableStateOf("") }

        Scaffold(
            topBar = {
                DefaultAppBar(
                    title = "Нове оголошення",
                    onBackClick = { navigator.pop() },
                    canNavigateBack = true
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Заголовок") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Текст оголошення") },
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                )

                Button(
                    onClick = {
                        announcementModel.publishAnnouncement(title, message, baseUIState)
                        navigator.pop()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Опублікувати")
                }
            }
        }
    }
}
