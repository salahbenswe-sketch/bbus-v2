package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BusTrackingData
import com.example.ui.theme.BusAccentAmber
import com.example.ui.theme.BusAccentCyan
import com.example.ui.theme.BusAccentEmerald
import com.example.ui.theme.StatusOfflineRed
import com.example.ui.theme.StatusOnlineGreen
import com.example.util.AlertStation
import java.util.Locale

@Composable
fun BusInfoGrid(
    data: BusTrackingData,
    isProximityAlertEnabled: Boolean = true,
    enabledStationsMap: Map<String, Boolean> = emptyMap(),
    lastTriggeredAlert: String? = null,
    onToggleProximityAlert: () -> Unit = {},
    onToggleStationAlert: (String, Boolean) -> Unit = { _, _ -> },
    onEnableAllStations: () -> Unit = {},
    onDisableAllStations: () -> Unit = {},
    onTestSound: (AlertStation) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Primary stats highlight banner
        BusPrimaryHighlightCard(data)

        // Coordinates Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            InfoMetricCard(
                title = "خط العرض",
                value = String.format(Locale.US, "%.6f", data.latitude),
                icon = Icons.Default.LocationOn,
                accentColor = BusAccentCyan,
                modifier = Modifier.weight(1f),
                tag = "latitude_card"
            )
            InfoMetricCard(
                title = "خط الطول",
                value = String.format(Locale.US, "%.6f", data.longitude),
                icon = Icons.Default.Navigation,
                accentColor = BusAccentCyan,
                modifier = Modifier.weight(1f),
                tag = "longitude_card"
            )
        }

        // Speed & Altitude Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            InfoMetricCard(
                title = "السرعة الحالية",
                value = String.format(Locale.US, "%.1f كم/س", data.speedKmh),
                icon = Icons.Default.Speed,
                accentColor = if (data.speedKmh > 0) BusAccentEmerald else StatusOfflineRed,
                modifier = Modifier.weight(1f),
                tag = "speed_card"
            )
            InfoMetricCard(
                title = "الارتفاع عن البحر",
                value = String.format(Locale.US, "%.0f م", data.altitudeMeters),
                icon = Icons.Default.Terrain,
                accentColor = BusAccentAmber,
                modifier = Modifier.weight(1f),
                tag = "altitude_card"
            )
        }

        // Distance Today & Total Distance
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            InfoMetricCard(
                title = "المسافة اليومية",
                value = String.format(Locale.US, "%.1f كم", data.distanceTodayKm),
                icon = Icons.Default.Timeline,
                accentColor = BusAccentCyan,
                modifier = Modifier.weight(1f),
                tag = "distance_today_card"
            )
            InfoMetricCard(
                title = "إجمالي المسافة",
                value = String.format(Locale.US, "%.0f كم", data.totalDistanceKm),
                icon = Icons.Default.AltRoute,
                accentColor = BusAccentAmber,
                modifier = Modifier.weight(1f),
                tag = "total_distance_card"
            )
        }

        // Extra details card
        BusSecondaryDetailsCard(data)
    }
}

