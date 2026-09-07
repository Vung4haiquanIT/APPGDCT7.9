package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.Vung4LogoBadge
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.RedPrimary
import com.example.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThanhTichScreen(
    viewModel: AppViewModel
) {
    val progressList by viewModel.progressList.collectAsState()
    val progressStatus by viewModel.progressStatus.collectAsState()
    val lessons by viewModel.lessons.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Vung4LogoBadge(size = 32.dp)
                        Text("THÀNH TÍCH & TIẾN ĐỘ", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = RedPrimary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(RedPrimary.copy(alpha = 0.2f), RoundedCornerShape(32.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(36.dp))
                        }
                        Column {
                            Text(text = "Tổng số bài hoàn thành", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${progressList.count { it.completed }} / ${lessons.size.coerceAtLeast(1)} bài",
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = RedPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Trạng thái: $progressStatus", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Lịch sử học tập gần đây",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            if (progressList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Chưa có dữ liệu tiến độ học tập.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(progressList) { progress ->
                    val lessonTitle = lessons.find { it.id == progress.lessonId }?.title ?: "Bài học"
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = if (progress.completed) Icons.Default.CheckCircle else Icons.Default.HourglassEmpty,
                                    contentDescription = null,
                                    tint = if (progress.completed) Color(0xFF2E7D32) else GoldPrimary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(text = lessonTitle, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (progress.completed) "Đã hoàn thành" else "Đang học",
                                        fontSize = 12.sp,
                                        color = if (progress.completed) Color(0xFF2E7D32) else GoldPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
