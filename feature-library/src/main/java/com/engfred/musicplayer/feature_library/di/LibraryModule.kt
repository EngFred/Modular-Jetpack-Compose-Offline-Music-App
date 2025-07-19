package com.engfred.musicplayer.feature_library.di
import android.content.Context
import com.engfred.musicplayer.feature_library.data.repository.AudioFileRepositoryImpl
import com.engfred.musicplayer.feature_library.data.source.local.ContentResolverDataSource
import com.engfred.musicplayer.feature_library.domain.repository.AudioFileRepository
import com.engfred.musicplayer.feature_library.domain.usecases.PermissionHandlerUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // Install in SingletonComponent for app-wide singletons
object LibraryModule {

    @Provides
    @Singleton
    fun provideContentResolverDataSource(@ApplicationContext context: Context): ContentResolverDataSource {
        return ContentResolverDataSource(context)
    }

    @Provides
    @Singleton
    fun provideAudioFileRepository(
        dataSource: ContentResolverDataSource
    ): AudioFileRepository {
        return AudioFileRepositoryImpl(dataSource)
    }

    @Provides
    @Singleton
    fun providePermissionHandlerUseCase(
        @ApplicationContext context: Context
    ): PermissionHandlerUseCase {
        return PermissionHandlerUseCase(context)
    }
}