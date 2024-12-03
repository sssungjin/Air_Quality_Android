package com.monorama.airmonomatekr.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class LocationManager @Inject constructor(
    private val context: Context
) {
    suspend fun getCurrentLocation(): Pair<Double, Double>? = suspendCancellableCoroutine { continuation ->
        try {
            if (!hasLocationPermission()) {
                println("LocationManager: No location permission")
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
                !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                println("LocationManager: No location provider enabled")
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            val locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    println("LocationManager: New location received - Lat: ${location.latitude}, Lng: ${location.longitude}")
                    if (!continuation.isCompleted) {
                        continuation.resume(Pair(location.latitude, location.longitude))
                        locationManager.removeUpdates(this)
                    }
                }

                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                    println("LocationManager: Provider status changed - $provider: $status")
                }

                override fun onProviderEnabled(provider: String) {
                    println("LocationManager: Provider enabled - $provider")
                }

                override fun onProviderDisabled(provider: String) {
                    println("LocationManager: Provider disabled - $provider")
                    // 모든 provider가 비활성화된 경우에만 null 반환
                    if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
                        !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                        if (!continuation.isCompleted) {
                            continuation.resume(null)
                            locationManager.removeUpdates(this)
                        }
                    }
                }
            }

            // GPS와 Network provider 모두 시도
            var locationUpdateRequested = false
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)

            for (provider in providers) {
                if (locationManager.isProviderEnabled(provider)) {
                    try {
                        if (ActivityCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                        }
                        locationManager.requestLocationUpdates(
                            provider,
                            0L,    // 즉시 업데이트
                            0f,    // 모든 위치 변화 감지
                            locationListener,
                            Looper.getMainLooper()
                        )
                        locationUpdateRequested = true
                        println("LocationManager: Requested updates from $provider")
                    } catch (e: Exception) {
                        println("LocationManager: Error requesting updates from $provider - ${e.message}")
                    }
                }
            }

            if (!locationUpdateRequested) {
                println("LocationManager: No location provider available")
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            // 타임아웃 설정 (5초)
            Handler(Looper.getMainLooper()).postDelayed({
                if (!continuation.isCompleted) {
                    println("LocationManager: Location request timed out")
                    continuation.resume(null)
                    locationManager.removeUpdates(locationListener)
                }
            }, 5000)

            continuation.invokeOnCancellation {
                println("LocationManager: Location request cancelled")
                locationManager.removeUpdates(locationListener)
            }

        } catch (e: Exception) {
            println("LocationManager: Error getting location - ${e.message}")
            e.printStackTrace()
            if (!continuation.isCompleted) {
                continuation.resume(null)
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }
}