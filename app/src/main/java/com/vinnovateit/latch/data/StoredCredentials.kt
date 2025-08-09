package com.vinnovateit.latch.data

import android.content.Context

object StoredCredentials {
    /**
     * Returns the saved userId and password from Room database, or null if not found.
     */
    suspend fun getUserId(context: Context): String? {
        val db = CredentialDatabase.getInstance(context)
        return db.credentialDao().getCredential()?.registrationNumber
    }

    suspend fun getPassword(context: Context): String? {
        val db = CredentialDatabase.getInstance(context)
        return db.credentialDao().getCredential()?.password
    }
}
