package co.edu.uniautonoma.civita

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import co.edu.uniautonoma.civita.ui.CivitaApp
import co.edu.uniautonoma.civita.ui.theme.CivitaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CivitaTheme { CivitaApp() }
        }
    }
}
