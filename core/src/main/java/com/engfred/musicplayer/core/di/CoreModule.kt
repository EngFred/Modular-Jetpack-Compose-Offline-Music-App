package com.engfred.musicplayer.core.di

import com.engfred.musicplayer.core.data.source.SharedAudioDataSource
import com.engfred.musicplayer.core.mapper.AudioFileMapper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoreModule {

    @Provides
    @Singleton
    fun provideSharedAudioDataSource(): SharedAudioDataSource {
        return SharedAudioDataSource()
    }

    @Provides
    @Singleton
    fun provideAudioFileMapper(): AudioFileMapper {
        return AudioFileMapper()
    }
}