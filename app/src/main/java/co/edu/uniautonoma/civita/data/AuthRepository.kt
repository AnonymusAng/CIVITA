package co.edu.uniautonoma.civita.data

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.ClearCredentialStateRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AuthRepository(private val auth: FirebaseAuth) {
    fun observeSession() = callbackFlow<FirebaseUser?> {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.takeUnless { user -> user.isAnonymous }) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }
    suspend fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
    }
    suspend fun register(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email.trim(), password).await()
    }
    suspend fun resetPassword(email: String) { auth.sendPasswordResetEmail(email.trim()).await() }
    suspend fun google(context: Context) {
        val generatedId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        val manualId = context.resources.getIdentifier("google_web_client_id", "string", context.packageName)
        val clientId = if (generatedId != 0) context.getString(generatedId) else if (manualId != 0) context.getString(manualId) else ""
        check(clientId.isNotBlank()) { "Activa Google en Firebase y descarga nuevamente google-services.json. Falta el cliente OAuth web." }
        val option = GetSignInWithGoogleOption.Builder(clientId).build()
        val result = CredentialManager.create(context).getCredential(context,
            GetCredentialRequest.Builder().addCredentialOption(option).build())
        check(result.credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) { "Google no devolvió una credencial compatible." }
        val token = GoogleIdTokenCredential.createFrom(result.credential.data).idToken
        auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
    }
    fun logout() { auth.signOut() }
    suspend fun clearGoogleSession(context: Context) {
        CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
    }
}
