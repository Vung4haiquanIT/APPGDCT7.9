package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.components.Vung4LogoBadge
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.RedPrimary
import com.example.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThanhTichScreen(
    viewModel: AppViewModel
) {
    val progressList by viewModel.progressList.collectAsState()
    val progressStatus by viewModel.progressStatus.collectAsState()
    val lessons by viewModel.lessons.collectAsState()
    val userDoc by viewModel.userDoc.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val totalLessons = lessons.size.coerceAtLeast(1)
    val completedCount = progressList.count { it.completed }
    val completionPercentage = (completedCount * 100) / totalLessons

    // Tính điểm trung bình trắc nghiệm
    val scoredItems = progressList.filter { it.scorePercentage != null || (it.score != null && it.totalQuestions != null && it.totalQuestions > 0) }
    val averageScore = if (scoredItems.isNotEmpty()) {
        val totalPercents = scoredItems.sumOf { item ->
            item.scorePercentage ?: if (item.score != null && item.totalQuestions != null && item.totalQuestions > 0) {
                (item.score * 100 / item.totalQuestions)
            } else 100
        }
        totalPercents / scoredItems.size
    } else null

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
            // THẺ THÔNG TIN TÀI KHOẢN HỌC VIÊN
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (!userDoc?.avatarUrl.isNullOrEmpty()) {
                            Surface(
                                modifier = Modifier.size(48.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.5.dp, GoldPrimary),
                                shadowElevation = 2.dp
                            ) {
                                AsyncImage(
                                    model = userDoc?.avatarUrl,
                                    contentDescription = "Ảnh đại diện",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        } else {
                            Vung4LogoBadge(size = 48.dp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = userDoc?.name?.ifEmpty { currentUser?.displayName ?: currentUser?.email ?: "Học viên Vùng 4" } ?: "Học viên Vùng 4",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = RedPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Đơn vị: ${userDoc?.unit?.ifEmpty { "Vùng 4 Hải Quân" } ?: "Vùng 4 Hải Quân"}${if (!userDoc?.rank.isNullOrBlank()) " • ${userDoc?.rank}" else ""}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Hệ thống: $progressStatus",
                                fontSize = 11.sp,
                                color = if (progressStatus.startsWith("CONNECTED")) Color(0xFF2E7D32) else Color(0xFFE65100),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // THỐNG KÊ TỔNG QUAN
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Bài hoàn thành
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.TaskAlt, contentDescription = null, tint = RedPrimary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Đã hoàn thành", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$completedCount / $totalLessons",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = RedPrimary
                            )
                        }
                    }

                    // Điểm trung bình
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Điểm kiểm tra TB", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (averageScore != null) "$averageScore%" else "--",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = if (averageScore != null && averageScore >= 50) Color(0xFF1B5E20) else RedPrimary
                            )
                        }
                    }

                    // Tỷ lệ hoàn thành
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.PieChart, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Tiến độ khóa học", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$completionPercentage%",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Chi tiết tiến độ học tập & điểm kiểm tra theo bài",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = RedPrimary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            if (progressList.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.MenuBook, contentDescription = null, tint = RedPrimary, modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Chưa có dữ liệu tiến độ học tập.", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Hãy chọn bài học tại Trang chủ và hoàn thành 3 bước học tập.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            } else {
                items(progressList) { progress ->
                    val lessonTitle = lessons.find { it.id == progress.lessonId }?.title ?: progress.lessonId
                    val dateFormatted = try {
                        val sdf = SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault())
                        sdf.format(Date(progress.updatedAt))
                    } catch (e: Exception) { "" }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Tiêu đề bài học và trạng thái
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (progress.completed) Icons.Default.CheckCircle else Icons.Default.HourglassTop,
                                        contentDescription = null,
                                        tint = if (progress.completed) Color(0xFF2E7D32) else Color(0xFFE65100),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = lessonTitle,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (progress.completed) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                                ) {
                                    Text(
                                        text = if (progress.completed) "100% Hoàn thành" else "Đang học",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (progress.completed) Color(0xFF1B5E20) else Color(0xFFBF360C),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            // Chi tiết 3 bước & Điểm kiểm tra
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Điểm kiểm tra
                                if (progress.score != null || progress.scorePercentage != null) {
                                    val scoreText = if (progress.score != null && progress.totalQuestions != null) {
                                        "${progress.score}/${progress.totalQuestions} câu"
                                    } else "${progress.scorePercentage ?: 100}%"

                                    val isPassed = (progress.scorePercentage ?: 100) >= 50

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isPassed) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Quiz,
                                                contentDescription = null,
                                                tint = if (isPassed) Color(0xFF2E7D32) else Color(0xFFC62828),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "Điểm: $scoreText",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isPassed) Color(0xFF1B5E20) else Color(0xFFC62828)
                                            )
                                        }
                                    }
                                }

                                // Bước Slide
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (progress.viewedSlides || progress.completed) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = if (progress.viewedSlides || progress.completed) "Slide ✓" else "Slide --",
                                        fontSize = 10.sp,
                                        color = if (progress.viewedSlides || progress.completed) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                    )
                                }

                                // Bước Nội dung
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (progress.readContent || progress.completed) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = if (progress.readContent || progress.completed) "Nội dung ✓" else "Nội dung --",
                                        fontSize = 10.sp,
                                        color = if (progress.readContent || progress.completed) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            if (dateFormatted.isNotBlank()) {
                                Text(
                                    text = "Cập nhật gần nhất: $dateFormatted",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
