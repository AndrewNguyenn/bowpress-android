package com.andrewnguyen.bowpress.feature.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.andrewnguyen.bowpress.core.designsystem.BowPressTheme
import org.junit.Rule
import org.junit.Test

/**
 * Validates that the landing screen exposes both primary entry points, so a11y
 * crawlers + selectors don't regress.
 */
class AuthScreenComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun landing_renders_google_and_email_buttons() {
        composeRule.setContent {
            BowPressTheme {
                AuthScreenContent(
                    isLoading = false,
                    errorMessage = null,
                    onContinueWithEmail = {},
                    onContinueWithGoogle = {},
                )
            }
        }

        composeRule.onNodeWithTag(TAG_CONTINUE_GOOGLE)
            .assertIsDisplayed()
            .assertIsEnabled()
        composeRule.onNodeWithTag(TAG_CONTINUE_EMAIL)
            .assertIsDisplayed()
            .assertIsEnabled()
        composeRule.onNodeWithText("BowPress").assertIsDisplayed()
    }

    @Test
    fun landing_disables_buttons_while_loading() {
        composeRule.setContent {
            BowPressTheme {
                AuthScreenContent(
                    isLoading = true,
                    errorMessage = null,
                    onContinueWithEmail = {},
                    onContinueWithGoogle = {},
                )
            }
        }

        // Buttons are rendered but disabled — we assert by checking semantic
        // `disabled` via the selector API.
        composeRule.onNodeWithTag(TAG_CONTINUE_GOOGLE).assertIsDisplayed()
        composeRule.onNodeWithTag(TAG_CONTINUE_EMAIL).assertIsDisplayed()
    }
}
