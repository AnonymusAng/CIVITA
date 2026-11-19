package co.edu.uniautonoma.civita.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import co.edu.uniautonoma.civita.model.*
import co.edu.uniautonoma.civita.ui.*
import co.edu.uniautonoma.civita.ui.components.*
import co.edu.uniautonoma.civita.ui.theme.*

@Composable
fun ReportDetailScreen(report: UrbanReport?, state: CommunityUiState, uid: String, model: CommunityActions, onBack: ()->Unit) {
    var body by rememberSaveable { mutableStateOf("") }; var reason by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(state.message) { if (state.message in listOf("Comentario publicado", "Comentario de simulación publicado")) body="" }
    val id = state.selectedId ?: return
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        TextButton(onClick=onBack) { Text("← Volver a comunidad") }
        if (report == null) {
            if (state.loadingDetail) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text("Cargando reporte…", style = MaterialTheme.typography.bodyMedium)
            } else {
                EmptyState("Reporte no disponible", "No se encontró o no tienes acceso a este reporte. Comprueba la conexión e inténtalo de nuevo.")
            }
            return@Column
        }
        ReportCard(report)
        if (report.auditState == "visible") {
            AssistChip(onClick={}, label={ Text("Auditoría de contenido completada") }, enabled=false)
            Text("${report.auditSummary.ifBlank { "Apto para mostrarse; la auditoría no verifica los hechos." }} · La veracidad requiere fuentes o revisión oficial.",
                style=MaterialTheme.typography.bodySmall, color=Muted)
        } else {
            AssistChip(onClick={}, label={ Text("Pendiente de revisión humana") }, enabled=false)
            Text(report.auditSummary.ifBlank { "Este reporte aún no aparece en la comunidad." }, style=MaterialTheme.typography.bodySmall, color=Muted)
        }
        if (state.loadingDetail) LinearProgressIndicator(Modifier.fillMaxWidth())
        val confirms = state.validations.count { it.value == "confirm" }; val denies = state.validations.count { it.value == "deny" }
        Text("${confirms} ${if(confirms==1) "confirmación" else "confirmaciones"} · ${denies} ${if(denies==1) "desmentido" else "desmentidos"} · ${state.supports.size} ${if(state.supports.size==1) "apoyo" else "apoyos"}",style=MaterialTheme.typography.titleSmall)
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
        SectionTitle("Historial")
        state.history.forEach { Text("${historyLabel(it.fromStatus)} → ${historyLabel(it.toStatus)}\n${it.reason}",style=MaterialTheme.typography.bodySmall) }
        OutlinedTextField(reason,{reason=it.take(500)},label={Text("Motivo para moderación")},modifier=Modifier.fillMaxWidth(),minLines=2,enabled=!state.busy)
        TextButton(enabled=!state.busy && reason.trim().length>=10,onClick={model.flag(id,reason)}) { Text("Denunciar contenido") }
        if(state.moderator) {
            if (report.auditState != "visible") {
                SectionTitle("Auditoría humana", "Autoriza la publicación con un motivo registrado")
                OutlinedButton(enabled=!state.busy && reason.trim().length>=10,onClick={model.releaseAudit(id,reason)}) { Text("Autorizar en Comunidad") }
            }
            SectionTitle("Moderación", "El cambio queda registrado y notifica al autor")
            ReportStatus.entries.forEach { status -> OutlinedButton(enabled=!state.busy && reason.trim().length>=10 && report.status!=status,onClick={model.moderate(id,status.documentValue,reason)}) { Text("Cambiar a ${status.label}") } }
        }
        Spacer(Modifier.height(12.dp))
    }
}

private fun historyLabel(value: String): String = when(value) {
    "none" -> "Publicado"
    "review" -> "En revisión"
    "verified" -> "Verificado"
    "resolved" -> "Resuelto"
    else -> "Estado anterior"
}
