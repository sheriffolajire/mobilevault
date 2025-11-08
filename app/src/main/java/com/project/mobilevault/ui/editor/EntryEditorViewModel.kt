package com.project.mobilevault.ui.editor

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.mobilevault.di.ServiceLocator
import kotlinx.coroutines.launch

class EntryEditorViewModel : ViewModel() {
    data class UiState(val title: String = "", val content: String = "", val isLoading: Boolean = false)
    var state = mutableStateOf(UiState()); private set

    fun load(context: Context, id: Long?) {
        if (id == null) return
        val repo = ServiceLocator.vaultRepo(context)
        viewModelScope.launch {
            state.value = state.value.copy(isLoading = true)
            val pair = repo.getDecrypted(id)
            if (pair != null) {
                state.value = state.value.copy(title = pair.first.title, content = pair.second, isLoading = false)
            } else state.value = state.value.copy(isLoading = false)
        }
    }

    fun save(context: Context, id: Long?, title: String, content: String, onDone: () -> Unit) {
        val repo = ServiceLocator.vaultRepo(context)
        viewModelScope.launch {
            state.value = state.value.copy(isLoading = true)
            repo.upsertEncrypted(id, title, content)
            state.value = state.value.copy(isLoading = false)
            onDone()
        }
    }
}
