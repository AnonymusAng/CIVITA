# Catálogo de código · CIVITA 1.1.742

Las firmas y propiedades se conservan en los archivos Kotlin. Este índice permite ubicar cada implementación; los diagramas son vistas organizadas por responsabilidad.

## app/src/main/java/co/edu/uniautonoma/civita/MainActivity.kt

```kotlin
class MainActivity : ComponentActivity() {
override fun onCreate(savedInstanceState: Bundle?) {
```

## app/src/main/java/co/edu/uniautonoma/civita/data/AppContainer.kt

```kotlin
class AppContainer(context: Context) {
```

## app/src/main/java/co/edu/uniautonoma/civita/data/AuthRepository.kt

```kotlin
class AuthRepository(private val auth: FirebaseAuth) {
fun observeSession() = callbackFlow<FirebaseUser?> {
suspend fun login(email: String, password: String) {
suspend fun register(email: String, password: String) {
suspend fun resetPassword(email: String) { auth.sendPasswordResetEmail(email.trim()).await() }
suspend fun google(context: Context) {
fun logout() { auth.signOut() }
suspend fun clearGoogleSession(context: Context) {
```

## app/src/main/java/co/edu/uniautonoma/civita/data/CommunityRepository.kt

```kotlin
class CommunityRepository(private val auth: FirebaseAuth, private val db: FirebaseFirestore, private val storage: FirebaseStorage) {
private fun uid(): String = auth.currentUser?.takeUnless { it.isAnonymous }?.uid ?: error("Inicia sesión para continuar.")
private fun report(id: String) = db.collection("reports").document(id)
private fun user() = db.collection("users").document(uid())
private fun DocumentSnapshot.text(key: String) = getString(key).orEmpty()
suspend fun ensureProfile() {
suspend fun isModerator(): Boolean = auth.currentUser?.getIdToken(false)?.await()?.claims?.get("role") in listOf("moderator", "admin")
fun observeProfile(): Flow<UserProfile?> = callbackFlow {
fun observePreferences(): Flow<UserPreferences> = callbackFlow {
private fun <T> observe(query: Query, transform: (DocumentSnapshot) -> T): Flow<List<T>> = callbackFlow {
fun comments(id: String) = observe(report(id).collection("comments").orderBy("createdAt").limit(100)) {
fun validations(id: String) = observe(report(id).collection("validations").limit(1000)) { ReportValidation(it.id, it.text("value"), it.getLong("weight") ?: 1) }
fun supports(id: String) = observe(report(id).collection("supports").limit(1000)) { ReportSupport(it.id) }
fun evidence(id: String) = observe(report(id).collection("evidence").limit(3)) { ReportEvidence(it.id, it.text("storagePath"), it.text("mediaType"), it.getLong("sizeBytes") ?: 0, it.text("uploadedBy")) }
fun history(id: String) = observe(report(id).collection("history").orderBy("createdAt", Query.Direction.DESCENDING).limit(100)) { StatusChange(it.id, it.text("changedBy"), it.text("fromStatus"), it.text("toStatus"), it.text("reason")) }
fun events() = observe(db.collection("events").orderBy("startsAt").limit(100)) { UrbanEvent(it.id, it.text("categoryId"), it.text("title"), it.text("description"), it.getTimestamp("startsAt")?.toDate()?.time ?: 0, it.getTimestamp("endsAt")?.toDate()?.time ?: 0, it.text("status")) }
fun notifications() = observe(user().collection("notifications").orderBy("createdAt", Query.Direction.DESCENDING).limit(100)) { UserNotification(it.id, it.text("type"), it.text("title"), it.text("body"), it.text("sourceId"), it.getTimestamp("readAt") != null) }
fun subscriptions() = observe(user().collection("alertSubscriptions").limit(100)) { val p = it.getGeoPoint("center") ?: GeoPoint(0.0,0.0); AlertSubscription(it.id, it.text("categoryId"), p.latitude, p.longitude, it.getLong("radiusMeters") ?: 1500, it.getBoolean("active") ?: false) }
fun layers() = observe(db.collection("mapLayers").whereEqualTo("active", true).limit(100)) { MapLayer(it.id, it.text("name"), it.text("layerType"), true) }
fun features(layerId: String) = observe(db.collection("mapLayers").document(layerId).collection("features").limit(100)) { val p = it.getGeoPoint("location") ?: GeoPoint(0.0,0.0); LayerFeature(it.id, it.text("name"), p.latitude, p.longitude) }
suspend fun saveProfile(name: String) { require(name.trim().length in 2..100) { "El nombre debe tener entre 2 y 100 caracteres." }; user().update("fullName", name.trim()).await() }
suspend fun savePreferences(radius: Long, categoryIds: List<String>) {
suspend fun subscribe(categoryId: String, latitude: Double, longitude: Double, radius: Long) {
suspend fun removeSubscription(id: String) { user().collection("alertSubscriptions").document(id).delete().await() }
suspend fun markRead(id: String) { user().collection("notifications").document(id).update("readAt", FieldValue.serverTimestamp()).await() }
suspend fun comment(id: String, body: String, requestId: String) {
suspend fun validate(id: String, value: String) {
suspend fun support(id: String) {
suspend fun flag(id: String, details: String) {
suspend fun moderate(id: String, status: String, reason: String) {
suspend fun uploadEvidence(id: String, context: Context, uri: Uri) {
suspend fun evidenceUrl(path: String): String = storage.reference.child(path).downloadUrl.await().toString()
```

