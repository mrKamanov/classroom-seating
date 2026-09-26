package com.example.classroomseating.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Локальные настройки: активный класс редактирования.
 */
@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val ACTIVE_CLASS_ID = stringPreferencesKey("active_class_id")
    }

    val activeClassId: Flow<String?> =
        context.appDataStore.data.map { it[Keys.ACTIVE_CLASS_ID] }

    suspend fun setActiveClassId(classId: String) {
        context.appDataStore.edit { it[Keys.ACTIVE_CLASS_ID] = classId }
    }
}