package com.ykis.ykismobkmp.domain.mapper

import com.ykis.ykismobkmp.domain.entity.FastpayEntity
import com.ykis.ykismobkmp.db.FastpayTokenEntity

/**
 * [toDbFastpay] — Мапер з доменної моделі у сутність БД SQLDelight.
 */
fun FastpayEntity.toDbFastpay(): FastpayTokenEntity {
    return FastpayTokenEntity(
        id = this.id,
        name = this.name,
        biplanId = this.biplanId,
        okpo = this.okpo,
        osbbId = this.osbbId,
        fullUrl = this.fullUrl,
        token = this.token
    )
}

/**
 * [toDomainFastpay] — Мапер із сутності БД у доменну модель FastpayEntity.
 */
fun FastpayTokenEntity.toDomainFastpay(): FastpayEntity {
    return FastpayEntity(
        id = this.id,
        name = this.name,
        biplanId = this.biplanId,
        okpo = this.okpo,
        osbbId = this.osbbId,
        fullUrl = this.fullUrl,
        token = this.token
    )
}
