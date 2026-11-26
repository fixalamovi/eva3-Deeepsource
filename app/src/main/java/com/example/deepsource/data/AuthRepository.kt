package com.example.deepsource.data

import android.content.Context
import com.example.deepsource.BuildConfig
import com.example.deepsource.data.remote.*
import com.example.deepsource.domain.Session
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(private val context: Context) {
    fun canRestore(session: Session): Boolean {
        if (session.demo) return true
        if (!session.provider.startsWith("Correo") && !(BuildConfig.USE_SERVER && session.provider == "Google")) return false
        return if (BuildConfig.USE_SERVER)
            session.token.startsWith("ds_") && session.expiresAt > System.currentTimeMillis()
        else !session.token.startsWith("ds_")
    }
    suspend fun signIn(email: String, password: String): Session = withContext(Dispatchers.IO) {
        if (BuildConfig.USE_SERVER) projectsCall { BackendClient.projects.login(LoginBody(email, password)) }
        else LocalAccounts(context).use { it.signIn(email, password) }
    }
    internal suspend fun googleChallenge(): GoogleLoginChallenge = withContext(Dispatchers.IO) {
        check(BuildConfig.USE_SERVER) { "El acceso con Google requiere la variante conectada al servidor." }
        projectsCall { BackendClient.projects.googleChallenge() }
    }
    suspend fun loginWithGoogle(idToken: String): Session = withContext(Dispatchers.IO) {
        check(BuildConfig.USE_SERVER) { "El acceso con Google requiere la variante conectada al servidor." }
        projectsCall { BackendClient.projects.loginWithGoogle(GoogleLoginBody(idToken)) }
    }
    suspend fun register(name: String, email: String, password: String): Session = withContext(Dispatchers.IO) {
        if (BuildConfig.USE_SERVER) {
            try { projectsCall { BackendClient.projects.register(RegisterBody(name, email, password)) } }
            catch (e: BackendException) {
                if (e.status == 409) throw LocalEmailInUseException("Ya existe una cuenta con ese correo")
                throw e
            }
        }
        else LocalAccounts(context).use { it.register(name, email, password) }
    }
    suspend fun updateProfile(session: Session, name: String): Session = withContext(Dispatchers.IO) {
        if (session.demo) return@withContext session.copy(name = name.trim())
        if (BuildConfig.USE_SERVER) {
            backendCall { BackendClient.projects.profile(BackendClient.auth(session), NameBody(name)) }
                .copy(token = session.token, refreshToken = session.refreshToken)
        } else {
            LocalAccounts(context).use { it.updateName(session, name) }
            session.copy(name = name.trim())
        }
    }
    suspend fun changePassword(session: Session, current: String, next: String) = withContext(Dispatchers.IO) {
        require(!session.demo) { "Esta función está disponible para cuentas con contraseña" }
        if (BuildConfig.USE_SERVER) {
            try { backendUnit { BackendClient.projects.password(BackendClient.auth(session), PasswordBody(current, next)) } }
            catch (e: BackendException) {
                if (e.status == 400) throw LocalAuthenticationException(e.message.orEmpty())
                throw e
            }
        } else LocalAccounts(context).use { it.changePassword(session, current, next) }
    }
    private suspend fun <T> projectsCall(block: suspend () -> retrofit2.Response<T>): T =
        backendCall("Proyectos (8081)", block)

    suspend fun signOut(session: Session) {
        if (BuildConfig.USE_SERVER && !session.demo) {
            try { backendUnit { BackendClient.projects.logout(BackendClient.auth(session)) } }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { /* La copia local de la sesión se elimina incluso sin conexión. */ }
        }
    }
}
