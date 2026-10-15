package co.edu.uniautonoma.civita.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import co.edu.uniautonoma.civita.ui.AuthUiState
import co.edu.uniautonoma.civita.ui.theme.*

@Composable
fun AuthScreen(state: AuthUiState, onEmail: (String, String, Boolean) -> Unit,
    onGoogle: () -> Unit, onReset: (String) -> Unit, onModeChange: () -> Unit) {
    var register by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    // Password is intentionally never saved in the instance state.
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Navy, NavySoft, TealDark)))
        .safeDrawingPadding().imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(8.dp))
        Surface(shape = RoundedCornerShape(24.dp), color = Color.White.copy(alpha = .13f)) {
            Icon(Icons.Outlined.LocationOn, null, Modifier.padding(8.dp).size(24.dp), tint = Color.White)
        }
        Spacer(Modifier.height(8.dp))
        Text("CIVITA", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold,
            color = Color.White)
        Text("Tu ciudad. Tu voz.", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFBFE7E4))
        Spacer(Modifier.height(12.dp))
        Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (register) "Crea tu cuenta" else "Bienvenido de nuevo",
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Navy)
                Text(if (register) "Conecta con tu comunidad y comparte novedades." else "Conecta con tu comunidad en Popayán.",
                    style = MaterialTheme.typography.bodyMedium, color = Muted)
                OutlinedButton(onClick = onGoogle, enabled = !state.busy && !state.initializing,
                    modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) {
                    Text("G", fontWeight = FontWeight.Bold, color = Teal, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.width(12.dp)); Text("Continuar con Google")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HorizontalDivider(Modifier.weight(1f)); Text("  o con tu correo  ", color = Muted, style = MaterialTheme.typography.labelMedium)
                    HorizontalDivider(Modifier.weight(1f))
                }
                OutlinedTextField(value = email, onValueChange = { email = it; localError = null },
                    label = { Text("Correo electrónico") }, leadingIcon = { Icon(Icons.Outlined.MailOutline, null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true,
                    enabled = !state.busy, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp))
                OutlinedTextField(value = password, onValueChange = { password = it; localError = null },
                    label = { Text("Contraseña") }, leadingIcon = { Icon(Icons.Outlined.Lock, null) },
                    trailingIcon = { IconButton(onClick = { visible = !visible }) {
                        Icon(if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            if (visible) "Ocultar contraseña" else "Mostrar contraseña") } },
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), singleLine = true,
                    enabled = !state.busy, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp))
                if (register) {
                    OutlinedTextField(value = confirmation, onValueChange = { confirmation = it; localError = null },
                        label = { Text("Confirmar contraseña") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        enabled = !state.busy, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp))
                    Text("Usa al menos 8 caracteres.", color = Muted, style = MaterialTheme.typography.labelMedium)
                } else TextButton(onClick = { onReset(email) }, enabled = !state.busy,
                    modifier = Modifier.align(Alignment.End)) { Text("¿Olvidaste tu contraseña?") }
                (localError ?: state.error)?.let { Text(it, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium) }
                state.message?.let { Text(it, color = TealDark, style = MaterialTheme.typography.bodyMedium) }
                Button(onClick = {
                    if (register && password != confirmation) localError = "Las contraseñas no coinciden."
                    else onEmail(email, password, register)
                }, enabled = !state.busy && !state.initializing, modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(14.dp)) {
                    if (state.busy) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text(if (register) "Crear cuenta" else "Iniciar sesión", fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = { register = !register; password = ""; confirmation = ""; localError = null; onModeChange() },
                    enabled = !state.busy, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(if (register) "Ya tengo cuenta · Iniciar sesión" else "¿Eres nuevo? Crea una cuenta")
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("Hecha para una comunidad más conectada", color = Color(0xFFBFE7E4), style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(16.dp))
        }
        Text("By: Joan Erazo v.10742", fontStyle = FontStyle.Italic, color = Color.White,
            style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))
    }
}
