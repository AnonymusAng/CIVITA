package co.edu.uniautonoma.civita.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import co.edu.uniautonoma.civita.data.AuthRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

data class AuthUiState(
    val initializing: Boolean = true,
    val uid: String? = null,
    val displayName: String? = null,
    val email: String? = null,
    val busy: Boolean = false,
    val error: String? = null,
    val message: String? = null,
)

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = mutableState.asStateFlow()
    init {
        viewModelScope.launch { repository.observeSession().collect { user ->
            mutableState.update { it.copy(initializing = false, uid = user?.uid,
                displayName = user?.displayName, email = user?.email) }
        } }
    }
    fun email(email: String, password: String, register: Boolean) {
        val error = AuthValidation.error(email, password, register)
        if (error != null) { mutableState.update { it.copy(error = error, message = null) }; return }
        execute { if (register) repository.register(email, password) else repository.login(email, password) }
    }
    fun reset(email: String) {
        if (!AuthValidation.validEmail(email)) {
            mutableState.update { it.copy(error = "Escribe tu correo para recuperar la contraseña.", message = null) }; return
        }
        execute { repository.resetPassword(email)
            mutableState.update { it.copy(message = "Si el correo corresponde a una cuenta, recibirás instrucciones para recuperar tu acceso.") }
        }
    }
    fun google(context: Context) = execute { repository.google(context) }
    fun logout(context: Context) {
        repository.logout()
        mutableState.update { AuthUiState(initializing = false) }
        viewModelScope.launch { try { repository.clearGoogleSession(context) } catch (_: Exception) { /* Firebase ya cerró la sesión. */ } }
    }
    fun clearFeedback() { mutableState.update { it.copy(error = null, message = null) } }
    private fun execute(action: suspend () -> Unit) {
        if (state.value.busy) return
        mutableState.update { it.copy(busy = true, error = null, message = null) }
        viewModelScope.launch {
            try { withTimeout(45000) { action() } }
            catch (_: TimeoutCancellationException) { mutableState.update { it.copy(error = "La conexión tardó demasiado. Revisa tu internet y vuelve a intentar.") } }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutableState.update { it.copy(error = authErrorMessage(e)) } }
            finally { mutableState.update { it.copy(busy = false) } }
        }
    }
    class Factory(private val repository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(AuthViewModel::class.java))
            return AuthViewModel(repository) as T
        }
    }
}
object AuthValidation {
    fun validEmail(email: String): Boolean = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(email.trim())
    fun error(email: String, password: String, register: Boolean): String? = when {
        !validEmail(email) -> "Escribe un correo electrónico válido."
        password.isBlank() -> "Escribe tu contraseña."
        register && password.length < 8 -> "Usa una contraseña de al menos 8 caracteres."
        else -> null
    }
}
fun authErrorMessage(error: Exception): String = when (error) {
    is FirebaseNetworkException -> "No pudimos conectar. Comprueba tu conexión a internet y vuelve a intentar."
    is FirebaseAuthException -> when (error.errorCode) {
        "ERROR_INVALID_CREDENTIAL", "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND" -> "El correo o la contraseña no son correctos."
        "ERROR_EMAIL_ALREADY_IN_USE" -> "Este correo ya tiene una cuenta. Inicia sesión o recupera tu contraseña."
        "ERROR_WEAK_PASSWORD" -> "Elige una contraseña más segura."
        "ERROR_OPERATION_NOT_ALLOWED" -> "Este método de acceso aún no está habilitado en Firebase."
        "ERROR_USER_DISABLED" -> "Esta cuenta está deshabilitada. Contacta al administrador."
        "ERROR_TOO_MANY_REQUESTS" -> "Demasiados intentos. Espera unos minutos antes de volver a intentar."
        else -> "No se pudo iniciar sesión. Revisa tus datos y la configuración de Firebase."
    }
    is androidx.credentials.exceptions.GetCredentialCancellationException -> "Se canceló el acceso con Google."
    is androidx.credentials.exceptions.NoCredentialException -> "No encontramos una cuenta Google disponible en este dispositivo."
    is IllegalStateException -> error.message ?: "No se pudo completar el acceso."
    else -> "No se pudo completar el acceso. Revisa tu conexión y vuelve a intentar."
}
