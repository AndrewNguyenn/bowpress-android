package com.andrewnguyen.bowpress

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.core.graphics.toColorInt
import com.andrewnguyen.bowpress.core.designsystem.BowPressTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Kenrokuen is light-mode only — force the status + nav bars to the
        // AppPaper ground so the system chrome blends into the first row of
        // the splash / scaffold. `SystemBarStyle.light(...)` asks Android to
        // draw dark icons on top, which is what we want on a paper surface.
        val paperScrim = AppPaperArgb
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(paperScrim, paperScrim),
            navigationBarStyle = SystemBarStyle.light(paperScrim, paperScrim),
        )
        setContent {
            // BowPressTheme moved inside BowPressApp so it can subscribe to
            // the user's ThemePreference flow (DataStore-backed) and switch
            // light ↔ Yofuke dark at runtime. The testTag→resource-id opt-in
            // wraps the whole tree regardless of theme.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { testTagsAsResourceId = true },
            ) {
                BowPressApp()
            }
        }
    }

    private companion object {
        // Mirrors core-designsystem/Color.kt AppPaper (#EEF2EC). Duplicated
        // here because enableEdgeToEdge runs before Compose theming is set
        // up — we only need the raw argb int.
        val AppPaperArgb: Int = "#EEF2EC".toColorInt()
    }
}
