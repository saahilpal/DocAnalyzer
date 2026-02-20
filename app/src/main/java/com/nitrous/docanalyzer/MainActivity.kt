package com.nitrous.docanalyzer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.nitrous.docanalyzer.ui.screens.ChatScreen
import com.nitrous.docanalyzer.ui.theme.DocAnalyzerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Disable adjustResize behavior for root to allow manual IME handling
        enableEdgeToEdge()
        setContent {
            DocAnalyzerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    ChatScreen()
                }
            }
        }
    }
}
