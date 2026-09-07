package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.GoldPrimary

@Composable
fun Vung4LogoBadge(
    modifier: Modifier = Modifier,
    size: Dp = 100.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(6.dp, CircleShape)
            .clip(CircleShape)
            .border(
                2.dp,
                Brush.linearGradient(
                    listOf(
                        GoldPrimary,
                        Color(0xFFFEF3C7),
                        Color(0xFFB45309)
                    )
                ),
                CircleShape
            )
            .background(Color(0xFF7F1D1D)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_app_logo),
            contentDescription = "Logo GDCT Vùng 4 Hải quân",
            modifier = Modifier.fillMaxSize()
        )
    }
}
