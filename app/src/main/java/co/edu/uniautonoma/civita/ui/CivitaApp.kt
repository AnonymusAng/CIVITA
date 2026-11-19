package co.edu.uniautonoma.civita.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import co.edu.uniautonoma.civita.data.AppContainer
import co.edu.uniautonoma.civita.model.CivitaDestination
import co.edu.uniautonoma.civita.ui.screens.*

@Composable
fun CivitaApp() {
    if (co.edu.uniautonoma.civita.BuildConfig.SIMULATION) { SimulationApp(); return }
    val context = LocalContext.current.applicationContext
    val container = remember { AppContainer(context) }
    val repository = container.repository
    val authRepository = container.authRepository
    val communityRepository = container.communityRepository
    if (repository == null || authRepository == null || communityRepository == null) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Outlined.CloudOff, contentDescription = null,
                            modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("Conecta CIVITA con Firebase", style = MaterialTheme.typography.headlineSmall)
                        Text("No encontramos la configuración de Firebase para esta instalación.",
                            style = MaterialTheme.typography.bodyLarge)
                        SuggestionChip(onClick = {}, enabled = false,
                            label = { Text("app/google-services.json") })
                        Text("Agrega el archivo cliente de tu aplicación Android, sincroniza Gradle y vuelve a ejecutar. Esta versión no sustituye los datos del proyecto por ejemplos locales.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        return
    }
    val authModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory(authRepository))
    val authState by authModel.state.collectAsStateWithLifecycle()
    val activityContext = LocalContext.current
    if (authState.initializing) {
        CivitaStartupScreen()
        return
    }
    if (authState.uid == null) {
        AuthScreen(authState, authModel::email, { authModel.google(activityContext) },
            authModel::reset, authModel::clearFeedback)
        return
    }
    key(authState.uid) {
    val model: CivitaViewModel = viewModel(key = "reports-${authState.uid}", factory = CivitaViewModel.Factory(repository))
    DisposableEffect(model) { onDispose { model.stopObserving() } }
    val state by model.state.collectAsStateWithLifecycle()
    val communityModel: CommunityViewModel = viewModel(key="community-${authState.uid}",factory=CommunityViewModel.Factory(communityRepository))
    DisposableEffect(communityModel) { communityModel.start(); onDispose { communityModel.stop() } }
    val community by communityModel.state.collectAsStateWithLifecycle()
    var preferences by rememberSaveable { mutableStateOf(false) }
    var destinationName by rememberSaveable { mutableStateOf(CivitaDestination.Map.name) }
    val destination = CivitaDestination.valueOf(destinationName)
    Scaffold(bottomBar = {
        NavigationBar {
            CivitaDestination.entries.forEach { item ->
                val icon = when (item) {
                    CivitaDestination.Map -> Icons.Outlined.Map
                    CivitaDestination.Reports -> Icons.Outlined.AddCircleOutline
                    CivitaDestination.Alerts -> Icons.Outlined.Groups
                    CivitaDestination.Profile -> Icons.Outlined.AccountCircle
                }
                NavigationBarItem(selected = destination == item,
                    onClick = { communityModel.closeDetail(); preferences=false; destinationName = item.name },
                    icon = { Icon(icon, contentDescription = item.label) }, label = { Text(item.label) })
            }
        }
    }) { padding ->
        Column(Modifier.padding(padding)) {
            if (community.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            community.error?.let { error -> Card(Modifier.fillMaxWidth().padding(12.dp)) { Column(Modifier.padding(12.dp)) { Text(error); TextButton(onClick=communityModel::retry) { Text("Reintentar") } } } }
            community.message?.let { message -> Row(Modifier.fillMaxWidth().padding(horizontal=18.dp)) { Text(message,modifier=Modifier.weight(1f)); TextButton(onClick=communityModel::clearFeedback) { Text("Cerrar") } } }
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.error?.let { error ->
                Card(Modifier.fillMaxWidth().padding(12.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(error)
                        TextButton(onClick = model::reload) { Text("Reintentar conexión") }
                    }
                }
            }
            if (state.fromCache && !state.loading) {
                Text("Datos en caché · esperando sincronización del servidor",
                    modifier = Modifier.padding(horizontal = 18.dp), style = MaterialTheme.typography.labelMedium)
            }
            Box(Modifier.weight(1f)) {
                if (community.selectedId != null) {
                    ReportDetailScreen(community.detailReport ?: state.reports.firstOrNull { it.id == community.selectedId },community,requireNotNull(authState.uid),communityModel,communityModel::closeDetail)
                } else if (preferences) {
                    PreferencesScreen(community,state.categories,communityModel) { preferences=false }
                } else when (destination) {
                    CivitaDestination.Map -> MapScreen(state, model::selectCategory, communityModel::select) {
                        model.clearPublication(); destinationName = CivitaDestination.Reports.name
                    }
                    CivitaDestination.Reports -> ReportScreen(state, model::publish) {
                        model.clearPublication(); model.selectCategory(null)
                        destinationName = CivitaDestination.Map.name
                    }
                    CivitaDestination.Alerts -> CommunityScreen(state.reports,community,communityModel::select,communityModel::markRead,communityModel::selectLayer)
                    CivitaDestination.Profile -> ProfileScreen(state, authState, { preferences=true }) { authModel.logout(activityContext) }
                }
            }
        }
    }
}
}

@Composable
private fun CivitaStartupScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Outlined.LocationOn, contentDescription = null,
                modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            Text("CIVITA", style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary)
            CircularProgressIndicator()
            Text("Comprobando tu sesión…", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
