package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.example.util.BusAlertManager
import com.example.util.BusTripDirection
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.BusTrackingData
import com.example.ui.theme.BusAccentCyan
import com.example.ui.theme.BusAccentEmerald
import com.example.ui.theme.StatusOfflineRed
import com.example.util.AlertStation
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import java.util.Locale

@Composable
fun BusLiveCard(
    data: BusTrackingData?,
    onShowStations: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOnline = data?.isOnline == true
    var lastKnownDirection by remember { mutableStateOf(BusTripDirection.UNKNOWN) }
    val resolvedDirection = data?.let {
        BusAlertManager.resolveTripDirection(it.courseDegrees, it.speedKmh)
    } ?: BusTripDirection.UNKNOWN
    val tripDirection = if (resolvedDirection != BusTripDirection.UNKNOWN) {
        resolvedDirection
    } else {
        lastKnownDirection
    }
    if (resolvedDirection != BusTripDirection.UNKNOWN) {
        lastKnownDirection = resolvedDirection
    }
    val directionColor = when (tripDirection) {
        BusTripDirection.TO_BENI_ATLI -> BusAccentEmerald
        BusTripDirection.TO_MEDEA -> Color(0xFFEA580C)
        BusTripDirection.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(15.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsBus,
                        contentDescription = "الحافلة",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = data?.busName ?: "حافلة بني عطلي",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isOnline) "الموقع مباشر" else "بيانات الموقع غير متاحة",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = (if (isOnline) BusAccentEmerald else StatusOfflineRed).copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (isOnline) BusAccentEmerald else StatusOfflineRed, CircleShape)
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = if (isOnline) "متصلة" else "غير متصلة",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isOnline) BusAccentEmerald else StatusOfflineRed
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = data?.let { "آخر تحديث ${it.formattedFixTime}" } ?: "بانتظار أول تحديث للموقع",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (data != null) {
                        Text(
                            text = "السرعة ${data.speedKmh.roundToInt()} كم/س",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 5.dp)
                        )
                    } else {
                        Text(
                            text = "أعد المحاولة للتحقق من الاتصال",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 5.dp)
                        )
                    }
                }
                TextButton(onClick = onShowStations) {
                    Text("المحطات", fontWeight = FontWeight.SemiBold)
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private data class StationGroup(
    val title: String,
    val stations: List<AlertStation>
) {
    val primary: AlertStation get() = stations.first()
}

private val stationGroups = listOf(
    StationGroup("مسجد الفرقان", listOf(AlertStation.EL_FORKANE)),
    StationGroup("بلحضري", listOf(AlertStation.BELHADRI, AlertStation.BELHADRI_RETURN)),
    StationGroup("الشراشرية", listOf(AlertStation.CHRACHRIA, AlertStation.CHRACHRIA_RETURN)),
    StationGroup("نقطة الدورة", listOf(AlertStation.DOURA)),
    StationGroup("بني عطلي", listOf(AlertStation.BENI_ATLI, AlertStation.BENI_ATLI_RETURN)),
    StationGroup("الدهمي", listOf(AlertStation.ADAHMI, AlertStation.ADAHMI_RETURN)),
    StationGroup("بوعامر", listOf(AlertStation.BOUAMER, AlertStation.BOUAMER_RETURN)),
    StationGroup("الحانوت", listOf(AlertStation.EL_HANOUT))
)

@Composable
fun StationListScreen(
    data: BusTrackingData?,
    enabledStationsMap: Map<String, Boolean>,
    onStationClick: (AlertStation) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 14.dp)
        ) {
            Text(
                text = "محطات المسار",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "اختر محطة لعرض موقعها وإعداد التنبيه",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 15.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsBus,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = if (data?.isOnline == true) "تصل تنبيهات المحطات عند اقتراب الحافلة" else "حالة الاتصال غير متاحة حالياً",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(start = 9.dp)
                )
            }
        }

        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            items(stationGroups) { group ->
                StationRow(
                    group = group,
                    enabledStationsMap = enabledStationsMap,
                    onClick = { onStationClick(group.primary) }
                )
            }
        }
    }
}

