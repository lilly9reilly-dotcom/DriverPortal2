# 💻 دليل استخدام الصور في الكود

## 📌 كيفية استخدام كل صورة في التطبيق

---

## 🔐 1️⃣ شاشات تسجيل الدخول

### ملف: `login_background.jpg`

#### في XML Layout:
```xml
<!-- activity_login.xml -->
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@drawable/login_background">
    
    <!-- عناصر تسجيل الدخول هنا -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical"
        android:gravity="center">
        
        <!-- Email input -->
        <EditText
            android:id="@+id/emailInput"
            android:layout_width="300dp"
            android:layout_height="50dp"
            android:hint="البريد الإلكتروني" />
        
        <!-- Password input -->
        <EditText
            android:id="@+id/passwordInput"
            android:layout_width="300dp"
            android:layout_height="50dp"
            android:inputType="textPassword"
            android:hint="كلمة المرور" />
        
        <!-- Login Button -->
        <Button
            android:id="@+id/loginBtn"
            android:layout_width="300dp"
            android:layout_height="50dp"
            android:text="تسجيل الدخول" />
    </LinearLayout>
</FrameLayout>
```

#### في Kotlin/Java:
```kotlin
class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        // الخلفية تُطبق تلقائياً من XML
    }
}
```

---

### ملف: `splash_screen.xml`

#### تعريف في Android Manifest:
```xml
<!-- AndroidManifest.xml -->
<application>
    <activity
        android:name=".SplashActivity"
        android:exported="true"
        android:theme="@style/Theme.SplashScreen">
        <intent-filter>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent-filter>
    </activity>
</application>
```

#### ملف Theme:
```xml
<!-- values/themes.xml -->
<style name="Theme.SplashScreen" parent="Theme.MaterialComponents.Light">
    <item name="android:windowBackground">@drawable/splash_screen</item>
    <item name="android:windowNoTitle">true</item>
    <item name="android:windowFullscreen">true</item>
</style>
```

#### Kotlin Activity:
```kotlin
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        // الانتظار 2-3 ثواني ثم الانتقال
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }, 2500) // 2.5 ثانية
    }
}
```

---

## 📊 2️⃣ لوحة المعلومات (Dashboard)

### ملف: `driver_dash_bg.jpg`

#### في XML:
```xml
<!-- fragment_dashboard.xml -->
<?xml version="1.0" encoding="utf-8"?>
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@drawable/driver_dash_bg">
    
    <!-- إحصائيات -->
    <LinearLayout
        android:id="@+id/statsContainer"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp">
        
        <!-- بطاقة الرحلات -->
        <androidx.cardview.widget.CardView
            android:layout_width="match_parent"
            android:layout_height="120dp"
            android:layout_margin="8dp"
            app:cardCornerRadius="12dp"
            app:cardBackground="@drawable/driver_card_pattern">
            
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:orientation="horizontal"
                android:padding="16dp">
                
                <!-- أيقونة -->
                <ImageView
                    android:id="@+id/tripIcon"
                    android:layout_width="48dp"
                    android:layout_height="48dp"
                    android:src="@drawable/ic_trip_custom"
                    android:contentDescription="Trip Icon" />
                
                <!-- النص -->
                <LinearLayout
                    android:layout_width="0dp"
                    android:layout_height="match_parent"
                    android:layout_weight="1"
                    android:orientation="vertical"
                    android:paddingStart="16dp">
                    
                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="الرحلات اليومية"
                        android:textSize="14sp"
                        android:textColor="#999" />
                    
                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="125 كم"
                        android:textSize="24sp"
                        android:textStyle="bold"
                        android:textColor="#1F7FB8" />
                </LinearLayout>
            </LinearLayout>
        </androidx.cardview.widget.CardView>
        
        <!-- بطاقة الوقود -->
        <androidx.cardview.widget.CardView
            android:layout_width="match_parent"
            android:layout_height="120dp"
            android:layout_margin="8dp"
            app:cardCornerRadius="12dp"
            app:cardBackground="@drawable/driver_card_pattern">
            
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:orientation="horizontal"
                android:padding="16dp">
                
                <!-- أيقونة الوقود -->
                <ImageView
                    android:layout_width="48dp"
                    android:layout_height="48dp"
                    android:src="@drawable/fuel"
                    android:contentDescription="Fuel Icon" />
                
                <!-- النص -->
                <LinearLayout
                    android:layout_width="0dp"
                    android:layout_height="match_parent"
                    android:layout_weight="1"
                    android:orientation="vertical"
                    android:paddingStart="16dp">
                    
                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="الوقود المستخدم"
                        android:textSize="14sp"
                        android:textColor="#999" />
                    
                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="35 لتر"
                        android:textSize="24sp"
                        android:textStyle="bold"
                        android:textColor="#FF9800" />
                </LinearLayout>
            </LinearLayout>
        </androidx.cardview.widget.CardView>
    </LinearLayout>
</RelativeLayout>
```

