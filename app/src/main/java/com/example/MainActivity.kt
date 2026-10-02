package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.AppDatabase
import com.example.data.repository.RoletaRepository
import com.example.ui.AdminViewModel
import com.example.ui.AdminViewModelFactory
import com.example.ui.RoletaViewModel
import com.example.ui.RoletaViewModelFactory
import com.example.ui.screens.AdminLoginDialog
import com.example.ui.screens.AdminMainScreen
import com.example.ui.screens.PublicRoletaScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getInstance(applicationContext)
        val repository = RoletaRepository(database)

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F172A)
                ) {
                    RoletaApp(repository = repository)
                }
            }
        }
    }
}

@Composable
fun RoletaApp(repository: RoletaRepository) {
    val roletaViewModel: RoletaViewModel = viewModel(
        factory = RoletaViewModelFactory(repository)
    )
    val adminViewModel: AdminViewModel = viewModel(
        factory = AdminViewModelFactory(repository)
    )

    val adminState by adminViewModel.uiState.collectAsState()
    var currentScreen by remember { mutableStateOf("roleta") } // "roleta" or "admin"
    var showLoginDialog by remember { mutableStateOf(false) }

    when (currentScreen) {
        "admin" -> {
            AdminMainScreen(
                viewModel = adminViewModel,
                onBackToRoleta = {
                    currentScreen = "roleta"
                }
            )
        }
        else -> {
            PublicRoletaScreen(
                viewModel = roletaViewModel,
                onNavigateToAdmin = {
                    if (adminState.isAuthenticated) {
                        currentScreen = "admin"
                    } else {
                        showLoginDialog = true
                    }
                }
            )
        }
    }

    if (showLoginDialog) {
        AdminLoginDialog(
            onDismiss = { showLoginDialog = false },
            onAuthenticate = { pin ->
                val success = adminViewModel.authenticate(pin)
                if (success) {
                    showLoginDialog = false
                    currentScreen = "admin"
                }
                success
            }
        )
    }
}
