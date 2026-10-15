package co.edu.uniautonoma.civita.data

import androidx.compose.ui.graphics.Color
import co.edu.uniautonoma.civita.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.DateFormat

class FirebaseReportRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : ReportRepository {
    override suspend fun signIn(): String = auth.currentUser?.takeUnless { it.isAnonymous }?.uid
        ?: error("Inicia sesión con Google o correo para continuar.")

    override suspend fun loadConfig(): AppConfig {
        val doc = firestore.document("appConfig/public").get(Source.SERVER).await()
        require(doc.exists()) { "No existe appConfig/public en este proyecto Firebase." }
        val media = (doc.get("allowedMediaTypes") as? List<*>)?.map {
            it as? String ?: error("allowedMediaTypes contiene un valor inválido.")
        } ?: error("Falta allowedMediaTypes en appConfig/public.")
        val config = AppConfig(media, doc.requiredLong("maxEvidenceBytes"),
            doc.requiredLong("maxEvidenceCount").toInt(), doc.requiredLong("minRadiusMeters"),
            doc.requiredLong("maxRadiusMeters"))
        require(config.maxEvidenceBytes > 0 && config.maxEvidenceCount in 0..100 &&
            config.minRadiusMeters > 0 && config.maxRadiusMeters >= config.minRadiusMeters) {
            "Los límites de appConfig/public no son válidos."
        }
        return config
    }

    override fun observeCategories(): Flow<RepositorySnapshot<IncidentCategory>> = callbackFlow {
        val listener = firestore.collection("categories").whereEqualTo("active", true)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (snapshot == null) return@addSnapshotListener
                try {
                    val categories = snapshot.documents.map { doc ->
                        val duration = doc.requiredLong("defaultDurationHours")
                        require(duration in 1..168) { "Vigencia de categoría inválida: ${doc.id}" }
                        IncidentCategory(doc.id, doc.requiredString("name"),
                            parseColor(doc.requiredString("color")), duration)
                    }.sortedBy { it.label }
                    trySend(RepositorySnapshot(categories, snapshot.metadata.isFromCache))
                } catch (e: Exception) { close(e) }
            }
        awaitClose { listener.remove() }
    }

    override fun observeReports(): Flow<RepositorySnapshot<UrbanReport>> = callbackFlow {
        val listener = firestore.collection("reports")
            .orderBy("createdAt", Query.Direction.DESCENDING).limit(100)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (snapshot == null) return@addSnapshotListener
                try {
                    val reports = snapshot.documents.map { doc ->
                        val created = doc.getTimestamp("createdAt")
                        UrbanReport(doc.id, doc.requiredString("title"), doc.requiredString("categoryId"),
                            doc.requiredString("categoryName"), doc.requiredString("description"),
                            doc.requiredString("locationReference"), doc.requiredLong("severity").toInt(),
                            created?.toDate()?.let { DateFormat.getDateTimeInstance(
                                DateFormat.SHORT, DateFormat.SHORT).format(it) } ?: "Pendiente de sincronización",
                            ReportStatus.fromDocument(doc.requiredString("status")),
                            doc.requiredLong("confidence").toInt(), parseColor(doc.requiredString("categoryColor")), doc.requiredString("creatorId"))
                    }
                    trySend(RepositorySnapshot(reports, snapshot.metadata.isFromCache))
                } catch (e: Exception) { close(e) }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun publishReport(draft: ReportDraft, requestId: String): String {
        draft.validationError()?.let { throw IllegalArgumentException(it) }
        require(requestId.matches(Regex("^[A-Za-z0-9_-]{1,80}$"))) { "ID de publicación inválido." }
        val uid = auth.currentUser?.uid ?: error("Inicia una sesión antes de publicar.")
        val reference = firestore.collection("reports").document(requestId)
        // Transacción remota: sin cola offline y sin duplicar un reintento del mismo envío.
        return firestore.runTransaction { transaction ->
            val category = transaction.get(firestore.document("categories/${draft.categoryId}"))
            require(category.exists() && category.getBoolean("active") == true) { "La categoría ya no está activa." }
            val previous = transaction.get(reference)
            if (previous.exists()) {
                check(previous.getString("creatorId") == uid &&
                    previous.getString("categoryId") == draft.categoryId &&
                    previous.getString("title") == draft.title.trim() &&
                    previous.getString("description") == draft.description.trim() &&
                    previous.getString("locationReference") == draft.location.trim() &&
                    previous.getLong("severity") == draft.severity.toLong() &&
                    previous.getGeoPoint("location")?.latitude == draft.latitude &&
                    previous.getGeoPoint("location")?.longitude == draft.longitude) { "El identificador ya se usa en otro reporte." }
            } else {
                transaction.set(reference, mapOf(
                    "creatorId" to uid, "categoryId" to draft.categoryId,
                    "categoryName" to category.requiredString("name"),
                    "categoryColor" to category.requiredString("color"),
                    "title" to draft.title.trim(), "description" to draft.description.trim(),
                    "locationReference" to draft.location.trim(), "severity" to draft.severity,
                    "status" to ReportStatus.Review.documentValue, "confidence" to 0,
                    "location" to draft.latitude?.let { GeoPoint(it, requireNotNull(draft.longitude)) },
                    "geohash" to draft.latitude?.let { GeoHash.encode(it, requireNotNull(draft.longitude)) }.orEmpty(),
                    "expiresAt" to com.google.firebase.Timestamp(java.util.Date(System.currentTimeMillis() + category.requiredLong("defaultDurationHours") * 3600000)),
                    "evidenceCount" to 0,
                    "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp(),
                ))
                transaction.set(reference.collection("history").document("created"), mapOf("changedBy" to uid, "fromStatus" to "none", "toStatus" to "review", "reason" to "Publicación inicial del reporte", "createdAt" to FieldValue.serverTimestamp()))
            }
            reference.id
        }.await()
    }

    private fun DocumentSnapshot.requiredString(field: String): String =
        getString(field)?.takeIf { it.isNotBlank() } ?: error("Campo $field ausente o inválido en $reference.")
    private fun DocumentSnapshot.requiredLong(field: String): Long =
        getLong(field) ?: error("Campo $field ausente o inválido en $reference.")
    private fun parseColor(value: String): Color = Color(android.graphics.Color.parseColor(value))
}
