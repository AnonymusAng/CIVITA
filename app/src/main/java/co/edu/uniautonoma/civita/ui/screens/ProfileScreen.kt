package co.edu.uniautonoma.civita.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import co.edu.uniautonoma.civita.ui.CivitaUiState
import co.edu.uniautonoma.civita.ui.AuthUiState
import co.edu.uniautonoma.civita.ui.components.*
import co.edu.uniautonoma.civita.ui.theme.*

@Composable
fun ProfileScreen(state: CivitaUiState, authState: AuthUiState, onPreferences: () -> Unit, onLogout: () -> Unit) {
    Column(Modifier.fillMaxSize().background(SurfaceLight).verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CivitaBrand()
        SectionTitle("Tu perfil", authState.displayName ?: "Ciudadano CIVITA")
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(authState.email ?: "Cuenta Google", style = MaterialTheme.typography.titleMedium)
                Text("Gestiona tu cuenta y participa en tu comunidad.", style = MaterialTheme.typography.bodyMedium)
                Text("${state.categories.size} categorías · ${state.reports.size} novedades disponibles")
            }
        }
        Button(onClick = onPreferences, modifier = Modifier.fillMaxWidth()) { Text("Mi perfil y preferencias") }
        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { Text("Cerrar sesión") }
        Text("By: Joan Erazo v.10742", style = MaterialTheme.typography.bodySmall,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = Muted)
    }
}
