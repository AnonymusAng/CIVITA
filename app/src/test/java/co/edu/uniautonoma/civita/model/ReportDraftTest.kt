package co.edu.uniautonoma.civita.model

import org.junit.Assert.*
import org.junit.Test

class ReportDraftTest {
    private fun valid() = ReportDraft("movilidad", "Semáforo averiado",
        "El semáforo no cambia en esta intersección.", "Carrera 6 con calle 8", 2)

    @Test fun acceptsValidDraftAndTrimmedText() {
        assertNull(valid().validationError())
        assertNull(valid().copy(title = "  Semáforo averiado  ").validationError())
    }
    @Test fun rejectsMissingCategory() { assertNotNull(valid().copy(categoryId = "").validationError()) }
    @Test fun rejectsCategoryPathTraversal() { assertNotNull(valid().copy(categoryId = "a/b").validationError()) }
    @Test fun rejectsEmptyAndOversizedTitles() {
        assertNotNull(valid().copy(title = "   ").validationError())
        assertNotNull(valid().copy(title = "a".repeat(121)).validationError())
    }
    @Test fun rejectsInvalidDescriptionAndLocation() {
        assertNotNull(valid().copy(description = "corta").validationError())
        assertNotNull(valid().copy(location = "  ").validationError())
        assertNotNull(valid().copy(description = "a".repeat(2001)).validationError())
    }
    @Test fun enforcesSeverityRange() {
        assertNotNull(valid().copy(severity = 0).validationError())
        assertNotNull(valid().copy(severity = 4).validationError())
        assertNull(valid().copy(severity = 1).validationError())
        assertNull(valid().copy(severity = 3).validationError())
    }
    @Test fun mapsDocumentStatusAndRejectsUnknownValues() {
        assertEquals(ReportStatus.Verified, ReportStatus.fromDocument("verified"))
        try { ReportStatus.fromDocument("other"); fail("Expected rejection") }
        catch (_: IllegalArgumentException) { }
    }
}
