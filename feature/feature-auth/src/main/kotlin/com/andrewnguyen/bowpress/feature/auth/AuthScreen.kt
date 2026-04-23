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
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.andrewnguyen.bowpress.core.designsystem.BowPressTheme

/**
 * Landing screen: hero animation + three entry points (Google, Email, continue
 * anonymously is out of scope). Mirrors iOS `AuthView`.
 *
 * The ViewModel is resolved via Hilt — callers just need [onNavigateToEmail] and
 * [onSignedIn] wired. The enclosing nav graph collects signed-in events.
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
            .background(MaterialTheme.colorScheme.surface),
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
            Text(
                text = "BowPress",
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Tune smarter. Shoot better.",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(56.dp))

            Button(
                onClick = onContinueWithGoogle,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag(TAG_CONTINUE_GOOGLE),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF202124),
                ),
            ) {
                Text("Continue with Google", fontWeight = FontWeight.Medium)
            }

            Spacer(Modifier.height(14.dp))

            OutlinedButton(
                onClick = onContinueWithEmail,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag(TAG_CONTINUE_EMAIL),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    "Continue with Email",
                    color = LocalContentColor.current,
                    fontWeight = FontWeight.Medium,
                )
            }

            if (errorMessage != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Text(
            text = "By continuing you agree to our Terms of Service and Privacy Policy.",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(PaddingValues(horizontal = 32.dp, vertical = 24.dp)),
        )

        if (isLoading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
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
