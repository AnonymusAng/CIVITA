package co.edu.uniautonoma.civita.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import co.edu.uniautonoma.civita.model.ReportDraft
import co.edu.uniautonoma.civita.ui.CivitaUiState
import co.edu.uniautonoma.civita.ui.components.*
import co.edu.uniautonoma.civita.ui.theme.*

@Composable
fun ReportScreen(state: CivitaUiState, onPublish: (ReportDraft) -> Unit, onDone: () -> Unit) {
    var categoryId by rememberSaveable { mutableStateOf("") }
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var latitude by rememberSaveable { mutableStateOf("") }
    var longitude by rememberSaveable { mutableStateOf("") }
    var coordinatesError by remember { mutableStateOf<String?>(null) }
    var severity by rememberSaveable { mutableFloatStateOf(2f) }
    val success = state.publishedId
    val editable = !state.submitting && success == null
    Column(Modifier.fillMaxSize().background(SurfaceLight).verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CivitaBrand()
        SectionTitle("Nueva novedad", "Comparte información útil y verificable")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.categories.forEach { item ->
                FilterChip(selected = categoryId == item.id, onClick = { categoryId = item.id },
                    enabled = editable, label = { Text(item.label) })
            }
        }
        OutlinedTextField(title, { title = it }, enabled = editable, label = { Text("Título") },
            supportingText = { Text("Entre 5 y 120 caracteres") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(description, { description = it }, enabled = editable, label = { Text("Descripción") },
            supportingText = { Text("Entre 10 y 2000 caracteres") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
        OutlinedTextField(location, { location = it }, enabled = editable,
            label = { Text("Referencia del lugar") }, placeholder = { Text("Ej. Carrera 6 con calle 8, Popayán") },
            supportingText = { Text("Ubicación ingresada por ti; esta versión no usa GPS") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(latitude, { latitude=it }, enabled=editable, label={Text("Latitud (opcional)")}, modifier=Modifier.fillMaxWidth())
        OutlinedTextField(longitude, { longitude=it }, enabled=editable, label={Text("Longitud (opcional)")}, modifier=Modifier.fillMaxWidth())
        Text("Si ingresas coordenadas, usa ambas en formato decimal. No se obtiene tu ubicación automáticamente.",style=MaterialTheme.typography.bodySmall,color=Muted)
        coordinatesError?.let { Text(it,color=MaterialTheme.colorScheme.error) }
        Text("Nivel de afectación: ${severity.toInt()} de 3")
        Slider(value = severity, onValueChange = { severity = it }, enabled = editable, valueRange = 1f..3f, steps = 1)
        Text("Comparte una descripción y una referencia clara del lugar.",
            style = MaterialTheme.typography.bodySmall, color = Muted)
        state.publicationError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (success != null) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Tu novedad ya fue publicada", style = MaterialTheme.typography.titleMedium)
                    Text("Gracias por mantener informada a tu comunidad.")
                    Button(onClick = onDone) { Text("Ver novedades") }
                }
            }
        } else {
            Button(onClick = { if ((latitude.isNotBlank() || longitude.isNotBlank()) && (latitude.toDoubleOrNull()==null || longitude.toDoubleOrNull()==null)) {
                    coordinatesError="Ingresa ambas coordenadas en formato decimal o deja ambas vacías."
                } else { coordinatesError=null; onPublish(ReportDraft(categoryId, title, description, location, severity.toInt(),latitude.toDoubleOrNull(),longitude.toDoubleOrNull())) } },
                enabled = editable && !state.loading && state.error == null && state.config != null,
                modifier = Modifier.fillMaxWidth()) {
                Text(if (state.submitting) "Publicando…" else "Publicar reporte")
            }
            if (state.submitting) Text("Esperando confirmación del servidor. No vuelvas a pulsar Publicar.",
                style = MaterialTheme.typography.bodySmall)
        }
    }
}
