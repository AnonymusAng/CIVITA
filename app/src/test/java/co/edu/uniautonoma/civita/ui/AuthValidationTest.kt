package co.edu.uniautonoma.civita.ui

import org.junit.Assert.*
import org.junit.Test

class AuthValidationTest {
    @Test fun rejectsMalformedAddresses() {
        listOf("", "joan", "joan@", "joan @mail.co", "joan@mail").forEach { assertFalse(AuthValidation.validEmail(it)) }
    }
    @Test fun trimsEmailWithoutAlteringPassword() {
        assertNull(AuthValidation.error(" joan@example.com ", " contraseña segura ", true))
    }
    @Test fun registerRequiresEightCharacters() {
        assertNotNull(AuthValidation.error("joan@example.com", "1234567", true))
        assertNull(AuthValidation.error("joan@example.com", "12345678", true))
    }
    @Test fun loginAcceptsExistingShorterPasswords() {
        assertNull(AuthValidation.error("joan@example.com", "123456", false))
    }
    @Test fun rejectsBlankPassword() {
        assertNotNull(AuthValidation.error("joan@example.com", "   ", false))
    }
}
