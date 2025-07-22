package com.vinnovateit.autonetconnector.functionality2.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CredentialEntity::class], version = 2) // Updated version to 2
abstract class CredentialDatabase : RoomDatabase() {
    abstract fun credentialDao(): CredentialDao

    companion object {
        @Volatile
        private var INSTANCE: CredentialDatabase? = null

        fun getInstance(context: Context): CredentialDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CredentialDatabase::class.java,
                    "credential_db"
                )
                    .fallbackToDestructiveMigration() // This line avoids crashes on schema change during development
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
