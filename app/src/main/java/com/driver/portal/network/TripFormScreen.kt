package com.driver.portal.network
import com.driver.portal.R

import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.OilBarrel
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.driver.portal.DriverSession
import java.io.InputStream
import java.io.ByteArrayOutputStream
import java.util.Calendar

@Composable
fun TripFormScreen() {
    val context = LocalContext.current
    var uiState by remember { mutableStateOf(TripUiState()) }

    val driverName = DriverSession.getDriverName(context)
    val carNumber = DriverSession.getCarNumber(context)

    var loadDate by remember { mutableStateOf("") }
    var unloadDate by remember { mutableStateOf("") }
    var ownerType by remember { mutableStateOf("") }
    var driverKroa by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val tripTonPrice = 35500L

    val stations = remember {
        listOf(
            "محطة حلفاية",
            "محطة التاجي",
            "محطة الدورة",
            "محطة الرصافة",
            "محطات الشمال",
            "أخرى"
        )
    }
    var station by remember { mutableStateOf("") }
    var expandedStation by remember { mutableStateOf(false) }



    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageData by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }

    fun normalizeNumeric(value: String): String {
        val arabicDigits = mapOf(
            '٠' to '0', '١' to '1', '٢' to '2', '٣' to '3', '٤' to '4',
            '٥' to '5', '٦' to '6', '٧' to '7', '٨' to '8', '٩' to '9'
        )
        return buildString {
            value.forEach { ch ->
                append(arabicDigits[ch] ?: ch)
            }
        }
    }

    fun sanitizeDecimalInput(value: String): String {
        val normalized = normalizeNumeric(value)
        val builder = StringBuilder()
        var hasDecimal = false
        normalized.forEach { ch ->
            when {
                ch.isDigit() -> builder.append(ch)
                (ch == '.' || ch == ',') && !hasDecimal -> {
                    builder.append('.')
                    hasDecimal = true
                }
            }
        }
        return builder.toString()
    }

    fun quantityForMoney(value: Double): Double {
        return if (value >= 1000.0) value / 1000.0 else value
    }

    val quantityValue = normalizeNumeric(uiState.quantity).toDoubleOrNull() ?: 0.0
    val tripTotalAmount = (quantityForMoney(quantityValue) * tripTonPrice).toLong()

    val calendar = Calendar.getInstance()

    fun showDatePicker(onDateSelected: (String) -> Unit) {
        DatePickerDialog(
            context,
            { _, year, month, day -> onDateSelected("$year-${month + 1}-$day") },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            imageBitmap = bitmap
            val resized = Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width / 2).coerceAtLeast(1),
                (bitmap.height / 2).coerceAtLeast(1),
                true
            )
            val stream = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, 70, stream)
            val bytes = stream.toByteArray()
            imageData = "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val stream: InputStream? = context.contentResolver.openInputStream(uri)
                val bitmap = stream.use { BitmapFactory.decodeStream(it) }
                if (bitmap != null) {
                    imageBitmap = bitmap
                    val resized = Bitmap.createScaledBitmap(
                        bitmap,
                        (bitmap.width / 2).coerceAtLeast(1),
                        (bitmap.height / 2).coerceAtLeast(1),
                        true
                    )
                    val out = ByteArrayOutputStream()
                    resized.compress(Bitmap.CompressFormat.JPEG, 70, out)
                    val bytes = out.toByteArray()
                    imageData = "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
                }
            } catch (_: Exception) {
                Toast.makeText(context, "تعذر تحميل الصورة", Toast.LENGTH_LONG).show()
            }
        }
    }

    val primaryColor = Color(0xFF0B7A92)
    val primaryDark = Color(0xFF075E74)
    val backgroundTop = Color(0xFFD6EAF3)
    val backgroundBottom = Color(0xFFEFF7FB)
    val cardColor = Color.White
    val textDark = Color(0xFF1F2430)
    val textMuted = Color(0xFF6E7582)
    val successColor = Color(0xFF2E7D32)

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = textDark,
        unfocusedTextColor = textDark,
        focusedBorderColor = primaryColor,
        unfocusedBorderColor = Color(0xFFD0D5DD),
        focusedLabelColor = primaryColor,
        unfocusedLabelColor = textMuted,
        cursorColor = primaryColor,
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.driver_trip_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            backgroundTop.copy(alpha = 0.88f),
                            backgroundBottom.copy(alpha = 0.96f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = primaryColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "وصل السائق",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "السائق: $driverName",
                        color = Color.White.copy(alpha = 0.95f),
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Text(
                        text = "السيارة: $carNumber",
                        color = Color.White.copy(alpha = 0.95f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "بيانات الرحلة",
                        color = textDark,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    HorizontalDivider()

                    OutlinedTextField(
                        value = uiState.docNumber,
                        onValueChange = { uiState = uiState.copy(docNumber = DocNumberGuard.normalize(it)) },
                        label = { Text("رقم الوصل") },
                        leadingIcon = {
                            Icon(Icons.Default.Numbers, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = textFieldColors,
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = driverName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("اسم السائق") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = textFieldColors,
                        singleLine = true
                    )

                    Box {
                        OutlinedButton(
                            onClick = { expandedStation = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Place, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (station.isBlank()) "المحطة" else station)
                        }

                        DropdownMenu(
                            expanded = expandedStation,
                            onDismissRequest = { expandedStation = false }
                        ) {
                            stations.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item) },
                                    onClick = {
                                        station = item
                                        expandedStation = false
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = loadDate,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("تاريخ التحميل") },
                            leadingIcon = {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = textFieldColors,
                            trailingIcon = {
                                Icon(
                                    Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = primaryColor
                                )
                            }
                        )

                        OutlinedButton(
                            onClick = { showDatePicker { loadDate = it } },
                            modifier = Modifier.height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("اختيار")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = unloadDate,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("تاريخ التفريغ") },
                            leadingIcon = {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = textFieldColors,
                            trailingIcon = {
                                Icon(
                                    Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = primaryColor
                                )
                            }
                        )

                        OutlinedButton(
                            onClick = { showDatePicker { unloadDate = it } },
                            modifier = Modifier.height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("اختيار")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.quantity,
                            onValueChange = { uiState = uiState.updateQuantity(sanitizeDecimalInput(it)) },
                            label = { Text("الكمية المحملة (كغم أو طن)") },
                            leadingIcon = {
                                Icon(Icons.Default.Scale, contentDescription = null)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = textFieldColors,
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = uiState.liters,
                            onValueChange = { uiState = uiState.updateLiters(sanitizeDecimalInput(it)) },
                            label = { Text("لترات الكاز") },
                            leadingIcon = {
                                Icon(Icons.Default.OilBarrel, contentDescription = null)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = textFieldColors,
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = driverKroa,
                        onValueChange = { driverKroa = sanitizeDecimalInput(it) },
                        label = { Text("كروة السائق (دينار)") },
                        leadingIcon = {
                            Icon(Icons.Default.LocalShipping, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = textFieldColors,
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = ownerType,
                        onValueChange = { ownerType = it },
                        label = { Text("المالك (الشركة)") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = textFieldColors,
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = "%,d".format(tripTonPrice),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("سعر الطن (دينار)") },
                        leadingIcon = {
                            Icon(Icons.Default.LocalShipping, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = textFieldColors,
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = "%,d".format(tripTotalAmount),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("إجمالي الكمية المحملة (دينار)") },
                        leadingIcon = {
                            Icon(Icons.Default.Scale, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = textFieldColors,
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("ملاحظات") },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = textFieldColors
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "صورة الوصل",
                        color = textDark,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    HorizontalDivider()

                    Button(
                        onClick = { cameraLauncher.launch(null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(vertical = 15.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تصوير الوصل", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تحميل صورة", fontWeight = FontWeight.Bold)
                    }

                    if (imageBitmap != null) {
                        Image(
                            bitmap = imageBitmap!!.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .background(Color(0xFFF2F4F7), RoundedCornerShape(18.dp))
                        )

                        Surface(
                            color = successColor.copy(alpha = 0.10f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = successColor
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تم التقاط صورة الوصل بنجاح",
                                    color = successColor,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        Surface(
                            color = Color(0xFFF7F8FA),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Description,
                                    contentDescription = null,
                                    tint = textMuted
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "لم يتم تصوير الوصل بعد",
                                    color = textMuted
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    val normalizedDocNumber = DocNumberGuard.normalize(uiState.docNumber)

                    if (driverName.isBlank()) {
                        Toast.makeText(context, "اسم السائق غير محفوظ", Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    if (normalizedDocNumber.isBlank()) {
                        Toast.makeText(context, "أدخل رقم الوصل", Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    if (station.isBlank()) {
                        Toast.makeText(context, "اختر المحطة", Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    if (loadDate.isBlank()) {
                        Toast.makeText(context, "اختر تاريخ التحميل", Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    if (unloadDate.isBlank()) {
                        Toast.makeText(context, "اختر تاريخ التفريغ", Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    if (uiState.quantity.isBlank()) {
                        Toast.makeText(context, "أدخل الكمية", Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    if ((uiState.quantity.toDoubleOrNull() ?: 0.0) <= 0.0) {
                        Toast.makeText(context, "الكمية يجب أن تكون أكبر من صفر", Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    if (driverKroa.isBlank()) {
                        Toast.makeText(context, "أدخل كروة السائق", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    
                    if (ownerType.isBlank()) {
                        Toast.makeText(context, "أدخل اسم المالك أو الشركة", Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    if (imageData.isBlank()) {
                        Toast.makeText(context, "يجب تصوير أو تحميل الوصل", Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    sending = true

                    fun submitTripNow() {
                        val trip = TripRequest(
                            docNumber = normalizedDocNumber,
                            driverName = driverName,
                            carNumber = carNumber,
                            quantity = uiState.quantity,
                            loadDate = loadDate,
                            unloadDate = unloadDate,
                            liters = uiState.liters,
                            ownerType = ownerType,
                            destination = station,
                            factory = "",
                            bojer = "",
                            notes = notes,
                            price = driverKroa,
                            kroa = driverKroa,
                            fare = driverKroa,
                            fileData = imageData
                        )

                        TripRepository.sendTrip(
                            trip,
                            onSuccess = {
                                DocNumberGuard.markUsed(context, normalizedDocNumber)
                                sending = false
                                Toast.makeText(context, "تم الإرسال", Toast.LENGTH_LONG).show()

                                uiState = TripUiState()
                                loadDate = ""
                                unloadDate = ""
                                ownerType = ""
                                driverKroa = ""
                                notes = ""
                                station = ""
                                imageBitmap = null
                                imageData = ""
                            },
                            onError = { error ->
                                sending = false
                                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                            }
                        )
                    }

                    TripRepository.checkDocNumber(
                        normalizedDocNumber,
                        driverName = driverName,
                        carNumber = carNumber,
                        onResult = { result ->
                            when (result) {
                                TripRepository.DocCheckResult.EXISTS -> {
                                    sending = false
                                    Toast.makeText(
                                        context,
                                        "رقم الوصل مستخدم مسبقاً",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }

                                TripRepository.DocCheckResult.UNVERIFIED -> {
                                    Toast.makeText(
                                        context,
                                        "تعذر التحقق من رقم الوصل، سيتم الإرسال مباشرة والتحقق في الخادم",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    submitTripNow()
                                }

                                TripRepository.DocCheckResult.AVAILABLE -> {
                                    submitTripNow()
                                }
                            }
                        }
                    )
                },
                enabled = !sending,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryDark,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                if (sending) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Icon(Icons.Default.LocalShipping, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("حفظ وإرسال الوصل", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
