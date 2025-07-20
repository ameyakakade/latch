package com.vinnovateit.autonetconnector.functionality2.storage

import androidx.room.Entity
import androidx.room.PrimaryKey
 
@Entity(tableName = "credentials")
data class CredentialEntity(
    @PrimaryKey val registrationNumber: String,
    val password: String
) 