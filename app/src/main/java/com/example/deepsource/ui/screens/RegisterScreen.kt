package com.example.deepsource.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.deepsource.domain.validateRegistration
import com.example.deepsource.ui.components.*
import com.example.deepsource.ui.viewmodel.LoginViewModel

@Composable
internal fun RegisterScreen(vm: LoginViewModel, onBack: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmation by remember { mutableStateOf(false) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var emailInUse by remember { mutableStateOf<String?>(null) }
    val errors = validateRegistration(name, email, password, confirm)

    BackHandler { if (!vm.busy) onBack() }
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .statusBarsPadding().navigationBarsPadding().imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        TextButton(onClick = onBack, enabled = !vm.busy, contentPadding = PaddingValues(0.dp)) {
            Glyph("back")
            Spacer(Modifier.width(8.dp))
            Text("Iniciar sesión")
        }
        Text("Crear una cuenta", style = MaterialTheme.typography.headlineLarge)
        Field(name, { name = it.take(80) }, "Nombre completo",
            error = if (submitted) errors.name else null, readOnly = vm.busy, leading = "person")
        Field(email, { email = it.take(254); emailInUse = null }, "Correo electrónico",
            placeholder = "nombre@correo.com", keyboard = KeyboardType.Email,
            readOnly = vm.busy, leading = "mail",
            error = if (submitted) errors.email ?: emailInUse else null)
        Field(password, { password = it.take(128) }, "Contraseña",
            keyboard = KeyboardType.Password, readOnly = vm.busy, leading = "lock",
            placeholder = "Al menos 8 caracteres, letras y números",
            visual = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            error = if (submitted) errors.password else null,
            trailing = { PasswordEye(showPassword) { showPassword = !showPassword } })
        Field(confirm, { confirm = it.take(128) }, "Repite la contraseña",
            keyboard = KeyboardType.Password, readOnly = vm.busy, leading = "lock",
            visual = if (showConfirmation) VisualTransformation.None else PasswordVisualTransformation(),
            error = if (submitted) errors.confirmation else null,
            trailing = {
                PasswordEye(showConfirmation, "confirmación de contraseña") {
                    showConfirmation = !showConfirmation
                }
            })
        Text(if (com.example.deepsource.BuildConfig.USE_SERVER) "Crea tu cuenta de Deepsource." else "Cuenta local en este dispositivo.", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Primary(
            text = if (vm.busy) "Creando cuenta…" else "Crear cuenta",
            enabled = !vm.busy, loading = vm.busy
        ) {
            submitted = true
            if (errors.valid) vm.register(name.trim(), email.trim(), password) { emailInUse = it }
        }
        AccessError(vm.accessError)
        TextButton(onClick = onBack, enabled = !vm.busy,
            modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Ya tengo una cuenta")
        }
    }
}
