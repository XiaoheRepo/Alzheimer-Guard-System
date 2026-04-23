package com.xiaohelab.guard.android.di

import com.xiaohelab.guard.android.core.network.TokenRefresher
import com.xiaohelab.guard.android.feature.auth.data.AuthApi
import com.xiaohelab.guard.android.feature.auth.data.AuthRepositoryImpl
import com.xiaohelab.guard.android.feature.auth.domain.AuthRepository
import com.xiaohelab.guard.android.feature.me.data.UserApi
import com.xiaohelab.guard.android.feature.me.domain.UserRepository
import com.xiaohelab.guard.android.feature.me.domain.UserRepositoryImpl
import com.xiaohelab.guard.android.feature.profile.data.PatientApi
import com.xiaohelab.guard.android.feature.profile.domain.ProfileRepository
import com.xiaohelab.guard.android.feature.profile.domain.ProfileRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FeatureApiModule {
    @Provides @Singleton fun authApi(r: Retrofit): AuthApi = r.create(AuthApi::class.java)
    @Provides @Singleton fun userApi(r: Retrofit): UserApi = r.create(UserApi::class.java)
    @Provides @Singleton fun patientApi(r: Retrofit): PatientApi = r.create(PatientApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton abstract fun authRepo(impl: AuthRepositoryImpl): AuthRepository
    @Binds @Singleton abstract fun tokenRefresher(impl: AuthRepositoryImpl): TokenRefresher
    @Binds @Singleton abstract fun userRepo(impl: UserRepositoryImpl): UserRepository
    @Binds @Singleton abstract fun profileRepo(impl: ProfileRepositoryImpl): ProfileRepository
}
