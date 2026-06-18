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
 * [AnnouncementScreenModel] — Модель управління оголошеннями для жителів та адміністраторів.
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
     * [observeAnnouncements] — Запуск моніторингу стрічки новин.
     */
    fun observeAnnouncements(osbbId: Long) {
        observeJob?.cancel()
        _uiState.update { it.copy(isAnnouncementsLoading = true) }
        
        observeJob = screenModelScope.launch {
            try {
                chatRepo.observeAnnouncements(osbbId).collect { list ->
                    _uiState.update { state ->
                        // Використовуємо актуальний lastCheck зі стейту
                        val currentLastCheck = state.lastAnnouncementsCheck
                        val unreadCount = list.count { it.timestamp > currentLastCheck }
                        
                        println("[AnnouncementScreenModel]: Оновлення списку. Непрочитано: $unreadCount | LastCheck: $currentLastCheck")
                        
                        state.copy(
                            announcements = list,
                            unreadAnnouncementsCount = unreadCount,
                            isAnnouncementsLoading = false
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
     * [markAsRead] — Скидання бейджа непрочитаних оголошень.
     * ІСПРАВЛЕНО: Тепер беремо таймстемп останнього наявного оголошення, щоб гарантовано очистити список.
     */
    fun markAsRead() {
        val latestTimestamp = _uiState.value.announcements.firstOrNull()?.timestamp ?: currentTimeMillis()
        
        appSettings.putLong(LAST_CHECK_KEY, latestTimestamp)
        _uiState.update { 
            it.copy(
                lastAnnouncementsCheck = latestTimestamp,
                unreadAnnouncementsCount = 0
            ) 
        }
        println("[AnnouncementScreenModel.markAsRead]: Бейджі оголошень обнулені до $latestTimestamp.")
    }

    /**
     * [publishAnnouncement] — Публікація нового оголошення.
     */
    fun publishAnnouncement(
        baseState: BaseUIState,   // Головний стейт з UID та Роллю
        screenState: BaseUIState, // Стейт цієї моделі з чернеткою та шляхами до фото
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
            
            try {
                val role = baseState.userRole
                val targetOsbbId = when (role) {
                    UserRole.VodokanalUser, UserRole.YtkeUser, UserRole.TboUser -> 0L
                    UserRole.OsbbUser -> baseState.osbbId
                    else -> 0L
                }

                var imageUrl: String? = null
                var fileUrl: String? = null
                var fileName: String? = null

                // 1. ЗАВАНТАЖЕННЯ ЗОБРАЖЕННЯ
                val imgPath = screenState.announcementImagePath
                if (!imgPath.isNullOrBlank()) {
                    try {
                        val fileData = chatRepo.compressImage(imgPath)
                        val storagePath = "chat_images/announcements/${targetOsbbId}_${currentTimeMillis()}.jpg"
                        imageUrl = chatRepo.uploadFile(fileData, storagePath)
                    } catch (e: Exception) {
                        println("[AnnouncementScreenModel]: Помилка завантаження фото: ${e.message}")
                        SnackbarManager.showMessage("Помилка завантаження фото: ${e.message}")
                    }
                }

                // 2. ЗАВАНТАЖЕННЯ ФАЙЛУ
                val filePath = screenState.announcementFilePath
                if (!filePath.isNullOrBlank()) {
                    try {
                        val fileData = chatRepo.readFileAsBytes(filePath)
                        val ext = filePath.substringAfterLast(".", "file")
                        
                        // ФІКС: Використовуємо реальне ім'я файлу зі стейту
                        fileName = screenState.announcementFileName ?: filePath.substringAfterLast("/")

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
                    authorRole = role.getSerialName(),
                    osbbId = targetOsbbId,
                    timestamp = currentTimeMillis(),
                    imageUrl = imageUrl,
                    fileUrl = fileUrl,
                    fileName = fileName
                )

                val result = chatRepo.publishAnnouncement(announcement)
                
                if (result.isSuccess) {
                    _uiState.update { 
                        it.copy(
                            isAnnouncementUploading = false,
                            announcementImagePath = null,
                            announcementFilePath = null,
                            announcementFileName = null,
                            announcementDraftTitle = "",
                            announcementDraftMessage = ""
                        ) 
                    }
                    SnackbarManager.showMessage("Оголошення опубліковано")
                    onSuccess()
                } else {
                    _uiState.update { it.copy(isAnnouncementUploading = false) }
                    SnackbarManager.showMessage("Помилка публікації")
                }
            } catch (e: Exception) {
                println("[AnnouncementScreenModel]: Критична помилка публікації: ${e.message}")
                _uiState.update { it.copy(isAnnouncementUploading = false) }
                SnackbarManager.showMessage("Помилка: ${e.message}")
                logService.logNonFatalCrash(e)
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

    fun setAnnouncementFilePath(path: String?, fileName: String? = null) {
        _uiState.update { it.copy(announcementFilePath = path, announcementFileName = fileName) }
    }

    /**
     * [deleteAnnouncement] — Видалення новини (доступно тільки адмінам).
     */
    fun deleteAnnouncement(announcementId: String) {
        screenModelScope.launch {
            try {
                val result = chatRepo.deleteAnnouncement(announcementId)
                if (result.isSuccess) {
                    SnackbarManager.showMessage("Оголошення видалено")
                }
            } catch (e: Exception) {
                SnackbarManager.showMessage("Помилка видалення")
            }
        }
    }

    fun setFilter(role: UserRole?) {
        _uiState.update { it.copy(announcementFilterRole = role) }
    }

    /**
     * [stopAllListeners] — Миттєва зупинка всіх фонових процесів.
     */
    fun stopAllListeners() {
        println("[AnnouncementScreenModel]: Зупинка моніторингу оголошень.")
        observeJob?.cancel()
    }
}
