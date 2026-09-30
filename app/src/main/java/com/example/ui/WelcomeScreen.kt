package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.BusAccentAmber
import com.example.ui.theme.BusAccentCyan
import com.example.ui.theme.BusAccentEmerald
import kotlinx.coroutines.delay

@Composable
fun WelcomeScreen(
    onEnterClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPreview = LocalInspectionMode.current
    var isVisible by remember { mutableStateOf(isPreview) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_glow")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    // التلاشي التلقائي بعد 3 ثوانٍ والانتقال المباشر
    LaunchedEffect(Unit) {
        isVisible = true
        delay(3000L)
        onEnterClicked()
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF101013),
                            Color(0xFF1B1517),
                            Color(0xFF17171B),
                            Color(0xFF100F12)
                        )
                    )
                )
        ) {
            // Ambient glowing orbs in background
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                BusAccentCyan.copy(alpha = 0.18f * pulseGlow),
                                Color.Transparent
                            )
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .size(240.dp)
                    .align(Alignment.BottomEnd)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                BusAccentAmber.copy(alpha = 0.12f * pulseGlow),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { 40 }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Live Status pill badge
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color(0xFF252327).copy(alpha = 0.88f),
                            border = BorderStroke(1.dp, BusAccentEmerald.copy(alpha = 0.4f)),
                            modifier = Modifier.padding(bottom = 20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(BusAccentEmerald, CircleShape)
                                        .shadow(4.dp, CircleShape, spotColor = BusAccentEmerald)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "نظام التتبع المباشر نشط الآن",
                                    color = BusAccentEmerald,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Logo Card with dual-tone glowing ring
                        Box(contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .size(200.dp)
                                    .background(
                                        Brush.sweepGradient(
                                            listOf(
                                                BusAccentCyan,
                                                Color(0xFF303137),
                                                BusAccentCyan,
                                                BusAccentCyan
                                            )
                                        ),
                                        shape = RoundedCornerShape(32.dp)
                                    )
                                    .padding(2.5.dp)
                            ) {
                                Card(
                                    shape = RoundedCornerShape(30.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF17171B)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("welcome_logo_card")
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.bus_brand_logo),
                                        contentDescription = "شعار حافلة بني عطلي",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(30.dp))
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // App Title & Tagline
                        Text(
                            text = "حافلة بني عطلي",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 32.sp,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "تتبع فوري ودقيق لحافلات النقل • حي بني عطلي",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF94A3B8), // Slate 400
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        // Feature badges (Modern Cards)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ModernFeaturePill(
                                icon = Icons.Default.Map,
                                title = "خرائط Google",
                                subtitle = "أقمار صناعية",
                                accentColor = BusAccentCyan,
                                modifier = Modifier.weight(1f)
                            )
                            ModernFeaturePill(
                                icon = Icons.Default.Radar,
                                title = "تنبيهات صوتية",
                                subtitle = "لكل المحطات",
                                accentColor = BusAccentAmber,
                                modifier = Modifier.weight(1f)
                            )
                            ModernFeaturePill(
                                icon = Icons.Default.ElectricBolt,
                                title = "تحديث حي",
                                subtitle = "كل 5 ثوانٍ",
                                accentColor = BusAccentEmerald,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "خدمة مجانية للمواطنين والطلبة • اتصال مباشر بالأقمار الصناعية",
                            color = Color(0xFF64748B),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernFeaturePill(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF252327).copy(alpha = 0.78f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(accentColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                color = Color(0xFF94A3B8),
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
