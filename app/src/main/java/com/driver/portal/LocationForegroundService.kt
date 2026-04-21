package com.driver.portal

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import kotlin.math.*

/**
 * LocationForegroundService - خدمة تتبع الموقع الذكية
 * تحسينات:
 * ✅ فلترة ذكية للبيانات (لا تكرار)
 * ✅ توقيت محسّن للإرسال
 * ✅ معالجة الأخطاء الشاملة
 * ✅ تحديثات الإخطار
 */
class LocationForegroundService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null

    private var driverName: String = ""
    private var carNumber: String = ""

    private var lastLat = 0.0
    private var lastLng = 0.0
    private var lastSendTime = 0L
    private var isTracking = false
    private var updateCount = 0

    private val client by lazy { OkHttpClient() }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopTracking()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_START, null -> {
                driverName = intent?.getStringExtra(EXTRA_DRIVER_NAME) ?: driverName
                carNumber = intent?.getStringExtra(EXTRA_CAR_NUMBER) ?: carNumber

                if (driverName.isBlank() || carNumber.isBlank()) {
                    stopSelf()
                    return START_NOT_STICKY
                }

                startAsForeground()
                startTrackingIfNeeded()
                return START_STICKY
            }

            else -> return START_NOT_STICKY
        }
    }

    override fun onDestroy() {
        stopTracking()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startAsForeground() {
        val notification = buildNotification("🟢 تتبع الموقع نشط")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startTrackingIfNeeded() {
        if (isTracking) return
        if (!hasLocationPermission()) {
            stopSelf()
            return
        }

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // معايير الطلب المحسّنة
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            4000L  // طلب تحديث كل 4 ثوان
        )
            .setMinUpdateIntervalMillis(3000L)  // أدنى فترة: 3 ثوان
            .setMinUpdateDistanceMeters(10f)   // أدنى مسافة: 10 متر
            .setWaitForAccurateLocation(true)  // انتظر دقة عالية
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation ?: return

                val lat = location.latitude
                val lng = location.longitude
                val speed = (location.speed.coerceAtLeast(0f) * 3.6f)
                val accuracy = if (location.hasAccuracy()) location.accuracy else -1f
                val bearing = if (location.hasBearing()) location.bearing else 0f
                val now = System.currentTimeMillis()

                updateCount++

                // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                // حساب المسافة من آخر موقع
                // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                val movedDistanceKm = distanceBetween(lastLat, lastLng, lat, lng)
                val movedMeters = movedDistanceKm * 1000

                // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                // معايير القرار الذكية (محسّنة للتحديث المستمر)
                // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                val moved = movedMeters >= 50          // 🚗 تحرك 50 متر (أكثر حساسية)
                val timePassed = (now - lastSendTime) >= 30000L   // ⏱️ 30 ثانية (بدل دقيقتين)
                val isMoving = speed > 1               // ⚡ سرعة > 1 كم/س (أي حركة)
                val accuracyOk = accuracy <= 50f || accuracy == -1f  // 📍 دقة مقبولة

                // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                // قرار الإرسال المحسّن (يرسل أكثر)
                // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                val shouldSend =
                    ((moved && isMoving && accuracyOk) || timePassed) || !isTracking  // يرسل دائماً عند البداية

                if (shouldSend) {
                    sendGPS(
                        driver = driverName,
                        car = carNumber,
                        lat = lat,
                        lng = lng,
                        speed = speed,
                        bearing = bearing,
                        accuracy = accuracy,
                        distanceKm = movedDistanceKm,
                        timestamp = now
                    )

                    lastLat = lat
                    lastLng = lng
                    lastSendTime = now

                    // تحديث الإخطار
                    updateNotification(
                        "✅ تحديث رقم: $updateCount | " +
                                "${"%.4f".format(lat)}, ${"%.4f".format(lng)} | " +
                                "${"%.1f".format(speed)} كم/س"
                    )
                }
            }
        }

        locationCallback?.let {
            fusedLocationClient.requestLocationUpdates(
                request,
                it,
                Looper.getMainLooper()
            )
        }

        isTracking = true
    }

    private fun stopTracking() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        locationCallback = null
        isTracking = false
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarse = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fine || coarse
    }

    /**
     * إرسال بيانات GPS إلى Google Apps Script
     * ⚠️ تأكد من تحديث WEB_APP_URL بالرابط الجديد
     */
    private fun sendGPS(
        driver: String,
        car: String,
        lat: Double,
        lng: Double,
        speed: Float,
        bearing: Float,
        accuracy: Float,
        distanceKm: Double,
        timestamp: Long
    ) {
        val json = JSONObject().apply {
            put("action", "gps")
            put("driverName", driver)      // 🔴 تأكد: driverName (ليس driver)
            put("carNumber", car)           // 🔴 تأكد: carNumber
            put("lat", lat)
            put("lng", lng)
            put("speed", speed)
            put("accuracy", accuracy)
            put("bearing", bearing)
            put("distance", distanceKm)
            put("timestamp", timestamp)
        }

        val requestBody = json.toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(WEB_APP_URL)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // ❌ خطأ في الإرسال
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseBody = response.body?.string()
                    // ✅ تم الإرسال بنجاح
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                response.close()
            }
        })
    }

    /**
     * حساب المسافة بين نقطتين (Haversine Formula)
     * النتيجة بـ الكيلومتر
     */
    private fun distanceBetween(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        if (lat1 == 0.0 && lon1 == 0.0) return 999.0

        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) *
                cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }

    private fun buildNotification(text: String): Notification {
        val stopIntent = Intent(this, LocationForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🚗 Driver Portal")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .addAction(0, "إيقاف", stopPendingIntent)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "🗺️ GPS Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "تتبع موقع السائق مستمر"
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "tracking_channel"
        private const val NOTIFICATION_ID = 1

        const val ACTION_START = "com.driver.portal.action.START_TRACKING"
        const val ACTION_STOP = "com.driver.portal.action.STOP_TRACKING"

        const val EXTRA_DRIVER_NAME = "driverName"
        const val EXTRA_CAR_NUMBER = "carNumber"

        private const val WEB_APP_URL =
            com.driver.portal.network.GoogleSheetConfig.EXEC_ENDPOINT
    }
}
