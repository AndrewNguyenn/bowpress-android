package com.andrewnguyen.bowpress.core.database

import android.content.Context
import androidx.room.Room
import com.andrewnguyen.bowpress.core.database.dao.ArrowConfigDao
import com.andrewnguyen.bowpress.core.database.dao.ArrowPlotDao
import com.andrewnguyen.bowpress.core.database.dao.BowConfigDao
import com.andrewnguyen.bowpress.core.database.dao.BowDao
import com.andrewnguyen.bowpress.core.database.dao.SessionDao
import com.andrewnguyen.bowpress.core.database.dao.SessionEndDao
import com.andrewnguyen.bowpress.core.database.dao.SuggestionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Hilt wiring for Room. Each DAO is a singleton owned by the database instance. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): BowPressDatabase =
        Room.databaseBuilder(context, BowPressDatabase::class.java, BowPressDatabase.NAME)
            .addMigrations(*Migrations.ALL)
            // Keep the destructive fallback as a last-resort escape hatch for development
            // builds where a schema was mutated without a matching migration.
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideBowDao(db: BowPressDatabase): BowDao = db.bowDao()
    @Provides fun provideBowConfigDao(db: BowPressDatabase): BowConfigDao = db.bowConfigDao()
    @Provides fun provideArrowConfigDao(db: BowPressDatabase): ArrowConfigDao = db.arrowConfigDao()
    @Provides fun provideSessionDao(db: BowPressDatabase): SessionDao = db.sessionDao()
    @Provides fun provideArrowPlotDao(db: BowPressDatabase): ArrowPlotDao = db.arrowPlotDao()
    @Provides fun provideSessionEndDao(db: BowPressDatabase): SessionEndDao = db.sessionEndDao()
    @Provides fun provideSuggestionDao(db: BowPressDatabase): SuggestionDao = db.suggestionDao()
}
