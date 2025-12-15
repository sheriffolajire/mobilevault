package com.project.mobilevault.ui.vault

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.mobilevault.data.db.VaultEntry
import com.project.mobilevault.di.ServiceLocator
import com.project.mobilevault.ui.login.LoginActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class VaultListViewModel : ViewModel() {
    data class UiState(
        val items: List<VaultEntry> = emptyList(),
        val isLoading: Boolean = true
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    fun load(context: Context) {
        val repo = ServiceLocator.vaultRepo(context)
        viewModelScope.launch {
            repo.entries()
                .onStart { _state.value = _state.value.copy(isLoading = true) }
                .collect { list -> _state.value = UiState(items = list, isLoading = false) }
        }
    }

    fun logout(context: Context) {
        ServiceLocator.session().clear()
        context.startActivity(Intent(context, LoginActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
    }
}