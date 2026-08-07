package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.data.models.CanvasReferenceCaptureSession
import com.example.data.models.CanvasReferenceNavigationRequest
import com.example.localization.AppLanguage
import com.example.localization.AppLocaleManager
import com.example.ui.editor.CanvasEditorScreen
import com.example.ui.editor.CanvasEditorViewModel
import com.example.ui.home.HomeScreen
import com.example.ui.home.HomeViewModel
import com.example.ui.localization.LanguageSetupScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocaleManager.wrap(newBase))
    }

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
                    if (AppLocaleManager.hasSelectedLanguage(this@MainActivity)) {
                        SketchpadApp(
                            userPrefsRepository = userPrefsRepository,
                            currentLanguage = AppLocaleManager.currentLanguage(this@MainActivity),
                            onLanguageSelected = { language ->
                                AppLocaleManager.setLanguage(this@MainActivity, language)
                                recreate()
                            }
                        )
                    } else {
                        LanguageSetupScreen { language ->
                            AppLocaleManager.setLanguage(this@MainActivity, language)
                            recreate()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SketchpadApp(
    userPrefsRepository: UserPreferencesRepository,
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val repository = remember { CanvasRepository(context) }
    val homeViewModel = remember { HomeViewModel(repository, userPrefsRepository) }

    val themeStyleOrdinal = userPrefsRepository.themeStyle.collectAsState(initial = 0).value
    val accentColorArgb = userPrefsRepository.accentColor.collectAsState(initial = 0xFF38BDF8.toInt()).value
    val isLeftHanded = userPrefsRepository.leftHandedMode.collectAsState(initial = false).value
    val palmRejectionEnabled = userPrefsRepository.palmRejectionEnabled.collectAsState(initial = true).value
    val pixelModeEnabled = userPrefsRepository.pixelModeEnabled.collectAsState(initial = true).value

    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var pendingReferenceCapture by remember { mutableStateOf<CanvasReferenceCaptureSession?>(null) }
    var pendingReferenceNavigation by remember { mutableStateOf<CanvasReferenceNavigationRequest?>(null) }

    NavHost(navController = navController, startDestination = "home") {
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
                palmRejectionEnabled = palmRejectionEnabled,
                pixelModeEnabled = pixelModeEnabled,
                currentLanguage = currentLanguage,
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
                onPalmRejectionChanged = { enabled ->
                    scope.launch {
                        userPrefsRepository.setPalmRejectionEnabled(enabled)
                    }
                },
                onPixelModeChanged = { enabled ->
                    scope.launch {
                        userPrefsRepository.setPixelModeEnabled(enabled)
                    }
                },
                onLanguageSelected = onLanguageSelected,
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
                onBackClick = {
                    pendingReferenceCapture = null
                    pendingReferenceNavigation = null
                    navController.popBackStack()
                },
                onOpenThemeSettings = { navController.navigate("theme_settings") },
                referenceCaptureSession = pendingReferenceCapture?.takeIf {
                    it.destination.canvasId == canvasId
                },
                referenceNavigationRequest = pendingReferenceNavigation?.takeIf {
                    it.canvasId == canvasId
                },
                onReferenceCaptureStarted = { session ->
                    pendingReferenceNavigation = null
                    pendingReferenceCapture = session
                    if (session.destination.canvasId != canvasId) {
                        navController.navigate("editor/${session.destination.canvasId}")
                    }
                },
                onReferenceCaptureFinished = {
                    pendingReferenceCapture = null
                },
                onOpenCanvasReference = { request ->
                    pendingReferenceCapture = null
                    pendingReferenceNavigation = request
                    if (request.canvasId != canvasId) {
                        navController.navigate("editor/${request.canvasId}")
                    }
                },
                onReferenceNavigationConsumed = {
                    pendingReferenceNavigation = null
                }
            )
        }
    }
}

