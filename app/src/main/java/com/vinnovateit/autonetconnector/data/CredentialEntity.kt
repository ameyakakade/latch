package com.vinnovateit.autonetconnector.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "credentials")
data class CredentialEntity(
    @PrimaryKey val id: String = "singleton",  // Always "singleton"
    val registrationNumber: String,
    val password: String
)
