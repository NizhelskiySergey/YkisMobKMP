package com.ykis.ykismobkmp.ui

/**
 * Глобальное состояние приложения.
 * Содержит данные профиля, текущей квартиры и метаданные для навигации.
 */

/**
 * [BaseUIState] — Единый источник истины (Single Source of Truth) для UI слоя ЮКИС.
 * Полностью типизирован под сквозной Long стандарт для бесшовной стыковки с СУБД SQLDelight.
 */

import com.ykis.ykismobkmp.domain.entity.ApartmentEntity
import com.ykis.ykismobkmp.domain.entity.RaionEntity
import com.ykis.ykismobkmp.domain.services.UserRole
import com.ykis.ykismobkmp.ui.navigation.ContentDetail
import com.ykis.ykismobkmp.ui.screens.appartment.ListMode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val className = "BaseUIState"

/**
 * [BaseUIState] — Монолитный кроссплатформенный стейт-снимок всего UI слоя приложения ЮКИС.
 */
@Serializable
data class BaseUIState(
  // --- 1. БЛОК АВТОРИЗАЦИИ (Firebase KMP) ---
  @SerialName("uid") val uid: String? = null,
  @SerialName("displayName") val displayName: String? = null,
  @SerialName("email") val email: String? = null,
  @SerialName("phone") val phone: String? = null,
  @SerialName("photoUrl") val photoUrl: String? = null,
  @SerialName("isTermsAccepted") val isTermsAccepted: Boolean? = false,
  @SerialName("userRole") val userRole: UserRole = UserRole.StandardUser,

  // --- 2. ДАННЫЕ ЖИЛЬЦА (SQLDelight / Ktor) ---
  @SerialName("apartment") val apartment: ApartmentEntity = ApartmentEntity(),
  @SerialName("apartments") val apartments: List<ApartmentEntity> = emptyList(),
  @SerialName("addressId") val addressId: Long = 0L, // Сквозной Long стандарт первичных ключей СУБД
  @SerialName("address") val address: String = "",
  @SerialName("kod") val kod: String = "",
  @SerialName("addressNumber") val addressNumber: String? = null,
  @SerialName("isApartmentsLoaded") val isApartmentsLoaded: Boolean = false,

  // --- 3. ДАННЫЕ АДМИНИСТРАТОРА (ОСББ / Горслужбы г. Южного) ---
  @SerialName("osbbId") val osbbId: Long = 0L, // ID предприятия (9997L - ТБО, 9998L - ЮТКЕ, 9999L - Водоканал)
  @SerialName("osmdId") val osmdId: Long = 0L, // Совместимость со старым API биллинга
  @SerialName("houseId") val houseId: Long = 0L,
  @SerialName("osbb") val osbb: String = "",
  @SerialName("raions") val raions: List<RaionEntity> = emptyList(),
  @SerialName("selectedRaionId") val selectedRaionId: Long = 0L,
  @SerialName("houses") val houses: List<ApartmentEntity> = emptyList(),
  @SerialName("selectedHouseId") val selectedHouseId: Long = 0L,
  @SerialName("searchMode") val searchMode: Boolean = false,
  @SerialName("listMode") val listMode: ListMode = ListMode.APARTMENTS,

  // --- 4. НОВОЕ: ИНТЕГРАЦИЯ ЧАТ-СИСТЕМЫ И ПУШ-ВЕЛОСИПЕДОВ (YkisMobPAM) ---
  @SerialName("currentChatUid") val currentChatUid: String? = null,              // Уникальный ID текущей ветки чата
  @SerialName("opponentUid") val opponentUid: String? = null,                  // UID собеседника (жильца или диспетчера)
  @SerialName("opponentName") val opponentName: String? = null,                // Имя собеседника на appBar чата
  @SerialName("opponentLogoUrl") val opponentLogoUrl: String? = null,            // Аватарка собеседника в MessageListItem
  @SerialName("activeRecipientFcmTokens") val activeRecipientFcmTokens: List<String> = emptyList(), // Токены для моментальной отправки пушей
  @SerialName("selectedImagePath") val selectedImagePath: String? = null,        // Путь к прикрепляемому файлу/фото из галереи KMP
  @SerialName("isOpponentTyping") val isOpponentTyping: Boolean = false,          // Индикатор "Собеседник набирает сообщение..."
  @SerialName("isForwarding") val isForwarding: Boolean = false,                  // Режим пересылки сообщения в другую службу
  @SerialName("assistantResponse") val assistantResponse: String? = null,        // Текст-подсказка, сгенерированный Gemini AI
  @SerialName("isLoadingAfterSending") val isLoadingAfterSending: Boolean = false, // Лоадер отправки тяжелого медиа-сообщения

  // --- 5. СОСТОЯНИЕ ИНТЕРФЕЙСА (Voyager / Adaptive UI) ---
  @SerialName("selectedContentDetail") val selectedContentDetail: ContentDetail = ContentDetail.BTI,
  @SerialName("isDetailOnlyOpen") val isDetailOnlyOpen: Boolean = false,
  @SerialName("showDetail") val showDetail: Boolean = false,

  // --- 6. СТАТУСЫ ЗАГРУЗКИ (Системные лоадеры) ---
  @SerialName("isLoading") val isLoading: Boolean = false,             // Фоновый процесс кэширования SQLDelight
  @SerialName("mainLoading") val mainLoading: Boolean = true,          // Холодный старт графа навигации
  @SerialName("isGlobalLoading") val isGlobalLoading: Boolean = false, // Блокирующий лоадер (Внесение Л/С)
  @SerialName("apartmentLoading") val apartmentLoading: Boolean = true, // Загрузка БТИ ведомостей

  // --- 7. ОШИБКИ ---
  @SerialName("error") val error: String? = null
) {
  init {
    // Логирование инициализации стейта согласно правилу [Класс.Метод]
    println("[$className.init]: Снимок BaseUIState обновлен. Активный о/р: $addressId, Роль: $userRole")
  }
}