## app/src/main/java/co/edu/uniautonoma/civita/data/FirebaseReportRepository.kt

```kotlin
class FirebaseReportRepository(
override suspend fun signIn(): String = auth.currentUser?.takeUnless { it.isAnonymous }?.uid
override suspend fun loadConfig(): AppConfig {
override fun observeCategories(): Flow<RepositorySnapshot<IncidentCategory>> = callbackFlow {
override fun observeReports(): Flow<RepositorySnapshot<UrbanReport>> = callbackFlow {
override suspend fun publishReport(draft: ReportDraft, requestId: String): String {
private fun DocumentSnapshot.requiredString(field: String): String =
private fun DocumentSnapshot.requiredLong(field: String): Long =
private fun parseColor(value: String): Color = Color(android.graphics.Color.parseColor(value))
```

## app/src/main/java/co/edu/uniautonoma/civita/data/ReportRepository.kt

```kotlin
interface ReportRepository {
suspend fun signIn(): String
suspend fun loadConfig(): AppConfig
fun observeCategories(): Flow<RepositorySnapshot<IncidentCategory>>
fun observeReports(): Flow<RepositorySnapshot<UrbanReport>>
suspend fun publishReport(draft: ReportDraft, requestId: String): String
```

## app/src/main/java/co/edu/uniautonoma/civita/model/CommunityModels.kt

```kotlin
data class UserProfile(val id: String, val fullName: String, val email: String, val role: String, val status: String, val trustLevel: Long)
data class UserPreferences(val alertRadiusMeters: Long = 1500, val categoryIds: List<String> = emptyList(), val pushEnabled: Boolean = false, val preciseLocationAllowed: Boolean = false)
data class ReportComment(val id: String, val authorId: String, val authorName: String, val body: String, val hidden: Boolean)
data class ReportValidation(val userId: String, val value: String, val weight: Long)
data class ReportSupport(val userId: String)
data class ReportEvidence(val id: String, val storagePath: String, val mediaType: String, val sizeBytes: Long, val uploadedBy: String)
data class StatusChange(val id: String, val changedBy: String, val fromStatus: String, val toStatus: String, val reason: String)
data class AbuseReport(val userId: String, val reason: String, val details: String)
data class ModerationAction(val id: String, val moderatorId: String, val reportId: String, val action: String, val reason: String)
data class UrbanEvent(val id: String, val categoryId: String, val title: String, val description: String, val startsAtMillis: Long, val endsAtMillis: Long, val status: String)
data class UserNotification(val id: String, val type: String, val title: String, val body: String, val sourceId: String, val read: Boolean)
data class AlertSubscription(val id: String, val categoryId: String, val latitude: Double, val longitude: Double, val radiusMeters: Long, val active: Boolean)
data class MapLayer(val id: String, val name: String, val layerType: String, val active: Boolean)
data class LayerFeature(val id: String, val name: String, val latitude: Double, val longitude: Double)
data class DeviceToken(val id: String, val token: String, val platform: String)
object GeoHash {
fun encode(latitude: Double, longitude: Double, precision: Int = 9): String {
```

## app/src/main/java/co/edu/uniautonoma/civita/model/Models.kt

