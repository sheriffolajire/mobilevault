package com.project.mobilevault.ui.editor

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.mobilevault.di.ServiceLocator
import kotlinx.coroutines.launch

class EntryEditorViewModel : ViewModel() {
    data class UiState(
        val title: String = "",
        val content: String = "",
        val isLoading: Boolean = false,
        val error: String? = null
    )
    var state = mutableStateOf(UiState()); private set

    fun load(context: Context, id: Long?) {
        if (id == null) return
        val repo = ServiceLocator.vaultRepo(context)
        viewModelScope.launch {
            state.value = state.value.copy(isLoading = true, error = null)
            try {
                val pair = repo.getDecrypted(id)
                if (pair != null) {
                    state.value = state.value.copy(title = pair.first.title, content = pair.second, isLoading = false)
                } else {
                    state.value = state.value.copy(isLoading = false, error = "Unable to open entry. It may be corrupted or tampered.")
                }
            } catch (t: Throwable) {
                state.value = state.value.copy(isLoading = false, error = "Failed to decrypt entry: ${t.message}")
            }
        }
    }

    fun save(context: Context, id: Long?, title: String, content: String, onDone: () -> Unit) {
        val repo = ServiceLocator.vaultRepo(context)
        viewModelScope.launch {
            state.value = state.value.copy(isLoading = true, error = null)
            try {
                repo.upsertEncrypted(id, title, content)
                state.value = state.value.copy(isLoading = false)
                onDone()
            } catch (t: Throwable) {
                state.value = state.value.copy(isLoading = false, error = "Failed to save: ${t.message}")
            }
        }
    }
}