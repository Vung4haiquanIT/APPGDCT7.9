package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.model.Lesson
import com.example.model.NotificationItem
import com.example.ui.components.Vung4LogoBadge
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.RedPrimary
import com.example.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.*

fun isNotificationPermissionGranted(context: Context): Boolean {
    val areNotifsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
    if (!areNotifsEnabled) return false
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

enum class NotificationFilter(val label: String) {
    ALL("Tất cả"),
    REMINDER("Nhắc học tập"),
    ADMIN("Web Quản trị"),
    UNREAD("Chưa đọc")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThongBaoScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onNavigateToHocTap: () -> Unit = {}
) {
    val context = LocalContext.current
    val notifications by viewModel.notifications.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()
    val lessons by viewModel.lessons.collectAsState()
    val progressList by viewModel.progressList.collectAsState()

    var selectedFilter by remember { mutableStateOf(NotificationFilter.ALL) }
    var activeLessonForPlayer by remember { mutableStateOf<Lesson?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current
    var isPermissionGranted by remember { mutableStateOf(isNotificationPermissionGranted(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isPermissionGranted = isNotificationPermissionGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // If player is open, render LessonPlayerScreen
    if (activeLessonForPlayer != null) {
        LessonPlayerScreen(
            lesson = activeLessonForPlayer!!,
            viewModel = viewModel,
            onBack = { activeLessonForPlayer = null }
        )
        return
    }

    // Permission launcher for Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        isPermissionGranted = isGranted || isNotificationPermissionGranted(context)
        if (isGranted) {
            viewModel.pushReminderToDevice()
            Toast.makeText(context, "Đã cấp quyền thông báo thành công!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Chưa cấp quyền nhận thông báo trên điện thoại", Toast.LENGTH_SHORT).show()
        }
    }

    val requestNotificationPermission = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                isPermissionGranted = true
                viewModel.pushReminderToDevice()
                Toast.makeText(context, "Đã cấp quyền thông báo trên điện thoại!", Toast.LENGTH_SHORT).show()
            } else {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                try {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Vui lòng bật thông báo trong Cài đặt của máy", Toast.LENGTH_SHORT).show()
                }
            } else {
                isPermissionGranted = true
                viewModel.pushReminderToDevice()
                Toast.makeText(context, "Đã bật thông báo trên điện thoại!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Filtered items
    val filteredList = remember(notifications, selectedFilter) {
        when (selectedFilter) {
            NotificationFilter.ALL -> notifications
            NotificationFilter.REMINDER -> notifications.filter { it.type == "reminder" }
            NotificationFilter.ADMIN -> notifications.filter { it.type == "admin" }
            NotificationFilter.UNREAD -> notifications.filter { !it.isRead }
        }
    }

    // Calculate missing lessons count
    val completedLessonIds = remember(progressList) {
        progressList.filter { it.completed }.map { it.lessonId }.toSet()
    }
    val incompleteLessons = remember(lessons, completedLessonIds) {
        lessons.filter { !completedLessonIds.contains(it.id) }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Vung4LogoBadge(size = 32.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "THÔNG BÁO & NHẮC NHỞ",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Hệ thống Quản trị & Tiến độ học tập",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("notification_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                },
                actions = {
                    if (unreadCount > 0) {
                        IconButton(
                            onClick = { viewModel.markAllNotificationsAsRead() },
                            modifier = Modifier.testTag("mark_all_read_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Đánh dấu tất cả đã đọc",
                                tint = RedPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // BANNER NHẮC NHỞ / CẤP QUYỀN VỀ ĐIỆN THOẠI (Chỉ hiển thị khi CHƯA cấp quyền trên máy, cấp xong lập tức ẩn đi)
            if (!isPermissionGranted) {
                item {
                    AnimatedVisibility(
                        visible = !isPermissionGranted,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = NavyPrimary.copy(alpha = 0.08f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(NavyPrimary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = NavyPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Bật thông báo đẩy về điện thoại",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = NavyPrimary
                                    )
                                    Text(
                                        text = "Cấp quyền để nhận nhắc nhở bài học thiếu tiến độ và chỉ đạo từ Web Quản trị kịp thời.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                FilledTonalButton(
                                    onClick = { requestNotificationPermission() },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("push_notification_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Cấp quyền",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // FILTER TABS
            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(NotificationFilter.values()) { filter ->
                        val isSelected = selectedFilter == filter
                        val count = when (filter) {
                            NotificationFilter.ALL -> notifications.size
                            NotificationFilter.REMINDER -> notifications.count { it.type == "reminder" }
                            NotificationFilter.ADMIN -> notifications.count { it.type == "admin" }
                            NotificationFilter.UNREAD -> unreadCount
                        }

                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = filter },
                            label = {
                                Text(
                                    text = "${filter.label} ($count)",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RedPrimary.copy(alpha = 0.15f),
                                selectedLabelColor = RedPrimary
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }

            // DANH SÁCH THÔNG BÁO
            if (filteredList.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 60.dp, start = 32.dp, end = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsNone,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Không có thông báo nào",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Các chỉ đạo từ Web Quản trị và nhắc nhở học tập sẽ xuất hiện tại đây.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                items(filteredList, key = { it.id }) { item ->
                    NotificationCard(
                        item = item,
                        onClick = {
                            viewModel.markNotificationAsRead(item.id)
                            if (item.type == "reminder" && !item.targetLessonId.isNullOrBlank()) {
                                val target = lessons.find { it.id == item.targetLessonId }
                                if (target != null) {
                                    activeLessonForPlayer = target
                                } else {
                                    onNavigateToHocTap()
                                }
                            }
                        },
                        onOpenLesson = {
                            viewModel.markNotificationAsRead(item.id)
                            if (!item.targetLessonId.isNullOrBlank()) {
                                val target = lessons.find { it.id == item.targetLessonId }
                                if (target != null) {
                                    activeLessonForPlayer = target
                                } else {
                                    onNavigateToHocTap()
                                }
                            } else {
                                onNavigateToHocTap()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationCard(
    item: NotificationItem,
    onClick: () -> Unit,
    onOpenLesson: () -> Unit
) {
    val isReminder = item.type == "reminder"
    val isUrgent = item.priority == "urgent" || item.priority == "high"

    val headerColor = when {
        isUrgent -> RedPrimary
        isReminder -> Color(0xFFE65100) // Dark Orange
        else -> NavyPrimary
    }

    val iconVector = when {
        isReminder -> Icons.Default.MenuBook
        isUrgent -> Icons.Default.Campaign
        else -> Icons.Default.Announcement
    }

    val categoryLabel = when {
        isReminder -> "TIẾN ĐỘ HỌC TẬP"
        isUrgent -> "CHỈ ĐẠO KHẨN - WEB ADMIN"
        else -> "THÔNG BÁO TỪ BỘ TƯ LỆNH"
    }

    val dateStr = remember(item.timestamp) {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        sdf.format(Date(item.timestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() }
            .testTag("notification_card_${item.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isRead) {
                MaterialTheme.colorScheme.surface
            } else {
                headerColor.copy(alpha = 0.05f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isRead) 1.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // ROW HEADER
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(headerColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = headerColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = categoryLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = headerColor
                    )
                    Text(
                        text = dateStr,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!item.isRead) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(RedPrimary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // TITLE
            Text(
                text = item.title,
                fontSize = 14.sp,
                fontWeight = if (!item.isRead) FontWeight.Bold else FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // MESSAGE
            if (item.message.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.message,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }

            // ACTION BUTTON FOR REMINDERS
            if (isReminder) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onOpenLesson,
                    colors = ButtonDefaults.buttonColors(containerColor = headerColor),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Vào học ngay",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
