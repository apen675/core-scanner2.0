package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.AvionicsScreen
import com.example.ui.theme.JetBlack
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AvionicsViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: AvionicsViewModel by viewModels {
        AvionicsViewModel.provideFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = JetBlack
                ) {
                    AvionicsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
