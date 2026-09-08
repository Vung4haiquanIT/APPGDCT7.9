package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.GoldPrimary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onFinished: () -> Unit
) {
    // Trạng thái hiệu ứng xuất hiện
    var startAnimation by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.55f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logo_scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "logo_alpha"
    )

    val textAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, delayMillis = 300, easing = LinearOutSlowInEasing),
        label = "text_alpha"
    )

    // Hiệu ứng vòng sáng hào quang xoay & nhịp thở mờ ảo phía sau Logo
    val infiniteTransition = rememberInfiniteTransition(label = "halo_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_alpha"
    )

    // Hiệu ứng 3 chấm chờ loading sinh động
    val dotCount = rememberInfiniteTransition(label = "dots")
    val dotStep by dotCount.animateValue(
        initialValue = 1,
        targetValue = 4,
        typeConverter = Int.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dot_step"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        // Thời gian chờ xuất hiện logo và nạp hệ thống (~2.4 giây)
        delay(2400)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF991B1B), // Đỏ tươi tâm
                        Color(0xFF7F1D1D), // Đỏ quân kỳ
                        Color(0xFF450A0A), // Đỏ thẫm
                        Color(0xFF1C0505)  // Nền viền tối trang nghiêm
                    ),
                    radius = 1100f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            // KHỐI LOGO VỚI HIỆU ỨNG HÀO QUANG & THU PHÓNG
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(230.dp)
                    .scale(scale)
                    .alpha(alpha)
            ) {
                // Vòng ánh hào quang vàng óng tỏa sáng phía sau logo
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .scale(pulseScale)
                        .alpha(pulseAlpha)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    GoldPrimary.copy(alpha = 0.65f),
                                    Color(0xFFF59E0B).copy(alpha = 0.35f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Logo chính hình chiếc khiên Vùng 4 Hải quân
                Image(
                    painter = painterResource(id = R.drawable.ic_app_logo),
                    contentDescription = "Huy hiệu Vùng 4 Hải quân",
                    modifier = Modifier
                        .width(162.dp)
                        .height(180.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // TIÊU ĐỀ & ĐƠN VỊ VỚI HIỆU ỨNG XUẤT HIỆN
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(textAlpha)
            ) {
                Text(
                    text = "QUÂN CHỦNG HẢI QUÂN",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFEF3C7),
                    letterSpacing = 2.5.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "GIÁO DỤC CHÍNH TRỊ",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Huy hiệu đơn vị
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(GoldPrimary.copy(alpha = 0.2f))
                        .padding(horizontal = 16.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "VÙNG 4 HẢI QUÂN",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary,
                        letterSpacing = 1.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Vững vàng chính trị · Giữ vững chủ quyền biển, đảo",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(52.dp))

            // HIỆU ỨNG CHỜ KHỞI ĐỘNG HỆ THỐNG
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(textAlpha)
            ) {
                // Hàng 3 dấu chấm loading nhấp nháy chuyển sắc
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) { index ->
                        val isActive = (index + 1) <= dotStep
                        Box(
                            modifier = Modifier
                                .size(if (isActive) 10.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isActive) GoldPrimary else Color.White.copy(alpha = 0.3f)
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Đang khởi động hệ thống học tập...",
                    fontSize = 12.sp,
                    color = Color(0xFFFEF3C7).copy(alpha = 0.85f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
