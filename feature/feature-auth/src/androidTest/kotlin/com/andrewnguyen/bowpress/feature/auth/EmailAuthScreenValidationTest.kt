package com.andrewnguyen.bowpress.feature.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.andrewnguyen.bowpress.core.designsystem.BowPressTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

/**
 * Form-validation tests for sign-up/sign-in. These run against the stateless
 * [EmailAuthScreenContent] so they don't need Hilt.
 */
class EmailAuthScreenValidationTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun signIn_submit_disabled_when_email_empty() {
        composeRule.setContent {
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

        composeRule.onNodeWithTag(TAG_SUBMIT_BUTTON).assertIsNotEnabled()

        composeRule.onNodeWithTag(TAG_PASSWORD_FIELD).performTextInput("longpassword")
        composeRule.onNodeWithTag(TAG_SUBMIT_BUTTON).assertIsNotEnabled()

        composeRule.onNodeWithTag(TAG_EMAIL_FIELD).performTextInput("a@b.com")
        composeRule.onNodeWithTag(TAG_SUBMIT_BUTTON).assertIsEnabled()
    }

    @Test
    fun signIn_submit_disabled_when_password_too_short() {
        composeRule.setContent {
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

        composeRule.onNodeWithTag(TAG_EMAIL_FIELD).performTextInput("a@b.com")
        composeRule.onNodeWithTag(TAG_PASSWORD_FIELD).performTextInput("short")
        composeRule.onNodeWithTag(TAG_SUBMIT_BUTTON).assertIsNotEnabled()
    }

    @Test
    fun signUp_requires_name_matching_passwords_and_long_password() {
        composeRule.setContent {
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

        composeRule.onNodeWithTag(TAG_SUBMIT_BUTTON).assertIsNotEnabled()

        composeRule.onNodeWithTag(TAG_EMAIL_FIELD).performTextInput("new@example.com")
        composeRule.onNodeWithTag(TAG_PASSWORD_FIELD).performTextInput("longenoughpw")
        composeRule.onNodeWithTag(TAG_CONFIRM_PASSWORD_FIELD).performTextInput("longenoughpw")
        // Name still missing
        composeRule.onNodeWithTag(TAG_SUBMIT_BUTTON).assertIsNotEnabled()

        composeRule.onNodeWithTag(TAG_NAME_FIELD).performTextInput("Jane Doe")
        composeRule.onNodeWithTag(TAG_SUBMIT_BUTTON).assertIsEnabled()
    }

    @Test
    fun signUp_confirm_password_mismatch_blocks_submit() {
        composeRule.setContent {
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

        composeRule.onNodeWithTag(TAG_NAME_FIELD).performTextInput("Jane")
        composeRule.onNodeWithTag(TAG_EMAIL_FIELD).performTextInput("a@b.com")
        composeRule.onNodeWithTag(TAG_PASSWORD_FIELD).performTextInput("longenoughpw")
        composeRule.onNodeWithTag(TAG_CONFIRM_PASSWORD_FIELD).performTextInput("different!")

        composeRule.onNodeWithTag(TAG_SUBMIT_BUTTON).assertIsNotEnabled()
    }

    @Test
    fun error_banner_renders_when_state_has_error() {
        composeRule.setContent {
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

        composeRule.onNodeWithTag(TAG_ERROR_BANNER).assertIsDisplayed()
    }

    @Test
    fun submit_invokes_callback_with_trimmed_inputs() {
        var captured: Triple<String, String, String>? = null
        composeRule.setContent {
            BowPressTheme {
                EmailAuthScreenContent(
                    state = AuthUiState(mode = AuthUiState.Mode.SIGN_IN),
                    onCancel = {},
                    onModeChange = {},
                    onSubmit = { n, e, p -> captured = Triple(n, e, p) },
                    onErrorDismiss = {},
                )
            }
        }

        composeRule.onNodeWithTag(TAG_EMAIL_FIELD).performTextInput("a@b.com")
        composeRule.onNodeWithTag(TAG_PASSWORD_FIELD).performTextInput("longpassword")
        composeRule.onNodeWithTag(TAG_SUBMIT_BUTTON).performClick()

        assertThat(captured).isNotNull()
        assertThat(captured!!.second).isEqualTo("a@b.com")
        assertThat(captured!!.third).isEqualTo("longpassword")
    }
}
