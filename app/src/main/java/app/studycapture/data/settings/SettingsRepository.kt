package app.studycapture.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Locale

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class Settings(
    /** Run the Russian/Cyrillic OCR engine in addition to the Latin one. */
    val cyrillicOcr: Boolean,
    val themeMode: ThemeMode,
)

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val cyrillicKey = booleanPreferencesKey("cyrillic_ocr")
    private val themeKey = stringPreferencesKey("theme_mode")

    private val cyrillicDefault: Boolean
        get() = Locale.getDefault().language in setOf("ru", "uk", "be", "kk", "bg", "sr")

    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            cyrillicOcr = prefs[cyrillicKey] ?: cyrillicDefault,
            themeMode = prefs[themeKey]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
        )
    }

    suspend fun current(): Settings = settings.first()

    suspend fun setCyrillicOcr(enabled: Boolean) {
        context.dataStore.edit { it[cyrillicKey] = enabled }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[themeKey] = mode.name }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
