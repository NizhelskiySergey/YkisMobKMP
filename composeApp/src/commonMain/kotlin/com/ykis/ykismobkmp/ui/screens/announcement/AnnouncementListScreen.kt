package com.ykis.ykismobkmp.ui.screens.announcement

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.ykis.ykismobkmp.domain.entity.AnnouncementEntity
import com.ykis.ykismobkmp.domain.services.UserRole
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.components.BaseCard
import com.ykis.ykismobkmp.ui.components.DefaultAppBar
import com.ykis.ykismobkmp.ui.screens.appartment.ApartmentScreenModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ykismobkmp.composeapp.generated.resources.Res
import ykismobkmp.composeapp.generated.resources.info

class AnnouncementListScreen(
    private val onDrawerClicked: () -> Unit
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val announcementModel = koinInject<AnnouncementScreenModel>()
        val apartmentModel = koinInject<ApartmentScreenModel>()
        val baseUIState by apartmentModel.uiState.collectAsState()
        val screenState by announcementModel.uiState.collectAsState()

        LaunchedEffect(Unit) {
            println("[AnnouncementListScreen]: Вхід у розділ, скидання бейджей.")
            announcementModel.markAsRead()
        }

        LaunchedEffect(baseUIState.osbbId) {
            announcementModel.observeAnnouncements(baseUIState.osbbId)
        }

        Scaffold(
            topBar = {
                DefaultAppBar(
                    title = "Оголошення",
                    onDrawerClick = onDrawerClicked,
                    canNavigateBack = false
                )
            },
            floatingActionButton = {
                if (baseUIState.userRole != UserRole.StandardUser) {
                    FloatingActionButton(
                        onClick = { 
                            navigator.push(CreateAnnouncementScreen())
                        },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Додати оголошення")
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                Crossfade(targetState = screenState.isAnnouncementsLoading) { isLoading ->
                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        AnnouncementList(
                            announcements = screenState.announcements,
                            isAdmin = baseUIState.userRole != UserRole.StandardUser,
                            onDeleteClick = { id -> announcementModel.deleteAnnouncement(id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnnouncementList(
    announcements: List<AnnouncementEntity>,
    isAdmin: Boolean,
    onDeleteClick: (String) -> Unit
) {
    if (announcements.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Оголошень поки що немає", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(announcements, key = { it.id }) { item ->
                AnnouncementItem(
                    item = item,
                    isAdmin = isAdmin,
                    onDeleteClick = onDeleteClick
                )
            }
        }
    }
}

@Composable
fun AnnouncementItem(
    item: AnnouncementEntity,
    isAdmin: Boolean,
    onDeleteClick: (String) -> Unit
) {
    val isGlobal = item.osbbId == 0L
    val uriHandler = LocalUriHandler.current
    
    BaseCard(
        modifier = Modifier.fillMaxWidth(),
        label = when {
            item.authorRole == UserRole.VodokanalUser -> "КП \"ЮЖВОДОКАНАЛ\""
            item.authorRole == UserRole.YtkeUser -> "КП тм \"ЮТКЕ\""
            item.authorRole == UserRole.TboUser -> "КП \"СПЕЦТРАНС\""
            !item.authorName.isNullOrBlank() -> item.authorName
            isGlobal -> "Міське оголошення"
            else -> "ОСББ"
        }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isAdmin) {
                    IconButton(
                        onClick = { onDeleteClick(item.id) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Видалити",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = item.message,
                style = MaterialTheme.typography.bodyMedium
            )

            // ИЗОБРАЖЕНИЕ (если есть)
            if (!item.imageUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Fit // ИСПРАВЛЕНО: Чтобы фото было видно целиком
                )
            }

            // ФАЙЛ (если есть)
            if (!item.fileUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { uriHandler.openUri(item.fileUrl) },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AttachFile, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = item.fileName ?: "Документ",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = com.ykis.ykismobkmp.core.utils.formatDateFull(item.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}
