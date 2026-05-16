package com.ykis.ykismobkmp.ui

/**
 * Глобальное состояние приложения.
 * Содержит данные профиля, текущей квартиры и метаданные для навигации.
 */

import com.ykis.ykismobkmp.domain.entity.ApartmentEntity
import com.ykis.ykismobkmp.domain.entity.RaionEntity
import com.ykis.ykismobkmp.domain.services.UserRole
import com.ykis.ykismobkmp.ui.navigation.ContentDetail
import com.ykis.ykismobkmp.ui.screens.appartment.ListMode
import kotlinx.serialization.Serializable

/**
 * [BaseUIState] — Единый источник истины (Single Source of Truth) для UI слоя ЮКИС.
 * Полностью типизирован под сквозной Long стандарт для бесшовной стыковки с СУБД SQLDelight.
 */
@Serializable
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
  // ИСПРАВЛЕНО: addressId изменен на тип Long под стандарты первичных ключей СУБД
  val addressId: Long = 0L,
  val address: String = "",
  val kod: String = "",
  val addressNumber: String? = null,
  val isApartmentsLoaded: Boolean = false,

  // --- ДАННЫЕ АДМИНИСТРАТОРА (ОСББ / Горслужбы) ---
  // ИСПРАВЛЕНО: Все системные ИД предприятий и домов переведены на тип Long под архитектуру Use Cases
  val osbbId: Long = 0L,            // ID предприятия (9997L, 9998L, 9999L и т.д.)
  val osmdId: Long = 0L,            // Совместимость со старым API биллинга Южного
  val houseId: Long = 0L,
  val osbb: String = "",
  val raions: List<RaionEntity> = emptyList(),
  // ИСПРАВЛЕНО: Идентификатор региона Одесской обл. изменен с Int? на Long под .sq выборки
  val selectedRaionId: Long = 0L,
  val houses: List<ApartmentEntity> = emptyList(),
  // ИСПРАВЛЕНО: Идентификатор выбранного дома изменен с Int? на Long
  val selectedHouseId: Long = 0L,
  val searchMode: Boolean = false,
  val listMode: ListMode = ListMode.APARTMENTS,

  // --- СОСТОЯНИЕ ИНТЕРФЕЙСА (Voyager / Adaptive UI) ---
  val selectedContentDetail: ContentDetail = ContentDetail.BTI,
  val isDetailOnlyOpen: Boolean = false,
  val showDetail: Boolean = false,
  val isForwarding: Boolean = false,
  val isOpponentTyping: Boolean = false,

  // --- СТАТУСЫ ЗАГРУЗКИ (Системные лоадеры) ---
  val isLoading: Boolean = false,       // Фоновый процесс кэширования
  val mainLoading: Boolean = true,      // Холодный старт экрана чатов
  val isGlobalLoading: Boolean = false, // Блокирующая загрузка (Внесение лицевого счета)
  val apartmentLoading: Boolean = true, // Загрузка БТИ данных из локальной базы

  // --- ОШИБКИ ---
  val error: String? = null
)

