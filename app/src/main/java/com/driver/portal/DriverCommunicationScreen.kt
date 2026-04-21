package com.driver.portal

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL

private const val COMM_PREF = "driver_contacts"
private const val CONTACTS_KEY = "saved_contacts"
private const val DRIVERS_CACHE_KEY = "drivers_cache"
private const val SUPPORT_PHONE = "07809830249"
private const val SUPPORT_WHATSAPP = "9647809830249"

data class SavedDriverContact(
    val name: String,
    val carNumber: String,
    val phone: String
)

data class CommunicationEntry(
    val name: String,
    val carNumber: String,
    val phone: String,
    val status: String,
    val isCurrentDriver: Boolean = false
)

@Composable
fun DriverCommunicationScreen(driverName: String) {
    val context = LocalContext.current

    var loading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableStateOf(0) }
    var liveDrivers by remember { mutableStateOf(loadCachedDrivers(context)) }

    var contactName by remember { mutableStateOf("") }
    var contactCar by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var quickMessage by remember {
        mutableStateOf("السلام عليكم، أحتاج التنسيق بخصوص النقلة الحالية.")
    }

    val myPhone = remember { DriverSession.getDriverPhone(context) }
    val myCarNumber = remember { DriverSession.getCarNumber(context) }

    val savedContacts = remember {
        mutableStateListOf<SavedDriverContact>().apply {
            addAll(loadSavedContacts(context))
        }
    }

    LaunchedEffect(driverName, myPhone, myCarNumber) {
        if (driverName.isNotBlank() && myPhone.isNotBlank()) {
            val myContact = SavedDriverContact(driverName, myCarNumber, myPhone)
            savedContacts.removeAll {
                it.name == myContact.name ||
                    (it.carNumber.isNotBlank() && it.carNumber == myContact.carNumber) ||
                    it.phone == myContact.phone
            }
            savedContacts.add(0, myContact)
            persistSavedContacts(context, savedContacts)
        }
    }

    LaunchedEffect(refreshKey) {
        loading = true
        hasError = false

        try {
            val fetchedDrivers = withContext(Dispatchers.IO) { fetchCommunicationDrivers() }
            liveDrivers = fetchedDrivers
            persistCachedDrivers(context, fetchedDrivers)
            loading = false
        } catch (_: Exception) {
            liveDrivers = loadCachedDrivers(context)
            loading = false
            hasError = liveDrivers.isEmpty()
        }
    }

    val directoryEntries = buildDirectoryEntries(
        currentDriverName = driverName,
        currentCarNumber = myCarNumber,
        currentPhone = myPhone,
        savedContacts = savedContacts,
        liveDrivers = liveDrivers
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFF7F3FF), Color(0xFFF1F6FF))
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                CommunicationHeaderCard(
                    driverName = driverName,
                    supportPhone = SUPPORT_PHONE,
                    onRefresh = { refreshKey++ },
                    onCallSupport = { openDialer(context, SUPPORT_PHONE) },
                    onOpenWhatsApp = {
                        openWhatsApp(
                            context = context,
                            phoneNumber = SUPPORT_WHATSAPP,
                            message = "مرحباً، أحتاج إلى تنسيق مباشر مع السواق من داخل التطبيق."
                        )
                    }
                )
            }

            item {
                ContactComposerCard(
                    contactName = contactName,
                    contactCar = contactCar,
                    contactPhone = contactPhone,
                    quickMessage = quickMessage,
                    onNameChange = { contactName = it },
                    onCarChange = { contactCar = it },
                    onPhoneChange = { contactPhone = it.filter { ch -> ch.isDigit() || ch == '+' } },
                    onMessageChange = { quickMessage = it },
                    onSave = {
                        if (contactName.isBlank() || contactPhone.isBlank()) {
                            Toast.makeText(context, "أدخل اسم السائق ورقم الهاتف", Toast.LENGTH_SHORT).show()
                            return@ContactComposerCard
                        }

                        val newContact = SavedDriverContact(
                            name = contactName.trim(),
                            carNumber = contactCar.trim(),
                            phone = contactPhone.trim()
                        )

                        savedContacts.removeAll {
                            it.phone == newContact.phone ||
                                it.name == newContact.name ||
                                (it.carNumber.isNotBlank() && it.carNumber == newContact.carNumber)
                        }
                        savedContacts.add(0, newContact)
                        persistSavedContacts(context, savedContacts)
                        Toast.makeText(context, "تم حفظ جهة التواصل", Toast.LENGTH_SHORT).show()
                    },
                    onCall = {
                        if (contactPhone.isBlank()) {
                            Toast.makeText(context, "أدخل رقم الهاتف أولاً", Toast.LENGTH_SHORT).show()
                        } else {
                            openDialer(context, contactPhone)
                        }
                    },
                    onSms = {
                        if (contactPhone.isBlank()) {
                            Toast.makeText(context, "أدخل رقم الهاتف أولاً", Toast.LENGTH_SHORT).show()
                        } else {
                            openSms(context, contactPhone, quickMessage)
                        }
                    },
                    onWhatsApp = {
                        if (contactPhone.isBlank()) {
                            Toast.makeText(context, "أدخل رقم الهاتف أولاً", Toast.LENGTH_SHORT).show()
                        } else {
                            openWhatsApp(context, contactPhone, quickMessage)
                        }
                    }
                )
            }

            item {
                SectionTitle(text = "دليل السواق")
            }

            item {
                HintCard(text = "الأرقام المحفوظة تبقى ظاهرة حتى لو كان السائق غير متصل أو عند ضعف الإنترنت، ويمكن تعديل أي رقم من داخل البطاقة مباشرة.")
            }

            when {
                loading && directoryEntries.isEmpty() -> {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF6C63FF))
                        }
                    }
                }

                directoryEntries.isEmpty() -> {
                    item {
                        HintCard(text = "لا توجد بيانات سواق محفوظة حالياً. أضف رقم أي سائق مرة واحدة وسيبقى ظاهرًا داخل التطبيق.")
                    }
                }

                else -> {
                    if (hasError) {
                        item {
                            HintCard(text = "يتم الآن عرض آخر دليل محفوظ داخل الجهاز حتى مع عدم توفر الإنترنت.")
                        }
                    }

                    items(directoryEntries) { entry ->
                        DirectoryContactCard(
                            entry = entry,
                            onEdit = {
                                contactName = entry.name
                                contactCar = entry.carNumber
                                contactPhone = entry.phone
                            },
                            onCall = {
                                if (entry.phone.isBlank()) {
                                    Toast.makeText(context, "رقم الهاتف غير مضاف بعد", Toast.LENGTH_SHORT).show()
                                } else {
                                    openDialer(context, entry.phone)
                                }
                            },
                            onMessage = {
                                if (entry.phone.isBlank()) {
                                    Toast.makeText(context, "رقم الهاتف غير مضاف بعد", Toast.LENGTH_SHORT).show()
                                } else {
                                    openWhatsApp(context, entry.phone, quickMessage)
                                }
                            },
                            onDelete = if (entry.phone.isNotBlank() && !entry.isCurrentDriver) {
                                {
                                    savedContacts.removeAll {
                                        it.name == entry.name ||
                                            (it.carNumber.isNotBlank() && it.carNumber == entry.carNumber)
                                    }
                                    persistSavedContacts(context, savedContacts)
                                }
                            } else null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunicationHeaderCard(
    driverName: String,
    supportPhone: String,
    onRefresh: () -> Unit,
    onCallSupport: () -> Unit,
    onOpenWhatsApp: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 10.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF6C63FF), Color(0xFF4B42D9))
                    )
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "مركز التواصل",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "اتصال ومراسلة السواق والدعم من داخل التطبيق للسائق: $driverName",
                    color = Color.White.copy(alpha = 0.88f)
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.16f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = "رقم الدعم الرسمي: ${formatPhoneForDisplay(supportPhone)}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = onRefresh,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color.White.copy(alpha = 0.18f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("تحديث")
                    }

                    FilledTonalButton(
                        onClick = onCallSupport,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color.White.copy(alpha = 0.18f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.SupportAgent, contentDescription = null)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("الدعم")
                    }

                    FilledTonalButton(
                        onClick = onOpenWhatsApp,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color.White.copy(alpha = 0.18f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("مراسلة")
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactComposerCard(
    contactName: String,
    contactCar: String,
    contactPhone: String,
    quickMessage: String,
    onNameChange: (String) -> Unit,
    onCarChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onMessageChange: (String) -> Unit,
    onSave: () -> Unit,
    onCall: () -> Unit,
    onSms: () -> Unit,
    onWhatsApp: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "محادثة واتصال سريع",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2430)
            )

            OutlinedTextField(
                value = contactName,
                onValueChange = onNameChange,
                label = { Text("اسم السائق") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = contactCar,
                onValueChange = onCarChange,
                label = { Text("رقم السيارة") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = contactPhone,
                onValueChange = onPhoneChange,
                label = { Text("رقم الهاتف") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = quickMessage,
                onValueChange = onMessageChange,
                label = { Text("الرسالة") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("حفظ")
                }

                Button(
                    onClick = onCall,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("اتصال")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onSms,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("SMS")
                }

                FilledTonalButton(
                    onClick = onWhatsApp,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("واتساب")
                }
            }
        }
    }
}

@Composable
private fun DirectoryContactCard(
    entry: CommunicationEntry,
    onEdit: () -> Unit,
    onCall: () -> Unit,
    onMessage: () -> Unit,
    onDelete: (() -> Unit)?
) {
    val statusColor = statusColor(entry.status)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF6C63FF).copy(alpha = 0.12f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF6C63FF))
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = entry.name.ifBlank { "سائق" },
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2430),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "السيارة: ${entry.carNumber.ifBlank { "-" }}",
                        color = Color(0xFF6E7582),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = statusLabel(entry.status),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF7F8FC)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "رقم الهاتف: ${entry.phone.ifBlank { "غير مضاف بعد" }.let(::formatPhoneForDisplay)}",
                        color = if (entry.phone.isBlank()) Color(0xFF8B90A0) else Color(0xFF1F2430),
                        fontWeight = if (entry.phone.isBlank()) FontWeight.Normal else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (entry.isCurrentDriver) {
                        Text(
                            text = "هذا رقمك المحفوظ داخل التطبيق",
                            color = Color(0xFF6C63FF),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onCall,
                    enabled = entry.phone.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("اتصال")
                }

                FilledTonalButton(
                    onClick = onMessage,
                    enabled = entry.phone.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("مراسلة")
                }

                FilledTonalButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("تعديل")
                }
            }

            if (onDelete != null) {
                FilledTonalButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("حذف الرقم المحفوظ")
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF1F2430),
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun HintCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            color = Color(0xFF5F6672)
        )
    }
}

