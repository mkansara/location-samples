/*
 * Copyright (C) 2020 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.gms.location.sample.locationupdatesbackgroundkotlin.data

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.sample.locationupdatesbackgroundkotlin.LocationUpdatesBroadcastReceiver
import com.google.android.gms.location.sample.locationupdatesbackgroundkotlin.hasPermission
import java.util.concurrent.TimeUnit

private const val TAG = "MyLocationManager"

/**
 * Manages all location related tasks for the app.
 */
class MyLocationManager private constructor(private val context: Context) {

    val trackingLocation: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>(false)
    }

    // The Fused Location Provider provides access to location tracking APIs.
    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    // Stores parameters for requests to the FusedLocationProviderApi.
    private val locationRequest: LocationRequest by lazy {
        LocationRequest()
            // Sets the desired interval for active location updates. This interval is inexact. You
            // may not receive updates at all if no location sources are available, or you may
            // receive them slower than requested. You may also receive updates faster than
            // requested if other applications are requesting location at a faster interval.
            //
            // IMPORTANT NOTE: Apps running on "O" devices (regardless of targetSdkVersion) may
            // receive updates less frequently than this interval when the app is no longer in the
            // foreground.
            .setInterval(TimeUnit.SECONDS.toMillis(60))

            // Sets the fastest rate for active location updates. This interval is exact, and your
            // application will never receive updates faster than this value.
            .setFastestInterval(TimeUnit.SECONDS.toMillis(30))

            // Sets the maximum time when batched location updates are delivered. Updates may be
            // delivered sooner than this interval.
            .setMaxWaitTime(TimeUnit.MINUTES.toMillis(2))

            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
    }

    private val locationUpdatePendingIntent: PendingIntent by lazy {
        // API level 26 and above (Oreo+) place limits on Services, so we use a BroadcastReceiver.
        val intent = Intent (context, LocationUpdatesBroadcastReceiver::class.java)
        intent.action = LocationUpdatesBroadcastReceiver.ACTION_PROCESS_UPDATES
        PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    fun startLocationUpdates() {
        Log.d(TAG, "startLocationUpdates()")

        if (!context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) return

        try {
            trackingLocation.value = true
            fusedLocationClient.requestLocationUpdates(locationRequest, locationUpdatePendingIntent)

        } catch (e: SecurityException) {
            trackingLocation.value = false
            e.printStackTrace()
        }
    }

    fun stopLocationUpdates() {
        Log.d(TAG, "stopLocationUpdates()")
        trackingLocation.value = false
        fusedLocationClient.removeLocationUpdates(locationUpdatePendingIntent)
    }

    companion object {
        @Volatile private var INSTANCE: MyLocationManager? = null

        fun getInstance(context: Context): MyLocationManager {

            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MyLocationManager(context).also { INSTANCE = it }
            }
        }
    }
}
