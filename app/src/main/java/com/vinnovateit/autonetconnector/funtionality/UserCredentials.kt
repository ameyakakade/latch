package com.vinnovateit.autonetconnector.funtionality

data class UserCredentials(
    val registrationNumber: String,
    val password: String,
    val wifiName: String
)

// get the user credentials from cache and store it here