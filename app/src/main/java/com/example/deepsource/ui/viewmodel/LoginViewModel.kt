package com.example.deepsource.ui.viewmodel

import android.content.Context
import android.content.MutableContextWrapper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.deepsource.AppState
import com.example.deepsource.BuildConfig
import com.example.deepsource.data.remote.BackendConnectionException
import com.example.deepsource.data.remote.BackendException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.CancellationException
import com.example.deepsource.data.LocalEmailInUseException
import com.example.deepsource.domain.Session
import kotlinx.coroutines.CoroutineScope

// Puerto pequeño: el ViewModel se prueba sin Application, SQLite ni cuentas reales.
internal interface LoginActions {
    val busy: Boolean
    fun execute(scope: CoroutineScope, action: suspend () -> Unit)
    suspend fun signIn(email: String, password: String): Session
    suspend fun register(name: String, email: String, password: String): Session
    suspend fun startSession(session: Session)
}

private class AppLoginActions(private val app: AppState) : LoginActions {
    override val busy get() = app.busy
    override fun execute(scope: CoroutineScope, action: suspend () -> Unit) = app.launchAction(scope, action)
    override suspend fun signIn(email: String, password: String) = app.auth.signIn(email, password)
    override suspend fun register(name: String, email: String, password: String) = app.auth.register(name, email, password)
    override suspend fun startSession(session: Session) = app.startSession(session)
}

internal class LoginViewModel(
    private val actions: LoginActions,
    private val app: AppState? = null
) : ViewModel() {
    constructor(app: AppState) : this(AppLoginActions(app), app)
    val busy get() = actions.busy
    var accessError by mutableStateOf<String?>(null)
        private set

    fun clearAccessError() { accessError = null }

    fun loginWithGoogle(context: Context) {
        val state = app ?: return
        if (!BuildConfig.USE_SERVER) {
            accessError = "Usa la variante serverDebug o serverRelease para entrar con Google."
            return
        }
        actions.execute(viewModelScope) {
            accessError = null
            try {
                val challenge = state.auth.googleChallenge()
                val option = GetSignInWithGoogleOption.Builder(challenge.clientId)
                    .setNonce(challenge.nonce)
                    .build()
                val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
                val credential = CredentialManager.create(context).getCredential(
                    context = MutableContextWrapper(context),
                    request = request
                ).credential
                check(credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) { "Google no devolvió una credencial válida." }
                val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
                actions.startSession(state.auth.loginWithGoogle(token))
            } catch (_: GetCredentialCancellationException) {
                // El usuario cerró el selector de cuentas.
            } catch (_: NoCredentialException) {
                accessError = "Agrega una cuenta de Google y usa un emulador con Google Play."
            } catch (_: GetCredentialException) {
                accessError = "No se pudo abrir Google. Revisa Google Play y la configuración OAuth."
            } catch (_: GoogleIdTokenParsingException) {
                accessError = "Google devolvió una credencial que no se pudo leer. Intenta nuevamente."
            } catch (error: BackendConnectionException) {
                accessError = error.message
            } catch (error: BackendException) {
                accessError = error.message
            }
        }
    }

    fun login(email: String, password: String) = actions.execute(viewModelScope) {
        accessError = null
        try {
            actions.startSession(actions.signIn(email, password))
        } catch (error: BackendConnectionException) {
            accessError = error.message
        } catch (error: BackendException) {
            accessError = error.message
        }
    }

    fun register(name: String, email: String, password: String, onEmailInUse: (String) -> Unit) =
        actions.execute(viewModelScope) {
            accessError = null
            val account = try {
                actions.register(name, email, password)
            } catch (error: LocalEmailInUseException) {
                onEmailInUse(error.message.orEmpty())
                return@execute
            } catch (error: BackendConnectionException) {
                accessError = error.message
                return@execute
            } catch (error: BackendException) {
                accessError = error.message
                return@execute
            }
            try {
                actions.startSession(account)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                accessError = "Tu cuenta se creó. No se pudieron cargar tus datos. " +
                    if (error is BackendConnectionException) error.message.orEmpty() + " Luego inicia sesión."
                    else "Vuelve al inicio de sesión e inténtalo de nuevo."
            }
        }

    fun demo() = actions.execute(viewModelScope) {
        actions.startSession(
            Session("demo_luis", "Luis Gómez", "luis.gomez@deepsource.demo", "DEV", true, "Demostración")
        )
    }
}
