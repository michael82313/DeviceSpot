package com.example.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class AppThemeMode(val title: String, val subtitle: String) {
    SYSTEM("Follow System", "Match device dark/light mode"),
    LIGHT("Light Theme", "Always bright & clean"),
    DARK("Dark Theme", "High-contrast dark mode")
}

object ThemePreferences {
    private const val PREFS_NAME = "devicespot_theme_prefs"
    private const val KEY_THEME_MODE = "app_theme_mode"
    private const val KEY_DYNAMIC_COLOR = "dynamic_system_color"

    private val _themeMode = mutableStateOf(AppThemeMode.SYSTEM)
    val themeMode: State<AppThemeMode> = _themeMode

    private val _dynamicColor = mutableStateOf(false)
    val dynamicColor: State<Boolean> = _dynamicColor

    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedMode = prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.name)
        _themeMode.value = try {
            AppThemeMode.valueOf(savedMode ?: AppThemeMode.SYSTEM.name)
        } catch (e: Exception) {
            AppThemeMode.SYSTEM
        }
        _dynamicColor.value = prefs.getBoolean(KEY_DYNAMIC_COLOR, false)
        isInitialized = true
    }

    fun setThemeMode(context: Context, mode: AppThemeMode) {
        _themeMode.value = mode
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME_MODE, mode.name)
            .apply()
    }

    fun setDynamicColor(context: Context, enabled: Boolean) {
        _dynamicColor.value = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DYNAMIC_COLOR, enabled)
            .apply()
    }

    @Composable
    fun resolveIsDark(): Boolean {
        val currentMode = _themeMode.value
        val isSystemDark = isSystemInDarkTheme()
        return when (currentMode) {
            AppThemeMode.SYSTEM -> isSystemDark
            AppThemeMode.LIGHT -> false
            AppThemeMode.DARK -> true
        }
    }
}

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryCyan,
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004F58),
    onPrimaryContainer = Color(0xFF9EEFFE),
    secondary = SecondaryAmber,
    onSecondary = Color(0xFF452B00),
    secondaryContainer = Color(0xFF623F00),
    onSecondaryContainer = Color(0xFFFFDEA3),
    tertiary = TertiaryEmerald,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceVariant = SurfaceVariantDark,
    onBackground = OnSurfaceDark,
    onSurface = OnSurfaceDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryCyanDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6F6FF),
    onPrimaryContainer = Color(0xFF001F24),
    secondary = SecondaryAmber,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFFFDEA3),
    onSecondaryContainer = Color(0xFF261900),
    tertiary = TertiaryEmerald,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceVariant = SurfaceVariantLight,
    onBackground = OnSurfaceLight,
    onSurface = OnSurfaceLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = ThemePreferences.resolveIsDark(),
    dynamicColor: Boolean = ThemePreferences.dynamicColor.value,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    ThemePreferences.init(context)

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
