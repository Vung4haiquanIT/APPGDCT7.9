package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.BannerItem
import com.example.model.Course
import com.example.model.Lesson
import com.example.ui.components.Vung4LogoBadge
import com.example.ui.theme.GoldPrimary
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
                    containerColor = MaterialTheme.colorScheme.surface
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
                    Text(
                        text = "Quản lý tiện ích",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    val utilities = listOf(
                        Triple("GDCT", Icons.Default.Book, "gdct"),
                        Triple("GDPL", Icons.Default.Balance, "gdpl"),
                        Triple("KIỂM TRA", Icons.AutoMirrored.Filled.Assignment, "kiem_tra"),
                        Triple("LỊCH SỬ\nTRUYỀN THỐNG", Icons.Default.AccountBalance, "lich_su"),
                        Triple("BIỂN ĐẢO\nVIỆT NAM", Icons.Default.Map, "bien_dao")
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(utilities) { util ->
                            Card(
                                modifier = Modifier
                                    .width(92.dp)
                                    .height(116.dp)
                                    .clickable { onCategoryClick(util.third) },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 6.dp, vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(RedPrimary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = util.second,
                                            contentDescription = util.first.replace("\n", " "),
                                            tint = RedPrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = util.first,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            lineHeight = 13.sp,
                                            maxLines = 2,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
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
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
                            .clickable { activeLessonForPlayer = lesson },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
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

            // Chuyên đề / Khóa học nổi bật (Real courses from Firestore)
            if (courses.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Chuyên đề nổi bật (${courses.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            TextButton(onClick = onNavigateToHocTap) {
                                Text("Xem tất cả", color = RedPrimary, fontWeight = FontWeight.Bold)
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = RedPrimary)
                            }
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            items(courses) { course ->
                                Card(
                                    modifier = Modifier
                                        .width(180.dp)
                                        .height(195.dp)
                                        .clickable { onNavigateToHocTap() },
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
                                                .height(108.dp)
                                                .background(
                                                    Brush.verticalGradient(
                                                        listOf(RedPrimary, Color(0xFF192841))
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Vung4LogoBadge(size = 64.dp)
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
                                                    text = course.title,
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
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                                    contentDescription = null,
                                                    tint = RedPrimary,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = "${lessons.count { it.courseId == course.id }} bài học",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
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
    }


}
