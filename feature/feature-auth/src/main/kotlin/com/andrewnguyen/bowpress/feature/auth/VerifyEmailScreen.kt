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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPond
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.BowPressTheme
import com.andrewnguyen.bowpress.core.designsystem.BPFonts
import com.andrewnguyen.bowpress.core.designsystem.bp.BPEyebrow
import com.andrewnguyen.bowpress.core.designsystem.bp.BPHairlineButton
import com.andrewnguyen.bowpress.core.designsystem.bp.BPPrimaryButton
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI

/**
 * 6-digit OTP verification screen. Kenrokuen restyle: paper background, Fraunces
 * italic title, BP-branded text field, BPPrimaryButton + BPHairlineButton actions.
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

    val fieldColors = OutlinedTextFieldDefaults.colors(
        unfocusedBorderColor = AppLine,
        focusedBorderColor = AppPondDk,
        unfocusedLabelColor = AppInk3,
        focusedLabelColor = AppPondDk,
    )

    Scaffold(
        containerColor = AppPaper,
        topBar = {
            TopAppBar(
                title = {},
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppPaper),
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = AppInk)
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
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Kenrokuen title block
                BPEyebrow(text = "BOWPRESS", tone = AppInk3, size = 10.sp)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Verify email.",
                    style = frauncesDisplay(28.sp, italic = true).copy(color = AppInk),
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    "We sent a 6-digit code to $email. The code expires in 10 minutes.",
                    style = interUI(14.sp).copy(color = AppInk3),
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = code,
                    onValueChange = { next -> code = next.filter(Char::isDigit).take(6) },
                    label = { Text("6-digit code", style = interUI(15.sp)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    textStyle = TextStyle(
                        fontFamily = BPFonts.Mono,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 24.sp,
                        letterSpacing = 8.sp,
                        color = AppInk,
                    ),
                    colors = fieldColors,
                    isError = state.error is AuthError.InvalidVerificationCode ||
                        state.error is AuthError.VerificationCodeExpired,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TAG_CODE_FIELD),
                )

                ErrorMessage(state.error)

                BPPrimaryButton(
                    title = "Verify",
                    onClick = { onSubmit(code) },
                    enabled = canSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TAG_VERIFY_SUBMIT),
                )

                // Resend — secondary hairline action
                val resendLabel = if (state.resendCooldownSeconds > 0) {
                    "Resend code in ${state.resendCooldownSeconds}s"
                } else {
                    "Resend code"
                }
                BPHairlineButton(
                    label = resendLabel,
                    onClick = { if (canResend) onResend() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TAG_RESEND_BUTTON),
                    borderTone = if (canResend) AppLine else AppLine,
                    labelTone = if (canResend) AppInk else AppInk3,
                )
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
        style = interUI(14.sp).copy(color = AppPond),
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
