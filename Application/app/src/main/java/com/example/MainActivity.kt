package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.data.preferences.TokenDataStore
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.TaskTrackerViewModel

class MainActivity : ComponentActivity() {

    private val tokenDataStore by lazy { TokenDataStore(applicationContext) }

    private val viewModel: TaskTrackerViewModel by viewModels {
        TaskTrackerViewModel.Factory(tokenDataStore)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkTheme by viewModel.themeIsDark.collectAsState()

            MyApplicationTheme(
                darkTheme = isDarkTheme,
                dynamicColor = false
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TaskTrackerApp(viewModel = viewModel)
                }
            }
        }
    }
}
