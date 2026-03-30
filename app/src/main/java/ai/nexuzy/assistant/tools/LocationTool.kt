package ai.nexuzy.assistant.tools

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

/**
 * LocationTool: Gets GPS location via FusedLocationProviderClient.
 *
 * GOOGLE MAPS API KEY SETUP:
 * ─────────────────────────────────────────────────────────────────────
 * 1. Go to: https://console.cloud.google.com/
 * 2. Create project → Enable "Maps SDK for Android" + "Geocoding API"
 * 3. Create API Key → restrict to your app's package + SHA-1
 * 4. Add to local.properties:
 *    MAPS_API_KEY=AIzaSy...your_key_here
 * 5. The key is auto-injected into AndroidManifest.xml via build.gradle
 *    <meta-data android:name="com.google.android.geo.API_KEY" android:value="${MAPS_API_KEY}" />
 * ─────────────────────────────────────────────────────────────────────
 *
 * The Geocoder below reverse-geocodes GPS coords → human city name
 * using the Maps API key (injected automatically via manifest).
 */
class LocationTool(private val context: Context) {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getLastKnownLocation(): Location? =
        suspendCancellableCoroutine { cont ->
            val cts = CancellationTokenSource()
            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { loc -> cont.resume(loc) }
                .addOnFailureListener {
                    // Fallback to last known
                    fusedClient.lastLocation
                        .addOnSuccessListener { loc -> cont.resume(loc) }
                        .addOnFailureListener { cont.resume(null) }
                }
            cont.invokeOnCancellation { cts.cancel() }
        }

    /**
     * Reverse geocodes GPS → city name using Android Geocoder (requires Maps API).
     * Falls back to "Kolkata, India" if location unavailable.
     */
    @Suppress("DEPRECATION")
    suspend fun getLocationSystemPrompt(): String {
        val loc = getLastKnownLocation() ?: return defaultPrompt()
        return try {
            val geocoder = Geocoder(context, Locale.ENGLISH)
            val addresses = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
            val city = addresses?.firstOrNull()?.locality ?: "Kolkata"
            val state = addresses?.firstOrNull()?.adminArea ?: "West Bengal"
            val country = addresses?.firstOrNull()?.countryName ?: "India"
            "System: User location = $city, $state, $country (${loc.latitude}, ${loc.longitude})"
        } catch (e: Exception) {
            "System: User location = Kolkata, West Bengal, India (${loc.latitude}, ${loc.longitude})"
        }
    }

    private fun defaultPrompt() =
        "System: User location = Kolkata, West Bengal, India (default — location permission not granted)"
}
