package ai.nexuzy.assistant.tools

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * LocationTool: Gets device location using FusedLocationProviderClient.
 * Injects location into AI system prompt for context-aware responses.
 */
class LocationTool(context: Context) {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getLastKnownLocation(): Location? =
        suspendCancellableCoroutine { cont ->
            fusedClient.lastLocation
                .addOnSuccessListener { location -> cont.resume(location) }
                .addOnFailureListener { cont.resume(null) }
        }

    /**
     * Returns a system prompt string to inject user location into the LLM.
     * Default fallback = Kolkata (David's location).
     */
    suspend fun getLocationSystemPrompt(): String {
        val loc = getLastKnownLocation()
        return if (loc != null) {
            "System: User location = ${loc.latitude}, ${loc.longitude} (Kolkata, West Bengal, India)"
        } else {
            "System: User location = Kolkata, West Bengal, India (default)"
        }
    }
}
