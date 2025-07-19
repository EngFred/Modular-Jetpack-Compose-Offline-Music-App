package com.engfred.musicplayer.feature_favorites.di

import android.content.Context
import androidx.room.Room
import com.engfred.musicplayer.feature_favorites.data.local.dao.FavoriteAudioFileDao
import com.engfred.musicplayer.feature_favorites.data.local.db.FavoritesDatabase
import com.engfred.musicplayer.feature_favorites.data.repository.FavoritesRepositoryImpl
import com.engfred.musicplayer.feature_favorites.domain.repository.FavoritesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FavoritesModule {

    @Provides
    @Singleton
    fun provideFavoritesDatabase(@ApplicationContext context: Context): FavoritesDatabase {
        return Room.databaseBuilder(
            context,
            FavoritesDatabase::class.java,
            "music_player_favorites_db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideFavoriteAudioFileDao(database: FavoritesDatabase): FavoriteAudioFileDao {
        return database.favoriteAudioFileDao()
    }

    @Provides
    @Singleton
    fun provideFavoritesRepository(repositoryImpl: FavoritesRepositoryImpl): FavoritesRepository {
        return repositoryImpl
    }
}
