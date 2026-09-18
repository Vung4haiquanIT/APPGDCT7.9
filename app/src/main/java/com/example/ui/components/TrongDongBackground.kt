package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.ui.theme.MyApplicationTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.example.R
import com.example.ui.theme.CreamBackground
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldPrimary

/**
 * Nền họa tiết hoa văn Trống Đồng Đông Sơn viền ánh vàng sang trọng & trang nghiêm,
 * giúp giao diện ứng dụng mang đậm bản sắc truyền thống Quân chủng Hải quân & Quân đội Nhân dân Việt Nam.
 */
@Composable
fun TrongDongBackground(
    modifier: Modifier = Modifier,
    watermarkAlpha: Float = 0.13f,
    showCornerBorders: Boolean = true,
    showTopBottomBorders: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFFDF9),
                        Color(0xFFFAF6EE),
                        Color(0xFFF7F2E7)
                    )
                )
            )
    ) {
        // 1. HOA VĂN TRỐNG ĐỒNG ĐÔNG SƠN MỜ CHÍNH GIỮA MÀN HÌNH (WATERMARK LỚN)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 30.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_trong_dong_pattern),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .aspectRatio(1f)
                    .alpha(watermarkAlpha),
                contentScale = ContentScale.Fit
            )
        }

        // 2. HOA VĂN TRỐNG ĐỒNG ĐÔNG SƠN PHỤ Ở GÓC TRÊN BÊN PHẢI (TẠO ĐỘ CHIỀU SÂU)
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.TopEnd
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_trong_dong_pattern),
                contentDescription = null,
                modifier = Modifier
                    .size(220.dp)
                    .offset(x = 60.dp, y = (-50).dp)
                    .alpha(watermarkAlpha * 0.7f),
                contentScale = ContentScale.Fit
            )
        }

        // 3. KHUNG VIỀN HỌA TIẾT VÀNG KIM TRỐNG ĐỒNG (CANVAS VẼ ĐỒNG TÂM VÀ GÓC HOA VĂN)
        if (showCornerBorders || showTopBottomBorders) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val goldColor = Color(0xFFD4AF37).copy(alpha = 0.35f)
                val goldAccent = Color(0xFFB45309).copy(alpha = 0.22f)
                val strokeW = 1.5.dp.toPx()

                // Đường chỉ viền vàng nhẹ trên cùng và dưới cùng
                if (showTopBottomBorders) {
                    // Viền trên
                    drawLine(
                        brush = Brush.horizontalGradient(
                            listOf(Color.Transparent, Color(0xFFD4AF37).copy(alpha = 0.45f), Color.Transparent)
                        ),
                        start = Offset(0f, 2f),
                        end = Offset(w, 2f),
                        strokeWidth = strokeW
                    )

                    // Viền dưới
                    drawLine(
                        brush = Brush.horizontalGradient(
                            listOf(Color.Transparent, Color(0xFFD4AF37).copy(alpha = 0.45f), Color.Transparent)
                        ),
                        start = Offset(0f, h - 2f),
                        end = Offset(w, h - 2f),
                        strokeWidth = strokeW
                    )
                }

                // Góc hoa văn trống đồng vàng kim ở 4 góc màn hình
                if (showCornerBorders) {
                    val cornerSize = 40.dp.toPx()
                    val margin = 8.dp.toPx()

                    // Góc trên - trái
                    val pathTopLeft = Path().apply {
                        moveTo(margin, margin + cornerSize)
                        lineTo(margin, margin)
                        lineTo(margin + cornerSize, margin)
                    }
                    drawPath(pathTopLeft, color = goldColor, style = Stroke(width = strokeW))

                    // Điểm nhấn chấm vàng góc trên-trái
                    drawCircle(
                        color = goldAccent,
                        radius = 2.5.dp.toPx(),
                        center = Offset(margin + 6.dp.toPx(), margin + 6.dp.toPx())
                    )

                    // Góc trên - phải
                    val pathTopRight = Path().apply {
                        moveTo(w - margin - cornerSize, margin)
                        lineTo(w - margin, margin)
                        lineTo(w - margin, margin + cornerSize)
                    }
                    drawPath(pathTopRight, color = goldColor, style = Stroke(width = strokeW))

                    drawCircle(
                        color = goldAccent,
                        radius = 2.5.dp.toPx(),
                        center = Offset(w - margin - 6.dp.toPx(), margin + 6.dp.toPx())
                    )

                    // Góc dưới - trái
                    val pathBottomLeft = Path().apply {
                        moveTo(margin, h - margin - cornerSize)
                        lineTo(margin, h - margin)
                        lineTo(margin + cornerSize, h - margin)
                    }
                    drawPath(pathBottomLeft, color = goldColor, style = Stroke(width = strokeW))

                    drawCircle(
                        color = goldAccent,
                        radius = 2.5.dp.toPx(),
                        center = Offset(margin + 6.dp.toPx(), h - margin - 6.dp.toPx())
                    )

                    // Góc dưới - phải
                    val pathBottomRight = Path().apply {
                        moveTo(w - margin - cornerSize, h - margin)
                        lineTo(w - margin, h - margin)
                        lineTo(w - margin, h - margin - cornerSize)
                    }
                    drawPath(pathBottomRight, color = goldColor, style = Stroke(width = strokeW))

                    drawCircle(
                        color = goldAccent,
                        radius = 2.5.dp.toPx(),
                        center = Offset(w - margin - 6.dp.toPx(), h - margin - 6.dp.toPx())
                    )
                }
            }
        }

        // 4. LỚP NỘI DUNG CHÍNH CỦA MÀN HÌNH
        content()
    }
}

@Preview(showBackground = true)
@Composable
fun TrongDongBackgroundPreview() {
    MyApplicationTheme {
        TrongDongBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

            }
        }
    }
}
