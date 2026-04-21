package com.driver.portal

import android.Manifest
import android.animation.ValueAnimator
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.os.Looper
import android.view.MotionEvent
import android.view.animation.LinearInterpolator
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polyline
import java.net.URL
import kotlin.math.abs

class PulseOverlay(
    private val getPoint: () -> GeoPoint?
) : Overlay() {

    var pulseRadius by mutableFloatStateOf(20f)

    private val fillPaint = Paint().apply {
        color = Color.argb(45, 230, 126, 34)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val ringPaint = Paint().apply {
        color = Color.argb(180, 191, 90, 18)
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }

    override fun draw(
        canvas: android.graphics.Canvas,
        mapView: MapView,
        shadow: Boolean
    ) {
        if (shadow) return

        val point = getPoint() ?: return
        val screen = mapView.projection.toPixels(point, null)

        canvas.drawCircle(screen.x.toFloat(), screen.y.toFloat(), pulseRadius, fillPaint)
        canvas.drawCircle(screen.x.toFloat(), screen.y.toFloat(), pulseRadius + 10f, ringPaint)
    }
}

private fun createFactoryLabelBitmap(context: Context, label: String): Bitmap {
    val density = context.resources.displayMetrics.density

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 14f * density
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    val horizontalPadding = 16f * density
    val width = (textPaint.measureText(label) + horizontalPadding * 2).toInt()
        .coerceAtLeast((120 * density).toInt())
    val height = (40 * density).toInt()

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E67E22")
        style = Paint.Style.FILL
    }

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
    }

    val rect = android.graphics.RectF(1f, 1f, width - 1f, height - 1f)
    canvas.drawRoundRect(rect, 16f * density, 16f * density, backgroundPaint)
    canvas.drawRoundRect(rect, 16f * density, 16f * density, borderPaint)

    val baseline = height / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
    canvas.drawText(label, width / 2f, baseline, textPaint)

    return bitmap
}