@Composable
private fun StationRow(
    group: StationGroup,
    enabledStationsMap: Map<String, Boolean>,
    onClick: () -> Unit
) {
    val alertEnabled = group.stations.any { enabledStationsMap[it.name] ?: true }
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "محطة",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = group.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (alertEnabled) "التنبيه مفعّل" else "التنبيه متوقف",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (alertEnabled) BusAccentEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
            Icon(
                imageVector = if (alertEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                contentDescription = if (alertEnabled) "التنبيه مفعّل" else "التنبيه متوقف",
                tint = if (alertEnabled) BusAccentCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun StationDetailSheet(
    station: AlertStation,
    data: BusTrackingData?,
    enabledStationsMap: Map<String, Boolean>,
    onToggleAlert: (String, Boolean) -> Unit,
    onToggleGroup: (List<AlertStation>, Boolean) -> Unit = { list, checked ->
        list.forEach { onToggleAlert(it.name, checked) }
    },
    onTestSound: (AlertStation) -> Unit,
    onDismiss: () -> Unit
) {
    val group = stationGroups.first { station in it.stations }
    val alertEnabled = group.stations.any { enabledStationsMap[it.name] ?: true }
    val distanceMeters = data?.let {
        distanceInMeters(it.latitude, it.longitude, station.lat, station.lon)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(13.dp)
                    )
                }
                Column(modifier = Modifier.padding(start = 13.dp)) {
                    Text(
                        text = group.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "محطة على مسار حافلة بني عطلي",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(15.dp),
                    verticalArrangement = Arrangement.spacedBy(11.dp)
                ) {
                    DetailLine(
                        label = "الإحداثيات",
                        value = String.format(Locale.US, "%.5f, %.5f", station.lat, station.lon),
                        icon = Icons.Default.LocationOn
                    )
                    DetailLine(
                        label = "حالة الحافلة",
                        value = if (data?.isOnline == true) "متصلة الآن" else "غير متصلة",
                        icon = Icons.Default.DirectionsBus
                    )
                    DetailLine(
                        label = "المسافة المباشرة",
                        value = distanceMeters?.let(::formatDistance) ?: "غير متاحة",
                        icon = Icons.Default.Route
                    )
                    DetailLine(
                        label = "الوقت المتوقع",
                        value = "غير متاح من بيانات التتبّع الحالية",
                        icon = Icons.Default.Schedule
                    )
                }
            }

            Text(
                text = "يُحتسب تنبيه المحطة ضمن النطاق المحدد لها. المسافة المعروضة خط مستقيم وليست مسافة الطريق.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        onToggleGroup(group.stations, !alertEnabled)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (alertEnabled) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                        contentColor = if (alertEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = if (alertEnabled) Icons.Default.NotificationsOff else Icons.Default.NotificationsActive,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp)
                    )
                    Text(
                        text = if (alertEnabled) "إيقاف التنبيه" else "تنبيهي عند الاقتراب",
                        modifier = Modifier.padding(start = 8.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                IconButton(
                    onClick = { onTestSound(group.primary) },
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "اختبار صوت التنبيه",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailLine(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Column(modifier = Modifier.padding(start = 10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun distanceInMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
    val earthRadiusMeters = 6_371_000.0
    val latitudeDelta = Math.toRadians(lat2 - lat1)
    val longitudeDelta = Math.toRadians(lon2 - lon1)
    val haversine = sin(latitudeDelta / 2).pow(2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(longitudeDelta / 2).pow(2)
    return (2 * earthRadiusMeters * atan2(sqrt(haversine), sqrt(1 - haversine))).roundToInt()
}

private fun formatDistance(meters: Int): String =
    if (meters >= 1_000) String.format("%.1f كم", meters / 1_000.0) else "$meters م"
