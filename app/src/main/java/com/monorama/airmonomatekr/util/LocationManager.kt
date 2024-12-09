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
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.common.api.ResolvableApiException
import android.app.Activity
import android.content.IntentSender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.os.Build
import com.google.android.gms.location.Priority

@Singleton
class LocationManager @Inject constructor(
    private val context: Context
) {
    private val _locationPermissionNeeded = MutableStateFlow(false)
    val locationPermissionNeeded: StateFlow<Boolean> = _locationPermissionNeeded.asStateFlow()

    suspend fun getCurrentLocation(): Pair<Double, Double>? = suspendCancellableCoroutine { continuation ->
        try {
            // 권한 체크 및 요청
            if (!hasLocationPermission()) {
                println("LocationManager: No location permission")
                _locationPermissionNeeded.value = true
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager

            // GPS 활성화 체크
            if (!locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
                showLocationSettingsDialog()
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            // Fused Location Provider 사용 (Android 10 이상)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
                    .setWaitForAccurateLocation(false)
                    .setMinUpdateIntervalMillis(2000L)
                    .build()

                val locationCallback = object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        locationResult.lastLocation?.let { location ->
                            println("LocationManager: Location received - Lat: ${location.latitude}, Lng: ${location.longitude}")
                            if (!continuation.isCompleted) {
                                continuation.resume(Pair(location.latitude, location.longitude))
                            }
                            // 위치 업데이트 중지
                            LocationServices.getFusedLocationProviderClient(context)
                                .removeLocationUpdates(this)
                        }
                    }
                }

                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    LocationServices.getFusedLocationProviderClient(context)
                        .requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

                    // 타임아웃 설정
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (!continuation.isCompleted) {
                            println("LocationManager: Location request timed out")
                            continuation.resume(null)
                            LocationServices.getFusedLocationProviderClient(context)
                                .removeLocationUpdates(locationCallback)
                        }
                    }, 10000) // 10초 타임아웃
                }
            } else {
                // 낮은 SDK 버전에서는 기존 방식 사용
                val lastKnownLocation = locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)

                if (lastKnownLocation != null) {
                    println("LocationManager: Using last known location")
                    continuation.resume(Pair(lastKnownLocation.latitude, lastKnownLocation.longitude))
                    return@suspendCancellableCoroutine
                }

                // 위치 업데이트 요청
                val locationListener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        println("LocationManager: New location received - Lat: ${location.latitude}, Lng: ${location.longitude}")
                        if (!continuation.isCompleted) {
                            continuation.resume(Pair(location.latitude, location.longitude))
                            locationManager.removeUpdates(this)
                        }
                    }

                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }

                // GPS와 Network provider 모두 시도
                var locationUpdateRequested = false
                val providers = listOf(android.location.LocationManager.GPS_PROVIDER, android.location.LocationManager.NETWORK_PROVIDER)

                for (provider in providers) {
                    if (locationManager.isProviderEnabled(provider)) {
                        try {
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
            }

        } catch (e: Exception) {
            println("LocationManager: Error getting location - ${e.message}")
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

    private fun showLocationSettingsDialog() {
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(
                LocationRequest.create().apply {
                    priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                }
            )

        val client = LocationServices.getSettingsClient(context)
        val task = client.checkLocationSettings(builder.build())

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && context is Activity) {
                try {
                    exception.startResolutionForResult(context, 1001)
                } catch (e: IntentSender.SendIntentException) {
                    e.printStackTrace()
                }
            }
        }
    }
}