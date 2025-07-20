package com.engfred.musicplayer.feature_player.di

import android.content.ComponentName
import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.SessionToken
import com.engfred.musicplayer.core.data.source.SharedAudioDataSource
import com.engfred.musicplayer.feature_player.data.repository.PlayerRepositoryImpl
import com.engfred.musicplayer.feature_player.data.service.AudioFileMapper
import com.engfred.musicplayer.feature_player.data.service.MusicNotificationProvider
import com.engfred.musicplayer.feature_player.data.service.MusicService
import com.engfred.musicplayer.feature_player.domain.repository.PlayerRepository
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

    @Provides
    @Singleton
    fun provideAudioFileMapper(): AudioFileMapper {
        return AudioFileMapper()
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
    fun providePlayerRepository(
        @ApplicationContext context: Context,
        sharedAudioDataSource: SharedAudioDataSource,
        audioFileMapper: AudioFileMapper
    ): PlayerRepository {
        return PlayerRepositoryImpl(context, sharedAudioDataSource, audioFileMapper)
    }
}