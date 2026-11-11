package com.auraface.auraface_app.di

import com.auraface.auraface_app.core.Constants
import com.auraface.auraface_app.data.network.api.*
import com.auraface.auraface_app.data.network.AuthInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi =
        retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideStudentApi(retrofit: Retrofit): StudentApi =
        retrofit.create(StudentApi::class.java)

    @Provides
    @Singleton
    fun provideTeacherApi(retrofit: Retrofit): TeacherApi =
        retrofit.create(TeacherApi::class.java)

    @Provides
    @Singleton
    fun provideSubjectApi(retrofit: Retrofit): SubjectApi =
        retrofit.create(SubjectApi::class.java)

    @Provides
    @Singleton
    fun provideGalleryApi(retrofit: Retrofit): GalleryApi =
        retrofit.create(GalleryApi::class.java)

    @Provides
    @Singleton
    fun provideAttendanceApi(retrofit: Retrofit): AttendanceApi =
        retrofit.create(AttendanceApi::class.java)

    @Provides
    @Singleton
    fun provideAdminApi(retrofit: Retrofit): AdminApi =
        retrofit.create(AdminApi::class.java)

    @Provides
    @Singleton
    fun provideSmartFeaturesApi(retrofit: Retrofit): SmartFeaturesApi =
        retrofit.create(SmartFeaturesApi::class.java)

    @Provides
    @Singleton
    fun provideChatApi(retrofit: Retrofit): ChatApi =
        retrofit.create(ChatApi::class.java)

    @Provides
    @Singleton
    fun providePlacementApi(retrofit: Retrofit): PlacementApi =
        retrofit.create(PlacementApi::class.java)

    @Provides
    @Singleton
    fun provideQuizApi(retrofit: Retrofit): QuizApi =
        retrofit.create(QuizApi::class.java)

    @Provides
    @Singleton
    fun providePulseApi(retrofit: Retrofit): PulseApi =
        retrofit.create(PulseApi::class.java)

    @Provides
    @Singleton
    fun provideQuestApi(retrofit: Retrofit): QuestApi =
        retrofit.create(QuestApi::class.java)

    @Provides
    @Singleton
    fun provideCanteenApi(retrofit: Retrofit): CanteenApi =
        retrofit.create(CanteenApi::class.java)

    @Provides
    @Singleton
    fun provideSpacesApi(retrofit: Retrofit): SpacesApi =
        retrofit.create(SpacesApi::class.java)

    @Provides
    @Singleton
    fun provideLostFoundApi(retrofit: Retrofit): LostFoundApi =
        retrofit.create(LostFoundApi::class.java)

    @Provides
    @Singleton
    fun provideErpApi(retrofit: Retrofit): ErpApi =
        retrofit.create(ErpApi::class.java)

    @Provides
    @Singleton
    fun provideWebSocketManager(client: OkHttpClient): com.auraface.auraface_app.data.network.WebSocketManager {
        return com.auraface.auraface_app.data.network.WebSocketManager(client)
    }
}
