package com.example.deepsource

import com.example.deepsource.data.LocalEmailInUseException
import com.example.deepsource.data.remote.BackendConnectionException
import java.io.IOException
import com.example.deepsource.domain.Session
import com.example.deepsource.ui.viewmodel.LoginActions
import com.example.deepsource.ui.viewmodel.LoginViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class LoginViewModelTest {
    private class FakeActions : LoginActions {
        override var busy = false
        val account = Session("srv_test", "Persona Prueba", "persona@example.com", token = "ds_test")
        val started = mutableListOf<Session>()
        var input = emptyList<String>()
        var failure: Throwable? = null
        var error: Throwable? = null
        var loadFailure: Throwable? = null
        override fun execute(scope: CoroutineScope, action: suspend () -> Unit) {
            if (busy) return
            busy = true
            try { runBlocking { action() } } catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = e } finally { busy = false }
        }
        override suspend fun signIn(email: String, password: String): Session {
            input = listOf(email, password)
            failure?.let { throw it }
            return account
        }
        override suspend fun register(name: String, email: String, password: String): Session {
            input = listOf(name, email, password)
            failure?.let { throw it }
            return account
        }
        override suspend fun startSession(session: Session) {
            loadFailure?.let { throw it }
            started += session
        }
    }
    private val actions = FakeActions()
    private val vm = LoginViewModel(actions)

    @Test fun successfulLoginStartsTheReturnedSession() {
        vm.login("persona@example.com", "Clave123")
        assertEquals(listOf("persona@example.com", "Clave123"), actions.input)
        assertEquals(listOf(actions.account), actions.started)
        assertFalse(vm.busy)
    }
    @Test fun rejectedLoginDoesNotStartASession() {
        actions.failure = IllegalStateException("Correo o contraseña incorrectos")
        vm.login("persona@example.com", "incorrecta")
        assertTrue(actions.started.isEmpty())
        assertSame(actions.failure, actions.error)
        assertFalse(vm.busy)
    }
    @Test fun registrationStartsTheCreatedSession() {
        var duplicate = false
        vm.register("Persona Prueba", "persona@example.com", "Clave123") { duplicate = true }
        assertEquals(listOf("Persona Prueba", "persona@example.com", "Clave123"), actions.input)
        assertEquals(listOf(actions.account), actions.started)
        assertFalse(duplicate)
    }
    @Test fun duplicateEmailIsReportedAtTheEmailField() {
        actions.failure = LocalEmailInUseException("Este correo ya existe")
        var message: String? = null
        vm.register("Persona Prueba", "persona@example.com", "Clave123") { message = it }
        assertEquals("Este correo ya existe", message)
        assertTrue(actions.started.isEmpty())
        assertNull(actions.error)
    }
    @Test fun otherRegistrationErrorsAreNotLabeledAsDuplicateEmail() {
        actions.failure = IllegalStateException("Servidor no disponible")
        var duplicate = false
        vm.register("Persona Prueba", "persona@example.com", "Clave123") { duplicate = true }
        assertFalse(duplicate)
        assertSame(actions.failure, actions.error)
    }
    @Test fun registrationConnectionFailureNamesTheAccountService() {
        actions.failure = BackendConnectionException("Proyectos (8081)", IOException())
        vm.register("Persona Prueba", "persona@example.com", "Clave123") { fail(it) }

        assertTrue(vm.accessError.orEmpty().contains("Proyectos (8081)"))
        assertFalse(vm.accessError.orEmpty().contains("Tu cuenta se creó"))
    }

    @Test fun accountCreationSuccessIsPreservedWhenLoadingRecordsFails() {
        actions.loadFailure = BackendConnectionException("Registros (8082)", IOException())
        vm.register("Persona Prueba", "persona@example.com", "Clave123") { fail(it) }

        assertTrue(vm.accessError.orEmpty().contains("Tu cuenta se creó"))
        assertTrue(vm.accessError.orEmpty().contains("Registros (8082)"))
    }

    @Test fun demoStartsWithoutCallingAuthentication() {
        actions.failure = IllegalStateException("No hay servidor")
        vm.demo()
        assertTrue(actions.input.isEmpty())
        assertTrue(actions.started.single().demo)
        assertEquals("demo", actions.started.single().workspace)
        assertEquals("Demostración", actions.started.single().provider)
    }
    @Test fun loadingPreventsDuplicateLoginRequests() {
        actions.busy = true
        assertTrue(vm.busy)
        vm.login("persona@example.com", "Clave123")
        assertTrue(actions.input.isEmpty())
    }
    @Test fun coroutineCancellationIsPreserved() {
        val cancellation = CancellationException("screen closed")
        actions.failure = cancellation
        assertSame(cancellation, runCatching { vm.login("persona@example.com", "Clave123") }.exceptionOrNull())
        assertTrue(actions.started.isEmpty())
        assertFalse(vm.busy)
    }
}
