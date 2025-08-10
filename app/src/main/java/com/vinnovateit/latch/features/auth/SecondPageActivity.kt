package com.vinnovateit.latch.features.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinnovateit.latch.data.CredentialDatabase
import com.vinnovateit.latch.data.CredentialEntity
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.VisualTransformation
import com.vinnovateit.latch.R
import com.vinnovateit.latch.features.home.MainActivity
import com.vinnovateit.latch.ui.theme.LatchTheme
import com.vinnovateit.latch.ui.theme.LocalIsDarkTheme
import com.vinnovateit.latch.ui.theme.SatoshiFontFamily

class SecondPageActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val editMode = intent.getBooleanExtra("editMode", false)
        setContent {
            LatchTheme {
                CredentialsScreen(
                    editMode = editMode,
                    onCredentialsSaved = {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun CredentialsScreen(editMode: Boolean, onCredentialsSaved: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var regNo by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(false) }
    var regNoFocused by remember { mutableStateOf(false) }
    var passwordFocused by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    val isDark = LocalIsDarkTheme.current

    // Load from DB
    LaunchedEffect(Unit) {
        val db = CredentialDatabase.getInstance(context)
        val existing = db.credentialDao().getCredential()
        if (existing != null && !editMode) {
            onCredentialsSaved()
        } else if (existing != null && !loaded) {
            regNo = existing.registrationNumber
            password = existing.password
            loaded = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
          .background(MaterialTheme.colorScheme.background)
            .paint(
                painter = if (isDark) painterResource(R.drawable.background_overlay_dark) else painterResource(R.drawable.background_overlay_light),
                contentScale = ContentScale.Crop
            )
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-40).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.credentials_title),
                fontSize = 35.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SatoshiFontFamily,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(id = R.string.credentials_subtitle),
                fontSize = 20.sp,
                fontFamily = SatoshiFontFamily,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(48.dp))

            TextField(
                value = regNo,
                onValueChange = { regNo = it },
                label = if (regNo.isEmpty() && !regNoFocused) { { Text(stringResource(id = R.string.registration_number), color = MaterialTheme.colorScheme.tertiary,) } } else null,
                singleLine = true,
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Username Icon",
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        regNoFocused = focusState.isFocused
                    },
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.tertiary,
                    fontSize = 18.sp,
                    fontFamily = SatoshiFontFamily
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
              colors = TextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                focusedContainerColor = MaterialTheme.colorScheme.background,
                unfocusedContainerColor = MaterialTheme.colorScheme.background,
                cursorColor = MaterialTheme.colorScheme.onBackground,
                focusedLabelColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                unfocusedLabelColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                focusedIndicatorColor = MaterialTheme.colorScheme.onBackground,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
              ),
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextField(
                value = password,
                onValueChange = { password = it },
                label = if (password.isEmpty() && !passwordFocused) { { Text(stringResource(id = R.string.password), color = MaterialTheme.colorScheme.tertiary,) } } else null,
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide Password" else "Show Password",
                        modifier = Modifier.clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { passwordVisible = !passwordVisible }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        passwordFocused = focusState.isFocused
                    },
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.tertiary,
                    fontSize = 18.sp,
                    fontFamily = SatoshiFontFamily
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
              colors = TextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                focusedContainerColor = MaterialTheme.colorScheme.background,
                unfocusedContainerColor = MaterialTheme.colorScheme.background,
                cursorColor = MaterialTheme.colorScheme.onBackground,
                focusedLabelColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                unfocusedLabelColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                focusedIndicatorColor = MaterialTheme.colorScheme.onBackground,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
              ),
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (regNo.isNotBlank() && password.isNotBlank()) {
                        scope.launch {
                            val db = CredentialDatabase.getInstance(context)
                            db.credentialDao().insertCredential(
                                CredentialEntity(id = "singleton", registrationNumber = regNo, password = password)
                            )

                            Toast.makeText(context, context.getString(R.string.credentials_saved_toast), Toast.LENGTH_SHORT).show()
                            onCredentialsSaved()
                        }
                    } else {
                        message = context.getString(R.string.credentials_error_message)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp),
                shape = RoundedCornerShape(7.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = if (editMode) stringResource(id = R.string.update_credentials) else stringResource(id = R.string.save_credentials),
                    fontSize = 18.sp,
                    fontFamily = SatoshiFontFamily,
                    fontWeight = FontWeight.Bold,

                )
            }

            if (message.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = message, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
