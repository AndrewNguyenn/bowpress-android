package com.andrewnguyen.bowpress.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPond
import com.andrewnguyen.bowpress.core.designsystem.BowPressTheme
import com.andrewnguyen.bowpress.core.designsystem.bp.BPEyebrow
import com.andrewnguyen.bowpress.core.designsystem.bp.BPHairlineButton
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI

/**
 * Landing / hero screen — Lottie animation + brand title + Google + email entry
 * points. Mirrors iOS `AuthView`.
 *
 * The Google button keeps its brand-mandated white card styling. The email CTA
 * becomes a BPHairlineButton. Background and typography move to the Kenrokuen
 * palette.
 */
@Composable
fun AuthScreen(
    onNavigateToEmail: () -> Unit,
    onSignedIn: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activityContext = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is AuthUiEvent.SignedIn) onSignedIn()
        }
    }

    AuthScreenContent(
        isLoading = state.isLoading,
        errorMessage = (state.error as? AuthError.UiMessage)?.message,
        onContinueWithEmail = onNavigateToEmail,
        onContinueWithGoogle = { viewModel.signInWithGoogle(activityContext) },
    )
}

@Composable
internal fun AuthScreenContent(
    isLoading: Boolean,
    errorMessage: String?,
    onContinueWithEmail: () -> Unit,
    onContinueWithGoogle: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppPaper),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            HeroAnimation()

            Spacer(Modifier.height(16.dp))

            // Brand eyebrow + Fraunces italic display title
            BPEyebrow(
                text = "BOWPRESS",
                tone = AppInk3,
                size = 10.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Tune smarter.",
                style = frauncesDisplay(28.sp, italic = true).copy(color = AppInk),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(8.dp))
            Text(
                text = "Shoot better.",
                style = interUI(16.sp).copy(color = AppInk3),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(56.dp))

            // Google Sign-In — brand styling preserved per Google guidelines
            Button(
                onClick = { if (!isLoading) onContinueWithGoogle() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag(TAG_CONTINUE_GOOGLE),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF202124),
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp),
            ) {
                Text("Continue with Google", fontWeight = FontWeight.Medium)
            }

            // iOS 7751e53 / 0fe6169 / 0959fbc: Continue-with-Email CTA + nested
            // EmailAuthScreen/VerifyEmailScreen are hidden from the UI because
            // there's no verified Resend domain to send verification mail from.
            // The service layer + composable bodies are left intact so the
            // surface can be reinstated by reverting this single edit. iOS
            // parity: Apple/Google only at the entry point.

            if (errorMessage != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = errorMessage,
                    style = interUI(14.sp).copy(color = AppPond),
                    textAlign = TextAlign.Center,
                )
            }
        }

        Text(
            text = "By continuing you agree to our Terms of Service and Privacy Policy.",
            style = interUI(11.sp).copy(color = AppInk3),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(PaddingValues(horizontal = 32.dp, vertical = 24.dp)),
        )

        if (isLoading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = AppPond)
            }
        }
    }
}

@Composable
private fun HeroAnimation() {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.archery_hero),
    )
    LottieAnimation(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        modifier = Modifier.size(160.dp),
    )
}

internal const val TAG_CONTINUE_GOOGLE = "auth_continue_google"
internal const val TAG_CONTINUE_EMAIL = "auth_continue_email"

@Preview(showBackground = true)
@Composable
private fun AuthScreenPreview() {
    BowPressTheme {
        AuthScreenContent(
            isLoading = false,
            errorMessage = null,
            onContinueWithEmail = {},
            onContinueWithGoogle = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AuthScreenLoadingPreview() {
    BowPressTheme {
        AuthScreenContent(
            isLoading = true,
            errorMessage = null,
            onContinueWithEmail = {},
            onContinueWithGoogle = {},
        )
    }
}
