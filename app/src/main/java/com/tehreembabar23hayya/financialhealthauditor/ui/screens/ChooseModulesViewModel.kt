package com.tehreembabar23hayya.financialhealthauditor.ui.screens

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ChooseModulesViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    private val allModuleIds = setOf(1, 2, 3, 4, 5)

    private val _selectedModuleIds = MutableStateFlow<Set<Int>>(allModuleIds)
    val selectedModuleIds: StateFlow<Set<Int>> = _selectedModuleIds.asStateFlow()

    init {
        loadSelectedModules()
    }

    private fun loadSelectedModules() {
        val savedSet = prefs.getStringSet(PREF_KEY_ENABLED_MODULES, null)
        if (savedSet == null) {
            _selectedModuleIds.value = allModuleIds
        } else {
            _selectedModuleIds.value = savedSet.mapNotNull { it.toIntOrNull() }.toSet()
        }
    }

    fun toggleModule(moduleId: Int) {
        _selectedModuleIds.update { current ->
            if (current.contains(moduleId)) {
                current - moduleId
            } else {
                current + moduleId
            }
        }
    }

    fun saveAndContinue(onNavigate: () -> Unit) {
        val stringSet = _selectedModuleIds.value.map { it.toString() }.toSet()
        prefs.edit().putStringSet(PREF_KEY_ENABLED_MODULES, stringSet).apply()
        onNavigate()
    }

    companion object {
        const val PREF_KEY_ENABLED_MODULES = "enabledModules"
    }
}
