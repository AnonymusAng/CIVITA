package co.edu.uniautonoma.civita.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import co.edu.uniautonoma.civita.data.ReportRepository
import co.edu.uniautonoma.civita.model.*
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import com.google.firebase.FirebaseNetworkException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CivitaUiState(
    val loading: Boolean = true,
    val categories: List<IncidentCategory> = emptyList(),
    val reports: List<UrbanReport> = emptyList(),
    val config: AppConfig? = null,
    val uid: String? = null,
    val selectedCategoryId: String? = null,
    val fromCache: Boolean = false,
    val error: String? = null,
    val submitting: Boolean = false,
    val publicationError: String? = null,
    val publishedId: String? = null,
) {
    val visibleReports: List<UrbanReport>
        get() = reports.filter { selectedCategoryId == null || it.categoryId == selectedCategoryId }
}

class CivitaViewModel(private val repository: ReportRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(CivitaUiState())
    val state: StateFlow<CivitaUiState> = mutableState.asStateFlow()
    private var observation: Job? = null
    private var pendingDraft: ReportDraft? = null
    private var pendingId: String? = null

    init { reload() }

    fun reload() {
        observation?.cancel()
        observation = viewModelScope.launch {
            mutableState.update { it.copy(loading = true, error = null) }
            try {
                val uid = withTimeout(20000) { repository.signIn() }
                val config = withTimeout(20000) { repository.loadConfig() }
                mutableState.update { it.copy(uid = uid, config = config) }
                combine(repository.observeCategories(), repository.observeReports()) { categories, reports ->
                    categories to reports
                }.collect { (categories, reports) ->
                    mutableState.update { old ->
                        old.copy(loading = false, categories = categories.items, reports = reports.items,
                            fromCache = categories.fromCache || reports.fromCache, error = null)
                    }
                }
            } catch (e: TimeoutCancellationException) { mutableState.update { it.copy(loading = false, submitting = false, error = "La conexión tardó demasiado. Revisa tu internet y reintenta.") } }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutableState.update { it.copy(loading = false, error = message(e)) } }
        }
    }

    fun stopObserving() { observation?.cancel(); observation = null }

    fun selectCategory(id: String?) {
        mutableState.update { it.copy(selectedCategoryId = if (it.selectedCategoryId == id) null else id) }
    }

    fun publish(draft: ReportDraft) {
        if (state.value.submitting) return
        draft.validationError()?.let { error ->
            mutableState.update { it.copy(publicationError = error) }; return
        }
        if (pendingDraft != draft || pendingId == null) {
            pendingDraft = draft
            pendingId = java.util.UUID.randomUUID().toString()
        }
        val requestId = requireNotNull(pendingId)
        mutableState.update { it.copy(submitting = true, publicationError = null, publishedId = null) }
        viewModelScope.launch {
            try {
                val id = withTimeout(30000) { repository.publishReport(draft, requestId) }
                mutableState.update { it.copy(submitting = false, publishedId = id) }
            } catch (e: TimeoutCancellationException) { mutableState.update { it.copy(submitting = false, publicationError = "No recibimos confirmación a tiempo. Reintenta el mismo envío para evitar duplicados.") } }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                mutableState.update { it.copy(submitting = false, publicationError = message(e)) }
            }
        }
    }

    fun clearPublication() {
        if (state.value.submitting) return
        pendingDraft = null; pendingId = null
        mutableState.update { it.copy(publishedId = null, publicationError = null) }
    }

    private fun message(error: Exception): String = when (error) {
        is FirebaseFirestoreException -> when (error.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> "Firestore denegó el acceso. Revisa la sesión y las reglas del proyecto."
            FirebaseFirestoreException.Code.UNAVAILABLE -> "No se pudo contactar con Firestore. Revisa la conexión y vuelve a intentar."
            FirebaseFirestoreException.Code.UNAUTHENTICATED -> "Tu sesión necesita renovarse. Cierra sesión y vuelve a entrar."
            FirebaseFirestoreException.Code.FAILED_PRECONDITION -> "La base de datos necesita completar su configuración. Contacta al administrador."
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> "La conexión tardó demasiado. Revisa tu internet y reintenta."
            FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED -> "El servicio está ocupado. Espera un momento y vuelve a intentar."
            else -> "No pudimos completar la operación. Vuelve a intentar en unos momentos."
        }
        is FirebaseAuthException -> authErrorMessage(error)
        is FirebaseNetworkException -> "No pudimos conectar. Revisa tu conexión a internet y vuelve a intentar."
        else -> error.message ?: "Ocurrió un error. Vuelve a intentar."
    }

    class Factory(private val repository: ReportRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(CivitaViewModel::class.java))
            return CivitaViewModel(repository) as T
        }
    }
}
