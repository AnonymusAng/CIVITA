package co.edu.uniautonoma.civita.ui

import android.content.Context
import android.net.Uri

interface CommunityActions {
    fun saveProfile(name: String)
    fun preferences(radius: Long, ids: List<String>)
    fun comment(id: String, body: String)
    fun validate(id: String, value: String)
    fun support(id: String)
    fun flag(id: String, reason: String)
    fun moderate(id: String, status: String, reason: String)
    fun upload(id: String, context: Context, uri: Uri)
    fun markRead(id: String)
    fun subscribe(category: String, lat: Double, lon: Double, radius: Long)
    fun unsubscribe(id: String)
    suspend fun evidenceUrl(path: String): String
}
