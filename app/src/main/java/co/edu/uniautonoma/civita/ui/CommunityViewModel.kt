package co.edu.uniautonoma.civita.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import co.edu.uniautonoma.civita.data.CommunityRepository
import co.edu.uniautonoma.civita.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID

data class CommunityUiState(
    val profile: UserProfile? = null, val preferences: UserPreferences = UserPreferences(),
    val moderator: Boolean = false, val events: List<UrbanEvent> = emptyList(),
    val notifications: List<UserNotification> = emptyList(), val subscriptions: List<AlertSubscription> = emptyList(),
    val layers: List<MapLayer> = emptyList(), val features: List<LayerFeature> = emptyList(),
    val comments: List<ReportComment> = emptyList(), val validations: List<ReportValidation> = emptyList(),
    val supports: List<ReportSupport> = emptyList(), val evidence: List<ReportEvidence> = emptyList(),
    val history: List<StatusChange> = emptyList(), val selectedId: String? = null,
    val busy: Boolean = false, val loadingDetail: Boolean = false, val error: String? = null, val message: String? = null,
)

class CommunityViewModel(private val repository: CommunityRepository) : ViewModel(), CommunityActions {
    private val mutable = MutableStateFlow(CommunityUiState())
    val state = mutable.asStateFlow()
    private var streams: Job? = null
    private var detail: Job? = null
    private var layer: Job? = null
    private var pendingComment: Triple<String,String,String>? = null
    private var actionJob: Job? = null
    fun start() {
        if (streams?.isActive == true) return
        streams = viewModelScope.launch {
            try {
                withTimeout(30000) { repository.ensureProfile() }
                mutable.update { it.copy(moderator = repository.isModerator(), error = null) }
                supervisorScope {
                    watch(repository.observeProfile()) { value -> copy(profile = value) }
                    watch(repository.observePreferences()) { value -> copy(preferences = value) }
                    watch(repository.events()) { value -> copy(events = value) }
                    watch(repository.notifications()) { value -> copy(notifications = value) }
                    watch(repository.subscriptions()) { value -> copy(subscriptions = value) }
                    watch(repository.layers()) { value -> copy(layers = value) }
                }
            } catch (e: CancellationException) { if (e is TimeoutCancellationException) fail(e) else throw e }
            catch (e: Exception) { fail(e) }
        }
    }
    fun retry() { streams?.cancel(); streams = null; start(); state.value.selectedId?.let(::select) }
    fun stop() { streams?.cancel(); streams = null; detail?.cancel(); detail = null; layer?.cancel(); actionJob?.cancel(); mutable.update { it.copy(busy=false,selectedId=null,error=null,message=null,loadingDetail=false) } }
    private fun fail(e: Exception) {
        val message = when {
            e is TimeoutCancellationException -> "La conexión tardó demasiado. Reintenta; una escritura puede haberse confirmado."
            e is com.google.firebase.storage.StorageException -> "No se pudo acceder a evidencias. Verifica que Cloud Storage y sus reglas estén configurados."
            e is com.google.firebase.firestore.FirebaseFirestoreException -> "No se pudo completar la operación en Firestore (${e.code}). Revisa la conexión y publica las reglas actualizadas."
            else -> e.message ?: "No se pudo completar la operación."
        }
        mutable.update { it.copy(error = message, busy = false, loadingDetail = false) }
    }
    private fun <T> CoroutineScope.watch(flow: Flow<T>, update: CommunityUiState.(T) -> CommunityUiState) = launch {
        try { flow.collect { value -> mutable.update { it.update(value) } } }
        catch (e: CancellationException) { throw e } catch (e: Exception) { fail(e) }
    }
    fun select(id: String) {
        detail?.cancel(); mutable.update { it.copy(selectedId=id, comments=emptyList(), validations=emptyList(), supports=emptyList(), evidence=emptyList(), history=emptyList(), loadingDetail=true, error=null) }
        detail = viewModelScope.launch {
            supervisorScope {
                watch(repository.comments(id)) { value -> copy(comments=value, loadingDetail=false) }
                watch(repository.validations(id)) { value -> copy(validations=value) }
                watch(repository.supports(id)) { value -> copy(supports=value) }
                watch(repository.evidence(id)) { value -> copy(evidence=value) }
                watch(repository.history(id)) { value -> copy(history=value) }
            }
        }
    }
    fun selectLayer(id: String) {
        layer?.cancel(); mutable.update { it.copy(features=emptyList()) }
        layer=viewModelScope.launch { try { repository.features(id).collect { value -> mutable.update { it.copy(features=value) } } } catch(e: CancellationException) { throw e } catch(e: Exception) { fail(e) } }
    }
    fun closeDetail() { detail?.cancel(); mutable.update { it.copy(selectedId=null, error=null, message=null) } }
    fun clearFeedback() { mutable.update { it.copy(error=null,message=null) } }
    private fun action(message: String = "Cambios guardados", block: suspend () -> Unit) {
        if (state.value.busy) return
        mutable.update { it.copy(busy=true,error=null,message=null) }
        actionJob = viewModelScope.launch {
            try { withTimeout(60000) { block() }; mutable.update { it.copy(busy=false,message=message) } }
            catch (e: CancellationException) { if (e is TimeoutCancellationException) fail(e) else { mutable.update { it.copy(busy=false) }; throw e } }
            catch (e: Exception) { fail(e) }
        }
    }
    override fun saveProfile(name: String) = action { repository.saveProfile(name) }
    override fun preferences(radius: Long, ids: List<String>) = action { repository.savePreferences(radius,ids) }
    override fun comment(id: String, body: String) {
        if (pendingComment?.first != id || pendingComment?.second != body) pendingComment = Triple(id,body,UUID.randomUUID().toString())
        val requestId = requireNotNull(pendingComment).third
        action("Comentario publicado") { repository.comment(id,body,requestId); pendingComment=null }
    }
    override fun validate(id: String, value: String) = action("Validación registrada") { repository.validate(id,value) }
    override fun support(id: String) = action { repository.support(id) }
    override fun flag(id: String, reason: String) = action("Aviso enviado a moderación") { repository.flag(id,reason) }
    override fun moderate(id: String, status: String, reason: String) = action("Estado e historial actualizados") { repository.moderate(id,status,reason) }
    override fun upload(id: String, context: Context, uri: Uri) = action("Evidencia adjuntada") { repository.uploadEvidence(id,context,uri) }
    override fun markRead(id: String) = action { repository.markRead(id) }
    override fun subscribe(category: String, lat: Double, lon: Double, radius: Long) = action { repository.subscribe(category,lat,lon,radius) }
    override fun unsubscribe(id: String) = action { repository.removeSubscription(id) }
    override suspend fun evidenceUrl(path: String) = repository.evidenceUrl(path)
    class Factory(private val repository: CommunityRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(CommunityViewModel::class.java)); return CommunityViewModel(repository) as T
        }
    }
}
