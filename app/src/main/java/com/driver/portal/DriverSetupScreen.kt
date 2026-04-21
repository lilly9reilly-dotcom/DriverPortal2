package com.driver.portal

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.driver.portal.network.ApiResponse
import com.driver.portal.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Composable
fun DriverSetupScreen(
    onSave: () -> Unit
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var car by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // ================= بيانات الشركة =================
    val companyPhone = "07809830249"
    val companyWhatsApp = "9647809830249"
    val companyWebsite = "https://example.com"

    val primary = MaterialTheme.colorScheme.primary

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedBorderColor = primary,
        unfocusedBorderColor = Color.White.copy(alpha = 0.65f),
        focusedLabelColor = primary,
        unfocusedLabelColor = Color.White.copy(alpha = 0.85f),
        cursorColor = primary,
        focusedContainerColor = Color.White.copy(alpha = 0.06f),
        unfocusedContainerColor = Color.White.copy(alpha = 0.04f)
    )

    fun openDialer(phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح الاتصال", Toast.LENGTH_SHORT).show()
        }
    }

    fun openWhatsApp(phoneNumber: String) {
        try {
            val url = "https://wa.me/$phoneNumber"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح واتساب", Toast.LENGTH_SHORT).show()
        }
    }

    fun openWebsite(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح الموقع", Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        Image(
            painter = painterResource(id = R.drawable.login_background),
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
                            Color(0xBB111317),
                            Color(0x99302016),
                            Color(0xCC171014)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            Spacer(modifier = Modifier.height(4.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    text = "شركة الناقلات النموذجية",
                    color = Color.White,
                    fontSize = 29.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "نقل آمن • خدمة متميزة",
                    color = Color.White.copy(alpha = 0.84f),
                    fontSize = 16.sp
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.13f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم السائق") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = fieldColors,
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it.filter { ch -> ch.isDigit() || ch == '+' } },
                        label = { Text("رقم الهاتف") },
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = fieldColors,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )

                    OutlinedTextField(
                        value = car,
                        onValueChange = { car = it },
                        label = { Text("رقم السيارة") },
                        leadingIcon = {
                            Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = Color.White)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = fieldColors,
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            if (isLoading) return@Button

                            val cleanName = name.trim()
                            val cleanPhone = phone.trim()
                            val cleanCar = car.trim()

                            var phoneFixed = cleanPhone
                            if (phoneFixed.startsWith("0")) {
                                phoneFixed = phoneFixed.substring(1)
                            }

                            if (cleanName.isEmpty() || cleanPhone.isEmpty() || cleanCar.isEmpty()) {
                                Toast.makeText(
                                    context,
                                    "يجب ملء جميع الحقول",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }

                            if (phoneFixed.length < 10) {
                                Toast.makeText(
                                    context,
                                    "رقم الهاتف غير مكتمل",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }

                            isLoading = true

                            RetrofitClient.instance.login(
                                name = cleanName,
                                phone = phoneFixed,
                                carNumber = cleanCar
                            ).enqueue(object : Callback<ApiResponse> {

                                override fun onResponse(
                                    call: Call<ApiResponse>,
                                    response: Response<ApiResponse>
                                ) {
                                    isLoading = false

                                    val res = response.body()

                                    if (res?.success == true &&
                                        res.driver != null &&
                                        res.carNumber != null
                                    ) {

                                        DriverSession.saveDriver(
                                            context,
                                            res.driver,
                                            phoneFixed,
                                            res.carNumber
                                        )

                                        val intent = Intent(
                                            context,
                                            LocationForegroundService::class.java
                                        ).apply {
                                            putExtra("driverName", res.driver)
                                            putExtra("carNumber", res.carNumber)
                                        }

                                        ContextCompat.startForegroundService(context, intent)

                                        if (res.newDriver == true) {
                                            Toast.makeText(
                                                context,
                                                "تم إنشاء حساب جديد",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "تم تسجيل الدخول",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }

                                        onSave()

                                    } else {
                                        Toast.makeText(
                                            context,
                                            res?.message ?: "فشل تسجيل الدخول",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }

                                override fun onFailure(
                                    call: Call<ApiResponse>,
                                    t: Throwable
                                ) {
                                    isLoading = false
                                    Toast.makeText(
                                        context,
                                        "تعذر الاتصال بالشبكة",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            })
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        shape = RoundedCornerShape(30.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primary,
                            contentColor = Color.White
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            Text(
                                text = "دخول",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FooterActionItem(
                        icon = Icons.Default.Phone,
                        title = "اتصال",
                        onClick = { openDialer(companyPhone) }
                    )

                    FooterActionItem(
                        icon = Icons.Default.Send,
                        title = "واتساب",
                        onClick = { openWhatsApp(companyWhatsApp) }
                    )

                    FooterActionItem(
                        icon = Icons.Default.Public,
                        title = "الموقع",
                        onClick = { openWebsite(companyWebsite) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "© شركة الناقلات النموذجية",
                    color = Color.White.copy(alpha = 0.60f),
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun FooterActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(Color.White.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = title,
            color = Color.White.copy(alpha = 0.88f),
            fontSize = 12.sp
        )
    }
}