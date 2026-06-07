package com.ykis.ykismobkmp.ui.screens.announcement

import cafe.adriel.voyager.core.model.screenModelScope
import com.russhwolf.settings.Settings
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.core.utils.SnackbarManager
import com.ykis.ykismobkmp.core.utils.currentTimeMillis
import com.ykis.ykismobkmp.domain.entity.AnnouncementEntity
import com.ykis.ykismobkmp.domain.repository.chat.ChatRepository
import com.ykis.ykismobkmp.domain.services.LogService
import com.ykis.ykismobkmp.domain.services.UserRole
import com.ykis.ykismobkmp.ui.BaseScreenModel
import com.ykis.ykismobkmp.ui.BaseUIState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * [AnnouncementScreenModel] — Модель управления объявлениями для жителей и администраторов.
 */
class AnnouncementScreenModel(
    private val chatRepo: ChatRepository,
    private val appSettings: Settings,
    logService: LogService
) : BaseScreenModel(logService) {
    
    private val className = "AnnouncementScreenModel"
    private var observeJob: Job? = null
    private val LAST_CHECK_KEY = "last_announcements_check_timestamp"

    init {
        val lastCheck = appSettings.getLong(LAST_CHECK_KEY, 0L)
        _uiState.update { it.copy(lastAnnouncementsCheck = lastCheck) }
    }

    /**
     * [observeAnnouncements] — Запуск мониторинга ленты новостей.
     */
    fun observeAnnouncements(osbbId: Long) {
        observeJob?.cancel()
        _uiState.update { it.copy(isAnnouncementsLoading = true) }
        
        observeJob = screenModelScope.launch {
            try {
                // Достаем свежий timestamp последнего чека из настроек
                val lastCheck = appSettings.getLong(LAST_CHECK_KEY, 0L)
                
                chatRepo.observeAnnouncements(osbbId).collect { list ->
                    _uiState.update { state ->
                        val unreadCount = list.count { it.timestamp > lastCheck }
                        println("[AnnouncementScreenModel]: Оновлення списку. Непрочитано: $unreadCount")
                        state.copy(
                            announcements = list,
                            unreadAnnouncementsCount = unreadCount,
                            isAnnouncementsLoading = false,
                            lastAnnouncementsCheck = lastCheck
                        ) 
                    }
                }
            } catch (e: Exception) {
                println("[AnnouncementScreenModel]: Критична помилка підписки: ${e.message}")
                _uiState.update { it.copy(isAnnouncementsLoading = false) }
            }
        }
    }

    /**
     * [markAsRead] — Сброс бейджа непрочитанных объявлений.
     */
    fun markAsRead() {
        val now = currentTimeMillis()
        appSettings.putLong(LAST_CHECK_KEY, now)
        _uiState.update { 
            it.copy(
                lastAnnouncementsCheck = now,
                unreadAnnouncementsCount = 0
            ) 
        }
        println("[AnnouncementScreenModel.markAsRead]: Бейджі оголошень обнулені.")
    }

    /**
     * [publishAnnouncement] — Публикация нового объявления.
     * ИСПРАВЛЕНО: Теперь принимает два стейта для 100% точности данных.
     */
    fun publishAnnouncement(
        baseState: BaseUIState,   // Главный стейт с UID и Ролью
        screenState: BaseUIState, // Стейт этой модели с черновиком и путями к фото
        onSuccess: () -> Unit
    ) {
        val title = screenState.announcementDraftTitle
        val message = screenState.announcementDraftMessage

        if (title.isBlank() || message.isBlank()) {
            SnackbarManager.showMessage("Заголовок та текст не можуть бути порожніми")
            return
        }

        screenModelScope.launch {
            _uiState.update { it.copy(isAnnouncementUploading = true) }
            
            val role = baseState.userRole
            val targetOsbbId = when (role) {
                UserRole.VodokanalUser, UserRole.YtkeUser, UserRole.TboUser -> 0L
                UserRole.OsbbUser -> baseState.osbbId
                else -> 0L
            }

            var imageUrl: String? = null
            var fileUrl: String? = null
            var fileName: String? = null

            // 1. ЗАГРУЗКА ИЗОБРАЖЕНИЯ (если есть)
            val imgPath = screenState.announcementImagePath
            if (!imgPath.isNullOrBlank()) {
                try {
                    val fileData = chatRepo.compressImage(imgPath)
                    // ИСПРАВЛЕНО: Используем путь chat_images, который уже разрешен в Storage Rules
                    val storagePath = "chat_images/announcements/${targetOsbbId}_${currentTimeMillis()}.jpg"
                    println("[AnnouncementScreenModel]: Загрузка фото в разрешенную ветку: $storagePath")
                    imageUrl = chatRepo.uploadFile(fileData, storagePath)
                    println("[AnnouncementScreenModel]: ФОТО УСПЕШНО ЗАГРУЖЕНО: $imageUrl")
                } catch (e: Exception) {
                    println("[AnnouncementScreenModel]: Критическая ошибка Storage: ${e.message}")
                    SnackbarManager.showMessage("Помилка завантаження фото: ${e.message}")
                }
            }

            // 2. ЗАГРУЗКА ФАЙЛА (если есть)
            val filePath = screenState.announcementFilePath
            if (!filePath.isNullOrBlank()) {
                try {
                    val fileData = chatRepo.readFileAsBytes(filePath)
                    val ext = filePath.substringAfterLast(".", "file")
                    val name = filePath.substringAfterLast("/")
                    fileName = name
                    // ИСПРАВЛЕНО: Аналогично для файлов
                    val storagePath = "chat_images/announcements/docs/${targetOsbbId}_${currentTimeMillis()}.$ext"
                    fileUrl = chatRepo.uploadFile(fileData, storagePath)
                } catch (e: Exception) {
                    println("[AnnouncementScreenModel]: Помилка завантаження файлу: ${e.message}")
                }
            }

            val announcement = AnnouncementEntity(
                title = title,
                message = message,
                authorUid = baseState.uid ?: "",
                authorName = when(role) {
                   UserRole.VodokanalUser -> "КП \"ЮЖВОДОКАНАЛ\""
                   UserRole.YtkeUser      -> "КП тм \"ЮТКЕ\""
                   UserRole.TboUser       -> "КП \"СПЕЦТРАНС\""
                   UserRole.OsbbUser      -> baseState.osbb.takeIf { it.isNotBlank() && it != "0" } ?: "ОСББ"
                   else -> "Адміністратор"
                },
                authorRole = role,
                osbbId = targetOsbbId,
                timestamp = currentTimeMillis(),
                imageUrl = imageUrl,
                fileUrl = fileUrl,
                fileName = fileName
            )

            val result = chatRepo.publishAnnouncement(announcement)
            
            _uiState.update { 
                it.copy(
                    isAnnouncementUploading = false,
                    announcementImagePath = null,
                    announcementFilePath = null,
                    announcementDraftTitle = "",
                    announcementDraftMessage = ""
                ) 
            }

            if (result.isSuccess) {
                SnackbarManager.showMessage("Оголошення опубліковано")
                onSuccess()
            } else {
                SnackbarManager.showMessage("Помилка публікації")
            }
        }
    }

    fun onDraftTitleChanged(title: String) {
        _uiState.update { it.copy(announcementDraftTitle = title) }
    }

    fun onDraftMessageChanged(message: String) {
        _uiState.update { it.copy(announcementDraftMessage = message) }
    }

    fun setAnnouncementImagePath(path: String?) {
        _uiState.update { it.copy(announcementImagePath = path) }
    }

    fun setAnnouncementFilePath(path: String?) {
        _uiState.update { it.copy(announcementFilePath = path) }
    }

    /**
     * [deleteAnnouncement] — Удаление новости (доступно только админам).
     */
    fun deleteAnnouncement(announcementId: String) {
        screenModelScope.launch {
            try {
                // Прямое удаление документа из Firestore
                val result = chatRepo.deleteAnnouncement(announcementId)
                if (result.isSuccess) {
                    SnackbarManager.showMessage("Оголошення видалено")
                }
            } catch (e: Exception) {
                SnackbarManager.showMessage("Помилка видалення")
            }
        }
    }

    /**
     * [stopAllListeners] — Мгновенная остановка всех фоновых процессов.
     */
    fun stopAllListeners() {
        println("[AnnouncementScreenModel]: Зупинка моніторингу оголошень.")
        observeJob?.cancel()
    }
}
