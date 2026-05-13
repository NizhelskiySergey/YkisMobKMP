package com.ykis.ykismobkmp.ui

/**
 * Глобальное состояние приложения.
 * Содержит данные профиля, текущей квартиры и метаданные для навигации.
 */

import com.ykis.mob.ui.navigation.ContentDetail
import com.ykis.ykismobkmp.domain.entity.ApartmentEntity
import com.ykis.ykismobkmp.domain.entity.RaionEntity
import com.ykis.ykismobkmp.ui.screens.appartment.ListMode

/**
 * [BaseUIState] — единый источник истины для UI слоя.
 * Используется в ApartmentViewModel для управления состоянием на всех платформах.
 */
data class BaseUIState(
  // --- БЛОК АВТОРИЗАЦИИ (Firebase KMP) ---
  val uid: String? = null,
  val displayName: String? = null,
  val email: String? = null,
  val photoUrl: String? = null,
  val userRole: UserRole = UserRole.StandardUser,

  // --- ДАННЫЕ ЖИЛЬЦА (SQLDelight / Ktor) ---
  val apartment: ApartmentEntity = ApartmentEntity(),
  val apartments: List<ApartmentEntity> = emptyList(),
  val addressId: Int = 0,
  val address: String = "",
  val kod: String = "",
  val addressNumber: String? = null,
  val isApartmentsLoaded: Boolean = false,

  // --- ДАННЫЕ АДМИНИСТРАТОРА (ОСББ / Горслужбы) ---
  val osbbId: Int = 0,            // ID предприятия (9997, 9998, 9999 и т.д.)
  val osmdId: Int = 0,            // Совместимость со старым API
  val houseId: Int = 0,
  val osbb: String = "",
  val raions: List<RaionEntity> = emptyList(),
  val selectedRegionId: Int? = null,
  val houses: List<ApartmentEntity> = emptyList(),
  val selectedHouseId: Int? = null,
  val searchMode: Boolean = false,
  val listMode: ListMode = ListMode.APARTMENTS,

  // --- СОСТОЯНИЕ ИНТЕРФЕЙСА (Voyager / Adaptive UI) ---
  val selectedContentDetail: ContentDetail = ContentDetail.BTI,
  val isDetailOnlyOpen: Boolean = false,
  val showDetail: Boolean = false,
  val isForwarding: Boolean = false,
  val isOpponentTyping: Boolean = false,
  // --- СТАТУСЫ ЗАГРУЗКИ (Системные лоадеры) ---
  val isLoading: Boolean = false,       // Фоновый процесс
  val mainLoading: Boolean = true,      // Холодный старт
  val isGlobalLoading: Boolean = false, // Блокирующая загрузка (Add Apartment)
  val apartmentLoading: Boolean = true, // Загрузка БТИ данных

  // --- ОШИБКИ ---
  val error: String? = null
)

/**
 * Роли пользователей в системе Ykis
 */
enum class UserRole {
  StandardUser,    // Житель
  OsbbUser,        // Админ ОСББ
  VodokanalUser,   // Водоканал (9998)
  YtkeUser,        // Теплосеть (9997)
  TboUser,         // Вывоз мусора (9999)
  Unknown          // Начальное состояние
}

