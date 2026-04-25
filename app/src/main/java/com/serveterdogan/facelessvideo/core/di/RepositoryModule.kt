package com.serveterdogan.facelessvideo.core.di

import com.serveterdogan.facelessvideo.data.repository.GeminiRepositoryImpl
import com.serveterdogan.facelessvideo.data.repository.TTSRepositoryImpl
import com.serveterdogan.facelessvideo.data.repository.VideoEditorRepositoryImpl
import com.serveterdogan.facelessvideo.data.repository.VideoRepositoryImpl
import com.serveterdogan.facelessvideo.domain.repository.GeminiRepository
import com.serveterdogan.facelessvideo.domain.repository.TTSRepository
import com.serveterdogan.facelessvideo.domain.repository.VideoEditorRepository
import com.serveterdogan.facelessvideo.domain.repository.VideoRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindGeminiRepository(
        geminiRepositoryImpl: GeminiRepositoryImpl
    ): GeminiRepository

    @Binds
    @Singleton
    abstract fun bindTTSRepository(
        ttsRepositoryImpl: TTSRepositoryImpl
    ): TTSRepository

    @Binds
    @Singleton
    abstract fun bindVideoRepository(
        videoRepositoryImpl: VideoRepositoryImpl
    ): VideoRepository

    @Binds
    @Singleton
    abstract fun bindVideoEditorRepository(
        videoEditorRepositoryImpl: VideoEditorRepositoryImpl
    ): VideoEditorRepository
}
