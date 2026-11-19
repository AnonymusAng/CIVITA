package co.edu.uniautonoma.civita.ui

interface CommunityActions {
    fun saveProfile(name: String)
    fun preferences(radius: Long, ids: List<String>)
    fun comment(id: String, body: String)
    fun validate(id: String, value: String)
    fun support(id: String)
    fun flag(id: String, reason: String)
    fun moderate(id: String, status: String, reason: String)
    fun releaseAudit(id: String, reason: String)
    fun markRead(id: String)
    fun subscribe(category: String, lat: Double, lon: Double, radius: Long)
    fun unsubscribe(id: String)
}
