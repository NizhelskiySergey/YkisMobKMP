package com.ykis.ykismobkmp.ui.screens.announcement

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.ykis.ykismobkmp.core.utils.formatDateFull
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ykismobkmp.composeapp.generated.resources.*

sealed class GroupedAnnouncement {
    data class DateHeader(val date: String) : GroupedAnnouncement()
    data class Item(val announcement: AnnouncementEntity) : GroupedAnnouncement()
}

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

        // ІСПРАВЛЕНО: Скидаємо бейджі кожного разу, коли список оновлюється, 
        // поки користувач знаходиться на цьому екрані.
        LaunchedEffect(screenState.announcements) {
            if (screenState.announcements.isNotEmpty()) {
                println("[AnnouncementListScreen]: Список оновлено, скидання бейджей.")
                announcementModel.markAsRead()
            }
        }

        LaunchedEffect(baseUIState.osbbId) {
            announcementModel.observeAnnouncements(baseUIState.osbbId)
        }

        var announcementToDelete by remember { mutableStateOf<AnnouncementEntity?>(null) }

        if (announcementToDelete != null) {
            AlertDialog(
                onDismissRequest = { announcementToDelete = null },
                title = { Text("Видалити оголошення?") },
                text = { Text("Ця дія безповоротна. Оголошення зникне у всіх мешканців.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            announcementModel.deleteAnnouncement(announcementToDelete!!.id)
                            announcementToDelete = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Видалити")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { announcementToDelete = null }) {
                        Text("Скасувати")
                    }
                }
            )
        }

        Scaffold(
            topBar = {
                Column {
                    DefaultAppBar(
                        title = stringResource(Res.string.announcements),
                        onDrawerClick = onDrawerClicked,
                        canNavigateBack = false
                    )
                    FilterChipsRow(
                        selectedRole = screenState.announcementFilterRole,
                        onRoleSelected = { announcementModel.setFilter(it) }
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                }
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
                        val filteredAnnouncements = remember(screenState.announcements, screenState.announcementFilterRole) {
                            val filter = screenState.announcementFilterRole
                            if (filter == null) screenState.announcements
                            else screenState.announcements.filter { it.authorRole.contains(filter.getSerialName(), true) }
                        }

                        val groupedItems = remember(filteredAnnouncements) {
                            filteredAnnouncements.groupBy { formatDateFull(it.timestamp) }
                                .flatMap { (date, items) ->
                                    listOf(GroupedAnnouncement.DateHeader(date)) + items.map { GroupedAnnouncement.Item(it) }
                                }
                        }

                        if (groupedItems.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(stringResource(Res.string.no_announcements), style = MaterialTheme.typography.bodyLarge)
                            }
                        } else {
                            AnnouncementList(
                                groupedItems = groupedItems,
                                isAdmin = baseUIState.userRole != UserRole.StandardUser,
                                currentUid = baseUIState.uid ?: "",
                                onDeleteClick = { announcement -> announcementToDelete = announcement }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterChipsRow(
    selectedRole: UserRole?,
    onRoleSelected: (UserRole?) -> Unit
) {
    val filters = listOf(
        null to stringResource(Res.string.all),
        UserRole.VodokanalUser to stringResource(Res.string.vodokanal),
        UserRole.YtkeUser to stringResource(Res.string.ytke),
        UserRole.TboUser to stringResource(Res.string.yzhtrans),
        UserRole.OsbbUser to stringResource(Res.string.osbb)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.FilterList, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filters.forEach { (role, label) ->
                FilterChip(
                    selected = selectedRole == role,
                    onClick = { onRoleSelected(role) },
                    label = { Text(label, fontSize = 12.sp) },
                    shape = RoundedCornerShape(16.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
    }
}

@Composable
fun AnnouncementList(
    groupedItems: List<GroupedAnnouncement>,
    isAdmin: Boolean,
    currentUid: String,
    onDeleteClick: (AnnouncementEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        groupedItems.forEach { grouped ->
            when (grouped) {
                is GroupedAnnouncement.DateHeader -> {
                    item(key = "header_${grouped.date}") {
                        DateHeaderChip(date = grouped.date)
                    }
                }
                is GroupedAnnouncement.Item -> {
                    item(key = grouped.announcement.id) {
                        AnnouncementItem(
                            item = grouped.announcement,
                            isAdmin = isAdmin,
                            currentUid = currentUid,
                            onDeleteClick = { onDeleteClick(it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DateHeaderChip(date: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = date,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun AnnouncementItem(
    item: AnnouncementEntity,
    isAdmin: Boolean,
    currentUid: String,
    onDeleteClick: (AnnouncementEntity) -> Unit
) {
    val isGlobal = item.osbbId == 0L
    val uriHandler = LocalUriHandler.current
    
    BaseCard(
        modifier = Modifier.fillMaxWidth(),
        label = when {
            item.authorRole.contains("Vodokanal", true) -> stringResource(Res.string.vodokanal)
            item.authorRole.contains("Ytke", true) -> stringResource(Res.string.ytke_short)
            item.authorRole.contains("Tbo", true) -> stringResource(Res.string.yzhtrans)
            !item.authorName.isNullOrBlank() -> item.authorName
            isGlobal -> stringResource(Res.string.city_announcement)
            else -> stringResource(Res.string.osbb)
        }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            val osbbLabel = stringResource(Res.string.osbb)
            val author = item.authorName.takeIf { it.isNotBlank() } ?: "Адміністрація"

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Векторна іконка замість емодзі (надійно для Web)
                Icon(
                    imageVector = if (item.osbbId == 0L) Icons.Default.Public else Icons.Default.Home,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = when {
                        item.osbbId == 0L -> stringResource(Res.string.all_citizens).replace("📍 ", "")
                        item.authorRole.contains("Osbb", ignoreCase = true) -> stringResource(Res.string.residents_of, osbbLabel).replace("🏠 ", "")
                        else -> stringResource(Res.string.residents_of, author).replace("🏠 ", "")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(6.dp))

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

                // Кнопка видалення доступна всім адмінам
                if (isAdmin) {
                    IconButton(
                        onClick = { onDeleteClick(item) },
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
                val isWeb = com.ykis.ykismobkmp.getPlatform().name.contains("Web", true)
                val ratio = if (item.imageWidth > 0 && item.imageHeight > 0) {
                    item.imageWidth.toFloat() / item.imageHeight.toFloat()
                } else null

                Spacer(modifier = Modifier.height(12.dp))
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (ratio != null) Modifier.aspectRatio(ratio) else Modifier)
                        .heightIn(max = 400.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = if (ratio != null && isWeb) ContentScale.FillBounds else ContentScale.Fit
                )

                // ДОДАНО: Посилання на завантаження фото (як у чаті)
                val photoName = if (!item.fileName.isNullOrBlank()) item.fileName else "image.jpg"
                Row(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clickable { uriHandler.openUri(item.imageUrl) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = photoName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
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
        }
    }
}
