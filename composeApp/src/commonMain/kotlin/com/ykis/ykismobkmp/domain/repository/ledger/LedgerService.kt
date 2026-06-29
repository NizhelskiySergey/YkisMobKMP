package com.ykis.ykismobkmp.domain.repository.ledger

// Импортируем 3 зафиксированных доменных сценария для начислений и платежей
import com.ykis.ykismobkmp.domain.repository.ledger.request.GetFlatServices
import com.ykis.ykismobkmp.domain.repository.ledger.request.GetTotalDebtServices
import com.ykis.ykismobkmp.domain.repository.ledger.request.GetFastpayTokenByOsbb

/**
 * [LedgerService] — Монолитный доменный сервис-комбайн финансово-бухгалтерского биллинга ЮКИС.
 */
class LedgerService(
  val getFlatServices: GetFlatServices,
  val getTotalDebtServices: GetTotalDebtServices,
  val getFastpayTokenByOsbb: GetFastpayTokenByOsbb
)
