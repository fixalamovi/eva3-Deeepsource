package com.example.deepsource.ui.viewmodel

import android.net.Uri
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.deepsource.AppState
import com.example.deepsource.data.LocalAuthenticationException
import com.example.deepsource.domain.validateName
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

internal class ProfileViewModel(private val app: AppState) : ViewModel() {
    val session get() = requireNotNull(app.session)
    val profilePhoto get() = app.profilePhoto
    val busy get() = app.busy
    var photoBusy by mutableStateOf(false)
        private set

    fun showMessage(message: String) {
        app.message = message
    }

    fun saveName(name: String) = app.launchAction(viewModelScope) {
        val nameError = validateName(name)
        require(nameError == null) { nameError.orEmpty() }
        app.updateSession(app.auth.updateProfile(session, name))
        showMessage("Perfil actualizado")
        app.updateWidgets()
    }

    fun changePassword(
        current: String,
        next: String,
        onUpdated: () -> Unit,
        onCurrentPasswordError: (String) -> Unit
    ) = app.launchAction(viewModelScope) {
        try {
            app.auth.changePassword(session, current, next)
        } catch (error: LocalAuthenticationException) {
            onCurrentPasswordError(error.message.orEmpty())
            return@launchAction
        }
        showMessage("Contraseña actualizada")
        onUpdated()
    }

    fun updateProfilePhoto(owner: String, source: Uri?, capture: File? = null) {
        if (app.session?.uid != owner || photoBusy) {
            capture?.delete()
            return
        }
        photoBusy = true
        viewModelScope.launch {
            try {
                val updated = withContext(Dispatchers.IO) {
                    if (source == null) {
                        app.photos.remove(owner)
                        null
                    } else {
                        app.photos.save(owner, source)
                    }
                }
                if (app.session?.uid == owner) {
                    app.profilePhoto = updated
                    showMessage(if (source == null) "Foto eliminada" else "Foto de perfil guardada")
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                if (app.session?.uid == owner) showMessage("No pudimos cambiar la foto de perfil")
            } finally {
                capture?.delete()
                photoBusy = false
            }
        }
    }

    fun logout() = app.launchAction(viewModelScope) {
        app.hours.toggleTimer(
            session, System.currentTimeMillis(), SystemClock.elapsedRealtime(), app.boot, pauseOnly = true
        )
        app.auth.signOut(session)
        app.endSession()
    }
}
