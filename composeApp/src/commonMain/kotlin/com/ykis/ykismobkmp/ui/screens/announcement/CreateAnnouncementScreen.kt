package com.ykis.ykismobkmp.ui.screens.announcement

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ykis.ykismobkmp.core.utils.rememberFilePicker
import com.ykis.ykismobkmp.ui.components.DefaultAppBar
import com.ykis.ykismobkmp.ui.navigation.CameraScreenDest
import com.ykis.ykismobkmp.ui.navigation.CameraTarget
import com.ykis.ykismobkmp.ui.screens.appartment.ApartmentScreenModel
import org.koin.compose.koinInject

class CreateAnnouncementScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val announcementModel = koinInject<AnnouncementScreenModel>()
        val apartmentModel = koinInject<ApartmentScreenModel>() // ИСПРАВЛЕНО: Нужен для получения профиля пользователя
        
        val baseUIState by apartmentModel.uiState.collectAsState()
        val screenState by announcementModel.uiState.collectAsState()

        val filePicker = rememberFilePicker()

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
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = screenState.announcementDraftTitle,
                    onValueChange = { announcementModel.onDraftTitleChanged(it) },
                    label = { Text("Заголовок") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = screenState.announcementDraftMessage,
                    onValueChange = { announcementModel.onDraftMessageChanged(it) },
                    label = { Text("Текст оголошення") },
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                )

                // БЛОК ВЛОЖЕНИЙ
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { navigator.push(CameraScreenDest(target = CameraTarget.ANNOUNCEMENT)) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CameraAlt, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Камера")
                    }

                    Button(
                        onClick = {
                            filePicker.pickFile { path ->
                                announcementModel.setAnnouncementFilePath(path)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AttachFile, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Файл")
                    }
                }

                // ПРЕДПРОСМОТР ВЛОЖЕНИЙ
                if (!screenState.announcementImagePath.isNullOrBlank()) {
                    AttachmentPreview(
                        name = "Фотографія з камери",
                        onClear = { announcementModel.setAnnouncementImagePath(null) }
                    )
                }

                if (!screenState.announcementFilePath.isNullOrBlank()) {
                    AttachmentPreview(
                        name = screenState.announcementFilePath?.substringAfterLast("/") ?: "Файл",
                        onClear = { announcementModel.setAnnouncementFilePath(null) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (screenState.isAnnouncementUploading) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text("Завантаження медіафайлів...", style = MaterialTheme.typography.labelSmall)
                    }
                } else {
                    Button(
                        onClick = {
                            // ИСПРАВЛЕНО: Передаем baseUIState (с профилем) и screenState (с черновиком)
                            announcementModel.publishAnnouncement(
                                baseState = baseUIState,
                                screenState = screenState,
                                onSuccess = {
                                    navigator.pop()
                                }
                            )
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

    @Composable
    fun AttachmentPreview(name: String, onClear: () -> Unit) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AttachFile, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                IconButton(onClick = onClear, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, null)
                }
            }
        }
    }
}
