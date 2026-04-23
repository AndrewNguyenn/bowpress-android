package com.andrewnguyen.bowpress.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrewnguyen.bowpress.core.designsystem.BowPressTheme

/**
 * 6-digit verification screen. We use a single `OutlinedTextField` constrained
 * to digits — simpler and more robust than 6 focus-hopping fields, matches
 * what Material guidelines suggest for OTP on Android.
 *
 * Resend has a 30s cooldown enforced by the ViewModel (mirrors iOS 60s — we
 * shorter since Android users expect tighter loops).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyEmailScreen(
    email: String,
    onCancel: () -> Unit,
    onVerified: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is AuthUiEvent.SignedIn) onVerified()
        }
    }

    VerifyEmailScreenContent(
        email = email,
        state = state,
        onCancel = onCancel,
        onSubmit = { code -> viewModel.verifyEmail(email, code) },
        onResend = { viewModel.resendVerification(email) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VerifyEmailScreenContent(
    email: String,
    state: AuthUiState,
    onCancel: () -> Unit,
    onSubmit: (String) -> Unit,
    onResend: () -> Unit,
) {
    var code by remember { mutableStateOf("") }
    val canSubmit = code.length == 6 && !state.isLoading
    val canResend = state.resendCooldownSeconds == 0 && !state.isLoading

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Verify email") },
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
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "Check your email",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "We sent a 6-digit code to $email. The code expires in 10 minutes.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = code,
                    onValueChange = { next -> code = next.filter(Char::isDigit).take(6) },
                    label = { Text("6-digit code") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 24.sp,
                        letterSpacing = 8.sp,
                    ),
                    isError = state.error is AuthError.InvalidVerificationCode ||
                        state.error is AuthError.VerificationCodeExpired,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TAG_CODE_FIELD),
                )

                ErrorMessage(state.error)

                Button(
                    onClick = { onSubmit(code) },
                    enabled = canSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag(TAG_VERIFY_SUBMIT),
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.height(22.dp),
                        )
                    } else {
                        Text("Verify")
                    }
                }

                TextButton(
                    onClick = onResend,
                    enabled = canResend,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TAG_RESEND_BUTTON),
                ) {
                    val label = if (state.resendCooldownSeconds > 0) {
                        "Resend code in ${state.resendCooldownSeconds}s"
                    } else {
                        "Resend code"
                    }
                    Text(label)
                }
            }
        }
    }
}

@Composable
private fun ErrorMessage(error: AuthError?) {
    if (error == null) return
    val text = when (error) {
        is AuthError.InvalidVerificationCode ->
            "Invalid code; ${error.attemptsRemaining} attempts remaining."
        is AuthError.VerificationCodeExpired ->
            "Your code has expired. Tap resend to get a new one."
        is AuthError.RateLimited ->
            "Too many attempts. Please wait and try again."
        is AuthError.InvalidCredentials ->
            "We couldn't verify those credentials. Please try again."
        is AuthError.UiMessage -> error.message
    }
    Text(
        text = text,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TAG_VERIFY_ERROR),
    )
}

internal const val TAG_CODE_FIELD = "verify_code"
internal const val TAG_VERIFY_SUBMIT = "verify_submit"
internal const val TAG_RESEND_BUTTON = "verify_resend"
internal const val TAG_VERIFY_ERROR = "verify_error"

@Preview(showBackground = true)
@Composable
private fun VerifyEmailPreview() {
    BowPressTheme {
        VerifyEmailScreenContent(
            email = "you@example.com",
            state = AuthUiState(),
            onCancel = {},
            onSubmit = {},
            onResend = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VerifyEmailErrorPreview() {
    BowPressTheme {
        VerifyEmailScreenContent(
            email = "you@example.com",
            state = AuthUiState(
                error = AuthError.InvalidVerificationCode(attemptsRemaining = 2),
                resendCooldownSeconds = 12,
            ),
            onCancel = {},
            onSubmit = {},
            onResend = {},
        )
    }
}
