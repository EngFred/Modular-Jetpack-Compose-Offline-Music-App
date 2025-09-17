package com.engfred.musicplayer.feature_player.di

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.SessionToken
import com.engfred.musicplayer.feature_player.data.service.ExoPlayerProvider
import com.engfred.musicplayer.feature_player.data.service.PlaybackService
import com.engfred.musicplayer.feature_player.data.service.MusicNotificationProvider
import com.engfred.musicplayer.feature_player.data.repository.PlaybackControllerImpl
import com.engfred.musicplayer.core.data.source.SharedAudioDataSource
import com.engfred.musicplayer.core.domain.repository.PlaybackController
import com.engfred.musicplayer.core.mapper.AudioFileMapper
import com.engfred.musicplayer.core.domain.usecases.PermissionHandlerUseCase
import com.engfred.musicplayer.core.domain.repository.PlaylistRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {

    @Provides
    @Singleton
    fun provideExoPlayerProvider(@ApplicationContext context: Context): ExoPlayerProvider {
        return ExoPlayerProvider(context)
    }

    @UnstableApi
    @Provides
    @Singleton
    fun provideSessionToken(@ApplicationContext context: Context): SessionToken {
        return SessionToken(context, ComponentName(context, PlaybackService::class.java))
    }

    @UnstableApi
    @Provides
    @Singleton
    fun provideMusicNotificationProvider(
        @ApplicationContext context: Context
    ): MusicNotificationProvider {
        return MusicNotificationProvider(context)
    }

    @UnstableApi
    @Provides
    @Singleton
    fun providePlayerController(
        sharedAudioDataSource: SharedAudioDataSource,
        audioFileMapper: AudioFileMapper,
        permissionHandlerUseCase: PermissionHandlerUseCase,
        playlistRepository: PlaylistRepository,
        @ApplicationContext context: Context,
        sessionToken: SessionToken,
    ): PlaybackController {
        return PlaybackControllerImpl(
            sharedAudioDataSource,
            audioFileMapper,
            permissionHandlerUseCase,
            playlistRepository,
            context,
            sessionToken,
        )
    }
}
