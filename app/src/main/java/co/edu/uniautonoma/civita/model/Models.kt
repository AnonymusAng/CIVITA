package co.edu.uniautonoma.civita.model

import androidx.compose.ui.graphics.Color

enum class CivitaDestination(val label: String) {
    Map("Mapa"), Reports("Reportes"), Alerts("Comunidad"), Profile("Perfil")
}

enum class ReportStatus(val label: String, val documentValue: String) {
    Review("En revisión", "review"), Verified("Verificado", "verified"), Resolved("Resuelto", "resolved");
    companion object {
        fun fromDocument(value: String): ReportStatus = entries.firstOrNull { it.documentValue == value }
            ?: throw IllegalArgumentException("Estado de reporte no reconocido: $value")
    }
}

data class IncidentCategory(
    val id: String,
    val label: String,
    val color: Color,
    val defaultDurationHours: Long = 4,
)

data class UrbanReport(
    val id: String,
    val title: String,
    val categoryId: String,
    val category: String,
    val description: String,
    val location: String,
    val severity: Int,
    val time: String,
    val status: ReportStatus,
    val confidence: Int,
    val color: Color,
    val creatorId: String = "",
)

data class AppConfig(
    val allowedMediaTypes: List<String>,
    val maxEvidenceBytes: Long,
    val maxEvidenceCount: Int,
    val minRadiusMeters: Long,
    val maxRadiusMeters: Long,
)

data class ReportDraft(
    val categoryId: String,
    val title: String,
    val description: String,
    val location: String,
    val severity: Int,
    val latitude: Double? = null,
    val longitude: Double? = null,
) {
    fun validationError(): String? = when {
        !categoryId.matches(Regex("^[A-Za-z0-9_-]{1,80}$")) -> "Selecciona una categoría válida."
        title.trim().length !in 5..120 -> "El título debe tener entre 5 y 120 caracteres."
        description.trim().length !in 10..2000 -> "La descripción debe tener entre 10 y 2000 caracteres."
        location.trim().length !in 3..200 -> "Ingresa una referencia del lugar de entre 3 y 200 caracteres."
        (latitude == null) != (longitude == null) -> "Ingresa ambas coordenadas o deja ambas vacías."
        latitude != null && (!latitude.isFinite() || latitude !in -90.0..90.0) -> "Latitud inválida."
        longitude != null && (!longitude.isFinite() || longitude !in -180.0..180.0) -> "Longitud inválida."
        severity !in 1..3 -> "La afectación debe estar entre 1 y 3."
        else -> null
    }
}

data class RepositorySnapshot<T>(val items: List<T>, val fromCache: Boolean)
