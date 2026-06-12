package com.ykis.ykismobkmp.ui

import com.ykis.ykismobkmp.domain.entity.AnnouncementEntity
import com.ykis.ykismobkmp.domain.entity.ApartmentEntity
import com.ykis.ykismobkmp.domain.entity.FamilyEntity
import com.ykis.ykismobkmp.domain.entity.HeatMeterEntity
import com.ykis.ykismobkmp.domain.entity.HeatReadingEntity
import com.ykis.ykismobkmp.domain.entity.RaionEntity
import com.ykis.ykismobkmp.domain.entity.ServiceEntity
import com.ykis.ykismobkmp.domain.entity.WaterMeterEntity
import com.ykis.ykismobkmp.domain.entity.WaterReadingEntity
import com.ykis.ykismobkmp.domain.services.UserRole
import com.ykis.ykismobkmp.ui.navigation.ContentDetail
import com.ykis.ykismobkmp.ui.screens.appartment.ListMode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val className = "BaseUIState"

/**
 * [BaseUIState] — Кросплатформенний стейт-знімок всього UI шару додатка ЮКІС м. Южне.
 * ИСПРАВЛЕНО НАМЕРТВО: Интегрированы детальные анкетные поля БТИ (nanim, площади, комнаты, жильцы),
 * что полностью ликвидирует пустые заглушки первой вкладки при холодном старте и синхронизации СУБД!
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
  @SerialName("familyList") val familyList: List<FamilyEntity> = emptyList(),

  // --- ИСПРАВЛЕНО НАМЕРТВО: ДОБАВЛЕНЫ ДЕТАЛЬНЫЕ АНКЕТНЫЕ ПОЛЯ БТИ ГИОЦ ---
  @SerialName("nanim") val nanim: String? = null,           // ФИО владельца/ответственного нанимателя
  @SerialName("fio") val fio: String = "",                  // НОВОЕ ПОЛЕ: Фамилия из профиля Firestore
  @SerialName("area_full") val areaFull: String? = null,     // Общая площадь жилого помещения в кв.м.
  @SerialName("area_otopl") val areaOtopl: String? = null,   // Отапливаемая площадь жилого помещения
  @SerialName("room") val room: String? = null,               // Количество зарегистрированных комнат
  @SerialName("tenant_tbo") val tenantTbo: String? = null,   // Число прописанных человек (норма ТБО/Воды)
  // ====================================================================

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

  // --- 4. ИНТЕГРАЦИЯ ЧАТ-СИСТЕМЫ И ПУШ-ВЕЛОСИПЕДОВ (YkisMobPAM) ---
  @SerialName("currentChatUid") val currentChatUid: String? = null,
  @SerialName("opponentUid") val opponentUid: String? = null,
  @SerialName("opponentName") val opponentName: String? = null,
  @SerialName("opponentLogoUrl") val opponentLogoUrl: String? = null,
  @SerialName("activeRecipientFcmTokens") val activeRecipientFcmTokens: List<String> = emptyList(),
  @SerialName("selectedImagePath") val selectedImagePath: String? = null,
  @SerialName("isOpponentTyping") val isOpponentTyping: Boolean = false,
  @SerialName("isForwarding") val isForwarding: Boolean = false,
  @SerialName("assistantResponse") val assistantResponse: String? = null,
  @SerialName("isLoadingAfterSending") val isLoadingAfterSending: Boolean = false,

  // --- 5. ФИНАНСОВЫЙ УЧЕТ (Ledger / Billing) ---
  @SerialName("totalDebt") val totalDebt: ServiceEntity = ServiceEntity(),
  @SerialName("monthlyServices") val monthlyServices: List<ServiceEntity> = emptyList(),
  @SerialName("serviceDetail") val serviceDetail: ContentDetail = ContentDetail.UNKNOWN,

  // --- 6. УЧЕТ ПОКАЗАНИЙ (Meters / Readings) ---
  @SerialName("waterMeterList") val waterMeterList: List<WaterMeterEntity> = emptyList(),
  @SerialName("waterReadings") val waterReadings: List<WaterReadingEntity> = emptyList(),
  @SerialName("selectedWaterMeter") val selectedWaterMeter: WaterMeterEntity = WaterMeterEntity(),
  @SerialName("lastWaterReading") val lastWaterReading: WaterReadingEntity? = null,
  @SerialName("newWaterReading") val newWaterReading: String = "",

  @SerialName("heatMeterList") val heatMeterList: List<HeatMeterEntity> = emptyList(),
  @SerialName("heatReadings") val heatReadings: List<HeatReadingEntity> = emptyList(),
  @SerialName("selectedHeatMeter") val selectedHeatMeter: HeatMeterEntity = HeatMeterEntity(),
  @SerialName("lastHeatReading") val lastHeatReading: HeatReadingEntity? = null,
  @SerialName("newHeatReading") val newHeatReading: String = "",

  @SerialName("isMetersLoading") val isMetersLoading: Boolean = false,
  @SerialName("isReadingsLoading") val isReadingsLoading: Boolean = false,
  @SerialName("isLastReadingLoading") val isLastReadingLoading: Boolean = false,
  @SerialName("selectedTab") val selectedTab: Int = 0,

  // --- 7. СОСТОЯНИЕ ИНТЕРФЕЙСА (Voyager / Adaptive UI) ---
  @SerialName("selectedContentDetail") val selectedContentDetail: ContentDetail = ContentDetail.BTI,
  @SerialName("isDetailOnlyOpen") val isDetailOnlyOpen: Boolean = false,
  @SerialName("showDetail") val showDetail: Boolean = false,

  // --- 6. СТАТУСЫ ЗАГРУЗКИ (Системные лоадеры) ---
  @SerialName("isLoading") val isLoading: Boolean = false,
  @SerialName("mainLoading") val mainLoading: Boolean = true,
  @SerialName("isGlobalLoading") val isGlobalLoading: Boolean = false,
  @SerialName("apartmentLoading") val apartmentLoading: Boolean = true,

  // --- 8. ОБЪЯВЛЕНИЯ (ANNOUNCEMENTS) ---
  @SerialName("announcements") val announcements: List<AnnouncementEntity> = emptyList(),
  @SerialName("isAnnouncementsLoading") val isAnnouncementsLoading: Boolean = false,
  @SerialName("unreadAnnouncementsCount") val unreadAnnouncementsCount: Int = 0,
  @SerialName("lastAnnouncementsCheck") val lastAnnouncementsCheck: Long = 0L,
  @SerialName("announcementImagePath") val announcementImagePath: String? = null,
  @SerialName("announcementFilePath") val announcementFilePath: String? = null,
  @SerialName("isAnnouncementUploading") val isAnnouncementUploading: Boolean = false,
  @SerialName("announcementDraftTitle") val announcementDraftTitle: String = "",
  @SerialName("announcementDraftMessage") val announcementDraftMessage: String = "",
  @SerialName("announcementFilterRole") val announcementFilterRole: UserRole? = null,

  // --- 9. ОШИБКИ ---
  @SerialName("error") val error: String? = null
) {
  init {
    // Логирование инициализации стейта согласно сквозному правилу [Класс.Метод]
    println("[$className.init]: Снімок BaseUIState оновлено в ОЗУ. Активний о/р: ${addressId}L, Наймач: $nanim, Роль: $userRole")
  }
}



