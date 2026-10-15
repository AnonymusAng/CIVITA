package co.edu.uniautonoma.civita.ui

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModelStore
import co.edu.uniautonoma.civita.data.ReportRepository
import co.edu.uniautonoma.civita.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CivitaViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    @Before fun prepare() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { store.clear(); Dispatchers.resetMain() }
    private fun model(repo: FakeRepository): CivitaViewModel = CivitaViewModel(repo).also { store.put("model", it) }
    private fun draft() = ReportDraft("movilidad", "Semáforo averiado", "No cambia en la intersección.", "Carrera 6", 2)

    @Test fun loadsRemoteStateAndFiltersByDocumentId() = runTest(dispatcher) {
        val repo = FakeRepository()
        val model = model(repo)
        advanceUntilIdle()
        assertFalse(model.state.value.loading)
        assertEquals("uid-test", model.state.value.uid)
        assertNotNull(model.state.value.config)
        assertEquals(2, model.state.value.visibleReports.size)
        model.selectCategory("movilidad")
        assertEquals(listOf("r1"), model.state.value.visibleReports.map { it.id })
        model.selectCategory("movilidad")
        assertEquals(2, model.state.value.visibleReports.size)
    }

    @Test fun exposesInitialFailureAndCanRetry() = runTest(dispatcher) {
        val repo = FakeRepository().apply { failConfig = true }
        val model = model(repo)
        advanceUntilIdle()
        assertEquals("Configuración no disponible", model.state.value.error)
        assertFalse(model.state.value.loading)
        repo.failConfig = false
        model.reload(); advanceUntilIdle()
        assertNull(model.state.value.error)
        assertEquals(2, model.state.value.reports.size)
    }

    @Test fun validationStopsInvalidPublicationBeforeNetworkCall() = runTest(dispatcher) {
        val repo = FakeRepository(); val model = model(repo); advanceUntilIdle()
        model.publish(draft().copy(title = "")); advanceUntilIdle()
        assertTrue(repo.requests.isEmpty())
        assertNotNull(model.state.value.publicationError)
        assertFalse(model.state.value.submitting)
    }

    @Test fun ignoresDoubleClickAndReportsConfirmedId() = runTest(dispatcher) {
        val repo = FakeRepository(); val model = model(repo); advanceUntilIdle()
        model.publish(draft()); model.publish(draft()); advanceUntilIdle()
        assertEquals(1, repo.requests.size)
        assertEquals(repo.requests.single(), model.state.value.publishedId)
        assertFalse(model.state.value.submitting)
        model.clearPublication()
        assertNull(model.state.value.publishedId)
    }

    @Test fun retriesSameDraftWithSameIdAfterFailure() = runTest(dispatcher) {
        val repo = FakeRepository().apply { failPublish = true }
        val model = model(repo); advanceUntilIdle()
        model.publish(draft()); advanceUntilIdle()
        assertNotNull(model.state.value.publicationError)
        assertNull(model.state.value.publishedId)
        repo.failPublish = false
        model.publish(draft()); advanceUntilIdle()
        assertEquals(2, repo.requests.size)
        assertEquals(repo.requests[0], repo.requests[1])
        assertEquals(repo.requests[1], model.state.value.publishedId)
    }

    @Test fun distinguishesCachedSnapshotsFromServerUpdates() = runTest(dispatcher) {
        val repo = FakeRepository(); val model = model(repo); advanceUntilIdle()
        repo.reports.value = repo.reports.value.copy(fromCache = true)
        advanceUntilIdle(); assertTrue(model.state.value.fromCache)
        repo.reports.value = repo.reports.value.copy(fromCache = false)
        advanceUntilIdle(); assertFalse(model.state.value.fromCache)
    }

    @Test fun configTimeoutStopsLoadingAndOffersRetry() = runTest(dispatcher) {
        val repo = FakeRepository().apply { configDelay = 25000 }
        val model = model(repo); advanceUntilIdle()
        assertFalse(model.state.value.loading)
        assertTrue(model.state.value.error!!.contains("tardó demasiado"))
        repo.configDelay = 0
        model.reload(); advanceUntilIdle()
        assertNull(model.state.value.error)
        assertNotNull(model.state.value.config)
    }

    @Test fun publicationTimeoutKeepsRequestIdForRetry() = runTest(dispatcher) {
        val repo = FakeRepository().apply { publishDelay = 35000 }
        val model = model(repo); advanceUntilIdle()
        model.publish(draft()); advanceUntilIdle()
        assertFalse(model.state.value.submitting)
        assertNull(model.state.value.publishedId)
        assertNotNull(model.state.value.publicationError)
        val originalId = repo.requests.single()
        repo.publishDelay = 0
        model.publish(draft()); advanceUntilIdle()
        assertEquals(listOf(originalId, originalId), repo.requests)
        assertEquals(originalId, model.state.value.publishedId)
    }

    private class FakeRepository : ReportRepository {
        var configDelay = 0L
        var publishDelay = 0L
        var failConfig = false
        var failPublish = false
        val requests = mutableListOf<String>()
        private val categories = MutableStateFlow(RepositorySnapshot(listOf(
            IncidentCategory("movilidad", "Movilidad", Color(0xFF1976D2)),
            IncidentCategory("obras", "Obras", Color(0xFFF4A340))), false))
        val reports = MutableStateFlow(RepositorySnapshot(listOf(report("r1", "movilidad"), report("r2", "obras")), false))
        override suspend fun signIn() = "uid-test"
        override suspend fun loadConfig(): AppConfig {
            kotlinx.coroutines.delay(configDelay)
            if (failConfig) error("Configuración no disponible")
            return AppConfig(listOf("image/jpeg"), 10485760, 3, 250, 15000)
        }
        override fun observeCategories() = categories
        override fun observeReports() = reports
        override suspend fun publishReport(draft: ReportDraft, requestId: String): String {
            requests.add(requestId)
            kotlinx.coroutines.delay(publishDelay)
            if (failPublish) error("Sin conexión")
            return requestId
        }
        private fun report(id: String, category: String) = UrbanReport(id, "Título de muestra", category,
            category, "Descripción de muestra", "Lugar", 2, "Hoy", ReportStatus.Review, 0, Color(0xFF1976D2))
    }
}
