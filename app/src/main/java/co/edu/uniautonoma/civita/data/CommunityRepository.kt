package co.edu.uniautonoma.civita.data

import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color
import co.edu.uniautonoma.civita.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.DateFormat
import java.util.UUID

class CommunityRepository(private val auth: FirebaseAuth, private val db: FirebaseFirestore) {
    private fun uid(): String = auth.currentUser?.takeUnless { it.isAnonymous }?.uid ?: error("Inicia sesión para continuar.")
    private fun report(id: String) = db.collection("reports").document(id)
    private fun user() = db.collection("users").document(uid())
    private fun DocumentSnapshot.text(key: String) = getString(key).orEmpty()
    private fun DocumentSnapshot.requiredString(key: String) = getString(key)?.takeIf { it.isNotBlank() }
        ?: error("Campo $key ausente o inválido en $reference.")
    private fun DocumentSnapshot.requiredLong(key: String) = getLong(key)
        ?: error("Campo $key ausente o inválido en $reference.")

    suspend fun ensureProfile() {
        val account = auth.currentUser ?: error("Sesión ausente.")
        val profile = user(); val prefs = profile.collection("preferences").document("settings")
        db.runTransaction { tx ->
            val previous = tx.get(profile); val preferences = tx.get(prefs)
            if (!previous.exists()) tx.set(profile, mapOf("fullName" to (account.displayName?.trim()?.takeIf { it.length >= 2 }?.take(100) ?: "Ciudadano CIVITA"),
                "email" to account.email.orEmpty(), "role" to "citizen", "status" to "active", "trustLevel" to 1,
                "createdAt" to FieldValue.serverTimestamp()))
            if (!preferences.exists()) tx.set(prefs, mapOf("alertRadiusMeters" to 1500, "categoryIds" to emptyList<String>(),
                "pushEnabled" to false, "preciseLocationAllowed" to false))
        }.await()
    }
    suspend fun isModerator(): Boolean = auth.currentUser?.getIdToken(false)?.await()?.claims?.get("role") in listOf("moderator", "admin")
    fun observeProfile(): Flow<UserProfile?> = callbackFlow {
        val listener = user().addSnapshotListener { doc, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            trySend(doc?.takeIf { it.exists() }?.let { UserProfile(it.id, it.text("fullName"), it.text("email"), it.text("role"), it.text("status"), it.getLong("trustLevel") ?: 1) })
        }; awaitClose { listener.remove() }
    }
    fun observePreferences(): Flow<UserPreferences> = callbackFlow {
        val listener = user().collection("preferences").document("settings").addSnapshotListener { doc, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            val categories = (doc?.get("categoryIds") as? List<*>)?.filterIsInstance<String>().orEmpty()
            trySend(UserPreferences(doc?.getLong("alertRadiusMeters") ?: 1500, categories,
                doc?.getBoolean("pushEnabled") ?: false, doc?.getBoolean("preciseLocationAllowed") ?: false))
        }; awaitClose { listener.remove() }
    }
    private fun <T> observe(query: Query, transform: (DocumentSnapshot) -> T): Flow<List<T>> = callbackFlow {
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            try { trySend(snapshot?.documents?.map(transform).orEmpty()) } catch (e: Exception) { close(e) }
        }; awaitClose { listener.remove() }
    }
    fun comments(id: String) = observe(report(id).collection("comments").orderBy("createdAt").limit(100)) {
        ReportComment(it.id, it.text("authorId"), it.text("authorName"), it.text("body"), it.getBoolean("hidden") ?: false)
    }
    fun validations(id: String) = observe(report(id).collection("validations").limit(1000)) { ReportValidation(it.id, it.text("value"), it.getLong("weight") ?: 1) }
    fun supports(id: String) = observe(report(id).collection("supports").limit(1000)) { ReportSupport(it.id) }
    fun history(id: String) = observe(report(id).collection("history").orderBy("createdAt", Query.Direction.DESCENDING).limit(100)) { StatusChange(it.id, it.text("changedBy"), it.text("fromStatus"), it.text("toStatus"), it.text("reason")) }
    fun moderationQueue() = observe(db.collection("reports").whereIn("auditState", listOf("pending", "processing", "manual_review")).limit(100)) { it.toUrbanReport() }

    private fun DocumentSnapshot.toUrbanReport(): UrbanReport {
        val created = getTimestamp("createdAt")
        return UrbanReport(id, requiredString("title"), requiredString("categoryId"), requiredString("categoryName"),
            requiredString("description"), requiredString("locationReference"), requiredLong("severity").toInt(),
            created?.toDate()?.let { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(it) } ?: "Fecha pendiente",
            ReportStatus.fromDocument(getString("status") ?: "review"), requiredLong("confidence").toInt(),
            Color(AndroidColor.parseColor(requiredString("categoryColor"))), getString("creatorId").orEmpty(),
            getString("auditState") ?: "pending", getString("auditSummary").orEmpty())
    }

    suspend fun reportById(id: String): UrbanReport? {
        require(id.matches(Regex("^[A-Za-z0-9_-]{1,80}$"))) { "Identificador de reporte inválido." }
        val doc = report(id).get().await()
        if (!doc.exists()) return null
        val created = doc.getTimestamp("createdAt")
        val color = doc.requiredString("categoryColor")
        return UrbanReport(
            id = doc.id,
            title = doc.requiredString("title"),
            categoryId = doc.requiredString("categoryId"),
            category = doc.requiredString("categoryName"),
            description = doc.requiredString("description"),
            location = doc.requiredString("locationReference"),
            severity = doc.requiredLong("severity").toInt(),
            time = created?.toDate()?.let { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(it) }
                ?: "Pendiente de sincronización",
            status = ReportStatus.fromDocument(doc.getString("status") ?: "review"),
            confidence = doc.requiredLong("confidence").toInt(),
            color = Color(AndroidColor.parseColor(color)),
            creatorId = doc.getString("creatorId") ?: "",
            auditState = doc.getString("auditState") ?: "pending",
            auditSummary = doc.getString("auditSummary").orEmpty(),
        )
    }
    fun events() = observe(db.collection("events").orderBy("startsAt").limit(100)) { UrbanEvent(it.id, it.text("categoryId"), it.text("title"), it.text("description"), it.getTimestamp("startsAt")?.toDate()?.time ?: 0, it.getTimestamp("endsAt")?.toDate()?.time ?: 0, it.text("status")) }
    fun notifications() = observe(user().collection("notifications").orderBy("createdAt", Query.Direction.DESCENDING).limit(100)) { UserNotification(it.id, it.text("type"), it.text("title"), it.text("body"), it.text("sourceId"), it.getTimestamp("readAt") != null) }
    fun subscriptions() = observe(user().collection("alertSubscriptions").limit(100)) { val p = it.getGeoPoint("center") ?: GeoPoint(0.0,0.0); AlertSubscription(it.id, it.text("categoryId"), p.latitude, p.longitude, it.getLong("radiusMeters") ?: 1500, it.getBoolean("active") ?: false) }
    fun layers() = observe(db.collection("mapLayers").whereEqualTo("active", true).limit(100)) { MapLayer(it.id, it.text("name"), it.text("layerType"), true) }
    fun features(layerId: String) = observe(db.collection("mapLayers").document(layerId).collection("features").limit(100)) { val p = it.getGeoPoint("location") ?: GeoPoint(0.0,0.0); LayerFeature(it.id, it.text("name"), p.latitude, p.longitude) }

    suspend fun saveProfile(name: String) { require(name.trim().length in 2..100) { "El nombre debe tener entre 2 y 100 caracteres." }; user().update("fullName", name.trim()).await() }
    suspend fun savePreferences(radius: Long, categoryIds: List<String>) {
        require(radius in 250..15000 && categoryIds.size <= 20)
        user().collection("preferences").document("settings").update(mapOf("alertRadiusMeters" to radius, "categoryIds" to categoryIds.distinct())).await()
    }
    suspend fun subscribe(categoryId: String, latitude: Double, longitude: Double, radius: Long) {
        GeoHash.encode(latitude, longitude); require(radius in 250..15000)
        user().collection("alertSubscriptions").document(categoryId).set(mapOf("categoryId" to categoryId,
            "center" to GeoPoint(latitude, longitude), "radiusMeters" to radius, "active" to true)).await()
    }
    suspend fun removeSubscription(id: String) { user().collection("alertSubscriptions").document(id).delete().await() }
    suspend fun markRead(id: String) { user().collection("notifications").document(id).update("readAt", FieldValue.serverTimestamp()).await() }
    suspend fun comment(id: String, body: String, requestId: String) {
        require(body.trim().length in 1..1000) { "Escribe un comentario de hasta 1000 caracteres." }
        val author = uid(); val profile = user().get(Source.SERVER).await(); val ref = report(id).collection("comments").document(requestId)
        db.runTransaction { tx ->
            val previous = tx.get(ref)
            if (!previous.exists()) tx.set(ref, mapOf("authorId" to author, "authorName" to profile.text("fullName"), "body" to body.trim(), "hidden" to false, "createdAt" to FieldValue.serverTimestamp()))
            else check(previous.text("authorId") == author && previous.text("body") == body.trim())
        }.await()
    }
    suspend fun validate(id: String, value: String) {
        require(value in listOf("confirm", "deny"))
        val author = uid(); val ref = report(id)
        db.runTransaction { tx ->
            val doc = tx.get(ref); check(doc.text("creatorId") != author) { "No puedes validar tu propio reporte." }
            tx.set(ref.collection("validations").document(author), mapOf("value" to value, "weight" to 1, "createdAt" to FieldValue.serverTimestamp()))
        }.await()
    }
    suspend fun support(id: String) {
        val ref = report(id).collection("supports").document(uid())
        db.runTransaction { tx -> if (tx.get(ref).exists()) tx.delete(ref) else tx.set(ref, mapOf("createdAt" to FieldValue.serverTimestamp())) }.await()
    }
    suspend fun flag(id: String, details: String) {
        require(details.trim().length in 10..1000) { "Describe el motivo con entre 10 y 1000 caracteres." }
        report(id).collection("abuseReports").document(uid()).set(mapOf("reason" to "inappropriate", "details" to details.trim(), "createdAt" to FieldValue.serverTimestamp())).await()
    }
    suspend fun moderate(id: String, status: String, reason: String) {
        require(status in listOf("review", "verified", "resolved")); require(reason.trim().length in 10..500)
        val moderator = uid(); val action = UUID.randomUUID().toString(); val ref = report(id)
        db.runTransaction { tx ->
            val doc = tx.get(ref); check(doc.text("status") != status) { "El reporte ya tiene ese estado." }
            tx.update(ref, mapOf("status" to status, "updatedAt" to FieldValue.serverTimestamp(), "lastActionId" to action))
            tx.set(ref.collection("history").document(action), mapOf("changedBy" to moderator, "fromStatus" to doc.text("status"), "toStatus" to status, "reason" to reason.trim(), "createdAt" to FieldValue.serverTimestamp()))
            tx.set(db.collection("moderationActions").document(action), mapOf("moderatorId" to moderator, "reportId" to id, "action" to "change_status", "reason" to reason.trim(), "createdAt" to FieldValue.serverTimestamp()))
            tx.set(db.collection("users").document(doc.text("creatorId")).collection("notifications").document(action), mapOf("type" to "report_status", "title" to "Tu reporte cambió de estado", "body" to reason.trim(), "sourceId" to id, "readAt" to null, "createdAt" to FieldValue.serverTimestamp(), "actionId" to action))
        }.await()
    }

    suspend fun releaseAudit(id: String, reason: String) {
        require(reason.trim().length in 10..500) { "Explica por qué autorizas mostrar el reporte (10 a 500 caracteres)." }
        val reviewer = uid(); val reviewId = UUID.randomUUID().toString(); val ref = report(id)
        db.runTransaction { tx ->
            val doc = tx.get(ref)
            check(doc.exists() && doc.text("auditState") in listOf("pending", "processing", "manual_review")) {
                "El reporte no está pendiente de auditoría."
            }
            val review = db.collection("reportAuditReviews").document(reviewId)
            tx.update(ref, mapOf("auditState" to "visible", "auditSummary" to "Revisión humana: ${reason.trim().take(180)}",
                "auditReasonCode" to "human_release", "auditUpdatedAt" to FieldValue.serverTimestamp(),
                "auditReviewerId" to reviewer, "auditReviewId" to reviewId))
            tx.set(review, mapOf("reportId" to id, "reviewerId" to reviewer, "action" to "release",
                "reason" to reason.trim(), "createdAt" to FieldValue.serverTimestamp()))
        }.await()
    }
}
