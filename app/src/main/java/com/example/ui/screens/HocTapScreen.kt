package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Lesson
import com.example.ui.components.Vung4LogoBadge
import com.example.ui.theme.RedPrimary
import com.example.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HocTapScreen(
    viewModel: AppViewModel
) {
    val courses by viewModel.courses.collectAsState()
    val lessons by viewModel.lessons.collectAsState()
    var selectedCourseId by remember { mutableStateOf<String?>(null) }
    var activeLessonForPlayer by remember { mutableStateOf<Lesson?>(null) }

    if (activeLessonForPlayer != null) {
        LessonPlayerScreen(
            lesson = activeLessonForPlayer!!,
            viewModel = viewModel,
            onBack = { activeLessonForPlayer = null }
        )
        return
    }

    val filteredLessons = if (selectedCourseId == null) {
        lessons
    } else {
        lessons.filter { it.courseId == selectedCourseId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Vung4LogoBadge(size = 32.dp)
                        Text("HỌC TẬP CHÍNH TRỊ - VÙNG 4", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = RedPrimary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Course Filter Chips
            ScrollableTabRow(
                selectedTabIndex = if (selectedCourseId == null) 0 else courses.indexOfFirst { it.id == selectedCourseId } + 1,
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedCourseId == null,
                    onClick = { selectedCourseId = null },
                    text = { Text("Tất cả bài học (${lessons.size})", fontWeight = FontWeight.Bold) }
                )
                courses.forEach { course ->
                    Tab(
                        selected = selectedCourseId == course.id,
                        onClick = { selectedCourseId = course.id },
                        text = { Text(course.title.ifEmpty { "Chuyên đề" }, maxLines = 1, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (filteredLessons.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(48.dp), tint = RedPrimary)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Chưa có bài học nào được đồng bộ từ Web Quản Trị.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    items(filteredLessons) { lesson ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { activeLessonForPlayer = lesson },
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(RedPrimary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayCircle,
                                        contentDescription = null,
                                        tint = RedPrimary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = lesson.title.ifEmpty { "Bài học chính trị" },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        maxLines = 2
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
