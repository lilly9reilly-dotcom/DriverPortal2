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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.Calendar

@Composable
fun FactoryFormScreen() {
    val context = LocalContext.current

    val driverName = DriverSession.getDriverName(context)
    val carNumber = DriverSession.getCarNumber(context)

    var docNumber by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var factoryName by remember { mutableStateOf("") }
    var loadDate by remember { mutableStateOf("") }
    var unloadDate by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val factoryTonPrice = 8500L

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

    val factories = remember {
        com.driver.portal.FactoryCatalog.all.map { it.name } + "أخرى"
    }

    var expandedFactory by remember { mutableStateOf(false) }

    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageData by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }

    fun normalizeNumeric(value: String): String {
        val arabicDigits = mapOf(
            '٠' to '0', '١' to '1', '٢' to '2', '٣' to '3', '٤' to '4',
            '٥' to '5', '٦' to '6', '٧' to '7', '٨' to '8', '٩' to '9'
        )
        return buildString {
            value.forEach { ch -> append(arabicDigits[ch] ?: ch) }
        }
    }

    val quantityValue = normalizeNumeric(quantity).toDoubleOrNull() ?: 0.0
    val factoryTotalAmount = (quantityValue * factoryTonPrice).toLong()

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            imageBitmap = bitmap

            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream)
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
                    val out = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, out)
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
    val backgroundTop = Color(0xFFD5EAF2)
    val backgroundBottom = Color(0xFFEFF7FB)
    val cardColor = MaterialTheme.colorScheme.surface
    val textDark = MaterialTheme.colorScheme.onSurface
    val textMuted = MaterialTheme.colorScheme.onSurfaceVariant
    val successColor = Color(0xFF2E7D32)

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = textDark,
        unfocusedTextColor = textDark,
        focusedBorderColor = primaryColor,
        unfocusedBorderColor = Color(0xFFD0D5DD),
        focusedLabelColor = primaryColor,
        unfocusedLabelColor = textMuted,
        cursorColor = primaryColor,
        unfocusedContainerColor = Color.White
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.driver_factory_bg),
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
                        text = "وصل المعمل",
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
                        text = "بيانات الوصل",
                        color = textDark,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    androidx.compose.material3.HorizontalDivider()

                    OutlinedTextField(
                        value = docNumber,
                        onValueChange = { docNumber = it },
                        label = { Text("رقم الوصل") },
                        leadingIcon = {
                            Icon(Icons.Default.Numbers, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = fieldColors,
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = driverName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("اسم السائق") },
                        leadingIcon = {
                            Icon(Icons.Default.LocalShipping, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = fieldColors,
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = carNumber,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("رقم السيارة") },
                        leadingIcon = {
                            Icon(Icons.Default.LocalShipping, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = fieldColors,
                        singleLine = true
                    )

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
                                Icon(Icons.Default.Numbers, contentDescription = null)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = fieldColors,
                            singleLine = true
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
                                Icon(Icons.Default.Numbers, contentDescription = null)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = fieldColors,
                            singleLine = true
                        )

                        OutlinedButton(
                            onClick = { showDatePicker { unloadDate = it } },
                            modifier = Modifier.height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("اختيار")
                        }
                    }

                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text("الكمية (طن)") },
                        leadingIcon = {
                            Icon(Icons.Default.Scale, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = fieldColors,
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = ownerName,
                        onValueChange = { ownerName = it },
                        label = { Text("اسم المالك") },
                        leadingIcon = {
                            Icon(Icons.Default.LocalShipping, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = fieldColors,
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = "%,d".format(factoryTonPrice),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("سعر الطن (دينار)") },
                        leadingIcon = {
                            Icon(Icons.Default.LocalShipping, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = fieldColors,
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
                            Text(if (factoryName.isBlank()) "اسم المعمل المطلوب" else factoryName)
                        }

                        DropdownMenu(
                            expanded = expandedFactory,
                            onDismissRequest = { expandedFactory = false }
                        ) {
                            factories.forEach { factory ->
                                DropdownMenuItem(
                                    text = { Text(factory) },
                                    onClick = {
                                        factoryName = factory
                                        expandedFactory = false
                                    }
                                )
                            }
                        }
                    }

                    if (factoryName.isNotBlank()) {
                        Surface(
                            color = primaryColor.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = "الوجهة المختارة: $factoryName",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                color = primaryColor,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
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
                        text = "صورة الوصل والملاحظات",
                        color = textDark,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    androidx.compose.material3.HorizontalDivider()

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
                                    Icons.Default.CameraAlt,
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

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("الملاحظات") },
                        leadingIcon = {
                            Icon(Icons.Default.Scale, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = fieldColors
                    )
                }
            }

            Button(
                onClick = {
                    val normalizedDocNumber = DocNumberGuard.normalize(docNumber)

                    if (normalizedDocNumber.isBlank()) {
                        Toast.makeText(context, "أدخل رقم الوصل", Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    if (quantity.isBlank()) {
                        Toast.makeText(context, "أدخل الكمية", Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    if (ownerName.isBlank()) {
                        Toast.makeText(context, "أدخل اسم المالك", Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    if (factoryName.isBlank()) {
                        Toast.makeText(context, "اختر اسم المعمل", Toast.LENGTH_LONG).show()
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

                    if (imageData.isBlank()) {
                        Toast.makeText(context, "يجب تصوير أو تحميل الوصل", Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    sending = true

                    fun submitFactoryNow() {
                        val pricingNote = "سعر الطن: ${factoryTonPrice} | إجمالي: ${factoryTotalAmount}"
                        val mergedNotes = if (notes.isBlank()) pricingNote else "$notes\n$pricingNote"
                        val request = FactoryRequest(
                            docNumber = normalizedDocNumber,
                            driverName = driverName,
                            carNumber = carNumber,
                            loadDate = loadDate,
                            unloadDate = unloadDate,
                            quantity = quantity,
                            owner = ownerName,
                            factory = factoryName,
                            fileData = imageData,
                            notes = mergedNotes
                        )

                        TripRepository.sendFactory(
                            request,
                            {
                                DocNumberGuard.markUsed(context, normalizedDocNumber)
                                sending = false
                                Toast.makeText(
                                    context,
                                    "تم إرسال وصل المعمل",
                                    Toast.LENGTH_LONG
                                ).show()

                                docNumber = ""
                                quantity = ""
                                ownerName = ""
                                factoryName = ""
                                loadDate = ""
                                unloadDate = ""
                                notes = ""
                                imageBitmap = null
                                imageData = ""
                            },
                            { error ->
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
                                    if (DocNumberGuard.isUsedLocally(context, normalizedDocNumber)) {
                                        sending = false
                                        Toast.makeText(
                                            context,
                                            "رقم الوصل مستخدم مسبقاً (تحقق محلي)",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "تعذر التحقق من السيرفر، تم الاعتماد على الحماية المحلية",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        submitFactoryNow()
                                    }
                                }

                                TripRepository.DocCheckResult.AVAILABLE -> {
                                    submitFactoryNow()
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
                    Text("حفظ وإرسال", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
