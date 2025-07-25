package com.engfred.musicplayer.feature_equalizer.di

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.engfred.musicplayer.core.domain.repository.EqualizerController // Assuming this is your interface
import com.engfred.musicplayer.feature_equalizer.data.EqualizerControllerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class EqualizerModule {
    @Binds
    @Singleton
    @OptIn(UnstableApi::class)
    abstract fun bindEqualizerController(
        equalizerControllerImpl: EqualizerControllerImpl // Parameter type
    ): EqualizerController // Return type
}