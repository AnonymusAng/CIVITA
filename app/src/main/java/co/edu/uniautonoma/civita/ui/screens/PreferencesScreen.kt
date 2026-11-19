package co.edu.uniautonoma.civita.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import co.edu.uniautonoma.civita.model.*
import co.edu.uniautonoma.civita.ui.*
import co.edu.uniautonoma.civita.ui.components.*

@Composable
fun PreferencesScreen(state: CommunityUiState, categories: List<IncidentCategory>, model: CommunityActions, onBack: ()->Unit) {
    var name by rememberSaveable(state.profile?.fullName) { mutableStateOf(state.profile?.fullName.orEmpty()) }
    var radius by rememberSaveable(state.preferences.alertRadiusMeters) { mutableFloatStateOf(state.preferences.alertRadiusMeters.toFloat()) }
    var ids by remember(state.preferences.categoryIds) { mutableStateOf(state.preferences.categoryIds) }
    var lat by rememberSaveable { mutableStateOf("") }; var lon by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        TextButton(onClick=onBack) { Text("← Volver al perfil") }; SectionTitle("Tu cuenta", "Preferencias guardadas en tu cuenta")
        OutlinedTextField(name,{name=it.take(100)},label={Text("Nombre público")},modifier=Modifier.fillMaxWidth())
        Button(enabled=!state.busy && name.trim().length>=2,onClick={model.saveProfile(name)}) { Text("Guardar nombre") }
        Text("Radio de interés: ${radius.toInt()} metros")
        Slider(radius,{radius=it},valueRange=250f..15000f,enabled=!state.busy)
        categories.forEach { category -> FilterChip(selected=category.id in ids,onClick={ids=if(category.id in ids) ids-category.id else ids+category.id},label={Text(category.label)}) }
        Button(enabled=!state.busy,onClick={model.preferences(radius.toLong(),ids)}) { Text("Guardar preferencias") }
        SectionTitle("Suscripciones por zona")
        Text("Define un centro con coordenadas. La suscripción queda guardada; el envío automático push no está activado.",style=MaterialTheme.typography.bodySmall)
        OutlinedTextField(lat,{lat=it.take(24)},label={Text("Latitud del centro")},
            keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Ascii),modifier=Modifier.fillMaxWidth(),singleLine=true)
        OutlinedTextField(lon,{lon=it.take(24)},label={Text("Longitud del centro")},
            keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Ascii),modifier=Modifier.fillMaxWidth(),singleLine=true)
        val latitude=lat.toDoubleOrNull(); val longitude=lon.toDoubleOrNull()
        if ((lat.isNotBlank() || lon.isNotBlank()) &&
            (latitude == null || longitude == null || latitude !in -90.0..90.0 || longitude !in -180.0..180.0))
            Text("Ingresa una latitud entre -90 y 90 y una longitud entre -180 y 180.",
                style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error)
        categories.filter { it.id in ids }.forEach { category -> OutlinedButton(enabled=!state.busy && latitude!=null && longitude!=null && latitude in -90.0..90.0 && longitude in -180.0..180.0,onClick={model.subscribe(category.id,requireNotNull(latitude),requireNotNull(longitude),radius.toLong())}) { Text("Suscribirme a ${category.label}") } }
        state.subscriptions.forEach { subscription -> Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            Text("${subscription.categoryId} · ${subscription.radiusMeters} m"); TextButton(enabled=!state.busy,onClick={model.unsubscribe(subscription.id)}) { Text("Quitar") }
        } }
    }
}
