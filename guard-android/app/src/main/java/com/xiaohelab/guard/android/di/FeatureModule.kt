package com.xiaohelab.guard.android.di

import com.xiaohelab.guard.android.core.network.TokenRefresher
import com.xiaohelab.guard.android.feature.ai.data.AiSessionApi
import com.xiaohelab.guard.android.feature.ai.domain.AiRepository
import com.xiaohelab.guard.android.feature.ai.domain.AiRepositoryImpl
import com.xiaohelab.guard.android.feature.auth.data.AuthApi
import com.xiaohelab.guard.android.feature.auth.data.AuthRepositoryImpl
import com.xiaohelab.guard.android.feature.auth.domain.AuthRepository
import com.xiaohelab.guard.android.feature.clue.data.ClueApi
import com.xiaohelab.guard.android.feature.clue.domain.ClueRepository
import com.xiaohelab.guard.android.feature.clue.domain.ClueRepositoryImpl
import com.xiaohelab.guard.android.feature.mat.data.MaterialOrderApi
import com.xiaohelab.guard.android.feature.mat.domain.MaterialOrderRepository
import com.xiaohelab.guard.android.feature.mat.domain.MaterialOrderRepositoryImpl
import com.xiaohelab.guard.android.feature.me.data.UserApi
import com.xiaohelab.guard.android.feature.me.domain.UserRepository
import com.xiaohelab.guard.android.feature.me.domain.UserRepositoryImpl
import com.xiaohelab.guard.android.feature.notification.data.NotificationApi
import com.xiaohelab.guard.android.feature.notification.domain.NotificationRepository
import com.xiaohelab.guard.android.feature.notification.domain.NotificationRepositoryImpl
import com.xiaohelab.guard.android.feature.profile.data.PatientApi
import com.xiaohelab.guard.android.feature.profile.domain.ProfileRepository
import com.xiaohelab.guard.android.feature.profile.domain.ProfileRepositoryImpl
import com.xiaohelab.guard.android.feature.tag.data.TagApi
import com.xiaohelab.guard.android.feature.tag.domain.TagRepository
import com.xiaohelab.guard.android.feature.tag.domain.TagRepositoryImpl
import com.xiaohelab.guard.android.feature.task.data.RescueTaskApi
import com.xiaohelab.guard.android.feature.task.domain.TaskRepository
import com.xiaohelab.guard.android.feature.task.domain.TaskRepositoryImpl
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
    // M3-A 标签域
    @Provides @Singleton fun tagApi(r: Retrofit): TagApi = r.create(TagApi::class.java)
    // M3-B 物资域
    @Provides @Singleton fun materialOrderApi(r: Retrofit): MaterialOrderApi = r.create(MaterialOrderApi::class.java)
    // M5-A 任务域
    @Provides @Singleton fun rescueTaskApi(r: Retrofit): RescueTaskApi = r.create(RescueTaskApi::class.java)
    // M5-B 线索域
    @Provides @Singleton fun clueApi(r: Retrofit): ClueApi = r.create(ClueApi::class.java)
    // M6 通知域
    @Provides @Singleton fun notificationApi(r: Retrofit): NotificationApi = r.create(NotificationApi::class.java)
    // M7 AI 域
    @Provides @Singleton fun aiSessionApi(r: Retrofit): AiSessionApi = r.create(AiSessionApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton abstract fun authRepo(impl: AuthRepositoryImpl): AuthRepository
    @Binds @Singleton abstract fun tokenRefresher(impl: AuthRepositoryImpl): TokenRefresher
    @Binds @Singleton abstract fun userRepo(impl: UserRepositoryImpl): UserRepository
    @Binds @Singleton abstract fun profileRepo(impl: ProfileRepositoryImpl): ProfileRepository
    // M3-A
    @Binds @Singleton abstract fun tagRepo(impl: TagRepositoryImpl): TagRepository
    // M3-B
    @Binds @Singleton abstract fun matRepo(impl: MaterialOrderRepositoryImpl): MaterialOrderRepository
    // M5-A
    @Binds @Singleton abstract fun taskRepo(impl: TaskRepositoryImpl): TaskRepository
    // M5-B
    @Binds @Singleton abstract fun clueRepo(impl: ClueRepositoryImpl): ClueRepository
    // M6
    @Binds @Singleton abstract fun notifRepo(impl: NotificationRepositoryImpl): NotificationRepository
    // M7
    @Binds @Singleton abstract fun aiRepo(impl: AiRepositoryImpl): AiRepository
}

