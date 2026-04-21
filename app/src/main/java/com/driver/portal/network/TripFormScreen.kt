package com.driver.portal.network

import android.app.DatePickerDialog
import android.graphics.Bitmap
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
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Notes
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
import androidx.compose.material3.Divider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.driver.portal.DriverSession
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
    var price by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var factoryName by remember { mutableStateOf("") }
    var factoryVoucher by remember { mutableStateOf("") }

    val stations = remember { listOf("محطة حلفاية", "محطة التاجي", "محطات الشمال", "أخرى") }
    var station by remember { mutableStateOf("") }
    var expandedStation by remember { mutableStateOf(false) }

    val factories = remember { com.driver.portal.FactoryCatalog.all.map { it.name } + "أخرى" }
    var expandedFactory by remember { mutableStateOf(false) }

    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageData by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }

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

    val primaryColor = Color(0xFF5B4FD3)
    val primaryDark = Color(0xFF4338CA)
    val backgroundTop = Color(0xFFF4F2FF)
    val backgroundBottom = Color(0xFFE9ECFF)
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(backgroundTop, backgroundBottom)
                )
            )
    ) {
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

                    Divider()

                    OutlinedTextField(
                        value = uiState.docNumber,
                        onValueChange = { uiState = uiState.copy(docNumber = it) },
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
                            onValueChange = { uiState = uiState.updateQuantity(it) },
                            label = { Text("الكمية (طن)") },
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
                            onValueChange = { uiState = uiState.updateLiters(it) },
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

                    Box {
                        OutlinedButton(
                            onClick = { expandedFactory = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Warehouse, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (factoryName.isBlank()) "اسم المعمل" else factoryName)
                        }

                        DropdownMenu(
                            expanded = expandedFactory,
                            onDismissRequest = { expandedFactory = false }
                        ) {
                            factories.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item) },
                                    onClick = {
                                        factoryName = item
                                        expandedFactory = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = factoryVoucher,
                        onValueChange = { factoryVoucher = it },
                        label = { Text("رقم بوچر المعمل") },
                        leadingIcon = {
                            Icon(Icons.Default.Description, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = textFieldColors,
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("السعر (دينار)") },
                        leadingIcon = {
                            Icon(Icons.Default.LocalShipping, contentDescription = null)
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
                            Icon(Icons.Default.Notes, contentDescription = null)
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

                    Divider()

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
                    if (driverName.isBlank()) {
                        Toast.makeText(context, "اسم السائق غير محفوظ", Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    if (uiState.docNumber.isBlank()) {
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

                    if (uiState.quantity.isBlank()) {
                        Toast.makeText(context, "أدخل الكمية", Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    if (ownerType.isBlank()) {
                        Toast.makeText(context, "أدخل اسم المالك أو الشركة", Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    if (price.isBlank()) {
                        Toast.makeText(context, "أدخل السعر", Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    if (imageData.isBlank()) {
                        Toast.makeText(context, "يجب تصوير الوصل", Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    sending = true

                    TripRepository.checkDocNumber(
                        uiState.docNumber,
                        onResult = { exists ->
                            if (exists) {
                                sending = false
                                Toast.makeText(
                                    context,
                                    "رقم الوصل مستخدم مسبقاً",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                val trip = TripRequest(
                                    docNumber = uiState.docNumber,
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
                                    notes = TripNotesFormatter.merge(notes, factoryName, factoryVoucher),
                                    price = price,
                                    fileData = imageData
                                )

                                TripRepository.sendTrip(
                                    trip,
                                    onSuccess = {
                                        sending = false
                                        Toast.makeText(context, "تم الإرسال", Toast.LENGTH_LONG).show()

                                        uiState = TripUiState()
                                        loadDate = ""
                                        unloadDate = ""
                                        ownerType = ""
                                        price = ""
                                        notes = ""
                                        factoryName = ""
                                        factoryVoucher = ""
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
