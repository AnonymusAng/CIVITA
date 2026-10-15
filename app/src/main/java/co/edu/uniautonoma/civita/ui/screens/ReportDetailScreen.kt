package co.edu.uniautonoma.civita.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import co.edu.uniautonoma.civita.model.*
import co.edu.uniautonoma.civita.ui.*
import co.edu.uniautonoma.civita.ui.components.*
import co.edu.uniautonoma.civita.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ReportDetailScreen(report: UrbanReport?, state: CommunityUiState, uid: String, model: CommunityActions, onBack: ()->Unit) {
    val context = LocalContext.current; val scope = rememberCoroutineScope()
    var body by rememberSaveable { mutableStateOf("") }; var reason by rememberSaveable { mutableStateOf("") }
    var openError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(state.message) { if (state.message in listOf("Comentario publicado", "Comentario de simulación publicado")) body="" }
    val id = state.selectedId ?: return
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) model.upload(id,context,uri) }
    Column(Modifier.fillMaxSize().background(SurfaceLight).verticalScroll(rememberScrollState()).padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        TextButton(onClick=onBack) { Text("← Volver a comunidad") }
        if (report == null) { EmptyState("Reporte no disponible", "Puede estar fuera de las últimas 100 novedades. Consulta un reporte reciente."); return@Column }
        ReportCard(report)
        if (state.loadingDetail) LinearProgressIndicator(Modifier.fillMaxWidth())
        val confirms = state.validations.count { it.value == "confirm" }; val denies = state.validations.count { it.value == "deny" }
        Text("$confirms confirmaciones · $denies desmentidos · ${state.supports.size} apoyos",style=MaterialTheme.typography.titleSmall)
        Text("Las validaciones expresan participación ciudadana. La verificación oficial corresponde a moderación.",style=MaterialTheme.typography.bodySmall,color=Muted)
        val open = report.status != ReportStatus.Resolved
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            OutlinedButton(enabled=!state.busy && open && report.creatorId!=uid,onClick={model.validate(id,"confirm")}) { Text(if(state.validations.any{it.userId==uid && it.value=="confirm"}) "✓ Confirmado" else "Confirmar") }
            OutlinedButton(enabled=!state.busy && open && report.creatorId!=uid,onClick={model.validate(id,"deny")}) { Text("Desmentir") }
        }
        OutlinedButton(enabled=!state.busy && open,onClick={model.support(id)}) { Text(if(state.supports.any{it.userId==uid}) "Retirar mi apoyo" else "Apoyar reporte") }
        SectionTitle("Conversación")
        if (state.comments.isEmpty()) Text("Aún no hay comentarios. Aporta información útil.",color=Muted)
        state.comments.forEach { comment -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)) {
            Text(comment.authorName,style=MaterialTheme.typography.labelLarge)
            Text(if(comment.hidden) "Comentario oculto por moderación" else comment.body)
        } } }
        OutlinedTextField(body,{body=it.take(1000)},label={Text("Tu comentario")},modifier=Modifier.fillMaxWidth(),enabled=!state.busy && open)
        Button(enabled=!state.busy && open && body.isNotBlank(),onClick={model.comment(id,body)}) { Text("Enviar comentario") }
        SectionTitle("Evidencias")
        Text("El autor puede adjuntar hasta 3 archivos JPEG, PNG o MP4, de máximo 10 MiB cada uno.",style=MaterialTheme.typography.bodySmall)
        state.evidence.forEach { item -> OutlinedButton(enabled=!state.busy,onClick={ scope.launch {
            try { val url=model.evidenceUrl(item.storagePath); context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url))) }
            catch (_: Exception) { openError="No se pudo abrir la evidencia. Verifica la conexión y Cloud Storage." }
        }}) { Text("Abrir ${item.mediaType} · ${item.sizeBytes/1024} KiB") } }
        openError?.let { Text(it,color=MaterialTheme.colorScheme.error) }
        if(report.creatorId==uid) OutlinedButton(enabled=!state.busy && open && state.evidence.size<3,onClick={picker.launch(arrayOf("image/jpeg","image/png","video/mp4"))}) { Text("Adjuntar evidencia como autor") }
        SectionTitle("Historial")
        state.history.forEach { Text("${it.fromStatus} → ${it.toStatus}\n${it.reason}",style=MaterialTheme.typography.bodySmall) }
        OutlinedTextField(reason,{reason=it.take(500)},label={Text("Motivo para moderación")},modifier=Modifier.fillMaxWidth(),minLines=2,enabled=!state.busy)
        TextButton(enabled=!state.busy && reason.trim().length>=10,onClick={model.flag(id,reason)}) { Text("Denunciar contenido") }
        if(state.moderator) {
            SectionTitle("Moderación", "El cambio queda registrado y notifica al autor")
            ReportStatus.entries.forEach { status -> OutlinedButton(enabled=!state.busy && reason.trim().length>=10 && report.status!=status,onClick={model.moderate(id,status.documentValue,reason)}) { Text("Cambiar a ${status.label}") } }
        }
        Spacer(Modifier.height(12.dp))
    }
}
