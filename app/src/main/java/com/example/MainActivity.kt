package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AppNavigation
import com.example.ui.screens.SetupScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.FinanceViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: FinanceViewModel by viewModels()
    private var sharedText by mutableStateOf<String?>(null)
    private var sharedUriStr by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        handleIntent(intent)
        
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val isSetupComplete by viewModel.isSetupComplete.collectAsStateWithLifecycle()
                var showSplash by remember { mutableStateOf(true) }
                
                Crossfade(
                    targetState = showSplash,
                    animationSpec = tween(durationMillis = 600),
                    label = "splashCrossfade"
                ) { isSplashing ->
                    if (isSplashing) {
                        SplashScreen(
                            onSplashFinished = {
                                showSplash = false
                            }
                        )
                    } else {
                        if (isSetupComplete) {
                            AppNavigation(viewModel, sharedText, sharedUriStr)
                        } else {
                            SetupScreen(viewModel)
                        }
                    }
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }
    
    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SEND -> {
                sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: intent.getStringExtra(Intent.EXTRA_SUBJECT)
                val uri = intent.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM)
                if (uri != null) {
                    sharedUriStr = uri.toString()
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = intent.getParcelableArrayListExtra<android.net.Uri>(Intent.EXTRA_STREAM)
                if (!uris.isNullOrEmpty()) {
                    sharedUriStr = uris.first().toString()
                }
                sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            }
        }
    }
}
