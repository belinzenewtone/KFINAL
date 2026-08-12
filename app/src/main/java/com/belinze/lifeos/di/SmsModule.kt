package com.belinze.lifeos.di

import android.content.Context
import com.lifeos.sms.SmsService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides [SmsService] to the DI graph.
 *
 * [SmsService] lives in the :sms Gradle module which does not have the Hilt plugin,
 * so we cannot use @HiltAndroidEntryPoint there. Instead we provide it manually here
 * so it can be injected into LifeOsApplication (and other components) via @Inject.
 */
@Module
@InstallIn(SingletonComponent::class)
object SmsModule {

    @Provides
    @Singleton
    fun provideSmsService(@ApplicationContext context: Context): SmsService =
        SmsService(context)
}
