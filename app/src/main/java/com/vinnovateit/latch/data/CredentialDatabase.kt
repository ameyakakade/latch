package com.vinnovateit.latch.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [CredentialEntity::class], version = 2)
abstract class CredentialDatabase : RoomDatabase() {
    abstract fun credentialDao(): CredentialDao
}
