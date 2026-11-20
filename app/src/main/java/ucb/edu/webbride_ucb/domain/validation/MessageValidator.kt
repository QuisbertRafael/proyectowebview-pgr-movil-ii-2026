package ucb.edu.webbride_ucb.domain.validation

/** Validación pura; puede probarse sin Android. */
object MessageValidator {
    const val MAX_LENGTH = 2000
    fun isValid(text: String): Boolean = text.trim().length in 1..MAX_LENGTH
}