private suspend fun fetchCommunicationDrivers(): List<DriverLocation> {
    val url = com.driver.portal.network.GoogleSheetConfig.execUrl("drivers")
    val response = URL(url).readText()
    val json = JSONObject(response)
    val driversArray = json.optJSONArray("drivers") ?: JSONArray()
    val list = mutableListOf<DriverLocation>()

    for (i in 0 until driversArray.length()) {
        val obj = driversArray.getJSONObject(i)
        list.add(
            DriverLocation(
                driver = obj.optString("driver"),
                carNumber = obj.optString("carNumber"),
                lat = obj.optDouble("lat"),
                lng = obj.optDouble("lng"),
                status = obj.optString("status", "offline")
            )
        )
    }

    return list.sortedWith(compareBy<DriverLocation>({ statusRank(it.status) }, { it.driver }))
}

private fun buildDirectoryEntries(
    currentDriverName: String,
    currentCarNumber: String,
    currentPhone: String,
    savedContacts: List<SavedDriverContact>,
    liveDrivers: List<DriverLocation>
): List<CommunicationEntry> {
    val map = linkedMapOf<String, CommunicationEntry>()

    fun key(name: String, carNumber: String): String = "${name.trim()}|${carNumber.trim()}"

    savedContacts.forEach { contact ->
        val safeName = contact.name.trim().ifBlank { "سائق" }
        map[key(safeName, contact.carNumber)] = CommunicationEntry(
            name = safeName,
            carNumber = contact.carNumber.trim(),
            phone = contact.phone.trim(),
            status = "offline",
            isCurrentDriver = safeName == currentDriverName || contact.carNumber.trim() == currentCarNumber
        )
    }

    liveDrivers.forEach { driver ->
        val safeName = driver.driver.trim().ifBlank { "سائق" }
        val safeCar = driver.carNumber.trim()
        val mapKey = key(safeName, safeCar)
        val existing = map[mapKey]

        map[mapKey] = CommunicationEntry(
            name = safeName,
            carNumber = safeCar,
            phone = existing?.phone.orEmpty(),
            status = driver.status.ifBlank { existing?.status ?: "offline" },
            isCurrentDriver = existing?.isCurrentDriver == true || safeName == currentDriverName || safeCar == currentCarNumber
        )
    }

    if (currentDriverName.isNotBlank()) {
        val mapKey = key(currentDriverName, currentCarNumber)
        val existing = map[mapKey]
        map[mapKey] = CommunicationEntry(
            name = currentDriverName,
            carNumber = currentCarNumber,
            phone = currentPhone.ifBlank { existing?.phone.orEmpty() },
            status = existing?.status ?: "offline",
            isCurrentDriver = true
        )
    }

    return map.values.sortedWith(
        compareBy<CommunicationEntry>(
            { !it.isCurrentDriver },
            { statusRank(it.status) },
            { it.name }
        )
    )
}

