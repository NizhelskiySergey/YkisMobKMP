package com.ykis.ykismobkmp.ui.screens.announcement

import cafe.adriel.voyager.core.model.screenModelScope
import com.ykis.ykismobkmp.core.utils.Resource
import com.ykis.ykismobkmp.core.utils.SnackbarManager
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
    logService: LogService
) : BaseScreenModel(logService) {
    
    private val className = "AnnouncementScreenModel"
    private var observeJob: Job? = null

    /**
     * [observeAnnouncements] — Запуск мониторинга ленты новостей.
     */
    fun observeAnnouncements(osbbId: Long) {
        observeJob?.cancel()
        _uiState.update { it.copy(isAnnouncementsLoading = true) }
        
        observeJob = screenModelScope.launch {
            try {
                chatRepo.observeAnnouncements(osbbId).collect { list ->
                    _uiState.update { 
                        it.copy(
                            announcements = list,
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
     * [publishAnnouncement] — Публикация нового объявления.
     * ИСПРАВЛЕНО: Четкое разделение области видимости (Глобально/ОСББ).
     */
    fun publishAnnouncement(title: String, message: String, baseState: BaseUIState) {
        if (title.isBlank() || message.isBlank()) {
            SnackbarManager.showMessage("Заголовок та текст не можуть бути порожніми")
            return
        }

        screenModelScope.launch {
            val role = baseState.userRole
            
            // ОПРЕДЕЛЯЕМ ЦЕЛЕВУЮ АУДИТОРИЮ:
            // Если пишет городская служба — видят ВСЕ (0). Если ОСББ — только свои.
            val targetOsbbId = when (role) {
                UserRole.VodokanalUser, UserRole.YtkeUser, UserRole.TboUser -> 0L
                UserRole.OsbbUser -> baseState.osbbId ?: 0L
                else -> 0L
            }

            val announcement = AnnouncementEntity(
                title = title,
                message = message,
                authorUid = baseState.uid ?: "",
                authorName = when(role) {
                   UserRole.VodokanalUser -> "КП \"ЮЖВОДОКАНАЛ\""
                   UserRole.YtkeUser      -> "КП тм \"ЮТКЕ\""
                   UserRole.TboUser       -> "КП \"СПЕЦТРАНС\""
                   else -> baseState.displayName ?: "Адміністратор"
                },
                authorRole = role,
                osbbId = targetOsbbId,
                timestamp = com.ykis.ykismobkmp.core.utils.currentTimeMillis()
            )

            val result = chatRepo.publishAnnouncement(announcement)
            if (result.isSuccess) {
                SnackbarManager.showMessage("Оголошення опубліковано")
            } else {
                SnackbarManager.showMessage("Помилка публікації")
            }
        }
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
}
