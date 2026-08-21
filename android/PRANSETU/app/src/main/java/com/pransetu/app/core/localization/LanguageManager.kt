package com.pransetu.app.core.localization

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

/**
 * Supported language option with native script and English display names.
 */
data class LanguageOption(
    val code: String,
    val nativeName: String,
    val englishName: String,
    val stateOrRegion: String = ""
)

/**
 * Manages runtime locale switching without requiring app reinstall.
 * 
 * Uses AppCompat's per-app language API which works on Android 13+ natively
 * and on older versions through AppCompat and dynamic ConfigurationContext.
 * 
 * Covers all major Indian Scheduled Languages with native script representations.
 */
object LanguageManager {

    /**
     * Complete list of all major Indian languages supported by PRANSETU.
     */
    val supportedLanguages = listOf(
        LanguageOption("en", "English", "English", "National / Global"),
        LanguageOption("or", "ଓଡ଼ିଆ", "Odia", "Odisha"),
        LanguageOption("hi", "हिन्दी", "Hindi", "National / North & Central"),
        LanguageOption("bn", "বাংলা", "Bengali", "West Bengal & Eastern India"),
        LanguageOption("te", "తెలుగు", "Telugu", "Andhra Pradesh & Telangana"),
        LanguageOption("ta", "தமிழ்", "Tamil", "Tamil Nadu & Puducherry"),
        LanguageOption("mr", "मराठी", "Marathi", "Maharashtra"),
        LanguageOption("gu", "ગુજરાતી", "Gujarati", "Gujarat"),
        LanguageOption("kn", "ಕನ್ನಡ", "Kannada", "Karnataka"),
        LanguageOption("ml", "മലയാളം", "Malayalam", "Kerala"),
        LanguageOption("pa", "ਪੰਜਾਬੀ", "Punjabi", "Punjab"),
        LanguageOption("as", "অসমীয়া", "Assamese", "Assam & North East"),
        LanguageOption("ur", "اُردُو", "Urdu", "Pan-India / Jammu & Kashmir")
    )

    /**
     * Changes the app's locale using AppCompatDelegate (per-app language API).
     */
    fun setAppLocale(context: Context, languageCode: String) {
        val localeList = LocaleListCompat.forLanguageTags(languageCode)
        if (localeList.isEmpty) {
            return
        }
        try {
            AppCompatDelegate.setApplicationLocales(localeList)
        } catch (_: Exception) {}
    }

    /**
     * Gets the current app locale code.
     */
    fun getAppLocale(context: Context): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (!locales.isEmpty) {
            locales[0]?.language ?: "en"
        } else {
            context.resources.configuration.locales.get(0)?.language ?: "en"
        }
    }

    /**
     * Applies the given language code to a context for locale-aware configuration.
     */
    fun applyLocale(context: Context, languageCode: String): Context {
        val locale = Locale.forLanguageTag(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        return context.createConfigurationContext(config)
    }

    /**
     * Gets display name for a language code.
     */
    fun getLanguageDisplayName(code: String): String {
        val option = supportedLanguages.find { it.code.equals(code, ignoreCase = true) }
        return if (option != null) "${option.nativeName} (${option.englishName})" else code
    }
}