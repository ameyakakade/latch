package com.vinnovateit.latch.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vinnovateit.latch.desktop.resources.Res
import com.vinnovateit.latch.desktop.resources.credentials_error_message
import com.vinnovateit.latch.desktop.resources.credentials_subtitle
import com.vinnovateit.latch.desktop.resources.credentials_title
import com.vinnovateit.latch.desktop.resources.password
import com.vinnovateit.latch.desktop.resources.registration_number
import com.vinnovateit.latch.desktop.resources.save_credentials
import org.jetbrains.compose.resources.stringResource

/**
 * Credential entry.
 *
 * Saved via the platform CredentialStore, which on Windows means DPAPI -- the
 * blob is tied to the logged-in Windows account.
 */
@Composable
fun CredentialsScreen(
    onSave: (userId: String, password: String) -> Unit,
) {
    var regNo by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val errorMessage = stringResource(Res.string.credentials_error_message)

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.credentials_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.credentials_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = regNo,
            onValueChange = { regNo = it; error = null },
            label = { Text(stringResource(Res.string.registration_number)) },
            singleLine = true,
            isError = error != null && regNo.isBlank(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = pass,
            onValueChange = { pass = it; error = null },
            label = { Text(stringResource(Res.string.password)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = error != null && pass.isBlank(),
            modifier = Modifier.fillMaxWidth(),
        )

        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                if (regNo.isBlank() || pass.isBlank()) {
                    error = errorMessage
                } else {
                    onSave(regNo.trim(), pass)
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(Res.string.save_credentials))
        }
    }
}
