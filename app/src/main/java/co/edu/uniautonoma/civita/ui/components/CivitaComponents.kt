package co.edu.uniautonoma.civita.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import co.edu.uniautonoma.civita.model.ReportStatus
import co.edu.uniautonoma.civita.model.UrbanReport
import co.edu.uniautonoma.civita.ui.theme.Muted
import co.edu.uniautonoma.civita.ui.theme.Navy
import co.edu.uniautonoma.civita.ui.theme.Teal

@Composable
fun CivitaBrand(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).background(Teal, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Color.White)
        }
        Column(Modifier.padding(start = 10.dp)) {
            Text("CIVITA", style = MaterialTheme.typography.titleLarge, color = Navy, fontWeight = FontWeight.ExtraBold)
            Text("Popayán se informa", style = MaterialTheme.typography.bodyMedium, color = Muted)
        }
    }
}

@Composable
fun SectionTitle(title: String, subtitle: String? = null, action: String? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = Navy)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Muted)
        }
        if (action != null) Text(action, color = Teal, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun StatusPill(status: ReportStatus) {
    val (bg, fg) = when (status) {
        ReportStatus.Review -> Color(0xFFFFF0D8) to Color(0xFF996000)
        ReportStatus.Verified -> Color(0xFFDDF5EC) to Color(0xFF087A5A)
        ReportStatus.Resolved -> Color(0xFFE7ECF2) to Color(0xFF506074)
    }
    Surface(color = bg, shape = RoundedCornerShape(50)) {
        Text(status.label, color = fg, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
    }
}

@Composable
fun ReportCard(report: UrbanReport, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.size(12.dp).background(report.color, CircleShape))
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(report.title, style = MaterialTheme.typography.titleMedium, color = Navy)
                Text(report.description, style = MaterialTheme.typography.bodyMedium, color = Muted)
                Text("${report.category} · Afectación ${report.severity}/3", style = MaterialTheme.typography.labelLarge, color = Teal)
                Text("${report.location} · ${report.time}", style = MaterialTheme.typography.bodyMedium, color = Muted)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    StatusPill(report.status)
                    Text("Ver participación en Comunidad", style = MaterialTheme.typography.labelLarge, color = Teal)
                }
            }
        }
    }
}

@Composable
fun EmptyState(title: String, description: String) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Navy)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = Muted)
        }
    }
}
