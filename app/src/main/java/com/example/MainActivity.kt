package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.MainScreen
import com.example.ui.MainViewModel
import com.example.ui.components.SetupWizardScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ObsidianDark

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ObsidianDark
                ) {
                    val setupState by viewModel.setupWizardState.collectAsState()

                    Crossfade(
                        targetState = setupState.isFirstBootRequired,
                        label = "setup_crossfade"
                    ) { isFirstBoot ->
                        if (isFirstBoot) {
                            SetupWizardScreen(
                                state = setupState,
                                onStartSetup = { viewModel.startSetupDownloadPipeline() },
                                onCompleteSetup = { viewModel.completeSetupWizard() }
                            )
                        } else {
                            MainScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
