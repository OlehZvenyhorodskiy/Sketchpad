package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.runtime.collectAsState
import com.example.data.repository.CanvasRepository
import com.example.data.repository.UserPreferencesRepository
import com.example.ui.auth.LoginScreen
import com.example.ui.editor.CanvasEditorScreen
import com.example.ui.editor.CanvasEditorViewModel
import com.example.ui.home.HomeScreen
import com.example.ui.home.HomeViewModel
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val userPrefsRepository = remember { UserPreferencesRepository(context) }
            val themeStyleOrdinal = userPrefsRepository.themeStyle.collectAsState(initial = 0).value
            val accentColorArgb = userPrefsRepository.accentColor.collectAsState(initial = 0xFF38BDF8.toInt()).value

            val style = com.example.ui.theme.AppThemeStyle.entries.getOrElse(themeStyleOrdinal) {
                com.example.ui.theme.AppThemeStyle.SYSTEM_DEFAULT
            }
            val accentColor = androidx.compose.ui.graphics.Color(accentColorArgb)

            MyApplicationTheme(
                themeStyle = style,
                accentColor = accentColor
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SketchpadApp(userPrefsRepository = userPrefsRepository)
                }
            }
        }
    }
}

@Composable
fun SketchpadApp(userPrefsRepository: UserPreferencesRepository) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val repository = remember { CanvasRepository(context) }
    val isLoggedIn = userPrefsRepository.isLoggedIn.collectAsState(initial = false).value
    val homeViewModel = remember { HomeViewModel(repository, userPrefsRepository) }

    val themeStyleOrdinal = userPrefsRepository.themeStyle.collectAsState(initial = 0).value
    val accentColorArgb = userPrefsRepository.accentColor.collectAsState(initial = 0xFF38BDF8.toInt()).value
    val isLeftHanded = userPrefsRepository.leftHandedMode.collectAsState(initial = false).value

    val scope = androidx.compose.runtime.rememberCoroutineScope()

    val startDestination = if (isLoggedIn) "home" else "login"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(
                userPreferencesRepository = userPrefsRepository,
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onSkip = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            HomeScreen(
                viewModel = homeViewModel,
                onCanvasClick = { canvasId ->
                    navController.navigate("editor/$canvasId")
                },
                onOpenThemeSettings = {
                    navController.navigate("theme_settings")
                }
            )
        }

        composable("theme_settings") {
            com.example.ui.home.ThemeSettingsScreen(
                currentThemeOrdinal = themeStyleOrdinal,
                currentAccentArgb = accentColorArgb,
                isLeftHanded = isLeftHanded,
                onThemeSelected = { style ->
                    scope.launch {
                        userPrefsRepository.setThemeStyle(style)
                    }
                },
                onAccentColorChanged = { colorInt ->
                    scope.launch {
                        userPrefsRepository.setAccentColor(colorInt)
                    }
                },
                onLeftHandedChanged = { enabled ->
                    scope.launch {
                        userPrefsRepository.setLeftHandedMode(enabled)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "editor/{canvasId}",
            arguments = listOf(navArgument("canvasId") { type = NavType.StringType })
        ) { backStackEntry ->
            val canvasId = backStackEntry.arguments?.getString("canvasId") ?: return@composable
            val editorViewModel = remember(canvasId) {
                CanvasEditorViewModel(repository, canvasId, context)
            }
            CanvasEditorScreen(
                viewModel = editorViewModel,
                onBackClick = { navController.popBackStack() },
                onOpenThemeSettings = { navController.navigate("theme_settings") }
            )
        }
    }
}