```kotlin
enum class CivitaDestination(val label: String) {
enum class ReportStatus(val label: String, val documentValue: String) {
fun fromDocument(value: String): ReportStatus = entries.firstOrNull { it.documentValue == value }
data class IncidentCategory(
data class UrbanReport(
data class AppConfig(
data class ReportDraft(
fun validationError(): String? = when {
data class RepositorySnapshot<T>(val items: List<T>, val fromCache: Boolean)
```

## app/src/main/java/co/edu/uniautonoma/civita/ui/AuthViewModel.kt

```kotlin
data class AuthUiState(
class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
fun email(email: String, password: String, register: Boolean) {
fun reset(email: String) {
fun google(context: Context) = execute { repository.google(context) }
fun logout(context: Context) {
fun clearFeedback() { mutableState.update { it.copy(error = null, message = null) } }
private fun execute(action: suspend () -> Unit) {
class Factory(private val repository: AuthRepository) : ViewModelProvider.Factory {
override fun <T : ViewModel> create(modelClass: Class<T>): T {
object AuthValidation {
fun validEmail(email: String): Boolean = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(email.trim())
fun error(email: String, password: String, register: Boolean): String? = when {
fun authErrorMessage(error: Exception): String = when (error) {
```

## app/src/main/java/co/edu/uniautonoma/civita/ui/CivitaApp.kt

```kotlin
fun CivitaApp() {
```

## app/src/main/java/co/edu/uniautonoma/civita/ui/CivitaViewModel.kt

```kotlin
data class CivitaUiState(
class CivitaViewModel(private val repository: ReportRepository) : ViewModel() {
fun reload() {
fun stopObserving() { observation?.cancel(); observation = null }
fun selectCategory(id: String?) {
fun publish(draft: ReportDraft) {
fun clearPublication() {
private fun message(error: Exception): String = when (error) {
class Factory(private val repository: ReportRepository) : ViewModelProvider.Factory {
override fun <T : ViewModel> create(modelClass: Class<T>): T {
```

## app/src/main/java/co/edu/uniautonoma/civita/ui/CommunityActions.kt

```kotlin
interface CommunityActions {
fun saveProfile(name: String)
fun preferences(radius: Long, ids: List<String>)
fun comment(id: String, body: String)
fun validate(id: String, value: String)
fun support(id: String)
fun flag(id: String, reason: String)
fun moderate(id: String, status: String, reason: String)
fun upload(id: String, context: Context, uri: Uri)
fun markRead(id: String)
fun subscribe(category: String, lat: Double, lon: Double, radius: Long)
fun unsubscribe(id: String)
suspend fun evidenceUrl(path: String): String
```

## app/src/main/java/co/edu/uniautonoma/civita/ui/CommunityViewModel.kt

```kotlin
data class CommunityUiState(
class CommunityViewModel(private val repository: CommunityRepository) : ViewModel(), CommunityActions {
fun start() {
fun retry() { streams?.cancel(); streams = null; start(); state.value.selectedId?.let(::select) }
fun stop() { streams?.cancel(); streams = null; detail?.cancel(); detail = null; layer?.cancel(); actionJob?.cancel(); mutable.update { it.copy(busy=false,selectedId=null,error=null,message=null,loadingDetail=false) } }
private fun fail(e: Exception) {
private fun <T> CoroutineScope.watch(flow: Flow<T>, update: CommunityUiState.(T) -> CommunityUiState) = launch {
fun select(id: String) {
fun selectLayer(id: String) {
fun closeDetail() { detail?.cancel(); mutable.update { it.copy(selectedId=null, error=null, message=null) } }
fun clearFeedback() { mutable.update { it.copy(error=null,message=null) } }
private fun action(message: String = "Cambios guardados", block: suspend () -> Unit) {
override fun saveProfile(name: String) = action { repository.saveProfile(name) }
override fun preferences(radius: Long, ids: List<String>) = action { repository.savePreferences(radius,ids) }
override fun comment(id: String, body: String) {
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
```

## app/src/main/java/co/edu/uniautonoma/civita/ui/SimulationApp.kt