private fun statusRank(status: String): Int = when (status.lowercase()) {
    "online" -> 0
    "stopped" -> 1
    else -> 2
}

private fun statusLabel(status: String): String = when (status.lowercase()) {
    "online" -> "متصل"
    "stopped" -> "متوقف"
    else -> "غير متصل"
}

private fun statusColor(status: String): Color = when (status.lowercase()) {
    "online" -> Color(0xFF10B981)
    "stopped" -> Color(0xFFFF9800)
    else -> Color(0xFF9E9E9E)
}

private fun openDialer(context: Context, phone: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
    } catch (_: Exception) {
        Toast.makeText(context, "تعذر فتح الاتصال", Toast.LENGTH_SHORT).show()
    }
}

private fun openSms(context: Context, phone: String, message: String) {
    try {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$phone")
            putExtra("sms_body", message)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "تعذر فتح الرسائل", Toast.LENGTH_SHORT).show()
    }
}

private fun openWhatsApp(context: Context, phoneNumber: String, message: String) {
    try {
        val normalizedPhone = normalizeWhatsAppPhone(phoneNumber)
        val url = "https://wa.me/$normalizedPhone?text=${Uri.encode(message)}"
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: Exception) {
        Toast.makeText(context, "تعذر فتح واتساب", Toast.LENGTH_SHORT).show()
    }
}

