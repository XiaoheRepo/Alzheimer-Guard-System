package com.xiaohelab.guard.android.data.di

import com.xiaohelab.guard.android.data.remote.api.AiApiService
import com.xiaohelab.guard.android.data.remote.api.AuthApiService
import com.xiaohelab.guard.android.data.remote.api.ClueApiService
import com.xiaohelab.guard.android.data.remote.api.NotificationApiService
import com.xiaohelab.guard.android.data.remote.api.OrderApiService
import com.xiaohelab.guard.android.data.remote.api.PatientApiService
import com.xiaohelab.guard.android.data.remote.api.TaskApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {

    @Provides @Singleton
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService =
        retrofit.create(AuthApiService::class.java)

    @Provides @Singleton
    fun provideTaskApiService(retrofit: Retrofit): TaskApiService =
        retrofit.create(TaskApiService::class.java)

    @Provides @Singleton
    fun provideClueApiService(retrofit: Retrofit): ClueApiService =
        retrofit.create(ClueApiService::class.java)

    @Provides @Singleton
    fun providePatientApiService(retrofit: Retrofit): PatientApiService =
        retrofit.create(PatientApiService::class.java)

    @Provides @Singleton
    fun provideNotificationApiService(retrofit: Retrofit): NotificationApiService =
        retrofit.create(NotificationApiService::class.java)

    @Provides @Singleton
    fun provideAiApiService(retrofit: Retrofit): AiApiService =
        retrofit.create(AiApiService::class.java)

    @Provides @Singleton
    fun provideOrderApiService(retrofit: Retrofit): OrderApiService =
        retrofit.create(OrderApiService::class.java)
}
