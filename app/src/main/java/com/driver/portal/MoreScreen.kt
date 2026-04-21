package com.driver.portal

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MoreScreen(
    onOpenHistory: () -> Unit,
    onOpenMaintenance: () -> Unit,
    onOpenWallet: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenCommunication: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val primaryDark = MaterialTheme.colorScheme.primaryContainer
    val textPrimary = Color.White
    val textSecondary = Color.White.copy(alpha = 0.82f)

    Box(modifier = Modifier.fillMaxSize()) {

        Image(
            painter = painterResource(id = R.drawable.more_bg),
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
                            Color(0xCC121317),
                            Color(0xAA3B2416),
                            Color(0xDD111115)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            MoreHeaderCard(
                primary = primary,
                primaryDark = primaryDark,
                textPrimary = textPrimary,
                textSecondary = textSecondary
            )

            MoreItemCard(
                title = "السجل",
                subtitle = "استعراض السجلات والحركات السابقة",
                icon = Icons.Default.List,
                accent = primary,
                onClick = onOpenHistory
            )

            MoreItemCard(
                title = "الصيانة",
                subtitle = "متابعة طلبات ومعلومات الصيانة",
                icon = Icons.Default.Build,
                accent = MaterialTheme.colorScheme.secondary,
                onClick = onOpenMaintenance
            )

            MoreItemCard(
                title = "المحفظة",
                subtitle = "الأرباح والرصيد والكميات",
                icon = Icons.Default.AccountBalanceWallet,
                accent = Color(0xFF2E7D32),
                onClick = onOpenWallet
            )

            MoreItemCard(
                title = "التواصل",
                subtitle = "اتصال ورسائل سريعة بين السواق ومن داخل التطبيق",
                icon = Icons.Default.Chat,
                accent = Color(0xFF7B1FA2),
                onClick = onOpenCommunication
            )

            MoreItemCard(
                title = "الحساب",
                subtitle = "بيانات السائق والمعلومات الشخصية",
                icon = Icons.Default.Person,
                accent = Color(0xFF0288D1),
                onClick = onOpenProfile
            )

            MoreItemCard(
                title = "التقارير",
                subtitle = "التقارير والإحصاءات التفصيلية",
                icon = Icons.Default.Assessment,
                accent = MaterialTheme.colorScheme.tertiary,
                onClick = onOpenReports
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun MoreHeaderCard(
    primary: Color,
    primaryDark: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            primary.copy(alpha = 0.90f),
                            primaryDark.copy(alpha = 0.90f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "المزيد",
                    color = textPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "الوصول إلى الأقسام الإضافية وإدارة معلوماتك بسهولة",
                    color = textSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun MoreItemCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 10.dp,
            pressedElevation = 4.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Surface(
                shape = CircleShape,
                color = accent.copy(alpha = 0.14f),
                modifier = Modifier.size(54.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accent,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = accent
                )
            }
        }
    }
}