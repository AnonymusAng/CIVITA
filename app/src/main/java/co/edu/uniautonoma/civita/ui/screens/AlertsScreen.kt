package co.edu.uniautonoma.civita.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import co.edu.uniautonoma.civita.ui.CivitaUiState
import co.edu.uniautonoma.civita.ui.components.*
import co.edu.uniautonoma.civita.ui.theme.*

@Composable
fun AlertsScreen(state: CivitaUiState) {
    LazyColumn(Modifier.fillMaxSize().background(SurfaceLight).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { CivitaBrand() }
        item { SectionTitle("Alertas", "Lo más reciente de tu comunidad") }
        if (state.reports.isEmpty() && !state.loading) item { EmptyState("Estás al día", "Aquí encontrarás las novedades recientes cuando estén disponibles.") }
        items(state.reports.take(10), key = { it.id }) { report -> ReportCard(report) }
    }
}