#### في Fragment:
```kotlin
class DashboardFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // جلب البيانات من الـ Backend
        val trips = getDailyTrips()
        val fuelUsed = getFuelUsed()
        
        // تحديث الواجهة
        view.findViewById<TextView>(R.id.tripsCount).text = "${trips} كم"
        view.findViewById<TextView>(R.id.fuelCount).text = "${fuelUsed} لتر"
    }
}
```

---

## 🚗 3️⃣ شاشة الرحلات (Trips)

### ملف: `driver_trip_bg.jpg`

#### في XML:
```xml
<!-- fragment_trips.xml -->
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@drawable/driver_trip_bg">
    
    <!-- RecyclerView لعرض الرحلات -->
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/tripsRecyclerView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:paddingTop="16dp"
        android:paddingBottom="80dp"
        android:clipToPadding="false" />
</FrameLayout>
```

#### Item Layout:
```xml
<!-- trip_item.xml -->
<?xml version="1.0" encoding="utf-8"?>
<androidx.cardview.widget.CardView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_margin="8dp"
    app:cardCornerRadius="12dp"
    app:cardBackground="@drawable/driver_card_pattern">
    
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp">
        
        <!-- رأس الرحلة -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical">
            
            <!-- أيقونة الرحلة -->
            <ImageView
                android:layout_width="32dp"
                android:layout_height="32dp"
                android:src="@drawable/ic_trip_custom"
                android:contentDescription="Trip" />
            
            <!-- معلومات الرحلة -->
            <LinearLayout
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:orientation="vertical"
                android:paddingStart="16dp">
                
                <TextView
                    android:id="@+id/tripTitle"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="من: القاهرة إلى: الجيزة"
                    android:textSize="16sp"
                    android:textStyle="bold"
                    android:textColor="#1A1A1A" />
                
                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="الحالة: مكتملة"
                    android:textSize="12sp"
                    android:textColor="#999" />
            </LinearLayout>
            
            <!-- أيقونة السهم -->
            <ImageView
                android:layout_width="24dp"
                android:layout_height="24dp"
                android:src="@drawable/arrow"
                android:contentDescription="Arrow" />
        </LinearLayout>
        
        <!-- تفاصيل الرحلة -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:marginTop="16dp"
            android:paddingTop="16dp"
            android:borderTop="1dp solid #EEE">
            
            <LinearLayout
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:orientation="vertical">
                
                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="الوقت"
                    android:textSize="10sp"
                    android:textColor="#999" />
                
                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="2:30"
                    android:textSize="14sp"
                    android:textStyle="bold"
                    android:textColor="#1F7FB8" />
            </LinearLayout>
            
            <LinearLayout
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:orientation="vertical">
                
                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="المسافة"
                    android:textSize="10sp"
                    android:textColor="#999" />
                
                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="45 كم"
                    android:textSize="14sp"
                    android:textStyle="bold"
                    android:textColor="#4CAF50" />
            </LinearLayout>
            
            <LinearLayout
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:orientation="vertical">
                
                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="الوقود"
                    android:textSize="10sp"
                    android:textColor="#999" />
                
                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="12 لتر"
                    android:textSize="14sp"
                    android:textStyle="bold"
                    android:textColor="#FF9800" />
            </LinearLayout>
        </LinearLayout>
    </LinearLayout>
</androidx.cardview.widget.CardView>
```

#### في Kotlin:
```kotlin
class TripsFragment : Fragment() {
    private lateinit var adapter: TripsAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_trips, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val recyclerView = view.findViewById<RecyclerView>(R.id.tripsRecyclerView)
        adapter = TripsAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        
        // جلب الرحلات من قاعدة البيانات
        viewModel.trips.observe(viewLifecycleOwner) { trips ->
            adapter.submitList(trips)
        }
    }
}
```

---

## ⛽ 4️⃣ شاشة الوقود (Fuel)

### ملف: `driver_factory_bg.jpg` و `fuel.png`

