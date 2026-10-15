package co.edu.uniautonoma.civita.data

import co.edu.uniautonoma.civita.model.*
import kotlinx.coroutines.flow.Flow

interface ReportRepository {
    suspend fun signIn(): String
    suspend fun loadConfig(): AppConfig
    fun observeCategories(): Flow<RepositorySnapshot<IncidentCategory>>
    fun observeReports(): Flow<RepositorySnapshot<UrbanReport>>
    suspend fun publishReport(draft: ReportDraft, requestId: String): String
}
