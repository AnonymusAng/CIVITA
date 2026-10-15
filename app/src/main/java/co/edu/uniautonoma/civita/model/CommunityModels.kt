package co.edu.uniautonoma.civita.model

/** IDs son rutas Firestore, no claves SQL. Las relaciones se describen en docs/database. */
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

/** Codificación geohash estándar; nunca se inventa una posición si el usuario no la proporciona. */
object GeoHash {
    fun encode(latitude: Double, longitude: Double, precision: Int = 9): String {
        require(latitude.isFinite() && latitude in -90.0..90.0)
        require(longitude.isFinite() && longitude in -180.0..180.0)
        require(precision in 1..12)
        val alphabet = "0123456789bcdefghjkmnpqrstuvwxyz"
        var loLat = -90.0; var hiLat = 90.0; var loLon = -180.0; var hiLon = 180.0
        var longitudeBit = true; var bits = 0; var value = 0
        val output = StringBuilder()
        while (output.length < precision) {
            value = value shl 1
            if (longitudeBit) {
                val mid = (loLon + hiLon) / 2
                if (longitude >= mid) { value++; loLon = mid } else hiLon = mid
            } else {
                val mid = (loLat + hiLat) / 2
                if (latitude >= mid) { value++; loLat = mid } else hiLat = mid
            }
            longitudeBit = !longitudeBit; bits++
            if (bits == 5) { output.append(alphabet[value]); bits = 0; value = 0 }
        }
        return output.toString()
    }
}