@Composable
fun DriverMapScreen(
    driverName: String,
    carNumber: String
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var mapView by remember { mutableStateOf<MapView?>(null) }
    var carMarker by remember { mutableStateOf<Marker?>(null) }
    var routePolyline by remember { mutableStateOf<Polyline?>(null) }
    var trackPolyline by remember { mutableStateOf<Polyline?>(null) }
    var pulseOverlay by remember { mutableStateOf<PulseOverlay?>(null) }

    var currentLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var currentRawLocation by remember { mutableStateOf<Location?>(null) }
    var destinationMarker by remember { mutableStateOf<Marker?>(null) }

    var followMode by remember { mutableStateOf(true) }
    var rotateMapWithCar by remember { mutableStateOf(true) }
    var showFactoryMenu by remember { mutableStateOf(false) }

    var speed by remember { mutableStateOf(0.0) }
    var eta by remember { mutableStateOf("-- min") }
    var distanceLeft by remember { mutableStateOf("-- km") }
    var selectedFactory by remember { mutableStateOf("غير محدد") }

    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    var locationCallback by remember { mutableStateOf<LocationCallback?>(null) }
    var routeJob by remember { mutableStateOf<Job?>(null) }
    var pulseAnimator by remember { mutableStateOf<ValueAnimator?>(null) }

    val factories = remember { FactoryCatalog.all }

    fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        locationCallback = null
    }

    fun startPulseAnimation() {
        pulseOverlay?.pulseRadius = 20f
        mapView?.invalidate()
    }

    fun stopPulseAnimation() {
        pulseAnimator?.cancel()
        pulseAnimator = null
    }

    fun ensureCarMarker() {
        val map = mapView ?: return

        if (carMarker == null) {
            val drawable = context.getDrawable(R.drawable.arrow) as? BitmapDrawable ?: return
            val bitmap = drawable.bitmap
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 25, 25, false)

            carMarker = Marker(map).apply {
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = BitmapDrawable(context.resources, scaledBitmap)
                title = carNumber
                infoWindow = null
            }

            map.overlays.add(carMarker)
        }

        if (pulseOverlay == null) {
            pulseOverlay = PulseOverlay { currentLocation }
            map.overlays.add(pulseOverlay)
            startPulseAnimation()
        }
    }

    fun ensureTrackPolyline() {
        val map = mapView ?: return

        if (trackPolyline == null) {
            trackPolyline = Polyline().apply {
                outlinePaint.color = Color.parseColor("#BF5A12")
                outlinePaint.strokeWidth = 7f
                outlinePaint.alpha = 170
            }
            map.overlays.add(trackPolyline)
        }
    }

    fun clearRoute() {
        mapView?.let { map ->
            routePolyline?.let { map.overlays.remove(it) }
            destinationMarker?.let { map.overlays.remove(it) }
            routePolyline = null
            destinationMarker = null
            map.invalidate()
        }
        selectedFactory = "غير محدد"
        eta = "-- min"
        distanceLeft = "-- km"
    }

    fun drawRoute(destination: FactoryDestination) {
        val map = mapView ?: return
        val destinationPoint = GeoPoint(destination.latitude, destination.longitude)

        destinationMarker?.let { map.overlays.remove(it) }
        destinationMarker = Marker(map).apply {
            position = destinationPoint
            title = destination.name
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = BitmapDrawable(
                context.resources,
                createFactoryLabelBitmap(context, destination.name)
            )
            infoWindow = null
        }
        map.overlays.add(destinationMarker)
        map.invalidate()

        val start = currentLocation
        if (start == null) {
            eta = "بانتظار GPS"
            distanceLeft = "-- km"
            return
        }

        routeJob?.cancel()
        routeJob = scope.launch(Dispatchers.IO) {
            try {
                val url = "https://router.project-osrm.org/route/v1/driving/" +
                        "${start.longitude},${start.latitude};" +
                        "${destination.longitude},${destination.latitude}" +
                        "?overview=full&geometries=geojson"

                val response = URL(url).readText()
                val json = JSONObject(response)
                val route = json.getJSONArray("routes").getJSONObject(0)

                val durationMinutes = route.getDouble("duration") / 60.0
                val distanceKm = route.getDouble("distance") / 1000.0

                val geometry = route.getJSONObject("geometry")
                val coords = geometry.getJSONArray("coordinates")

                val points = mutableListOf<GeoPoint>()
                for (i in 0 until coords.length()) {
                    val c = coords.getJSONArray(i)
                    points.add(GeoPoint(c.getDouble(1), c.getDouble(0)))
                }

                withContext(Dispatchers.Main) {
                    routePolyline?.let { map.overlays.remove(it) }

                    routePolyline = Polyline().apply {
                        outlinePaint.color = Color.parseColor("#E67E22")
                        outlinePaint.strokeWidth = 12f
                        outlinePaint.alpha = 230
                        setPoints(points)
                    }

                    eta = "${durationMinutes.toInt()} min"
                    distanceLeft = String.format("%.1f km", distanceKm)

                    map.overlays.add(routePolyline)
                    map.invalidate()
                }
            } catch (_: CancellationException) {
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    eta = "خطأ"
                    distanceLeft = "خطأ"
                }
                e.printStackTrace()
            }
        }
    }

    fun updateCamera(location: Location, point: GeoPoint) {
        val map = mapView ?: return
        val controller = map.controller

        if (followMode) {
            if (rotateMapWithCar && location.hasBearing()) {
                map.mapOrientation = -location.bearing
            } else {
                map.mapOrientation = 0f
            }

            controller.setZoom(18.0)

            val projection = map.projection
            val screenPoint = projection.toPixels(point, null)
            screenPoint.y += (map.height * 0.18).toInt()
            val offsetPoint = projection.fromPixels(screenPoint.x, screenPoint.y)

            controller.animateTo(offsetPoint)
        }
    }

    fun startLocationUpdates() {
        if (!hasLocationPermission() || locationCallback != null) return

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            1800L
        )
            .setMinUpdateIntervalMillis(900L)
            .setWaitForAccurateLocation(false)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                val map = mapView ?: return

                val newPoint = GeoPoint(location.latitude, location.longitude)
                val oldLocation = currentRawLocation

                currentRawLocation = location
                currentLocation = newPoint
                speed = location.speed.coerceAtLeast(0f) * 3.6

                ensureCarMarker()
                ensureTrackPolyline()

                carMarker?.position = newPoint
                if (location.hasBearing()) {
                    carMarker?.rotation = location.bearing
                }

                val shouldAppendTrack = oldLocation == null ||
                        abs(oldLocation.latitude - location.latitude) > 0.00001 ||
                        abs(oldLocation.longitude - location.longitude) > 0.00001

                if (shouldAppendTrack) {
                    trackPolyline?.addPoint(newPoint)
                }

                pulseOverlay?.let { map.invalidate() }
                updateCamera(location, newPoint)
                map.invalidate()
            }
        }

        locationCallback = callback

        try {
            fusedLocationClient.requestLocationUpdates(
                request,
                callback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    mapView?.onResume()
                    startLocationUpdates()
                    startPulseAnimation()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    stopLocationUpdates()
                    mapView?.onPause()
                }
                Lifecycle.Event.ON_DESTROY -> {
                    stopLocationUpdates()
                    stopPulseAnimation()
                    routeJob?.cancel()
                }
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            stopLocationUpdates()
            stopPulseAnimation()
            routeJob?.cancel()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = {
                Configuration.getInstance().load(
                    context,
                    context.getSharedPreferences("osm_prefs", Context.MODE_PRIVATE)
                )

                MapView(context).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    minZoomLevel = 5.0
                    maxZoomLevel = 20.0
                    controller.setZoom(16.5)

                    setOnTouchListener { _, event ->
                        if (event.action == MotionEvent.ACTION_DOWN) {
                            followMode = false
                        }
                        false
                    }

                    mapView = this
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { createdMap ->
                mapView = createdMap
            }
        )

        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .align(Alignment.TopCenter),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Text(
                    text = "السائق: $driverName",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "السيارة: $carNumber",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatChip("السرعة", "${"%.0f".format(speed)} كم/س")
                    StatChip("المتبقي", distanceLeft)
                    StatChip("ETA", eta)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("الوجهة: $selectedFactory")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("تدوير")
                        Spacer(modifier = Modifier.width(6.dp))
                        Switch(
                            checked = rotateMapWithCar,
                            onCheckedChange = { rotateMapWithCar = it }
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { showFactoryMenu = true },
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("اختيار المعمل")
            }

            Button(
                onClick = {
                    FactoryCatalog.findByName(selectedFactory)?.let {
                        FactoryCatalog.openInGoogleMaps(context, it)
                    }
                },
                enabled = FactoryCatalog.findByName(selectedFactory) != null,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("فتح Google Maps")
            }

            DropdownMenu(
                expanded = showFactoryMenu,
                onDismissRequest = { showFactoryMenu = false }
            ) {
                factories.forEach { destination ->
                    DropdownMenuItem(
                        text = { Text(destination.name) },
                        onClick = {
                            selectedFactory = destination.name
                            showFactoryMenu = false
                            followMode = true
                            drawRoute(destination)
                        }
                    )
                }

                DropdownMenuItem(
                    text = { Text("مسح المسار") },
                    onClick = {
                        showFactoryMenu = false
                        clearRoute()
                    }
                )
            }
        }

        FloatingActionButton(
            onClick = { followMode = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = null
            )
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}