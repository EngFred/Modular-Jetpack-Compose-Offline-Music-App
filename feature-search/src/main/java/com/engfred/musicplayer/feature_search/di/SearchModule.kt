package com.engfred.musicplayer.feature_search.di

import com.engfred.musicplayer.feature_library.domain.repository.AudioFileRepository
import com.engfred.musicplayer.feature_search.domain.usecases.SearchAudioFilesUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SearchModule {

    @Provides
    @Singleton
    fun provideSearchAudioFilesUseCase(
        audioFileRepository: AudioFileRepository // Hilt will inject this from feature-library's module
    ): SearchAudioFilesUseCase {
        return SearchAudioFilesUseCase(audioFileRepository)
    }
}