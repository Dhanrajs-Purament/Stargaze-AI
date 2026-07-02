package com.stargaze.ai.di

import javax.inject.Qualifier

/** Qualifies the (nullable) AI proxy base URL string so Hilt can distinguish it from other Strings. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AiProxyBaseUrl
