package co.edu.uniautonoma.civita.ui

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import co.edu.uniautonoma.civita.model.*
import co.edu.uniautonoma.civita.ui.screens.*

/** Sólo se activa al compilar con -PcivitaSimulation=true; applicationId distinto, sin Firebase. */
@Composable
fun SimulationApp() {
    val categories=remember { listOf(IncidentCategory("movilidad","Movilidad",Color(0xFF1976D2)),IncidentCategory("ambiente","Ambiente",Color(0xFF31A66A)),IncidentCategory("obras","Obras",Color(0xFFF4A340))) }
    var reports by remember { mutableStateOf(listOf(UrbanReport("simulation-report","Cruce peatonal por señalizar","movilidad","Movilidad","Ejemplo de simulación: falta señalización en el cruce peatonal.","Centro de Popayán · ejemplo",2,"Simulación local",ReportStatus.Review,0,Color(0xFF1976D2),"simulation-other"))) }
    var community by remember { mutableStateOf(CommunityUiState(profile=UserProfile("simulation-user","Ciudadano de prueba","simulacion@example.invalid","citizen","active",1),history=listOf(StatusChange("created","simulation-other","none","review","Publicación de ejemplo local")))) }
    var tab by remember { mutableIntStateOf(0) }; var preferences by remember { mutableStateOf(false) }; var publishedId by remember { mutableStateOf<String?>(null) }
    val actions=remember { object : CommunityActions {
        override fun saveProfile(name:String) { community=community.copy(profile=community.profile?.copy(fullName=name),message="Nombre guardado localmente") }
        override fun preferences(radius:Long,ids:List<String>) { community=community.copy(preferences=UserPreferences(radius,ids),message="Preferencias de simulación guardadas") }
        override fun comment(id:String,body:String) { community=community.copy(comments=community.comments+ReportComment("local-${community.comments.size}","simulation-user",community.profile?.fullName.orEmpty(),body,false),message="Comentario de simulación publicado") }
        override fun validate(id:String,value:String) { community=community.copy(validations=community.validations.filterNot{it.userId=="simulation-user"}+ReportValidation("simulation-user",value,1)) }
        override fun support(id:String) { community=community.copy(supports=if(community.supports.isEmpty()) listOf(ReportSupport("simulation-user")) else emptyList()) }
        override fun flag(id:String,reason:String) { community=community.copy(message="Denuncia simulada; no enviada") }
        override fun moderate(id:String,status:String,reason:String) { community=community.copy(message="La simulación no concede roles de moderador") }
        override fun upload(id:String,context:Context,uri:Uri) { community=community.copy(message="Archivos disponibles en la versión conectada a Cloud Storage") }
        override fun markRead(id:String) { community=community.copy(notifications=community.notifications.map{if(it.id==id) it.copy(read=true) else it}) }
        override fun subscribe(category:String,lat:Double,lon:Double,radius:Long) { community=community.copy(subscriptions=community.subscriptions.filterNot{it.id==category}+AlertSubscription(category,category,lat,lon,radius,true)) }
        override fun unsubscribe(id:String) { community=community.copy(subscriptions=community.subscriptions.filterNot{it.id==id}) }
        override suspend fun evidenceUrl(path:String):String = error("Sin evidencias reales en simulación")
    } }
    Scaffold(bottomBar={NavigationBar { listOf("Mapa","Publicar","Comunidad","Perfil").forEachIndexed { index,label -> NavigationBarItem(selected=tab==index,onClick={tab=index;preferences=false;community=community.copy(selectedId=null,message=null)},icon={Text(listOf("◎","+","◉","○")[index])},label={Text(label)}) } }}) { padding ->
        Column(Modifier.padding(padding)) {
            Surface(color=Color(0xFFFFF0D8)) { Text("SIMULACIÓN LOCAL · Sin conexión a Firebase",Modifier.fillMaxWidth().padding(8.dp),style=MaterialTheme.typography.labelMedium) }
            community.message?.let { Text(it,Modifier.padding(12.dp)) }
            val civita=CivitaUiState(loading=false,categories=categories,reports=reports,uid="simulation-user",config=AppConfig(listOf("image/jpeg","image/png","video/mp4"),10485760,3,250,15000),publishedId=publishedId)
            if(community.selectedId!=null) ReportDetailScreen(reports.firstOrNull{it.id==community.selectedId},community,"simulation-user",actions) {community=community.copy(selectedId=null)}
            else if(preferences) PreferencesScreen(community,categories,actions) {preferences=false}
            else when(tab) {
                0 -> MapScreen(civita,{}) {tab=1}
                1 -> ReportScreen(civita,{ draft ->
                    val error=draft.validationError()
                    if(error!=null) community=community.copy(message=error) else {
                        val id="local-${reports.size}";publishedId=id
                        reports=listOf(UrbanReport(id,draft.title,draft.categoryId,categories.first{it.id==draft.categoryId}.label,draft.description,draft.location,draft.severity,"Ahora · simulación",ReportStatus.Review,0,categories.first{it.id==draft.categoryId}.color,"simulation-user"))+reports
                    }
                }) { publishedId=null;tab=2 }
                2 -> CommunityScreen(reports,community,{community=community.copy(selectedId=it)},actions::markRead)
                3 -> ProfileScreen(civita,AuthUiState(uid="simulation-user",email="simulacion@example.invalid",displayName=community.profile?.fullName),{preferences=true}) { community=community.copy(message="Sesión de prueba local; no hay cuenta Firebase") }
            }
        }
    }
}
