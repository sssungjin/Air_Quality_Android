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
            // 위치 권한 체크
            if (!hasLocationPermission()) {
                println("LocationManager: No location permission")
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            // GPS 활성화 체크
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                println("LocationManager: GPS is disabled")
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            // 마지막 알려진 위치 먼저 확인
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (lastKnownLocation != null) {
                    println("LocationManager: Using last known location - Lat: ${lastKnownLocation.latitude}, Lng: ${lastKnownLocation.longitude}")
                    continuation.resume(Pair(lastKnownLocation.latitude, lastKnownLocation.longitude))
                    return@suspendCancellableCoroutine
                }
            } else {
                println("LocationManager: No location permission")
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            val locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    println("LocationManager: New location received - Lat: ${location.latitude}, Lng: ${location.longitude}")
                    continuation.resume(Pair(location.latitude, location.longitude))
                    locationManager.removeUpdates(this)
                }

                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                    println("LocationManager: Provider status changed - $provider: $status")
                }

                override fun onProviderEnabled(provider: String) {
                    println("LocationManager: Provider enabled - $provider")
                }

                override fun onProviderDisabled(provider: String) {
                    println("LocationManager: Provider disabled - $provider")
                    if (provider == LocationManager.GPS_PROVIDER) {
                        continuation.resume(null)
                        locationManager.removeUpdates(this)
                    }
                }
            }

            // GPS와 Network provider 모두 시도
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            var locationUpdateRequested = false

            for (provider in providers) {
                if (locationManager.isProviderEnabled(provider)) {
                    try {
                        locationManager.requestLocationUpdates(
                            provider,
                            1000L, // 1초마다 업데이트
                            1f,    // 1미터 이상 움직였을 때
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

            // 타임아웃 설정
            Handler(Looper.getMainLooper()).postDelayed({
                if (!continuation.isCompleted) {
                    println("LocationManager: Location request timed out")
                    continuation.resume(null)
                    locationManager.removeUpdates(locationListener)
                }
            }, 10000)

            continuation.invokeOnCancellation {
                println("LocationManager: Location request cancelled")
                locationManager.removeUpdates(locationListener)
            }

        } catch (e: Exception) {
            println("LocationManager: Error getting location - ${e.message}")
            e.printStackTrace()
            continuation.resume(null)
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