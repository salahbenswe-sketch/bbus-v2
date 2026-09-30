package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BusTrackingData
import com.example.ui.theme.BusAccentEmerald
import com.example.util.BusAlertManager
import com.example.util.BusTripDirection

@Composable
fun BusDirectionCard(
    data: BusTrackingData?,
    modifier: Modifier = Modifier
) {
    var lastKnownDirection by remember { mutableStateOf(BusTripDirection.UNKNOWN) }

    // استخدام المنطق الجغرافي المتقدم إذا توفرت الإحداثيات
    val resolvedDirection = data?.let { d ->
        if (d.latitude != 0.0 || d.longitude != 0.0) {
            BusAlertManager.resolveTripDirectionByLocation(d.latitude, d.longitude, d.courseDegrees, d.speedKmh)
        } else {
            BusAlertManager.resolveTripDirection(d.courseDegrees, d.speedKmh)
        }
    } ?: BusTripDirection.UNKNOWN

    if (resolvedDirection != BusTripDirection.UNKNOWN) {
        lastKnownDirection = resolvedDirection
    }
    val direction = if (resolvedDirection != BusTripDirection.UNKNOWN) resolvedDirection else lastKnownDirection

    val directionColor = when (direction) {
        BusTripDirection.TO_BENI_ATLI -> BusAccentEmerald
        BusTripDirection.TO_MEDEA    -> Color(0xFFEA580C)
        BusTripDirection.UNKNOWN     -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    // السهم يدور 180° عند اتجاه المدية (اليسار في RTL)
    val arrowRotation = when (direction) {
        BusTripDirection.TO_BENI_ATLI -> 180f
        BusTripDirection.TO_MEDEA    -> 0f
        BusTripDirection.UNKNOWN     -> 90f
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.93f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(directionColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingFlat,
                    contentDescription = "اتجاه الحافلة",
                    tint = directionColor,
                    modifier = Modifier
                        .size(14.dp)
                        .rotate(arrowRotation)
                )
            }
            Text(
                text = direction.label,
                style = MaterialTheme.typography.labelSmall,
                color = directionColor,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}
