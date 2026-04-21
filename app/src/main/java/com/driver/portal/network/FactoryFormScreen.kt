package com.driver.portal.network

import android.graphics.Bitmap
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.driver.portal.DriverSession
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FactoryFormScreen() {
    val context = LocalContext.current

    val driverName = DriverSession.getDriverName(context)
    val carNumber = DriverSession.getCarNumber(context)

    var docNumber by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var factoryName by remember { mutableStateOf("") }

    val factories = remember {
        com.driver.portal.FactoryCatalog.all.map { it.name } + "أخرى"
    }

    var expandedFactory by remember { mutableStateOf(false) }

    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageData by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }

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

    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryDark = MaterialTheme.colorScheme.primaryContainer
    val backgroundTop = MaterialTheme.colorScheme.background
    val backgroundBottom = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.60f)
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
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text("الكمية المحملة") },
                        leadingIcon = {
                            Icon(Icons.Default.Scale, contentDescription = null)
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
                        text = "صورة الوصل",
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
                }
            }

            Button(
                onClick = {
                    if (docNumber.isBlank()) {
                        Toast.makeText(context, "أدخل رقم الوصل", Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    if (quantity.isBlank()) {
                        Toast.makeText(context, "أدخل الكمية", Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    if (factoryName.isBlank()) {
                        Toast.makeText(context, "اختر اسم المعمل", Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    if (imageData.isBlank()) {
                        Toast.makeText(context, "يجب تصوير الوصل", Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    sending = true

                    val unloadDate = SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.getDefault()
                    ).format(Date())

                    val request = FactoryRequest(
                        docNumber = docNumber,
                        driverName = driverName,
                        carNumber = carNumber,
                        quantity = quantity,
                        factory = factoryName,
                        unloadDate = unloadDate,
                        fileData = imageData
                    )

                    TripRepository.sendFactory(
                        request,
                        {
                            sending = false
                            Toast.makeText(
                                context,
                                "تم إرسال وصل المعمل",
                                Toast.LENGTH_LONG
                            ).show()

                            docNumber = ""
                            quantity = ""
                            factoryName = ""
                            imageBitmap = null
                            imageData = ""
                        },
                        { error ->
                            sending = false
                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
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