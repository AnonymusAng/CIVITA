package co.edu.uniautonoma.civita.data

import android.content.Context
import co.edu.uniautonoma.civita.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class AppContainer(context: Context) {
    private val app = FirebaseApp.getApps(context).firstOrNull() ?: FirebaseApp.initializeApp(context)
    private val auth = app?.let { FirebaseAuth.getInstance(it) }
    private val db = app?.let { FirebaseFirestore.getInstance(it) }
    private val storage = app?.let { FirebaseStorage.getInstance(it) }
    init {
        if (BuildConfig.USE_FIREBASE_EMULATORS) {
            auth?.useEmulator("10.0.2.2", 9099)
            db?.useEmulator("10.0.2.2", 8080)
            storage?.useEmulator("10.0.2.2", 9199)
        }
    }
    val repository: ReportRepository? = if (auth != null && db != null) FirebaseReportRepository(auth, db) else null
    val authRepository: AuthRepository? = auth?.let { AuthRepository(it) }
    val communityRepository: CommunityRepository? = if (auth != null && db != null && storage != null) CommunityRepository(auth, db, storage) else null
}
