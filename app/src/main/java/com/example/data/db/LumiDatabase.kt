package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.ActivityEntity
import com.example.data.model.AttemptEntity
import com.example.data.model.H5PLibraryEntity

@Database(
    entities = [ActivityEntity::class, H5PLibraryEntity::class, AttemptEntity::class],
    version = 1,
    exportSchema = false
)
abstract class LumiDatabase : RoomDatabase() {
    abstract fun activityDao(): ActivityDao
    abstract fun libraryDao(): LibraryDao

    companion object {
        @Volatile
        private var INSTANCE: LumiDatabase? = null

        fun getDatabase(context: Context): LumiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LumiDatabase::class.java,
                    "lumi_database.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
