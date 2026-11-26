package com.example.deepsource

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performScrollTo
import com.example.deepsource.ui.screens.LoginForm
import com.example.deepsource.ui.theme.DeepsourceTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class LoginOptionsUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun emailLoginBecomesAvailableOnlyAfterBothFieldsAreFilled() {
        var submitted: Pair<String, String>? = null
        compose.setContent {
            DeepsourceTheme {
                LoginForm(false, { email, password -> submitted = email to password }, {}, {})
            }
        }
        val login = compose.onNode(hasText("Iniciar sesión") and hasClickAction())
        login.assertIsNotEnabled()
        compose.onNodeWithContentDescription("Correo electrónico")
            .performTextReplacement("persona@example.com")
        login.assertIsNotEnabled()
        compose.onNodeWithContentDescription("Contraseña").performTextReplacement("Clave1234")
        login.performScrollTo().assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals("persona@example.com" to "Clave1234", submitted) }
    }

    @Test fun demoAndRegistrationAreAvailableWithoutFillingTheEmailForm() {
        var demoOpened = false
        var registrationOpened = false
        compose.setContent {
            DeepsourceTheme {
                LoginForm(false, { _, _ -> }, { demoOpened = true }, { registrationOpened = true })
            }
        }
        compose.onNodeWithText("Explorar demostración").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText("Crear una cuenta").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertTrue(demoOpened)
            assertTrue(registrationOpened)
        }
    }
}
