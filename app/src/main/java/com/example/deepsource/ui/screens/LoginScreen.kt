package com.example.deepsource.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.deepsource.domain.validateLogin
import com.example.deepsource.ui.components.*
import com.example.deepsource.ui.viewmodel.LoginViewModel

@Composable
internal fun LoginScreen(vm: LoginViewModel) {
    var register by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    if (register) {
        ScreenEntrance("register", durationMillis = 360, offsetDivisor = 18) {
            RegisterScreen(vm, onBack = { vm.clearAccessError(); register = false })
        }
        return
    }
    ScreenEntrance("login-form", durationMillis = 360, offsetDivisor = 18) {
        LoginForm(
            busy = vm.busy,
            onLogin = vm::login,
            onDemo = { vm.demo() },
            onRegister = { vm.clearAccessError(); register = true },
            onGoogle = { vm.loginWithGoogle(context) },
            accessError = vm.accessError
        )
    }

}

@Composable
internal fun LoginForm(
    busy: Boolean,
    onLogin: (String, String) -> Unit,
    onDemo: () -> Unit,
    onRegister: () -> Unit,
    onGoogle: (() -> Unit)? = null,
    accessError: String? = null
) {
    var email by rememberSaveable { mutableStateOf("") }
    // Las contraseñas no se guardan en el estado persistente de la Activity.
    var password by remember { mutableStateOf("") }
    var reveal by remember { mutableStateOf(false) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    val errors = validateLogin(email, password)
    val canSubmit = !busy && email.isNotBlank() && password.isNotBlank()

    Column(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding().navigationBarsPadding().imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp, vertical = 36.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Brand(compact = true)
        Spacer(Modifier.height(44.dp))
        Text("Iniciar sesión", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(28.dp))
        Field(
            email, { email = it.take(254) }, "Correo electrónico",
            placeholder = "nombre@correo.com", keyboard = KeyboardType.Email,
            error = if (submitted) errors.email else null,
            leading = "mail", readOnly = busy
        )
        Spacer(Modifier.height(18.dp))
        Field(
            password, { password = it.take(128) }, "Contraseña",
            placeholder = "Tu contraseña", keyboard = KeyboardType.Password,
            error = if (submitted) errors.password else null,
            leading = "lock", readOnly = busy,
            visual = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
            trailing = { PasswordEye(reveal) { reveal = !reveal } }
        )
        Spacer(Modifier.height(28.dp))
        Primary(
            text = if (busy) "Iniciando sesión…" else "Iniciar sesión",
            enabled = canSubmit, loading = busy, animationMillis = 360
        ) {
            submitted = true
            if (errors.valid) onLogin(email.trim(), password)
        }
        AccessError(accessError)
        if (onGoogle != null) {
            Spacer(Modifier.height(12.dp))
            Secondary("Iniciar sesión con Google", enabled = !busy, onClick = onGoogle)
        }
        Spacer(Modifier.height(12.dp))
        TextButton(
            onClick = onRegister,
            enabled = !busy,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Crear una cuenta")
        }
        TextButton(
            onClick = onDemo,
            enabled = !busy,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Explorar demostración", color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
internal fun AccessError(message: String?) {
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            shape = MaterialTheme.shapes.small
        ) {
            Text(message.orEmpty(), modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium)
        }
    }
}
