package com.baiviet.core.data

import android.content.Context
import androidx.room.Room
import com.baiviet.core.data.db.BaiVietDatabase
import com.baiviet.core.data.db.SavedGameDao
import com.baiviet.core.data.db.StatsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): BaiVietDatabase =
        Room.databaseBuilder(context, BaiVietDatabase::class.java, "bai_viet.db").build()

    @Provides
    fun statsDao(db: BaiVietDatabase): StatsDao = db.stats()

    @Provides
    fun savedGameDao(db: BaiVietDatabase): SavedGameDao = db.savedGames()
}
