package com.vinnovateit.autonetconnector.functionality

data class UserCredentials(
  val registrationNumber: String,
  val password: String,
  val string: String
)

// get the user credentials from cache and store it here