package com.example.scripturestudyaid

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Highlight::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun highlightDao(): HighlightDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "scripture_study_database"
                )
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries() // Allowing main thread queries for simplicity as requested, but ideally should be async.
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
