package com.engfred.musicplayer.feature_player.di

import android.content.ComponentName
import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.SessionToken
import com.engfred.musicplayer.core.data.session.MediaControllerProvider
import com.engfred.musicplayer.core.data.source.SharedAudioDataSource
import com.engfred.musicplayer.core.domain.repository.PlayerController
import com.engfred.musicplayer.core.domain.repository.PlaylistRepository
import com.engfred.musicplayer.core.domain.usecases.PermissionHandlerUseCase
import com.engfred.musicplayer.core.mapper.AudioFileMapper
import com.engfred.musicplayer.feature_player.data.repository.PlayerControllerImpl
import com.engfred.musicplayer.feature_player.data.service.MusicNotificationProvider
import com.engfred.musicplayer.feature_player.data.service.MusicService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {

    @Provides
    @Singleton
    fun provideExoPlayer(
        @ApplicationContext context: Context
    ): ExoPlayer {
        return ExoPlayer.Builder(context).build()
    }

    @Provides
    @Singleton
    fun provideAudioAttributes(): AudioAttributes {
        return AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
    }

    @OptIn(UnstableApi::class)
    @Provides
    @Singleton
    fun provideSessionToken(@ApplicationContext context: Context): SessionToken {
        return SessionToken(context, ComponentName(context, MusicService::class.java))
    }


    @OptIn(UnstableApi::class)
    @Provides
    @Singleton
    fun provideMusicNotificationProvider(
        @ApplicationContext context: Context
    ): MusicNotificationProvider {
        return MusicNotificationProvider(context)
    }

    @OptIn(UnstableApi::class)
    @Provides
    @Singleton
    fun providePlayerController(
        sharedAudioDataSource: SharedAudioDataSource,
        audioFileMapper: AudioFileMapper,
        mediaControllerProvider: MediaControllerProvider,
        @ApplicationContext context: Context,
        permissionHandlerUseCase: PermissionHandlerUseCase,
        playlistRepository: PlaylistRepository
    ): PlayerController {
        return PlayerControllerImpl(
            sharedAudioDataSource,
            audioFileMapper,
            mediaControllerProvider,
            permissionHandlerUseCase,
            playlistRepository,
            context
        )
    }
}