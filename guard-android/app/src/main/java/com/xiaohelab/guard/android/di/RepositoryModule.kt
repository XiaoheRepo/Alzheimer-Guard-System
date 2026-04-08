package com.xiaohelab.guard.android.di

import com.xiaohelab.guard.android.data.repository.AiRepositoryImpl
import com.xiaohelab.guard.android.data.repository.AuthRepositoryImpl
import com.xiaohelab.guard.android.data.repository.ClueRepositoryImpl
import com.xiaohelab.guard.android.data.repository.NotificationRepositoryImpl
import com.xiaohelab.guard.android.data.repository.OrderRepositoryImpl
import com.xiaohelab.guard.android.data.repository.PatientRepositoryImpl
import com.xiaohelab.guard.android.data.repository.TaskRepositoryImpl
import com.xiaohelab.guard.android.domain.repository.AiRepository
import com.xiaohelab.guard.android.domain.repository.AuthRepository
import com.xiaohelab.guard.android.domain.repository.ClueRepository
import com.xiaohelab.guard.android.domain.repository.NotificationRepository
import com.xiaohelab.guard.android.domain.repository.OrderRepository
import com.xiaohelab.guard.android.domain.repository.PatientRepository
import com.xiaohelab.guard.android.domain.repository.TaskRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds @Singleton
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository

    @Binds @Singleton
    abstract fun bindClueRepository(impl: ClueRepositoryImpl): ClueRepository

    @Binds @Singleton
    abstract fun bindPatientRepository(impl: PatientRepositoryImpl): PatientRepository

    @Binds @Singleton
    abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository

    @Binds @Singleton
    abstract fun bindAiRepository(impl: AiRepositoryImpl): AiRepository

    @Binds @Singleton
    abstract fun bindOrderRepository(impl: OrderRepositoryImpl): OrderRepository
}