```kotlin
fun SimulationApp() {
override fun saveProfile(name:String) { community=community.copy(profile=community.profile?.copy(fullName=name),message="Nombre guardado localmente") }
override fun preferences(radius:Long,ids:List<String>) { community=community.copy(preferences=UserPreferences(radius,ids),message="Preferencias de simulación guardadas") }
override fun comment(id:String,body:String) { community=community.copy(comments=community.comments+ReportComment("local-${community.comments.size}","simulation-user",community.profile?.fullName.orEmpty(),body,false),message="Comentario de simulación publicado") }
override fun validate(id:String,value:String) { community=community.copy(validations=community.validations.filterNot{it.userId=="simulation-user"}+ReportValidation("simulation-user",value,1)) }
override fun support(id:String) { community=community.copy(supports=if(community.supports.isEmpty()) listOf(ReportSupport("simulation-user")) else emptyList()) }
override fun flag(id:String,reason:String) { community=community.copy(message="Denuncia simulada; no enviada") }
override fun moderate(id:String,status:String,reason:String) { community=community.copy(message="La simulación no concede roles de moderador") }
override fun upload(id:String,context:Context,uri:Uri) { community=community.copy(message="Archivos disponibles en la versión conectada a Cloud Storage") }
override fun markRead(id:String) { community=community.copy(notifications=community.notifications.map{if(it.id==id) it.copy(read=true) else it}) }
override fun subscribe(category:String,lat:Double,lon:Double,radius:Long) { community=community.copy(subscriptions=community.subscriptions.filterNot{it.id==category}+AlertSubscription(category,category,lat,lon,radius,true)) }
override fun unsubscribe(id:String) { community=community.copy(subscriptions=community.subscriptions.filterNot{it.id==id}) }
override suspend fun evidenceUrl(path:String):String = error("Sin evidencias reales en simulación")
```

## app/src/main/java/co/edu/uniautonoma/civita/ui/components/CivitaComponents.kt

```kotlin
fun CivitaBrand(modifier: Modifier = Modifier) {
fun SectionTitle(title: String, subtitle: String? = null, action: String? = null) {
fun StatusPill(status: ReportStatus) {
fun ReportCard(report: UrbanReport, modifier: Modifier = Modifier) {
fun EmptyState(title: String, description: String) {
```

## app/src/main/java/co/edu/uniautonoma/civita/ui/screens/AlertsScreen.kt

```kotlin
fun AlertsScreen(state: CivitaUiState) {
```

## app/src/main/java/co/edu/uniautonoma/civita/ui/screens/AuthScreen.kt

```kotlin
fun AuthScreen(state: AuthUiState, onEmail: (String, String, Boolean) -> Unit,
```

## app/src/main/java/co/edu/uniautonoma/civita/ui/screens/CommunityScreen.kt

```kotlin
fun CommunityScreen(reports: List<UrbanReport>, community: CommunityUiState, onSelect: (String)->Unit, onRead: (String)->Unit, onLayer: (String)->Unit = {}) {
```

## app/src/main/java/co/edu/uniautonoma/civita/ui/screens/MapScreen.kt

```kotlin
fun MapScreen(state: CivitaUiState, onCategory: (String?) -> Unit, onReport: () -> Unit) {
private fun StylizedMap(modifier: Modifier = Modifier) {
```

## app/src/main/java/co/edu/uniautonoma/civita/ui/screens/PreferencesScreen.kt

```kotlin
fun PreferencesScreen(state: CommunityUiState, categories: List<IncidentCategory>, model: CommunityActions, onBack: ()->Unit) {
```

## app/src/main/java/co/edu/uniautonoma/civita/ui/screens/ProfileScreen.kt

```kotlin
fun ProfileScreen(state: CivitaUiState, authState: AuthUiState, onPreferences: () -> Unit, onLogout: () -> Unit) {
```

## app/src/main/java/co/edu/uniautonoma/civita/ui/screens/ReportDetailScreen.kt

```kotlin
fun ReportDetailScreen(report: UrbanReport?, state: CommunityUiState, uid: String, model: CommunityActions, onBack: ()->Unit) {
```

## app/src/main/java/co/edu/uniautonoma/civita/ui/screens/ReportScreen.kt

```kotlin
fun ReportScreen(state: CivitaUiState, onPublish: (ReportDraft) -> Unit, onDone: () -> Unit) {
```

## app/src/main/java/co/edu/uniautonoma/civita/ui/theme/Color.kt

```kotlin
```

## app/src/main/java/co/edu/uniautonoma/civita/ui/theme/Theme.kt

```kotlin
fun CivitaTheme(content: @Composable () -> Unit) {
```

## app/src/main/java/co/edu/uniautonoma/civita/ui/theme/Type.kt

```kotlin
```
