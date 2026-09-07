package com.example.ui.screens

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.model.Lesson
import com.example.ui.components.LoginDialog
import com.example.ui.components.Vung4LogoBadge
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.RedPrimary
import com.example.viewmodel.AppViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Suppress("OPT_IN_IS_NOT_ENABLED", "UnstableApiUsage")
@OptIn(UnstableApi::class, ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LessonPlayerScreen(
    lesson: Lesson,
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val contents by viewModel.contents.collectAsState()
    val slides by viewModel.slides.collectAsState()
    val videos by viewModel.videos.collectAsState()
    val audios by viewModel.audios.collectAsState()
    val storageFiles by viewModel.storageFiles.collectAsState()

    val lessonContents = contents.filter { it.lessonId == lesson.id }.sortedBy { it.order }
    val lessonSlides = slides.filter { it.lessonId == lesson.id }.sortedBy { it.order }
    val lessonVideos = videos.filter { it.lessonId == lesson.id }
    val lessonAudios = audios.filter { it.lessonId == lesson.id }
    val lessonFiles = storageFiles.filter { 
        it.lessonId == lesson.id || it.entityId == lesson.id || it.category == "document" || it.category == "lesson" 
    }

    // Tabs: 0 = Slide, 1 = Nội dung (kèm Tài liệu & Kiểm tra), 2 = Video, 3 = Audio
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Slide", "Nội dung & Tài liệu", "Video", "Audio")

    // Auth & Progress Tracking State
    val userDoc by viewModel.userDoc.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val progressList by viewModel.progressList.collectAsState()
    val authActionLoading by viewModel.authActionLoading.collectAsState()
    val isCompleted = progressList.any { it.lessonId == lesson.id && it.completed }
    val isLoggedIn = userDoc != null || currentUser != null

    var showLoginDialog by remember { mutableStateOf(false) }
    var syncSuccessMessage by remember { mutableStateOf<String?>(null) }
    var syncErrorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmittingProgress by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedVideoUrl by remember { mutableStateOf(lessonVideos.firstOrNull()?.videoUrl) }
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = false
        }
    }

    var selectedAudioUrl by remember { mutableStateOf(lessonAudios.firstOrNull()?.audioUrl) }
    val audioPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = false
        }
    }

    var isAudioPlaying by remember { mutableStateOf(false) }
    var audioCurrentPosition by remember { mutableStateOf(0L) }
    var audioDuration by remember { mutableStateOf(0L) }
    var isUserDraggingSlider by remember { mutableStateOf(false) }

    // Quiz state
    var selectedQuizAnswers by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var quizSubmitted by remember { mutableStateOf(false) }
    var viewingFile by remember { mutableStateOf<com.example.model.StorageFileItem?>(null) }

    // Video completion listener
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    viewModel.updateLessonProgress(
                        lessonId = lesson.id,
                        completed = true,
                        onSuccess = {
                            syncSuccessMessage = "Đã xem hết video bài học! Tiến độ học tập đã được lưu và gửi về Web Quản trị."
                        },
                        onError = { err ->
                            syncErrorMessage = err
                        }
                    )
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    // Audio completion listener
    DisposableEffect(audioPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    viewModel.updateLessonProgress(
                        lessonId = lesson.id,
                        completed = true,
                        onSuccess = {
                            syncSuccessMessage = "Đã nghe xong audio bài học! Kết quả học tập đã được gửi về Web Quản trị."
                        },
                        onError = { err ->
                            syncErrorMessage = err
                        }
                    )
                }
            }
        }
        audioPlayer.addListener(listener)
        onDispose {
            audioPlayer.removeListener(listener)
        }
    }

    DisposableEffect(selectedVideoUrl) {
        if (!selectedVideoUrl.isNullOrBlank()) {
            val mediaItem = MediaItem.fromUri(Uri.parse(selectedVideoUrl))
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
        }
        onDispose { }
    }

    DisposableEffect(selectedAudioUrl) {
        if (!selectedAudioUrl.isNullOrBlank()) {
            val mediaItem = MediaItem.fromUri(Uri.parse(selectedAudioUrl))
            audioPlayer.setMediaItem(mediaItem)
            audioPlayer.prepare()
        }
        onDispose {
            audioPlayer.stop()
        }
    }

    // Audio progress ticker
    LaunchedEffect(audioPlayer) {
        while (true) {
            if (audioPlayer.isPlaying && !isUserDraggingSlider) {
                audioCurrentPosition = audioPlayer.currentPosition
                audioDuration = audioPlayer.duration.coerceAtLeast(1L)
            }
            delay(500)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
            audioPlayer.release()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Trạng thái tiến độ hiện tại
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.HourglassTop,
                                contentDescription = null,
                                tint = if (isCompleted) Color(0xFF2E7D32) else Color(0xFFE65100),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (isCompleted) "Trạng thái: Đã hoàn thành (Đã gửi Web Admin)" else "Trạng thái: Đang học",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isCompleted) Color(0xFF2E7D32) else Color(0xFFE65100)
                            )
                        }

                        if (!isLoggedIn) {
                            TextButton(
                                onClick = { showLoginDialog = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(14.dp), tint = RedPrimary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Đăng nhập", fontSize = 11.sp, color = RedPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Nút bấm hoàn thành và đồng bộ
                    Button(
                        onClick = {
                            isSubmittingProgress = true
                            viewModel.updateLessonProgress(
                                lessonId = lesson.id,
                                completed = true,
                                onSuccess = {
                                    isSubmittingProgress = false
                                    syncSuccessMessage = "✓ Tiến độ bài học đã được đồng bộ thành công về Web Quản trị!"
                                },
                                onError = { err ->
                                    isSubmittingProgress = false
                                    syncErrorMessage = err
                                    if (!isLoggedIn) {
                                        showLoginDialog = true
                                    }
                                }
                            )
                        },
                        enabled = !isSubmittingProgress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCompleted) Color(0xFF2E7D32) else RedPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isSubmittingProgress) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Đang đồng bộ về máy chủ...", color = Color.White, fontSize = 13.sp)
                        } else {
                            Icon(
                                imageVector = if (isCompleted) Icons.Default.CloudDone else Icons.Default.CloudUpload,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isCompleted) "CẬP NHẬT TIẾN ĐỘ VỀ WEB QUẢN TRỊ" else "HOÀN THÀNH BÀI HỌC & LƯU VỀ QUẢN TRỊ",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                        Column {
                            Text(
                                text = lesson.title.ifEmpty { "Chi tiết bài học" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                maxLines = 2
                            )
                        }
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
            // Lesson Header info card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Vung4LogoBadge(size = 38.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = lesson.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = RedPrimary,
                                maxLines = 2,
                                lineHeight = 20.sp
                            )
                        }
                    }

                    // Thông tin tài khoản & Kết nối Web Quản trị
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isLoggedIn) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isLoggedIn) Color(0xFF2E7D32) else Color(0xFFE65100))
                                )
                                Text(
                                    text = if (isLoggedIn) {
                                        "Tài khoản: ${userDoc?.name ?: currentUser?.email} (${userDoc?.unit ?: "Vùng 4 Hải Quân"})"
                                    } else {
                                        "Chế độ Khách (Chưa đăng nhập)"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isLoggedIn) Color(0xFF1B5E20) else Color(0xFFBF360C),
                                    maxLines = 1
                                )
                            }

                            if (!isLoggedIn) {
                                Text(
                                    text = "Đăng nhập ngay",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RedPrimary,
                                    modifier = Modifier.clickable { showLoginDialog = true }
                                )
                            } else {
                                Text(
                                    text = if (isCompleted) "✓ Đã hoàn thành" else "● Đang học",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCompleted) Color(0xFF2E7D32) else Color(0xFFE65100)
                                )
                            }
                        }
                    }
                }
            }

            // Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = RedPrimary,
                edgePadding = 16.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        },
                        selectedContentColor = RedPrimary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Tab Content Body
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        // TAB 0: SLIDES
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (lessonSlides.isNotEmpty()) {
                                val pagerState = rememberPagerState(pageCount = { lessonSlides.size })
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        HorizontalPager(
                                            state = pagerState,
                                            modifier = Modifier.fillMaxSize()
                                        ) { page ->
                                            val slide = lessonSlides[page]
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (slide.imageUrl.isNotBlank()) {
                                                    AsyncImage(
                                                        model = slide.imageUrl,
                                                        contentDescription = "Slide ${page + 1}",
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                } else {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .background(RedPrimary.copy(alpha = 0.1f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(64.dp), tint = RedPrimary)
                                                            Spacer(modifier = Modifier.height(8.dp))
                                                            Text("Slide số ${page + 1} (Chưa có hình ảnh)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        Surface(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(12.dp),
                                            color = Color.Black.copy(alpha = 0.6f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = " Slide ${pagerState.currentPage + 1} / ${lessonSlides.size} ",
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Button(
                                        onClick = {
                                            if (pagerState.currentPage > 0) {
                                                coroutineScope.launch {
                                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                                }
                                            }
                                        },
                                        enabled = pagerState.currentPage > 0,
                                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                                    ) {
                                        Icon(Icons.Default.ChevronLeft, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Slide trước")
                                    }

                                    Button(
                                        onClick = {
                                            if (pagerState.currentPage < lessonSlides.size - 1) {
                                                coroutineScope.launch {
                                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                                }
                                            }
                                        },
                                        enabled = pagerState.currentPage < lessonSlides.size - 1,
                                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                                    ) {
                                        Text("Slide sau")
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                                    }
                                }

                                if (pagerState.currentPage == lessonSlides.size - 1) {
                                    Button(
                                        onClick = {
                                            isSubmittingProgress = true
                                            viewModel.updateLessonProgress(
                                                lessonId = lesson.id,
                                                completed = true,
                                                onSuccess = {
                                                    isSubmittingProgress = false
                                                    syncSuccessMessage = "Đã hoàn thành học toàn bộ Slide! Tiến độ đã được lưu và gửi về Web Quản trị."
                                                },
                                                onError = { err ->
                                                    isSubmittingProgress = false
                                                    syncErrorMessage = err
                                                }
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Xác nhận đã học xong slide - Lưu về Web Quản trị", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Slideshow, contentDescription = null, modifier = Modifier.size(64.dp), tint = RedPrimary)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text("Chưa có slide bài giảng nào cho bài học này.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // TAB 1: NỘI DUNG (HTML rendering with bold/italic, followed immediately by Tài liệu & Câu hỏi kiểm tra)
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // 1. Lesson Text Contents
                            Text(
                                text = "Nội dung bài học",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = RedPrimary
                            )

                            if (lessonContents.isNotEmpty()) {
                                lessonContents.forEach { content ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            if (content.title.isNotBlank()) {
                                                Text(
                                                    text = content.title,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp,
                                                    color = RedPrimary
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                            }

                                            AndroidView(
                                                factory = { ctx ->
                                                    TextView(ctx).apply {
                                                        textSize = 14f
                                                        setTextColor(AndroidColor.parseColor("#333333"))
                                                        setLineSpacing(6f, 1.25f)
                                                    }
                                                },
                                                update = { tv ->
                                                    tv.text = HtmlCompat.fromHtml(
                                                        content.bodyHtml.ifEmpty { "Chưa có nội dung chi tiết." },
                                                        HtmlCompat.FROM_HTML_MODE_LEGACY
                                                    )
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            } else {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.Article, contentDescription = null, modifier = Modifier.size(40.dp), tint = RedPrimary)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = lesson.description.ifEmpty { "Nội dung chi tiết đang được cập nhật từ hệ thống Web Quản Trị Vùng 4." },
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Divider()
                            Spacer(modifier = Modifier.height(8.dp))

                            // 2. Tài liệu học tập & Tải về
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, tint = RedPrimary)
                                Text(
                                    text = "Tài liệu học tập & Tải về (${lessonFiles.size})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = RedPrimary
                                )
                            }

                            if (lessonFiles.isNotEmpty()) {
                                lessonFiles.forEach { file ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.Description, contentDescription = null, tint = RedPrimary, modifier = Modifier.size(26.dp))
                                                Column {
                                                    Text(
                                                        text = file.title.ifEmpty { file.fileName.ifEmpty { "Tài liệu bài học" } },
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        maxLines = 1
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = "Lưu & Xem trực tiếp trên app",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            Button(
                                                onClick = {
                                                    if (file.downloadUrl.isNotBlank()) {
                                                        viewingFile = file
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                                            ) {
                                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Xem ngay", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }

                                if (viewingFile != null) {
                                    val f = viewingFile!!
                                    val fmt = if (f.downloadUrl.contains(".pdf", ignoreCase = true) || f.fileName.contains(".pdf", ignoreCase = true)) "pdf" else "docx"
                                    com.example.ui.components.InAppDocumentViewerDialog(
                                        fileTitle = f.title.ifEmpty { f.fileName.ifEmpty { "Tài liệu" } },
                                        fileUrl = f.downloadUrl,
                                        fileFormat = fmt,
                                        onDismiss = { viewingFile = null }
                                    )
                                }
                            } else {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Chưa có tài liệu đính kèm nào cho bài học này.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Divider()
                            Spacer(modifier = Modifier.height(8.dp))

                            // 3. Câu hỏi kiểm tra (Quiz)
                            val sampleQuestions = listOf(
                                Triple(
                                    "1. Nội dung cốt lõi trong đường lối chính trị của Vùng 4 Hải quân là gì?",
                                    listOf("Kiên định mục tiêu độc lập dân tộc và chủ nghĩa xã hội", "Phát triển kinh tế biển đơn thuần", "Hợp tác quốc tế đa phương", "Xây dựng lực lượng tên lửa bờ"),
                                    0
                                ),
                                Triple(
                                    "2. Truyền thống vẻ vang của Quân chủng Hải quân Nhân dân Việt Nam là gì?",
                                    listOf("Đánh yếu thắng mạnh", "Chiến đấu anh dũng, mưu trí sáng tạo, làm chủ vùng biển, quyết chiến quyết thắng", "Phòng thủ biên giới đất liền", "Huấn luyện chuyên sâu"),
                                    1
                                ),
                                Triple(
                                    "3. Nhiệm vụ trọng tâm của cán bộ, chiến sĩ tại Vùng 4 Hải quân là gì?",
                                    listOf("Quản lý, bảo vệ vững chắc chủ quyền biển đảo, thềm lục địa thiêng liêng của Tổ quốc", "Tham gia tuần tra biên giới Tây Nam", "Phát triển công nghiệp đóng tàu", "Xây dựng hạ tầng du lịch biển"),
                                    0
                                )
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Quiz, contentDescription = null, tint = RedPrimary)
                                Text(
                                    text = "Câu hỏi kiểm tra kiến thức bài học",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = RedPrimary
                                )
                            }

                            sampleQuestions.forEachIndexed { qIndex, (question, options, correctIndex) ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = question,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))

                                        options.forEachIndexed { optIndex, option ->
                                            val isSelected = selectedQuizAnswers[qIndex] == optIndex
                                            val isCorrect = optIndex == correctIndex
                                            val cardColor = when {
                                                quizSubmitted && isCorrect -> Color(0xFFC8E6C9)
                                                quizSubmitted && isSelected && !isCorrect -> Color(0xFFFFCDD2)
                                                isSelected -> RedPrimary.copy(alpha = 0.15f)
                                                else -> MaterialTheme.colorScheme.surfaceVariant
                                            }
                                            val textColor = when {
                                                quizSubmitted && isCorrect -> Color(0xFF1B5E20)
                                                quizSubmitted && isSelected && !isCorrect -> Color(0xFFB71C1C)
                                                isSelected -> RedPrimary
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }

                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        if (!quizSubmitted) {
                                                            selectedQuizAnswers = selectedQuizAnswers + (qIndex to optIndex)
                                                        }
                                                    },
                                                shape = RoundedCornerShape(8.dp),
                                                color = cardColor
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    RadioButton(
                                                        selected = isSelected,
                                                        onClick = {
                                                            if (!quizSubmitted) {
                                                                selectedQuizAnswers = selectedQuizAnswers + (qIndex to optIndex)
                                                            }
                                                        }
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = option,
                                                        fontSize = 13.sp,
                                                        color = textColor,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    val correctCount = sampleQuestions.indices.count { qIndex ->
                                        val correctIndex = sampleQuestions[qIndex].third
                                        selectedQuizAnswers[qIndex] == correctIndex
                                    }
                                    quizSubmitted = true
                                    isSubmittingProgress = true
                                    viewModel.updateLessonProgress(
                                        lessonId = lesson.id,
                                        completed = true,
                                        score = correctCount,
                                        totalQuestions = sampleQuestions.size,
                                        onSuccess = {
                                            isSubmittingProgress = false
                                            syncSuccessMessage = "Hoàn thành kiểm tra: $correctCount/${sampleQuestions.size} câu đúng! Kết quả đã được đồng bộ về Web Quản trị."
                                        },
                                        onError = { err ->
                                            isSubmittingProgress = false
                                            syncErrorMessage = err
                                        }
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Nộp bài & Lưu kết quả về Quản trị", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }

                            if (quizSubmitted) {
                                val correctCount = sampleQuestions.indices.count { qIndex ->
                                    val correctIndex = sampleQuestions[qIndex].third
                                    selectedQuizAnswers[qIndex] == correctIndex
                                }
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = GoldPrimary.copy(alpha = 0.2f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "Kết quả kiểm tra: $correctCount / ${sampleQuestions.size} câu đúng",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (correctCount == sampleQuestions.size) "Xuất sắc! Bạn đã nắm vững kiến thức bài học." else "Hãy xem lại nội dung bài học để củng cố thêm kiến thức nhé.",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // TAB 2: VIDEO (Tách riêng)
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Video Bài Giảng",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = RedPrimary
                            )

                            if (lessonVideos.isNotEmpty() || !selectedVideoUrl.isNullOrBlank()) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(220.dp),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    AndroidView(
                                        factory = { ctx ->
                                            PlayerView(ctx).apply {
                                                player = exoPlayer
                                                useController = true
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                if (lessonVideos.size > 1) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        lessonVideos.forEach { vid ->
                                            Button(
                                                onClick = { selectedVideoUrl = vid.videoUrl },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (selectedVideoUrl == vid.videoUrl) RedPrimary else MaterialTheme.colorScheme.surfaceVariant
                                                ),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(vid.title.ifEmpty { "Video" }, fontSize = 11.sp, maxLines = 1)
                                            }
                                        }
                                    }
                                }
                            } else {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(48.dp), tint = RedPrimary)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("Chưa có video bài giảng nào cho bài học này.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        // TAB 3: AUDIO (Tách riêng)
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Audio Ghi Âm Bài Giảng",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = RedPrimary
                            )

                            if (lessonAudios.isNotEmpty() || !selectedAudioUrl.isNullOrBlank()) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(64.dp)
                                                .background(RedPrimary.copy(alpha = 0.15f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AudioFile,
                                                contentDescription = null,
                                                tint = RedPrimary,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Text(
                                            text = lessonAudios.firstOrNull { it.audioUrl == selectedAudioUrl }?.title ?: lesson.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        Slider(
                                            value = if (audioDuration > 0) audioCurrentPosition.toFloat() else 0f,
                                            onValueChange = { newValue ->
                                                isUserDraggingSlider = true
                                                audioCurrentPosition = newValue.toLong()
                                            },
                                            onValueChangeFinished = {
                                                isUserDraggingSlider = false
                                                audioPlayer.seekTo(audioCurrentPosition)
                                            },
                                            valueRange = 0f..(if (audioDuration > 0) audioDuration.toFloat() else 1f),
                                            colors = SliderDefaults.colors(
                                                thumbColor = RedPrimary,
                                                activeTrackColor = RedPrimary
                                            )
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = formatMillis(audioCurrentPosition), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(text = formatMillis(audioDuration), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        IconButton(
                                            onClick = {
                                                if (audioPlayer.isPlaying) {
                                                    audioPlayer.pause()
                                                    isAudioPlaying = false
                                                } else {
                                                    audioPlayer.play()
                                                    isAudioPlaying = true
                                                }
                                            },
                                            modifier = Modifier
                                                .size(56.dp)
                                                .background(RedPrimary, CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = if (isAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = "Play/Pause",
                                                tint = Color.White,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                }
                            } else {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.AudioFile, contentDescription = null, modifier = Modifier.size(48.dp), tint = RedPrimary)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("Chưa có audio ghi âm nào cho bài học này.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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

    // THÔNG BÁO KẾT QUẢ ĐỒNG BỘ TIẾN ĐỘ THÀNH CÔNG
    if (syncSuccessMessage != null) {
        AlertDialog(
            onDismissRequest = { syncSuccessMessage = null },
            icon = {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(40.dp))
            },
            title = {
                Text("ĐỒNG BỘ THÀNH CÔNG", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1B5E20))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(syncSuccessMessage ?: "", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        "Dữ liệu tiến độ học tập của tài khoản đã được đẩy lên Web Quản trị để theo dõi quá trình học tập.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { syncSuccessMessage = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("Đã hiểu", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // THÔNG BÁO LỖI HOẶC YÊU CẦU ĐĂNG NHẬP
    if (syncErrorMessage != null) {
        AlertDialog(
            onDismissRequest = { syncErrorMessage = null },
            icon = {
                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(36.dp))
            },
            title = {
                Text("TIẾN ĐỘ ĐÃ LƯU TRÊN MÁY", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFFE65100))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(syncErrorMessage ?: "", fontSize = 13.sp)
                    if (!isLoggedIn) {
                        Text(
                            "Đăng nhập tài khoản được cấp để hệ thống gửi tiến độ về máy chủ Web Quản trị.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = RedPrimary
                        )
                    }
                }
            },
            confirmButton = {
                if (!isLoggedIn) {
                    Button(
                        onClick = {
                            syncErrorMessage = null
                            showLoginDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                    ) {
                        Text("Đăng nhập ngay", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(onClick = { syncErrorMessage = null }) {
                        Text("Đóng")
                    }
                }
            },
            dismissButton = {
                if (!isLoggedIn) {
                    TextButton(onClick = { syncErrorMessage = null }) {
                        Text("Để sau")
                    }
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // HỘP THOẠI ĐĂNG NHẬP
    if (showLoginDialog) {
        LoginDialog(
            isLoading = authActionLoading,
            onDismiss = { showLoginDialog = false },
            onLogin = { usernameOrEmail, password, onError ->
                viewModel.loginWithAdminAccount(
                    emailOrUsername = usernameOrEmail,
                    pass = password,
                    onSuccess = {
                        showLoginDialog = false
                        // Tự động đẩy tiến độ bài học này ngay sau khi đăng nhập thành công
                        viewModel.updateLessonProgress(
                            lessonId = lesson.id,
                            completed = true,
                            onSuccess = {
                                syncSuccessMessage = "Đăng nhập thành công! Tiến độ bài học đã được tự động lưu về Web Quản trị."
                            }
                        )
                    },
                    onError = onError
                )
            }
        )
    }
}

private fun formatMillis(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
