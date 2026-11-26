package com.example.deepsource

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextReplacement
import com.example.deepsource.domain.validateRegistration
import com.example.deepsource.ui.components.Field
import com.example.deepsource.ui.theme.DeepsourceTheme
import org.junit.Rule
import org.junit.Test

class FieldValidationUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun registrationErrorAppearsWithoutTappingAnIconAndClearsAfterCorrection() {
        var email by mutableStateOf("correo-invalido")
        compose.setContent {
            DeepsourceTheme {
                val errors = validateRegistration("Usuario Prueba", email, "Clave1234", "Clave1234")
                Field(email, { email = it }, "Correo electrónico", error = errors.email)
            }
        }
        compose.onNodeWithText("Ingresa un correo válido").assertIsDisplayed()
        compose.onNodeWithContentDescription("Correo electrónico")
            .performTextReplacement("usuario@example.com")
        compose.waitForIdle()
        compose.onNodeWithText("Ingresa un correo válido").assertDoesNotExist()
    }

    @Test fun passwordMismatchExplainsWhyRegistrationCannotContinue() {
        compose.setContent {
            DeepsourceTheme {
                val errors = validateRegistration("Usuario Prueba", "usuario@example.com", "Clave1234", "Otra1234")
                Field("Otra1234", {}, "Repite la contraseña", error = errors.confirmation)
            }
        }
        compose.onNodeWithText("Las contraseñas no coinciden").assertIsDisplayed()
    }
}
