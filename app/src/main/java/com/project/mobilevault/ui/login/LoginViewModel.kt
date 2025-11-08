package com.project.mobilevault.ui.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.mobilevault.di.ServiceLocator
import com.project.mobilevault.repo.SessionKeyHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {
    data class UiState(
        val isInitialized: Boolean = false,
        val error: String? = null,
        val isLoading: Boolean = false
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    // Probe DB to decide whether to show Create vs Unlock when the screen opens
    fun refreshInitialized(context: Context) {
        viewModelScope.launch {
            val auth = ServiceLocator.authRepo(context)
            val init = auth.isInitialized()
            _state.value = _state.value.copy(isInitialized = init)
        }
    }

    fun onSubmit(context: Context, password: String, confirm: String?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val auth = ServiceLocator.authRepo(context)
            val session: SessionKeyHolder = ServiceLocator.session()
            try {
                val init = auth.isInitialized()
                if (!init) {
                    if (confirm.isNullOrBlank() || confirm != password) {
                        _state.value = _state.value.copy(isInitialized = false, isLoading = false, error = "Passwords do not match")
                        return@launch
                    }
                    auth.setupNewPassword(password.toCharArray())
                    // Ensure subsequent visits show Unlock
                    _state.value = _state.value.copy(isInitialized = true)
                }
                val dek = auth.tryLogin(password.toCharArray())
                if (dek == null) {
                    _state.value = _state.value.copy(isInitialized = true, isLoading = false, error = "Invalid password")
                    return@launch
                }
                session.setDek(dek)
                dek.fill(0)
                onSuccess()
            } catch (t: Throwable) {
                _state.value = _state.value.copy(error = t.message, isLoading = false)
            } finally {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }
}
