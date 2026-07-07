package app.aventurine.jetmapdemo.data.dataStore

import androidx.datastore.preferences.core.stringPreferencesKey

object PreferencesKey {
    val version = stringPreferencesKey(name = "version")
}