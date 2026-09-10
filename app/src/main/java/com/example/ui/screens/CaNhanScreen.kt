package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.ExamResultDoc
import com.example.ui.components.LoginDialog
import com.example.ui.components.Vung4LogoBadge
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.NavySecondary
import com.example.ui.theme.RedPrimary
import com.example.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaNhanScreen(
    viewModel: AppViewModel,
    onNavigateToThongBao: () -> Unit = {},
    onNavigateToDebug: () -> Unit = {}
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val userDoc by viewModel.userDoc.collectAsState()
    val userExamResults by viewModel.userExamResults.collectAsState()
    val authActionLoading by viewModel.authActionLoading.collectAsState()
    val authMessage by viewModel.authMessage.collectAsState()

    var showLoginDialog by remember { mutableStateOf(false) }
    var showAllExamHistoryDialog by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    val isAuthenticated = currentUser != null || userDoc != null

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Vung4LogoBadge(size = 32.dp)
                        Text("THÔNG TIN CÁ NHÂN", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // KHỐI THÔNG TIN TÀI KHOẢN (GỌN GÀNG, KHÔNG LAN MAN)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isAuthenticated) 
                            MaterialTheme.colorScheme.surfaceVariant 
                        else 
                            MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Avatar Huy hiệu
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Vung4LogoBadge(size = 84.dp)
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(if (isAuthenticated) Color(0xFF16A34A) else Color(0xFFEAB308))
                                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isAuthenticated) Icons.Default.Check else Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (!isAuthenticated) {
                            // CHẾ ĐỘ KHÁCH
                            Text(
                                text = "Khách (Chưa đăng nhập)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // NÚT ĐĂNG NHẬP GỌN GÀNG
                            Button(
                                onClick = { showLoginDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Login,
                                    contentDescription = null,
                                    tint = GoldPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ĐĂNG NHẬP",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                            }
                        } else {
                            // ĐÃ ĐĂNG NHẬP
                            Text(
                                text = userDoc?.name?.ifEmpty { currentUser?.displayName ?: currentUser?.email ?: "Học viên Vùng 4" } ?: "Học viên Vùng 4",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = RedPrimary.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = userDoc?.role?.ifEmpty { "Học viên" } ?: "Học viên",
                                        fontSize = 12.sp,
                                        color = RedPrimary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }

                                if (!userDoc?.rank.isNullOrEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = GoldPrimary.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = userDoc?.rank ?: "",
                                            fontSize = 12.sp,
                                            color = Color(0xFFB45309),
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = userDoc?.unit?.ifEmpty { "Vùng 4 Hải Quân" } ?: "Vùng 4 Hải Quân",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (!currentUser?.email.isNullOrBlank() || !userDoc?.email.isNullOrBlank()) {
                                Text(
                                    text = currentUser?.email ?: userDoc?.email ?: "",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            OutlinedButton(
                                onClick = { viewModel.logout() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = RedPrimary)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ĐĂNG XUẤT",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // THÀNH TÍCH ĐỢT THI & KIỂM TRA (ĐỒNG BỘ WEB QUẢN TRỊ)
            item {
                val totalExams = userExamResults.size
                val avgPercent = if (totalExams > 0) userExamResults.map { it.scorePercentage }.average().toInt() else 0
                val passRate = if (totalExams > 0) (userExamResults.count { it.passed } * 100 / totalExams) else 0

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(RedPrimary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = RedPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "THÀNH TÍCH THI & KIỂM TRA",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    // Text(
                                    //     text = "Đồng bộ thời gian thực với Web Quản trị",
                                    //     fontSize = 11.sp,
                                    //     color = MaterialTheme.colorScheme.onSurfaceVariant
                                    // )
                                }
                            }

                            // Surface(
                            //     shape = RoundedCornerShape(12.dp),
                            //     color = Color(0xFFE8F5E9)
                            // ) {
                            //     Row(
                            //         modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            //         verticalAlignment = Alignment.CenterVertically,
                            //         horizontalArrangement = Arrangement.spacedBy(4.dp)
                            //     ) {
                            //         Box(
                            //             modifier = Modifier
                            //                 .size(6.dp)
                            //                 .clip(CircleShape)
                            //                 .background(Color(0xFF2E7D32))
                            //         )
                            //         Text(
                            //             text = "Web Synced",
                            //             fontSize = 10.sp,
                            //             fontWeight = FontWeight.Bold,
                            //             color = Color(0xFF2E7D32)
                            //         )
                            //     }
                            // }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Aggregate Stats Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Total Exams
                            Card(
                                modifier = Modifier.weight(1f).padding(end = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = RedPrimary.copy(alpha = 0.06f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("$totalExams", fontWeight = FontWeight.Black, fontSize = 20.sp, color = RedPrimary)
                                    Text("Số bài thi", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            // Average Score
                            Card(
                                modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                                colors = CardDefaults.cardColors(containerColor = NavySecondary.copy(alpha = 0.06f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("$avgPercent%", fontWeight = FontWeight.Black, fontSize = 20.sp, color = NavySecondary)
                                    Text("Điểm trung bình", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            // Pass Rate
                            Card(
                                modifier = Modifier.weight(1f).padding(start = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32).copy(alpha = 0.06f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("$passRate%", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color(0xFF2E7D32))
                                    Text("Tỷ lệ Đạt", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "LỊCH SỬ CÁC ĐỢT THI GẦN ĐÂY",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (userExamResults.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.Assessment,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Chưa có kết quả thi nào được ghi nhận",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Hãy chuyển sang mục 'Kiểm tra' để tham gia các đợt thi trực tuyến và đồng bộ kết quả về hệ thống!",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Chỉ hiển thị tối đa 3 lần thi gần nhất
                                userExamResults.take(3).forEach { res ->
                                    val isPassed = res.passed
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = res.examName.ifEmpty { "Bài thi kiểm tra trắc nghiệm" },
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "Thời gian nộp: ${dateFormat.format(Date(res.timestamp))}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Column(horizontalAlignment = Alignment.End) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isPassed) Color(0xFFE8F5E9) else RedPrimary.copy(alpha = 0.12f)
                                                ) {
                                                    Text(
                                                        text = if (isPassed) "ĐẠT (${res.scorePercentage}%)" else "CHƯA ĐẠT (${res.scorePercentage}%)",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isPassed) Color(0xFF2E7D32) else RedPrimary,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "${res.score}/${res.totalQuestions} câu",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }

                                // Nút xem thêm nếu có nhiều hơn 3 lần thi
                                if (userExamResults.size > 3) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedButton(
                                        onClick = { showAllExamHistoryDialog = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = RedPrimary.copy(alpha = 0.05f),
                                            contentColor = RedPrimary
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, RedPrimary.copy(alpha = 0.35f))
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.History,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = RedPrimary
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Xem thêm lịch sử thi (${userExamResults.size} bài thi)",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = RedPrimary
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp),
                                                tint = RedPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // THÔNG BÁO & NHẮC NHỞ HỌC TẬP
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToThongBao() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(RedPrimary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = RedPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Thông báo & Nhắc nhở",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Thông báo từ Web Quản trị và nhắc nhở tiến độ học tập",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }


            // THÔNG TIN ỨNG DỤNG GỌN GÀNG
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Thông tin ứng dụng",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Text(text = "Đơn vị: Vùng 4 Hải Quân", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "Phiên bản: 1.0.0", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }


    // DIALOG ĐĂNG NHẬP
    if (showLoginDialog) {
        LoginDialog(
            isLoading = authActionLoading,
            onDismiss = { showLoginDialog = false },
            onLogin = { emailOrUser, pass, onErrorCallback ->
                viewModel.loginWithAdminAccount(
                    emailOrUsername = emailOrUser,
                    pass = pass,
                    onSuccess = {
                        showLoginDialog = false
                    },
                    onError = { err ->
                        onErrorCallback(err)
                    }
                )
            }
        )
    }

    // HỘP THOẠI XEM TOÀN BỘ LỊCH SỬ THI & KIỂM TRA
    if (showAllExamHistoryDialog) {
        Dialog(
            onDismissRequest = { showAllExamHistoryDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            BackHandler { showAllExamHistoryDialog = false }

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Vung4LogoBadge(size = 32.dp)
                                Column {
                                    Text(
                                        text = "LỊCH SỬ THI & KIỂM TRA",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Toàn bộ ${userExamResults.size} kết quả đã đồng bộ",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { showAllExamHistoryDialog = false }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Quay lại",
                                    tint = Color.White
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = RedPrimary,
                            titleContentColor = Color.White,
                            navigationIconContentColor = Color.White
                        )
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
                    ) {
                        // Thẻ tóm tắt thành tích tổng quan
                        item {
                            val totalExamsCount = userExamResults.size
                            val passedCount = userExamResults.count { it.passed }
                            val failedCount = totalExamsCount - passedCount
                            val avgScore = if (totalExamsCount > 0) userExamResults.map { it.scorePercentage }.average().toInt() else 0

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "TỔNG HỢP KẾT QUẢ THI",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text("$totalExamsCount", fontWeight = FontWeight.Black, fontSize = 20.sp, color = RedPrimary)
                                            Text("Tổng bài thi", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text("$avgScore%", fontWeight = FontWeight.Black, fontSize = 20.sp, color = NavySecondary)
                                            Text("Điểm TB", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text("$passedCount", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color(0xFF2E7D32))
                                            Text("Đạt", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text("$failedCount", fontWeight = FontWeight.Black, fontSize = 20.sp, color = RedPrimary)
                                            Text("Chưa đạt", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }

                        // Danh sách toàn bộ bài thi
                        items(userExamResults) { res ->
                            val isPassed = res.passed
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = res.examName.ifEmpty { "Bài thi kiểm tra trắc nghiệm" },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AccessTime,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = "Thời gian nộp: ${dateFormat.format(Date(res.timestamp))}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(horizontalAlignment = Alignment.End) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isPassed) Color(0xFFE8F5E9) else RedPrimary.copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                text = if (isPassed) "ĐẠT (${res.scorePercentage}%)" else "CHƯA ĐẠT (${res.scorePercentage}%)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isPassed) Color(0xFF2E7D32) else RedPrimary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = "${res.score}/${res.totalQuestions} câu",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
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
    }
}

