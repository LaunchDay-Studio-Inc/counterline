package dev.counterline

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import dev.counterline.core.designsystem.theme.CounterLineTheme
import dev.counterline.navigation.CounterLineApp

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CounterLineTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CounterLineApp()
                }
            }
        }
    }
}
