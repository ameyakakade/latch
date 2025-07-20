package com.vinnovateit.autonetconnector.functionality2.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CredentialDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCredential(credential: CredentialEntity)

    @Query("SELECT * FROM credentials LIMIT 1")
    suspend fun getCredential(): CredentialEntity?
} 