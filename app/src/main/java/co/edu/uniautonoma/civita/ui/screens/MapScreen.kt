package co.edu.uniautonoma.civita.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import co.edu.uniautonoma.civita.ui.CivitaUiState
import co.edu.uniautonoma.civita.ui.components.*
import co.edu.uniautonoma.civita.ui.theme.*

@Composable
fun MapScreen(state: CivitaUiState, onCategory: (String?) -> Unit, onReport: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().background(SurfaceLight).padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 16.dp)) {
        item { CivitaBrand() }
        item { SectionTitle("Tu ciudad, al día", "Descubre lo que comparte tu comunidad") }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = state.selectedCategoryId == null, onClick = { onCategory(null) }, label = { Text("Todas") })
                state.categories.forEach { category ->
                    FilterChip(selected = state.selectedCategoryId == category.id,
                        onClick = { onCategory(category.id) }, label = { Text(category.label) })
                }
            }
        }
        item {
            // Se conserva el lenguaje visual previo, sin fingir geolocalización.
            StylizedMap(Modifier.fillMaxWidth().height(160.dp))
            Text("Vista ilustrativa de Popayán · sin ubicación en tiempo real",
                style = MaterialTheme.typography.labelSmall, color = Muted)
        }
        item {
            Button(onClick = onReport, enabled = !state.loading && state.error == null && state.categories.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Teal)) {
                Text("Reportar una novedad")
            }
        }
        item { SectionTitle("Novedades", "${state.visibleReports.size} ${if (state.visibleReports.size == 1) "novedad compartida" else "novedades compartidas"}") }
        if (state.visibleReports.isEmpty() && !state.loading) item { EmptyState("Todo tranquilo por aquí", "Cuando tu comunidad publique novedades, aparecerán en esta sección.") }
        items(state.visibleReports, key = { it.id }) { report -> ReportCard(report) }
    }
}
@Composable
private fun StylizedMap(modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE9EFE9))) {
        Canvas(Modifier.fillMaxSize()) {
            val road = Color.White
            for (x in 0..5) drawLine(road, Offset(size.width * x / 5f, 0f), Offset(size.width * (x + 1) / 6f, size.height), 13f, StrokeCap.Round)
            for (y in 1..4) drawLine(road, Offset(0f, size.height * y / 5f), Offset(size.width, size.height * (y + .2f) / 5f), 11f, StrokeCap.Round)
            val river = Path().apply {
                moveTo(size.width * .08f, size.height * .85f)
                cubicTo(size.width * .25f, size.height * .55f, size.width * .62f, size.height * .65f, size.width * .95f, size.height * .18f)
            }
            drawPath(river, Color(0xFF82C9E8), style = Stroke(12f, cap = StrokeCap.Round))
            val markers = listOf(
                Triple(.25f, .28f, Color(0xFFE8505B)), Triple(.55f, .43f, Color(0xFFF4A340)),
                Triple(.73f, .20f, Color(0xFF2E77D0)), Triple(.68f, .76f, Color(0xFF31A66A)),
            )
            markers.forEach { (x,y,color) ->
                drawCircle(Color.White, 18f, Offset(size.width*x,size.height*y))
                drawCircle(color, 12f, Offset(size.width*x,size.height*y))
            }
            drawCircle(Color.White, 19f, Offset(size.width*.42f,size.height*.58f))
            drawCircle(Teal, 13f, Offset(size.width*.42f,size.height*.58f))
        }
    }
}
