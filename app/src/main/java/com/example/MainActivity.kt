package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.data.firebase.configureAppCheck
import com.example.ui.screens.NileshSnapchatApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // STEP 1: Configure App Check with launch intent before setContent / ViewModels
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        setContent {
            NileshSnapchatApp()
        }
    }
}
