package com.ykis.ykismobkmp.di

import com.ykis.ykismobkmp.domain.services.LogService
import com.ykis.ykismobkmp.domain.services.LogServiceImpl
import org.koin.dsl.module

// В iosMain/kotlin/di/Koin.ios.kt
val platformModule = module {
  single<LogService> { LogServiceImpl() }
}
