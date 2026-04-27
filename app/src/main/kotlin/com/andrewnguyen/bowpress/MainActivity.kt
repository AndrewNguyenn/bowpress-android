package com.andrewnguyen.bowpress

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.graphics.toColorInt
import com.andrewnguyen.bowpress.core.designsystem.BowPressTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
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
            BowPressTheme {
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