#### في XML:
```xml
<!-- fragment_fuel.xml -->
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@drawable/driver_factory_bg">
    
    <ScrollView
        android:layout_width="match_parent"
        android:layout_height="match_parent">
        
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="16dp">
            
            <!-- مؤشر الوقود -->
            <CardView
                android:layout_width="match_parent"
                android:layout_height="150dp"
                android:layout_margin="8dp"
                app:cardCornerRadius="12dp">
                
                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="match_parent"
                    android:orientation="vertical"
                    android:gravity="center"
                    android:padding="16dp">
                    
                    <ImageView
                        android:layout_width="48dp"
                        android:layout_height="48dp"
                        android:src="@drawable/fuel"
                        android:contentDescription="Fuel Icon" />
                    
                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="الوقود المتبقي"
                        android:textSize="16sp"
                        android:layout_marginTop="8dp" />
                    
                    <ProgressBar
                        android:id="@+id/fuelProgress"
                        android:layout_width="200dp"
                        android:layout_height="20dp"
                        android:layout_marginTop="16dp"
                        style="@android:style/Widget.ProgressBar.Horizontal"
                        android:progress="75" />
                    
                    <TextView
                        android:id="@+id/fuelAmount"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="45 من 60 لتر"
                        android:textSize="14sp"
                        android:layout_marginTop="8dp"
                        android:textColor="#FF9800" />
                </LinearLayout>
            </CardView>
            
            <!-- الإحصائيات -->
            <CardView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_margin="8dp"
                app:cardCornerRadius="12dp">
                
                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:padding="16dp">
                    
                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="الاستهلاك اليومي"
                        android:textSize="16sp"
                        android:textStyle="bold" />
                    
                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="متوسط: 8.5 لتر/100 كم"
                        android:textSize="14sp"
                        android:layout_marginTop="8dp" />
                    
                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="التكلفة: $95"
                        android:textSize="14sp"
                        android:textColor="#FF9800"
                        android:layout_marginTop="4dp" />
                </LinearLayout>
            </CardView>
        </LinearLayout>
    </ScrollView>
</FrameLayout>
```

---

## 🔧 5️⃣ شاشة الصيانة

### ملف: `maintenance_bg.jpg`

```kotlin
class MaintenanceFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_maintenance, container, false)
    }
}
```

---

## 🗺️ 6️⃣ شاشة الخريطة

### ملف: `map_bg.jpg` و `arrow.png`

```kotlin
class MapFragment : Fragment() {
    private var map: GoogleMap? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync { googleMap ->
            map = googleMap
            
            // إضافة أيقونة الاتجاه
            val customArrowIcon = BitmapDescriptorFactory.fromResource(R.drawable.arrow)
            val markerOptions = MarkerOptions()
                .position(LatLng(30.0444, 31.2357))
                .title("موقع المركبة")
                .icon(customArrowIcon)
            
            map?.addMarker(markerOptions)
        }
    }
}
```

---

## 📱 7️⃣ شاشة المزيد

### ملف: `more_bg.jpg` و `truck.png`

```kotlin
class MoreFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_more, container, false)
    }
}
```

---

## 🏠 8️⃣ أيقونات التطبيق

### الملفات: `ic_launcher_background.xml` و `ic_launcher_foreground.xml`

#### في Android Manifest:
```xml
<application
    android:icon="@mipmap/ic_launcher"
    android:roundIcon="@mipmap/ic_launcher_round">
</application>
```

#### في build.gradle:
```gradle
android {
    compileSdk 34
    
    defaultConfig {
        applicationId "com.driverportal"
        minSdk 24
        targetSdk 34
        versionCode 1
        versionName "1.0.0"
    }
}
```

---

## 🎯 قائمة التحقق - الاستخدام الفعلي

- [x] ✅ login_background.xml - LoginActivity
- [x] ✅ splash_screen.xml - SplashActivity
- [x] ✅ driver_dash_bg.jpg - DashboardFragment
- [x] ✅ driver_card_pattern.png - CardView items
- [x] ✅ driver_trip_bg.jpg - TripsFragment
- [x] ✅ ic_trip_custom.png - Navigation & Lists
- [x] ✅ arrow.png - Maps & UI elements
- [x] ✅ driver_factory_bg.jpg - FuelFragment
- [x] ✅ fuel.png - Fuel icon in UI
- [x] ✅ maintenance_bg.jpg - MaintenanceFragment
- [x] ✅ map_bg.jpg - MapFragment
- [x] ✅ more_bg.jpg - MoreFragment
- [x] ✅ truck.png - Vehicle icon
- [x] ✅ ic_launcher_*.xml - App launcher icon

---

**كل الصور جاهزة للاستخدام مباشرة في الكود! 🚀**