private fun normalizeWhatsAppPhone(phoneNumber: String): String {
    val clean = phoneNumber.filter { it.isDigit() }
    return when {
        clean.startsWith("964") -> clean
        clean.startsWith("0") -> "964${clean.drop(1)}"
        else -> clean
    }
}

private fun formatPhoneForDisplay(phone: String): String {
    val clean = phone.filter { it.isDigit() }
    return when {
        clean.length == 11 && clean.startsWith("0") -> "${clean.take(4)} ${clean.drop(4).take(3)} ${clean.drop(7)}"
        clean.length == 12 && clean.startsWith("964") -> "+${clean.take(3)} ${clean.drop(3).take(3)} ${clean.drop(6).take(3)} ${clean.drop(9)}"
        clean.length == 13 && clean.startsWith("964") -> "+${clean.take(3)} ${clean.drop(3).take(3)} ${clean.drop(6).take(3)} ${clean.drop(9)}"
        clean.isNotBlank() -> clean
        else -> phone
    }
}

private fun loadSavedContacts(context: Context): List<SavedDriverContact> {
    val prefs = context.getSharedPreferences(COMM_PREF, Context.MODE_PRIVATE)
    val raw = prefs.getString(CONTACTS_KEY, "[]") ?: "[]"
    val array = JSONArray(raw)
    val list = mutableListOf<SavedDriverContact>()

    for (i in 0 until array.length()) {
        val obj = array.getJSONObject(i)
        list.add(
            SavedDriverContact(
                name = obj.optString("name"),
                carNumber = obj.optString("carNumber"),
                phone = obj.optString("phone")
            )
        )
    }

    return list
}

private fun persistSavedContacts(context: Context, contacts: List<SavedDriverContact>) {
    val array = JSONArray()
    contacts.forEach { contact ->
        array.put(
            JSONObject().apply {
                put("name", contact.name)
                put("carNumber", contact.carNumber)
                put("phone", contact.phone)
            }
        )
    }

    context.getSharedPreferences(COMM_PREF, Context.MODE_PRIVATE)
        .edit()
        .putString(CONTACTS_KEY, array.toString())
        .apply()
}

private fun persistCachedDrivers(context: Context, drivers: List<DriverLocation>) {
    val array = JSONArray()
    drivers.forEach { driver ->
        array.put(
            JSONObject().apply {
                put("driver", driver.driver)
                put("carNumber", driver.carNumber)
                put("lat", driver.lat)
                put("lng", driver.lng)
                put("status", driver.status)
            }
        )
    }

    context.getSharedPreferences(COMM_PREF, Context.MODE_PRIVATE)
        .edit()
        .putString(DRIVERS_CACHE_KEY, array.toString())
        .apply()
}

private fun loadCachedDrivers(context: Context): List<DriverLocation> {
    val prefs = context.getSharedPreferences(COMM_PREF, Context.MODE_PRIVATE)
    val raw = prefs.getString(DRIVERS_CACHE_KEY, "[]") ?: "[]"
    val array = JSONArray(raw)
    val list = mutableListOf<DriverLocation>()

    for (i in 0 until array.length()) {
        val obj = array.getJSONObject(i)
        list.add(
            DriverLocation(
                driver = obj.optString("driver"),
                carNumber = obj.optString("carNumber"),
                lat = obj.optDouble("lat"),
                lng = obj.optDouble("lng"),
                status = obj.optString("status", "offline")
            )
        )
    }

    return list.sortedWith(compareBy<DriverLocation>({ statusRank(it.status) }, { it.driver }))
}