package com.vinnovateit.latch.data

import android.content.Context
import com.vinnovateit.latch.utils.EncryptionUtils

object StoredCredentials {
    /**
     * Returns the saved userId and password from Room database, or null if not found.
     */
    suspend fun getUserId(context: Context): String? {
        val db = CredentialDatabase.getInstance(context)
        val encryptedRegNo = db.credentialDao().getCredential()?.registrationNumber
        return encryptedRegNo?.let { 
            try {
                EncryptionUtils.decrypt(it)
            } catch (e: Exception) {
                // Return as-is for backward compatibility
                it
            }
        }
    }

    suspend fun getPassword(context: Context): String? {
        val db = CredentialDatabase.getInstance(context)
        val encryptedPassword = db.credentialDao().getCredential()?.password
        return encryptedPassword?.let { 
            try {
                EncryptionUtils.decrypt(it)
            } catch (e: Exception) {
                // Return as-is for backward compatibility
                it
            }
        }
    }
}
