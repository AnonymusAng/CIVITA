package co.edu.uniautonoma.civita.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import co.edu.uniautonoma.civita.model.ReportDraft
import co.edu.uniautonoma.civita.ui.CivitaUiState
import co.edu.uniautonoma.civita.ui.components.*
import co.edu.uniautonoma.civita.ui.theme.*

@Composable
fun ReportScreen(state: CivitaUiState, onPublish: (ReportDraft) -> Unit, onDone: () -> Unit) {
    var categoryId by rememberSaveable { mutableStateOf(state.selectedCategoryId.orEmpty()) }
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var latitude by rememberSaveable { mutableStateOf("") }
    var longitude by rememberSaveable { mutableStateOf("") }
    var coordinatesError by remember { mutableStateOf<String?>(null) }
    var severity by rememberSaveable { mutableFloatStateOf(2f) }
    LaunchedEffect(state.categories, state.selectedCategoryId) {
        if (categoryId.isBlank()) categoryId = state.selectedCategoryId
            ?: state.categories.firstOrNull()?.id.orEmpty()
    }
    val success = state.publishedId
    val editable = !state.submitting && success == null
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CivitaBrand()
        SectionTitle("Reportar una novedad", "Ayúdanos a entender qué ocurre en tu ciudad")
        Text("Categoría", style = MaterialTheme.typography.titleSmall)
        if (state.loading && state.categories.isEmpty()) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text("Cargando categorías…", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else if (state.categories.isEmpty()) {
            EmptyState("Categorías no disponibles", "Revisa tu conexión e inténtalo de nuevo.")
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.categories.forEach { item ->
                FilterChip(selected = categoryId == item.id, onClick = { categoryId = item.id },
                    enabled = editable, label = { Text(item.label) })
            }
        }
        OutlinedTextField(title, { title = it.take(120) }, enabled = editable, label = { Text("Título") },
            supportingText = { Text("${title.length}/120 · mínimo 5 caracteres") },
            modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(description, { description = it.take(2000) }, enabled = editable, label = { Text("Descripción") },
            supportingText = { Text("${description.length}/2000 · mínimo 10 caracteres") },
            modifier = Modifier.fillMaxWidth(), minLines = 3)
        OutlinedTextField(location, { location = it }, enabled = editable,
            label = { Text("Referencia del lugar") }, placeholder = { Text("Ej. Carrera 6 con calle 8, Popayán") },
            supportingText = { Text("${location.length}/200 · referencia escrita; GPS automático no está activo") },
            modifier = Modifier.fillMaxWidth(), singleLine = true)
        Text("Coordenadas opcionales", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(latitude, { latitude=it.take(24); coordinatesError=null }, enabled=editable,
                label={Text("Latitud")}, placeholder={Text("2.44")}, keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Ascii),
                modifier=Modifier.weight(1f), singleLine=true)
            OutlinedTextField(longitude, { longitude=it.take(24); coordinatesError=null }, enabled=editable,
                label={Text("Longitud")}, placeholder={Text("-76.61")}, keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Ascii),
                modifier=Modifier.weight(1f), singleLine=true)
        }
        Text("Incluye ambas coordenadas en grados decimales o deja los dos campos vacíos.",
            style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        coordinatesError?.let { Text(it,color=MaterialTheme.colorScheme.error) }
        Text("Nivel de afectación", style=MaterialTheme.typography.titleSmall)
        Text("${severity.toInt()} de 3 · ${listOf("Leve", "Moderada", "Alta")[severity.toInt()-1]}",
            style=MaterialTheme.typography.bodyMedium, color=MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(value = severity, onValueChange = { severity = it }, enabled = editable, valueRange = 1f..3f, steps = 1)
        Text("Comparte una descripción y una referencia clara del lugar.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("El agente propio de CIVITA analiza el texto con reglas deterministas dentro de Firebase; no envía tu reporte a servicios externos. No incluyas datos personales. La auditoría no verifica que el hecho haya ocurrido.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        state.publicationError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (success != null) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Reporte recibido", style = MaterialTheme.typography.titleMedium)
                    Text("La auditora de CIVITA revisará pertinencia y contenido. Si lo aprueba, aparecerá en Comunidad; la IA no confirma que el hecho haya ocurrido.")
                    Button(onClick = onDone) { Text("Ver novedades") }
                }
            }
        } else {
            Button(onClick = { if ((latitude.isNotBlank() || longitude.isNotBlank()) && (latitude.toDoubleOrNull()==null || longitude.toDoubleOrNull()==null)) {
                    coordinatesError="Ingresa ambas coordenadas en formato decimal o deja ambas vacías."
                } else { coordinatesError=null; onPublish(ReportDraft(categoryId, title, description, location, severity.toInt(),latitude.toDoubleOrNull(),longitude.toDoubleOrNull())) } },
                enabled = editable && !state.loading && state.error == null && state.config != null,
                modifier = Modifier.fillMaxWidth().heightIn(min=52.dp)) {
                Text(if (state.submitting) "Publicando…" else "Publicar reporte")
            }
            if (state.submitting) Text("Esperando confirmación del servidor. No vuelvas a pulsar Publicar.",
                style = MaterialTheme.typography.bodySmall)
        }
    }
}
