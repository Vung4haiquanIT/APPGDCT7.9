package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.BannerItem
import com.example.model.Course
import com.example.model.Lesson
import com.example.ui.components.SavedDocumentsDialog
import com.example.ui.components.Vung4LogoBadge
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.NavySecondary
import com.example.ui.theme.RedPrimary
import com.example.viewmodel.AppViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrangChuScreen(
    viewModel: AppViewModel,
    onNavigateToHocTap: () -> Unit,
    onNavigateToThongBao: () -> Unit,
    onNavigateToDebug: () -> Unit = {},
    onCategoryClick: (String) -> Unit
) {
    val courses by viewModel.courses.collectAsState()
    val lessons by viewModel.lessons.collectAsState()
    val userDoc by viewModel.userDoc.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()
    val progressList by viewModel.progressList.collectAsState()
    val recentLessonIds by viewModel.recentLessonIds.collectAsState()
    val storageFiles by viewModel.storageFiles.collectAsState()

    val context = LocalContext.current
    var showSavedDocsDialog by remember { mutableStateOf(false) }
    val savedDocsList = remember(lessons, storageFiles, showSavedDocsDialog) {
        com.example.ui.components.getAllSavedDocuments(context, lessons, storageFiles)
    }
    val savedDocsCount = savedDocsList.size

    // Danh sách bài học xem gần đây (ghép từ recentLessonIds và lịch sử progressList)
    val recentLessons = remember(recentLessonIds, progressList, lessons) {
        val lessonMap = lessons.associateBy { it.id }
        val orderedLessonIds = mutableListOf<String>()

        // 1. Ưu tiên bài học vừa xem gần đây nhất
        for (id in recentLessonIds) {
            if (id !in orderedLessonIds && lessonMap.containsKey(id)) {
                orderedLessonIds.add(id)
            }
        }

        // 2. Kế tiếp là các bài học đã có tiến độ trong progressList (sắp xếp mới nhất trước)
        val progressSorted = progressList.sortedByDescending { it.updatedAt }
        for (p in progressSorted) {
            if (p.lessonId !in orderedLessonIds && lessonMap.containsKey(p.lessonId)) {
                orderedLessonIds.add(p.lessonId)
            }
        }

        orderedLessonIds.mapNotNull { lessonMap[it] }
    }

    val banners by viewModel.banners.collectAsState()
    val bannerList = remember(banners) {
        if (banners.isNotEmpty()) banners.take(5) else BannerItem.getDefaultMilitaryBanners()
    }
    val pagerState = rememberPagerState(pageCount = { bannerList.size })

    LaunchedEffect(bannerList.size) {
        if (bannerList.size > 1) {
            while (true) {
                delay(4000L)
                val nextPage = (pagerState.currentPage + 1) % bannerList.size
                pagerState.animateScrollToPage(
                    page = nextPage,
                    animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
                )
            }
        }
    }

    var activeLessonForPlayer by remember { mutableStateOf<Lesson?>(null) }

    if (activeLessonForPlayer != null) {
        LessonPlayerScreen(
            lesson = activeLessonForPlayer!!,
            viewModel = viewModel,
            onBack = { activeLessonForPlayer = null }
        )
        return
    }

    val isGuest = currentUser == null && userDoc == null
    val userName = if (isGuest) {
        "Khách (Chưa đăng nhập)"
    } else {
        userDoc?.name?.takeIf { it.isNotBlank() } 
            ?: currentUser?.displayName?.takeIf { !it.isNullOrBlank() } 
            ?: currentUser?.email?.substringBefore("@")
            ?: "Cán bộ, Chiến sĩ"
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(RedPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Chào mừng bạn!",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = userName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToHocTap) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Tìm kiếm",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Box {
                        IconButton(onClick = onNavigateToThongBao) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Thông báo",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (unreadCount > 0) {
                            Badge(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-6).dp, y = 6.dp),
                                containerColor = Color.Red
                            ) {
                                Text("$unreadCount", color = Color.White, fontSize = 10.sp)
                            }
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Poster & Banner Ngang Tự Động Chuyển Động (Tối đa 5 poster)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(185.dp)
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        val currentBanner = bannerList.getOrNull(page) ?: return@HorizontalPager
                        Card(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable {
                                    if (!currentBanner.targetLessonId.isNullOrBlank()) {
                                        val targetLesson = lessons.firstOrNull { it.id == currentBanner.targetLessonId }
                                        if (targetLesson != null) {
                                            activeLessonForPlayer = targetLesson
                                        }
                                    }
                                },
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = RedPrimary)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                if (currentBanner.imageUrl.isNotBlank()) {
                                    // Poster dạng hình ảnh đăng tải từ Web Quản trị: Chỉ hiển thị ảnh, không đè chữ lên poster
                                    AsyncImage(
                                        model = currentBanner.imageUrl,
                                        contentDescription = currentBanner.title.ifEmpty { "Poster tuyên truyền" },
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    // Poster khẩu hiệu chính trị quân sự Vùng 4 (Gradient & Huy hiệu)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(RedPrimary, Color(0xFFB71C1C), Color(0xFF880E4F))
                                                )
                                            )
                                            .padding(horizontal = 20.dp, vertical = 16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Star,
                                                        contentDescription = null,
                                                        tint = GoldPrimary,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = currentBanner.title,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 16.sp,
                                                    color = Color.White,
                                                    lineHeight = 21.sp,
                                                    maxLines = 3,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (currentBanner.subtitle.isNotBlank()) {
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text(
                                                        text = currentBanner.subtitle,
                                                        fontSize = 11.sp,
                                                        color = Color.White.copy(alpha = 0.92f),
                                                        lineHeight = 15.sp,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Vung4LogoBadge(size = 86.dp)
                                        }
                                    }
                                }
                            }
                        }
                    }


                    // Thanh chỉ báo (Carousel Indicators: Thanh bo tròn mở rộng khi active, tròn khi inactive)
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(bannerList.size) { index ->
                            val isSelected = pagerState.currentPage == index
                            val indicatorWidth by animateDpAsState(
                                targetValue = if (isSelected) 24.dp else 6.dp,
                                animationSpec = tween(300),
                                label = "indicatorWidth"
                            )
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .height(6.dp)
                                    .width(indicatorWidth)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (isSelected) GoldPrimary else Color.White.copy(alpha = 0.5f))
                            )
                        }
                    }
                }
            }

            // Nhắc nhở học tập & tiến độ từ Web Quản trị
            val completedIds = progressList.filter { it.completed }.map { it.lessonId }.toSet()
            val incompleteCount = lessons.count { !completedIds.contains(it.id) }
            if (incompleteCount > 0) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToThongBao() },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFF3E0)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFE0B2)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = Color(0xFFE65100),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Nhắc nhở: Có $incompleteCount bài học thiếu tiến độ",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFFBF360C)
                                )
                                Text(
                                    text = "Nhấn để xem thông báo quản trị và cập nhật tiến độ học tập.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF5D4037)
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                tint = Color(0xFFBF360C),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Quản lý tiện ích
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tiện ích",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        if (savedDocsCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFE8F5E9),
                                border = BorderStroke(1.dp, Color(0xFF81C784).copy(alpha = 0.7f)),
                                modifier = Modifier.clickable { showSavedDocsDialog = true }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Icon(
                                        Icons.Default.DownloadDone,
                                        contentDescription = null,
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "$savedDocsCount tài liệu đã lưu",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }
                        }
                    }

                    data class UtilityEntry(
                        val title: String,
                        val icon: ImageVector,
                        val id: String,
                        val color: Color,
                        val badge: Int = 0
                    )

                    val utilities = listOf(
                        UtilityEntry("GDCT", Icons.Default.Book, "gdct", RedPrimary),
                        UtilityEntry("GDPL", Icons.Default.Balance, "gdpl", NavySecondary),
                        UtilityEntry("TỦ SÁCH\nPHÁP LUẬT", Icons.Default.AutoStories, "tu_sach_phap_luat", Color(0xFF00695C)),
                        UtilityEntry("KIỂM TRA", Icons.AutoMirrored.Filled.Assignment, "kiem_tra", Color(0xFFE65100)),
                        UtilityEntry("LỊCH SỬ\nTRUYỀN THỐNG", Icons.Default.AccountBalance, "lich_su", Color(0xFF6A1B9A)),
                        UtilityEntry("BIỂN ĐẢO\nVIỆT NAM", Icons.Default.Map, "bien_dao", Color(0xFF0277BD)),
                        UtilityEntry("TÀI LIỆU\nĐÃ LƯU", Icons.Default.FolderSpecial, "tai_lieu_da_luu", Color(0xFF2E7D32), badge = savedDocsCount)
                    )

                    val utilityRows = utilities.chunked(4)

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        utilityRows.forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowItems.forEach { util ->
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(106.dp)
                                            .clickable {
                                                if (util.id == "tai_lieu_da_luu") {
                                                    showSavedDocsDialog = true
                                                } else {
                                                    onCategoryClick(util.id)
                                                }
                                            },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f)),
                                        border = BorderStroke(1.dp, util.color.copy(alpha = 0.16f)),
                                        elevation = CardDefaults.cardElevation(2.5.dp)
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(horizontal = 4.dp, vertical = 8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(util.color.copy(alpha = 0.12f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = util.icon,
                                                        contentDescription = util.title.replace("\n", " "),
                                                        tint = util.color,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(30.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = util.title,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.5.sp,
                                                        lineHeight = 13.5.sp,
                                                        maxLines = 2,
                                                        textAlign = TextAlign.Center,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }

                                            if (util.badge > 0) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(top = 5.dp, end = 5.dp)
                                                        .size(18.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF2E7D32)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = if (util.badge > 99) "99+" else util.badge.toString(),
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color.White
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                val remainingSlots = 4 - rowItems.size
                                repeat(remainingSlots) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // Danh sách bài học đã đăng tải (Real uploaded lessons)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Bài học đã đăng tải (${lessons.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(onClick = onNavigateToHocTap) {
                        Text("Xem tất cả", color = RedPrimary, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = RedPrimary)
                    }
                }
            }

            if (lessons.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(40.dp), tint = RedPrimary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Chưa có bài học nào được đăng tải từ Web Quản Trị Vùng 4.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(lessons.take(5)) { lesson ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.recordLessonViewed(lesson.id)
                                activeLessonForPlayer = lesson
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(RedPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = null,
                                    tint = RedPrimary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = lesson.title.ifEmpty { "Bài học chính trị" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 2,
                                    color = MaterialTheme.colorScheme.onSurface
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

            // Mục "Xem gần đây" (Thay thế cho "Chuyên đề nổi bật")
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = RedPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = if (recentLessons.isNotEmpty()) "Xem gần đây (${recentLessons.size})" else "Xem gần đây",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        if (recentLessons.isNotEmpty()) {
                            TextButton(onClick = onNavigateToHocTap) {
                                Text("Xem tất cả", color = RedPrimary, fontWeight = FontWeight.Bold)
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = RedPrimary)
                            }
                        }
                    }

                    if (recentLessons.isEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToHocTap() },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)),
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
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(RedPrimary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HistoryEdu,
                                        contentDescription = null,
                                        tint = RedPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Chưa có bài học xem gần đây",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Nhấn để khám phá các bài học và bắt đầu học tập.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                    contentDescription = null,
                                    tint = RedPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            items(recentLessons) { lesson ->
                                val course = courses.find { it.id == lesson.courseId }
                                val progress = progressList.find { it.lessonId == lesson.id }
                                val isCompleted = progress?.completed == true

                                Card(
                                    modifier = Modifier
                                        .width(220.dp)
                                        .height(195.dp)
                                        .clickable {
                                            viewModel.recordLessonViewed(lesson.id)
                                            activeLessonForPlayer = lesson
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    elevation = CardDefaults.cardElevation(2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(98.dp)
                                                .background(
                                                    Brush.verticalGradient(
                                                        listOf(RedPrimary, Color(0xFF192841))
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Vung4LogoBadge(size = 56.dp)

                                            // Huy hiệu trạng thái góc trên bên phải
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(8.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(
                                                        if (isCompleted) Color(0xFF2E7D32)
                                                        else Color(0xFF0D47A1).copy(alpha = 0.85f)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.PlayCircle,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(10.dp)
                                                    )
                                                    Text(
                                                        text = if (isCompleted) "Đã học" else "Đang học",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                }
                                            }
                                        }

                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(38.dp),
                                                contentAlignment = Alignment.TopStart
                                            ) {
                                                Text(
                                                    text = lesson.title.ifEmpty { "Bài học chính trị" },
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    lineHeight = 17.sp,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = course?.title?.ifEmpty { "Chuyên đề Vùng 4" } ?: "Chuyên đề Vùng 4",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )

                                                if (progress?.scorePercentage != null) {
                                                    Text(
                                                        text = "${progress.scorePercentage}%",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (progress.scorePercentage >= 70) Color(0xFF2E7D32) else Color(0xFFE65100)
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.PlayArrow,
                                                        contentDescription = null,
                                                        tint = RedPrimary,
                                                        modifier = Modifier.size(16.dp)
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
        }
    }

    if (showSavedDocsDialog) {
        SavedDocumentsDialog(
            lessons = lessons,
            storageFiles = storageFiles,
            onNavigateToHocTap = {
                showSavedDocsDialog = false
                onNavigateToHocTap()
            },
            onDismiss = { showSavedDocsDialog = false }
        )
    }
}
