package co.edu.uniautonoma.civita.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import co.edu.uniautonoma.civita.model.*
import co.edu.uniautonoma.civita.ui.*
import co.edu.uniautonoma.civita.ui.components.*
import co.edu.uniautonoma.civita.ui.theme.*
import java.text.DateFormat
import java.util.Date

@Composable
fun CommunityScreen(reports: List<UrbanReport>, community: CommunityUiState, onSelect: (String)->Unit, onRead: (String)->Unit, onLayer: (String)->Unit = {}) {
    var tab by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().background(SurfaceLight)) {
        Column(Modifier.padding(18.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
            CivitaBrand(); SectionTitle("Comunidad", "Participa, consulta y mantente informado")
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                listOf("Reportes","Eventos","Buzón","Capas").forEachIndexed { index,label -> FilterChip(selected=tab==index,onClick={tab=index},label={Text(label)}) }
            }
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding=PaddingValues(horizontal=18.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            when(tab) {
                0 -> {
                    if (reports.isEmpty()) item { EmptyState("Sin reportes", "Publica la primera novedad de tu comunidad.") }
                    items(reports,key={it.id}) { ReportCard(it, Modifier.clickable { onSelect(it.id) }); TextButton(onClick={onSelect(it.id)}) { Text("Ver detalle y participar") } }
                }
                1 -> {
                    if (community.events.isEmpty()) item { EmptyState("Sin eventos programados", "Los eventos oficiales publicados por la administración aparecerán aquí.") }
                    items(community.events,key={it.id}) { event -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text(event.title,style=MaterialTheme.typography.titleMedium); Text(event.description)
                        Text("${DateFormat.getDateTimeInstance().format(Date(event.startsAtMillis))} · ${event.status}",style=MaterialTheme.typography.labelMedium)
                    } } }
                }
                3 -> {
                    item { Text("Capas configuradas y puntos oficiales. La vista principal es un esquema ilustrativo, sin navegación GPS.",style=MaterialTheme.typography.bodySmall) }
                    items(community.layers,key={it.id}) { layer -> OutlinedButton(onClick={onLayer(layer.id)}) { Text(layer.name) } }
                    items(community.features,key={it.id}) { feature -> Text("${feature.name} · ${feature.latitude}, ${feature.longitude}") }
                }
                2 -> {
                    item { Text("Notificaciones dentro de la app. Las suscripciones guardan tus preferencias; el envío push requiere un servicio adicional.",style=MaterialTheme.typography.bodySmall,color=Muted) }
                    if (community.notifications.isEmpty()) item { EmptyState("Estás al día", "Recibirás aquí los cambios de estado de tus reportes.") }
                    items(community.notifications,key={it.id}) { notice -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) {
                        Text(notice.title,style=MaterialTheme.typography.titleMedium); Text(notice.body)
                        TextButton(onClick={onSelect(notice.sourceId)}) { Text("Ver reporte") }
                        if (!notice.read) TextButton(onClick={onRead(notice.id)}) { Text("Marcar como leída") }
                    } } }
                }
            }
        }
    }
}