@Composable
fun AppInfoCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("app_info_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, BusAccentCyan.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = BusAccentCyan.copy(alpha = 0.15f),
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = BusAccentCyan,
                    modifier = Modifier
                        .padding(14.dp)
                        .fillMaxSize()
                )
            }

            Text(
                text = "معلومات عن التطبيق",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = BusAccentCyan.copy(alpha = 0.06f),
                border = BorderStroke(1.dp, BusAccentCyan.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "تطبيق حافلة بني عطلي.. صُمم بشغف لتسهيل تنقلات أبناء حيّنا العزيز وتيسير يومهم. لا نبتغي من هذا العمل سوى خدمتكم وتخفيف مشقة الطريق عنكم. كل ما نرجوه منكم هو دعوة صادقة بظهر الغيب يتقبلها الله منا ومنكم.",
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 24.sp),
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun BusPrimaryHighlightCard(data: BusTrackingData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("primary_highlight_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = when (data.busIndex) {
                            3 -> BusAccentEmerald.copy(alpha = 0.15f)
                            2 -> BusAccentCyan.copy(alpha = 0.15f)
                            else -> BusAccentAmber.copy(alpha = 0.15f)
                        },
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsBus,
                            contentDescription = null,
                            tint = when (data.busIndex) {
                                3 -> BusAccentEmerald
                                2 -> BusAccentCyan
                                else -> BusAccentAmber
                            },
                            modifier = Modifier
                                .padding(9.dp)
                                .fillMaxSize()
                        )
                    }
                    Column {
                        Text(
                            text = data.busName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "المعرف: ${data.uniqueId}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (data.isOnline) StatusOnlineGreen.copy(alpha = 0.15f) else StatusOfflineRed.copy(alpha = 0.15f),
                    border = BorderStroke(
                        1.dp,
                        if (data.isOnline) StatusOnlineGreen.copy(alpha = 0.4f) else StatusOfflineRed.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = if (data.isOnline) StatusOnlineGreen else StatusOfflineRed,
                                    shape = CircleShape
                                )
                        )
                        Text(
                            text = if (data.isOnline) "متصل الآن" else "غير متصل",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (data.isOnline) StatusOnlineGreen else StatusOfflineRed
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProximityAlertCard(
    isEnabled: Boolean,
    enabledStationsMap: Map<String, Boolean>,
    lastTriggeredAlert: String?,
    onToggle: () -> Unit,
    onToggleStationAlert: (String, Boolean) -> Unit,
    onToggleStationGroup: (List<AlertStation>, Boolean) -> Unit = { list, checked ->
        list.forEach { onToggleStationAlert(it.name, checked) }
    },
    onEnableAllStations: () -> Unit,
    onDisableAllStations: () -> Unit,
    onTestSound: (AlertStation) -> Unit,
    modifier: Modifier = Modifier
) {
    val physicalStations = listOf(
        Pair("مسجد الفرقان", listOf(AlertStation.EL_FORKANE)),
        Pair("بلحضري", listOf(AlertStation.BELHADRI, AlertStation.BELHADRI_RETURN)),
        Pair("الشراشرية", listOf(AlertStation.CHRACHRIA, AlertStation.CHRACHRIA_RETURN)),
        Pair("نقطة الدورة", listOf(AlertStation.DOURA)),
        Pair("محطة بني عطلي", listOf(AlertStation.BENI_ATLI, AlertStation.BENI_ATLI_RETURN)),
        Pair("محطة الدهمي", listOf(AlertStation.ADAHMI, AlertStation.ADAHMI_RETURN)),
        Pair("محطة بوعامر", listOf(AlertStation.BOUAMER, AlertStation.BOUAMER_RETURN)),
        Pair("محطة الحانوت", listOf(AlertStation.EL_HANOUT))
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("proximity_alert_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, BusAccentAmber.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Master Header Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isEnabled) BusAccentAmber.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, if (isEnabled) BusAccentAmber.copy(alpha = 0.4f) else Color.Transparent),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Alert",
                            tint = if (isEnabled) BusAccentAmber else Color.Gray,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "تحديد التنبيهات المخصصة للمحطات",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "تفعيل أو إيقاف التنبيه لكل محطة بشكل مفرد",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = BusAccentAmber
                    )
                )
            }

            if (isEnabled) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Bulk Controls Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "اختر المحطات المراد استقبال تنبيهاتها:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            onClick = onEnableAllStations,
                            colors = ButtonDefaults.textButtonColors(contentColor = BusAccentCyan)
                        ) {
                            Text(text = "تفعيل الكل", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        TextButton(
                            onClick = onDisableAllStations,
                            colors = ButtonDefaults.textButtonColors(contentColor = StatusOfflineRed)
                        ) {
                            Text(text = "إيقاف الكل", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Per-station list items
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    physicalStations.forEach { (stationTitle, stationsList) ->
                        val primaryStation = stationsList.first()
                        val isStationEnabled = stationsList.any { enabledStationsMap[it.name] ?: true }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isStationEnabled) BusAccentCyan.copy(alpha = 0.08f) else Color.Gray.copy(alpha = 0.05f),
                            border = BorderStroke(
                                1.dp,
                                if (isStationEnabled) BusAccentCyan.copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    IconButton(
                                        onClick = { onTestSound(primaryStation) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = "تجربة الصوت",
                                            tint = if (isStationEnabled) BusAccentCyan else Color.Gray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Text(
                                        text = stationTitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isStationEnabled) MaterialTheme.colorScheme.onSurface else Color.Gray
                                    )
                                }

                                Switch(
                                    checked = isStationEnabled,
                                    onCheckedChange = { isChecked ->
                                        onToggleStationGroup(stationsList, isChecked)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = BusAccentCyan,
                                        uncheckedThumbColor = Color.LightGray,
                                        uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BusSecondaryDetailsCard(data: BusTrackingData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("secondary_details_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "بيانات وحالة الحافلة المباشرة",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            DetailItemRow(label = "زاوية الاتجاه", value = String.format(Locale.US, "%.0f°", data.courseDegrees), icon = Icons.Default.CompassCalibration)
            data.fuelPercent?.let { fuel ->
                DetailItemRow(label = "مستوى الوقود", value = String.format(Locale.US, "%.0f%%", fuel), icon = Icons.Default.LocalGasStation)
            }
            DetailItemRow(label = "حالة المحرك", value = if (data.isIgnitionOn) "شغال" else "متوقف", icon = Icons.Default.Key)
            data.phone?.let { phone ->
                DetailItemRow(label = "رقم هاتف الحافلة", value = phone, icon = Icons.Default.Phone)
            }
            DetailItemRow(label = "وقت آخر تحديث", value = data.formattedFixTime, icon = Icons.Default.Refresh)
        }
    }
}

@Composable
private fun DetailItemRow(
    label: String,
    value: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun InfoMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    tag: String = ""
) {
    Card(
        modifier = modifier.testTag(tag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
