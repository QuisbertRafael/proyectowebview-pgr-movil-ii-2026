package ucb.edu.webbride_ucb.domain.validation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageValidatorTest {
    @Test fun rejectsEmptyAndSpaces() {
        assertFalse(MessageValidator.isValid(""))
        assertFalse(MessageValidator.isValid("   "))
    }
    @Test fun acceptsValidMessage() {
        assertTrue(MessageValidator.isValid("Hola desde Kotlin"))
        assertTrue(MessageValidator.isValid("a".repeat(2000)))
    }
    @Test fun rejectsTooLong() {
        assertFalse(MessageValidator.isValid("a".repeat(2001)))
    }
}
