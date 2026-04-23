package com.andrewnguyen.bowpress.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrewnguyen.bowpress.core.designsystem.BowPressTheme

/**
 * Sign-in / sign-up form. Defaults to sign-in; the user toggles with the segmented
 * control. On success the ViewModel emits [AuthUiEvent.NavigateToVerify]
 * (sign-up) or [AuthUiEvent.SignedIn] (sign-in) — the nav layer handles routing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailAuthScreen(
    onCancel: () -> Unit,
    onSignedIn: () -> Unit,
    onNavigateToVerify: (email: String) -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AuthUiEvent.SignedIn -> onSignedIn()
                is AuthUiEvent.NavigateToVerify -> onNavigateToVerify(event.email)
            }
        }
    }

    EmailAuthScreenContent(
        state = state,
        onCancel = onCancel,
        onModeChange = viewModel::setMode,
        onSubmit = { name, email, password ->
            if (state.mode == AuthUiState.Mode.SIGN_IN) {
                viewModel.signIn(email, password)
            } else {
                viewModel.signUp(name, email, password)
            }
        },
        onErrorDismiss = viewModel::clearError,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EmailAuthScreenContent(
    state: AuthUiState,
    onCancel: () -> Unit,
    onModeChange: (AuthUiState.Mode) -> Unit,
    onSubmit: (name: String, email: String, password: String) -> Unit,
    onErrorDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(state.email) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val isCreate = state.mode == AuthUiState.Mode.CREATE_ACCOUNT
    val emailValid = email.contains("@")
    val passwordValid = password.length >= 8
    val nameValid = name.trim().isNotEmpty()
    val passwordsMatch = password == confirmPassword
    val canSubmit = emailValid && passwordValid &&
        (!isCreate || (nameValid && passwordsMatch)) && !state.isLoading

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isCreate) "Create Account" else "Sign In") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !isCreate,
                        onClick = { onModeChange(AuthUiState.Mode.SIGN_IN) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    ) { Text("Sign In") }
                    SegmentedButton(
                        selected = isCreate,
                        onClick = { onModeChange(AuthUiState.Mode.CREATE_ACCOUNT) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    ) { Text("Create Account") }
                }

                if (isCreate) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; if (state.error != null) onErrorDismiss() },
                        label = { Text("Full name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(TAG_NAME_FIELD),
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; if (state.error != null) onErrorDismiss() },
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TAG_EMAIL_FIELD),
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; if (state.error != null) onErrorDismiss() },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    supportingText = {
                        if (password.isNotEmpty() && !passwordValid) {
                            Text(
                                "Password must be at least 8 characters.",
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    },
                    isError = password.isNotEmpty() && !passwordValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TAG_PASSWORD_FIELD),
                )

                if (isCreate) {
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        supportingText = {
                            if (confirmPassword.isNotEmpty() && !passwordsMatch) {
                                Text(
                                    "Passwords do not match.",
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        },
                        isError = confirmPassword.isNotEmpty() && !passwordsMatch,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(TAG_CONFIRM_PASSWORD_FIELD),
                    )
                }

                ErrorBanner(state.error)

                Spacer(Modifier.height(4.dp))

                Button(
                    onClick = { onSubmit(name, email, password) },
                    enabled = canSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag(TAG_SUBMIT_BUTTON),
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.height(22.dp),
                        )
                    } else {
                        Text(if (isCreate) "Create Account" else "Sign In")
                    }
                }
            }

            if (state.isLoading) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.05f)),
                )
            }
        }
    }
}

@Composable
private fun ErrorBanner(error: AuthError?) {
    if (error == null) return
    val text = when (error) {
        is AuthError.InvalidCredentials ->
            "We couldn't verify those credentials. Please try again."
        is AuthError.RateLimited ->
            "Too many attempts. Please wait a minute and try again."
        is AuthError.InvalidVerificationCode ->
            "Invalid code; ${error.attemptsRemaining} attempts remaining."
        is AuthError.VerificationCodeExpired ->
            "Your verification code has expired. Tap resend to get a new one."
        is AuthError.UiMessage -> error.message
    }
    Text(
        text = text,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TAG_ERROR_BANNER),
    )
}

internal const val TAG_NAME_FIELD = "email_auth_name"
internal const val TAG_EMAIL_FIELD = "email_auth_email"
internal const val TAG_PASSWORD_FIELD = "email_auth_password"
internal const val TAG_CONFIRM_PASSWORD_FIELD = "email_auth_confirm_password"
internal const val TAG_SUBMIT_BUTTON = "email_auth_submit"
internal const val TAG_ERROR_BANNER = "email_auth_error"

@Preview(showBackground = true)
@Composable
private fun EmailAuthSignInPreview() {
    BowPressTheme {
        EmailAuthScreenContent(
            state = AuthUiState(mode = AuthUiState.Mode.SIGN_IN),
            onCancel = {},
            onModeChange = {},
            onSubmit = { _, _, _ -> },
            onErrorDismiss = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmailAuthCreateAccountPreview() {
    BowPressTheme {
        EmailAuthScreenContent(
            state = AuthUiState(mode = AuthUiState.Mode.CREATE_ACCOUNT),
            onCancel = {},
            onModeChange = {},
            onSubmit = { _, _, _ -> },
            onErrorDismiss = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmailAuthErrorPreview() {
    BowPressTheme {
        EmailAuthScreenContent(
            state = AuthUiState(
                mode = AuthUiState.Mode.SIGN_IN,
                error = AuthError.InvalidCredentials,
            ),
            onCancel = {},
            onModeChange = {},
            onSubmit = { _, _, _ -> },
            onErrorDismiss = {},
        )
    }
}
