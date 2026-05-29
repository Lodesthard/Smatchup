package com.example.smatchup.i18n

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * App-managed language selection (independent of the system locale).
 *
 * The chosen language is persisted in SharedPreferences and applied by wrapping the base
 * context in [MainActivity.attachBaseContext]. Changing the language calls Activity.recreate(),
 * which re-runs attachBaseContext so Compose `stringResource` picks up the new locale.
 *
 * Self-managed (no AppCompat) to stay compatible with the Compose-only, minSdk 24 setup.
 */
object LocaleManager {

    const val EN = "en"
    const val FR = "fr"
    val SUPPORTED = listOf(EN, FR)

    private const val PREFS = "smatchup_prefs"
    private const val KEY_LANG = "app_language"

    /** Returns the persisted language, or the device default narrowed to a supported one. */
    fun getLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val stored = prefs.getString(KEY_LANG, null)
        return stored ?: deviceDefault()
    }

    fun setLanguage(context: Context, language: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANG, language)
            .apply()
    }

    /** Returns a context whose resources resolve against the selected language. */
    fun wrap(context: Context): Context {
        val locale = Locale(getLanguage(context))
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    private fun deviceDefault(): String {
        val lang = Locale.getDefault().language
        return if (lang == FR) FR else EN
    }
}
