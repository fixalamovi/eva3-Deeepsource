package com.example.deepsource

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

internal class NavigationViewModel(application: Application) : AndroidViewModel(application) {
    val state = AppState(application)
    var loading by mutableStateOf(true)
        private set

    init {
        viewModelScope.launch {
            try {
                state.restoreSession()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                state.message = "No pudimos abrir los datos. Vuelve a iniciar sesión"
            } finally {
                loading = false
            }
        }
    }

    fun refresh() {
        if (!loading && state.session != null) {
            state.launchAction(viewModelScope) { state.refresh() }
        }
    }

    override fun onCleared() {
        state.hours.close()
        super.onCleared()
    }
}
