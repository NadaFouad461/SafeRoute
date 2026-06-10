package com.example.saferoute.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.saferoute.data.EmergencyDao

@Database(
    // merge EmergencyLog table with table  RoomGrid
    entities = [EmergencyLog::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun emergencyDao(): EmergencyDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {

            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "safe_route_db"
                ).fallbackToDestructiveMigration().build()

                INSTANCE = instance
                instance
            }
        }
    }
}