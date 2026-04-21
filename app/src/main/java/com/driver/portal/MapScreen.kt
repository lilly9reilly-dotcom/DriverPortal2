package com.driver.portal

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.fillMaxSize
import kotlinx.coroutines.*
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.net.URL

@Composable
fun MapScreen(driverName: String, lat: Double, lng: Double) {

    val context = LocalContext.current
    var drivers by remember { mutableStateOf(listOf<DriverLocation>()) }
    var routeLine by remember { mutableStateOf<Polyline?>(null) }

    // تحديث مواقع السائقين كل 10 ثواني
    LaunchedEffect(true) {
        while (true) {
            drivers = fetchDrivers()
            delay(10000)
        }
    }

    AndroidView(
        factory = {
            Configuration.getInstance().load(
                context,
                context.getSharedPreferences("osm", Context.MODE_PRIVATE)
            )

            val mapView = MapView(context)
            mapView.setTileSource(TileSourceFactory.MAPNIK)
            mapView.setMultiTouchControls(true)

            val mapController = mapView.controller
            mapController.setZoom(14.0)
            mapController.setCenter(GeoPoint(lat, lng))

            mapView
        },
        update = { mapView ->

            mapView.overlays.clear()

            // إعادة رسم المسار إذا موجود
            routeLine?.let {
                mapView.overlays.add(it)
            }

            // موقعي أنا
            val myMarker = Marker(mapView)
            myMarker.position = GeoPoint(lat, lng)
            myMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            myMarker.title = "أنا"
            mapView.overlays.add(myMarker)

            // السائقين
            drivers.forEach { driver ->

                val marker = Marker(mapView)
                marker.position = GeoPoint(driver.lat, driver.lng)
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                marker.title = driver.driver + " - " + driver.carNumber

                marker.setOnMarkerClickListener { m, map ->

                    CoroutineScope(Dispatchers.IO).launch {

                        val route = fetchRoute(driver.driver)

                        withContext(Dispatchers.Main){

                            val polyline = Polyline()

                            route.forEach {
                                polyline.addPoint(GeoPoint(it.lat, it.lng))
                            }

                            routeLine = polyline
                            map.overlays.add(routeLine)
                            map.invalidate()
                        }
                    }

                    true
                }

                mapView.overlays.add(marker)
            }

            mapView.invalidate()
        },
        modifier = Modifier.fillMaxSize()
    )
}


// ================== جلب مواقع السائقين ==================
suspend fun fetchDrivers(): List<DriverLocation> {
    return try {
        val url = com.driver.portal.network.GoogleSheetConfig.execUrl("drivers")
        val response = URL(url).readText()
        val json = JSONObject(response)
        val driversArray = json.getJSONArray("drivers")

        val list = mutableListOf<DriverLocation>()

        for (i in 0 until driversArray.length()) {
            val obj = driversArray.getJSONObject(i)
            list.add(
                DriverLocation(
                    driver = obj.getString("driver"),
                    carNumber = obj.getString("carNumber"),
                    lat = obj.getDouble("lat"),
                    lng = obj.getDouble("lng"),
                    status = obj.getString("status")
                )
            )
        }

        list
    } catch (e: Exception) {
        emptyList()
    }
}


// ================== جلب مسار السائق ==================
suspend fun fetchRoute(driverName: String): List<DriverLocation> {
    return try {
        val url = com.driver.portal.network.GoogleSheetConfig.execUrl(
            "route",
            "driverName" to driverName
        )
        val response = URL(url).readText()
        val json = JSONObject(response)
        val pointsArray = json.getJSONArray("points")

        val list = mutableListOf<DriverLocation>()

        for (i in 0 until pointsArray.length()) {
            val obj = pointsArray.getJSONObject(i)
            list.add(
                DriverLocation(
                    driver = driverName,
                    carNumber = "",
                    lat = obj.getDouble("lat"),
                    lng = obj.getDouble("lng"),
                    status = ""
                )
            )
        }

        list
    } catch (e: Exception) {
        emptyList()
    }
}