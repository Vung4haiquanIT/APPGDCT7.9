package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.Vung4LogoBadge
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.RedPrimary
import com.example.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirebaseDebugScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val firebaseStatus by viewModel.firebaseStatus.collectAsState()
    val firestoreStatus by viewModel.firestoreStatus.collectAsState()
    val courses by viewModel.courses.collectAsState()
    val lessons by viewModel.lessons.collectAsState()
    val contents by viewModel.contents.collectAsState()
    val slides by viewModel.slides.collectAsState()
    val videos by viewModel.videos.collectAsState()
    val audios by viewModel.audios.collectAsState()
    val storageFiles by viewModel.storageFiles.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val userDocStatus by viewModel.userDocStatus.collectAsState()
    val progressStatus by viewModel.progressStatus.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Vung4LogoBadge(size = 36.dp)
                        Text("VÙNG 4 HẢI QUÂN - ADMIN", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = RedPrimary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = { viewModel.checkConnectionAndStartRealtime() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Làm mới", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Vung4LogoBadge(size = 72.dp)
                        Column {
                            Text(
                                text = "HỆ THỐNG GIÁO DỤC CHÍNH TRỊ",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = RedPrimary
                            )
                            Text(
                                text = "Vùng 4 Hải Quân - Dự án: gdctv4-4e1f3",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                DebugStatusCard(
                    title = "Firebase Core",
                    status = firebaseStatus,
                    isSuccess = firebaseStatus.startsWith("CONNECTED")
                )
            }

            item {
                DebugStatusCard(
                    title = "Firestore (default)",
                    status = firestoreStatus,
                    isSuccess = firestoreStatus == "CONNECTED"
                )
            }

            item {
                DebugCollectionCard(
                    title = "Courses",
                    count = courses.size,
                    emptyMessage = "Firestore hiện chưa có dữ liệu courses."
                )
            }

            item {
                DebugCollectionCard(
                    title = "Lessons",
                    count = lessons.size,
                    emptyMessage = "Firestore hiện chưa có dữ liệu lessons."
                )
            }

            item {
                DebugCollectionCard(
                    title = "Contents",
                    count = contents.size,
                    emptyMessage = "Firestore hiện chưa có dữ liệu contents."
                )
            }

            item {
                DebugCollectionCard(
                    title = "Slides",
                    count = slides.size,
                    emptyMessage = "Firestore hiện chưa có dữ liệu slides."
                )
            }

            item {
                DebugCollectionCard(
                    title = "Videos",
                    count = videos.size,
                    emptyMessage = "Firestore hiện chưa có dữ liệu videos."
                )
            }

            item {
                DebugCollectionCard(
                    title = "Audios",
                    count = audios.size,
                    emptyMessage = "Firestore hiện chưa có dữ liệu audios."
                )
            }

            item {
                DebugCollectionCard(
                    title = "StorageFiles",
                    count = storageFiles.size,
                    emptyMessage = "Firestore hiện chưa có dữ liệu storageFiles."
                )
            }

            item {
                DebugStatusCard(
                    title = "Users (Auth: ${currentUser?.email ?: "Chưa đăng nhập"})",
                    status = userDocStatus,
                    isSuccess = userDocStatus == "CONNECTED"
                )
            }

            item {
                DebugStatusCard(
                    title = "Progress",
                    status = progressStatus,
                    isSuccess = progressStatus.startsWith("CONNECTED")
                )
            }

            if (!errorMessage.isNullOrBlank()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Lỗi Realtime: $errorMessage",
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DebugStatusCard(title: String, status: String, isSuccess: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = status, fontSize = 13.sp, color = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            }
            Icon(
                imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                tint = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun DebugCollectionCard(title: String, count: Int, emptyMessage: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    text = "$count documents",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (count > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            if (count == 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = emptyMessage, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
