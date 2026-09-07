package com.example.ui.screens

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.util.Log
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.model.Lesson
import com.example.ui.components.InAppDocumentViewerDialog
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

    // Tabs: 0 = Slide, 1 = Nội dung & Tài liệu, 2 = Video, 3 = Audio
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Slide", "Nội dung & Tài liệu", "Video", "Audio")

    // Auth & Progress Tracking State
    val userDoc by viewModel.userDoc.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val progressList by viewModel.progressList.collectAsState()
    val authActionLoading by viewModel.authActionLoading.collectAsState()
    val isLoggedIn = userDoc != null || currentUser != null

    // Tìm dữ liệu tiến độ đã lưu trước đó của bài học này
    val existingProgress = remember(progressList, lesson.id) {
        progressList.find { it.lessonId == lesson.id }
    }

    // Danh sách các slide đã xem đủ ít nhất 5 giây
    var viewedSlideIndices by remember(lesson.id, existingProgress, lessonSlides.size) {
        mutableStateOf<Set<Int>>(
            if (existingProgress?.viewedSlides == true || existingProgress?.completed == true) {
                (0 until lessonSlides.size).toSet()
            } else {
                emptySet()
            }
        )
    }

    // Thời gian đã xem ở slide hiện tại (đếm từ 0s -> 5s)
    var currentSlideDwellSeconds by remember { mutableIntStateOf(0) }

    // Tỷ lệ cuộn nội dung cao nhất học viên đã đạt được (0.0 -> 1.0)
    var maxContentScrollRatio by remember(lesson.id, existingProgress) {
        mutableFloatStateOf(
            if (existingProgress?.readContent == true || existingProgress?.completed == true) 1f else 0f
        )
    }

    // 3. Trả lời câu hỏi trắc nghiệm & nộp bài
    var quizSubmitted by remember(lesson.id, existingProgress) {
        mutableStateOf(
            existingProgress?.passedQuiz == true ||
            existingProgress?.completed == true ||
            (existingProgress?.score != null)
        )
    }

    var currentScore by remember(lesson.id, existingProgress) {
        mutableStateOf(existingProgress?.score)
    }
    var currentTotalQuestions by remember(lesson.id, existingProgress) {
        mutableStateOf(existingProgress?.totalQuestions ?: 3)
    }
    var currentScorePercent by remember(lesson.id, existingProgress) {
        mutableStateOf(existingProgress?.scorePercentage)
    }

    // Danh sách câu hỏi trắc nghiệm kiểm tra kiến thức bài học
    val sampleQuestions = remember {
        listOf(
            Triple(
                "1. Nội dung cốt lõi trong đường lối chính trị của Vùng 4 Hải quân là gì?",
                listOf(
                    "Kiên định mục tiêu độc lập dân tộc và chủ nghĩa xã hội",
                    "Phát triển kinh tế biển đơn thuần",
                    "Hợp tác quốc tế đa phương",
                    "Xây dựng lực lượng tên lửa bờ"
                ),
                0
            ),
            Triple(
                "2. Truyền thống vẻ vang của Quân chủng Hải quân Nhân dân Việt Nam là gì?",
                listOf(
                    "Đánh yếu thắng mạnh",
                    "Chiến đấu anh dũng, mưu trí sáng tạo, làm chủ vùng biển, quyết chiến quyết thắng",
                    "Phòng thủ biên giới đất liền",
                    "Huấn luyện chuyên sâu"
                ),
                1
            ),
            Triple(
                "3. Nhiệm vụ trọng tâm của cán bộ, chiến sĩ tại Vùng 4 Hải quân là gì?",
                listOf(
                    "Quản lý, bảo vệ vững chắc chủ quyền biển đảo, thềm lục địa thiêng liêng của Tổ quốc",
                    "Tham gia tuần tra biên giới Tây Nam",
                    "Phát triển công nghiệp đóng tàu",
                    "Xây dựng hạ tầng du lịch biển"
                ),
                0
            )
        )
    }

    // Trạng thái đáp án người dùng đã chọn
    var selectedQuizAnswers by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }

    // Tính toán tỷ lệ từng phần:
    // 1. Tỷ lệ Slide: xem slide nào đủ 5s mới tính phần trăm slide đó
    val slideRatio = remember(viewedSlideIndices, lessonSlides.size) {
        if (lessonSlides.isEmpty()) 1f
        else (viewedSlideIndices.size.toFloat() / lessonSlides.size).coerceIn(0f, 1f)
    }
    val hasViewedAllSlides = lessonSlides.isEmpty() || viewedSlideIndices.size >= lessonSlides.size

    // 2. Tỷ lệ Nội dung & Tài liệu: kéo từ trên xuống dưới trên tổng chiều dài nội dung
    val contentRatio = maxContentScrollRatio.coerceIn(0f, 1f)
    val hasReadContent = maxContentScrollRatio >= 0.95f

    // 3. Tỷ lệ Trắc nghiệm: nộp bài là đạt 100% phần trắc nghiệm hoặc tính theo số câu đã làm
    val quizRatio = remember(quizSubmitted, selectedQuizAnswers, sampleQuestions.size) {
        if (quizSubmitted) 1f
        else if (sampleQuestions.isNotEmpty()) (selectedQuizAnswers.size.toFloat() / sampleQuestions.size).coerceIn(0f, 1f)
        else 1f
    }

    // TỔNG TIẾN ĐỘ HOÀN THÀNH: Chia đều 100% cho các phần (Slide, Nội dung, Trắc nghiệm)
    val progressPercentage = remember(slideRatio, contentRatio, quizRatio, lessonSlides.size, quizSubmitted, hasViewedAllSlides, hasReadContent) {
        if (lessonSlides.isEmpty()) {
            // Không có slide: Nội dung 50%, Trắc nghiệm 50%
            if (hasReadContent && quizSubmitted) 100
            else {
                val p = (contentRatio * 50f) + (quizRatio * 50f)
                p.toInt().coerceIn(0, 99)
            }
        } else {
            // Có slide: Slide 33.33%, Nội dung 33.33%, Trắc nghiệm 33.34%
            if (hasViewedAllSlides && hasReadContent && quizSubmitted) 100
            else {
                val p = (slideRatio * 33.333f) + (contentRatio * 33.333f) + (quizRatio * 33.334f)
                p.toInt().coerceIn(0, 99)
            }
        }
    }
    val isFullyCompleted = progressPercentage == 100

    var showLoginDialog by remember { mutableStateOf(false) }
    var completionCelebrationDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedVideoUrl by remember { mutableStateOf(lessonVideos.firstOrNull()?.videoUrl) }
    var videoErrorMessage by remember { mutableStateOf<String?>(null) }
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = false
        }
    }

    var selectedAudioUrl by remember { mutableStateOf(lessonAudios.firstOrNull()?.audioUrl) }
    var audioErrorMessage by remember { mutableStateOf<String?>(null) }
    val audioPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = false
        }
    }

    var isAudioPlaying by remember { mutableStateOf(false) }
    var audioCurrentPosition by remember { mutableStateOf(0L) }
    var audioDuration by remember { mutableStateOf(0L) }
    var isUserDraggingSlider by remember { mutableStateOf(false) }
    var viewingFile by remember { mutableStateOf<com.example.model.StorageFileItem?>(null) }

    // Tự động lưu và đồng bộ tiến độ về Web Quản trị trong nền
    fun triggerAutoSave(
        scoreVal: Int? = currentScore,
        totalVal: Int? = currentTotalQuestions,
        forceComplete: Boolean = isFullyCompleted
    ) {
        val totalQ = totalVal ?: sampleQuestions.size
        val score = scoreVal ?: (if (quizSubmitted) (currentScore ?: totalQ) else null)

        viewModel.updateLessonProgress(
            lessonId = lesson.id,
            completed = forceComplete,
            score = score,
            totalQuestions = totalQ,
            viewedSlides = hasViewedAllSlides,
            readContent = hasReadContent,
            passedQuiz = quizSubmitted,
            onSuccess = {
                // Tự động đồng bộ thành công
            },
            onError = {
                // Đã lưu offline trên máy
            }
        )
    }

    // Tự động lưu và đồng bộ tiến độ về Web Quản trị trong nền khi phần trăm tăng hoặc hoàn thành
    var lastSyncedPercent by remember { mutableIntStateOf(-1) }
    LaunchedEffect(progressPercentage, isFullyCompleted) {
        if (progressPercentage > lastSyncedPercent) {
            lastSyncedPercent = progressPercentage
            triggerAutoSave(forceComplete = isFullyCompleted)
        }
    }

    // Video listener & Error handling
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    maxContentScrollRatio = 1f
                }
            }
            override fun onPlayerError(error: PlaybackException) {
                Log.w("LessonPlayerScreen", "Video player error: ${error.message}")
                videoErrorMessage = "Không thể phát video (URL không khả dụng hoặc lỗi kết nối)"
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    // Audio listener & Error handling
    DisposableEffect(audioPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    maxContentScrollRatio = 1f
                    isAudioPlaying = false
                }
            }
            override fun onPlayerError(error: PlaybackException) {
                Log.w("LessonPlayerScreen", "Audio player error: ${error.message}")
                isAudioPlaying = false
                audioErrorMessage = "Không thể phát audio (URL không khả dụng hoặc lỗi kết nối)"
            }
        }
        audioPlayer.addListener(listener)
        onDispose {
            audioPlayer.removeListener(listener)
        }
    }

    // Chuẩn bị video chỉ khi người dùng mở Tab Video (Tab 2)
    LaunchedEffect(selectedTab, selectedVideoUrl) {
        if (selectedTab == 2 && !selectedVideoUrl.isNullOrBlank()) {
            try {
                videoErrorMessage = null
                val mediaItem = MediaItem.fromUri(Uri.parse(selectedVideoUrl))
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
            } catch (e: Exception) {
                videoErrorMessage = "Không thể tải video: ${e.localizedMessage}"
            }
        } else if (selectedTab != 2) {
            exoPlayer.pause()
        }
    }

    // Chuẩn bị audio chỉ khi người dùng mở Tab Audio (Tab 3)
    LaunchedEffect(selectedTab, selectedAudioUrl) {
        if (selectedTab == 3 && !selectedAudioUrl.isNullOrBlank()) {
            try {
                audioErrorMessage = null
                val mediaItem = MediaItem.fromUri(Uri.parse(selectedAudioUrl))
                audioPlayer.setMediaItem(mediaItem)
                audioPlayer.prepare()
            } catch (e: Exception) {
                audioErrorMessage = "Không thể tải audio: ${e.localizedMessage}"
            }
        } else if (selectedTab != 3) {
            audioPlayer.pause()
            isAudioPlaying = false
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
            try {
                exoPlayer.stop()
                exoPlayer.release()
            } catch (e: Exception) { }
            try {
                audioPlayer.stop()
                audioPlayer.release()
            } catch (e: Exception) { }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 8.dp)
                    ) {
                        Vung4LogoBadge(size = 36.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = lesson.title.ifEmpty { "Chi tiết bài học" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                maxLines = 2,
                                lineHeight = 19.sp,
                                color = Color.White
                            )
                            if (isLoggedIn) {
                                Text(
                                    text = "${userDoc?.name ?: currentUser?.email ?: ""} • ${userDoc?.unit ?: "Vùng 4 Hải Quân"}",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.9f),
                                    maxLines = 1
                                )
                            }
                        }

                        // Hiển thị phần trăm tiến độ bài học gọn gàng trên thanh tiêu đề
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (progressPercentage == 100) Color(0xFF2E7D32) else GoldPrimary,
                            shadowElevation = 2.dp
                        ) {
                            Text(
                                text = "$progressPercentage%",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (progressPercentage == 100) Color.White else Color(0xFF8B0000),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
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
            // Thanh chỉ báo phần trăm tiến độ thanh mảnh, hiện đại
            LinearProgressIndicator(
                progress = { progressPercentage / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = if (progressPercentage == 100) Color(0xFF2E7D32) else GoldPrimary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            // Tabs học tập
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
                                fontSize = 14.sp
                            )
                        },
                        selectedContentColor = RedPrimary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Toàn bộ không gian hiển thị nội dung học tập rộng tối đa
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        // TAB 0: SLIDES
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            if (lessonSlides.isNotEmpty()) {
                                val pagerState = rememberPagerState(pageCount = { lessonSlides.size })

                                // Bộ đếm thời gian: Học viên phải xem slide hiện tại ít nhất 5 giây mới tính là đã xem
                                LaunchedEffect(pagerState.currentPage, selectedTab) {
                                    if (selectedTab == 0 && lessonSlides.isNotEmpty()) {
                                        val page = pagerState.currentPage
                                        if (!viewedSlideIndices.contains(page)) {
                                            currentSlideDwellSeconds = 0
                                            while (currentSlideDwellSeconds < 5) {
                                                kotlinx.coroutines.delay(1000L)
                                                currentSlideDwellSeconds++
                                            }
                                            viewedSlideIndices = viewedSlideIndices + page
                                        } else {
                                            currentSlideDwellSeconds = 5
                                        }
                                    }
                                }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = CardDefaults.cardElevation(2.dp)
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
                                                            .background(RedPrimary.copy(alpha = 0.08f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(64.dp), tint = RedPrimary)
                                                            Spacer(modifier = Modifier.height(10.dp))
                                                            Text("Slide số ${page + 1}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // Badge hiển thị trạng thái xem slide và đếm ngược 5 giây
                                        val isCurrentSlideViewed = viewedSlideIndices.contains(pagerState.currentPage)
                                        Surface(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(12.dp),
                                            color = Color.Black.copy(alpha = 0.75f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                if (isCurrentSlideViewed) {
                                                    Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = null,
                                                        tint = Color(0xFF4CAF50),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Text(
                                                        text = "Slide ${pagerState.currentPage + 1}/${lessonSlides.size} (Đã xem ${viewedSlideIndices.size}/${lessonSlides.size})",
                                                        color = Color.White,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                } else {
                                                    CircularProgressIndicator(
                                                        progress = { currentSlideDwellSeconds / 5f },
                                                        modifier = Modifier.size(14.dp),
                                                        color = GoldPrimary,
                                                        strokeWidth = 2.dp
                                                    )
                                                    Text(
                                                        text = "Slide ${pagerState.currentPage + 1}/${lessonSlides.size} • Đang xem: ${currentSlideDwellSeconds}/5s",
                                                        color = Color.White,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
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
                                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.ChevronLeft, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Slide trước", fontSize = 14.sp)
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
                                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Slide sau", fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Default.ChevronRight, contentDescription = null)
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
                                        Text(
                                            text = "Bài học này chưa có slide.",
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // TAB 1: NỘI DUNG & TÀI LIỆU & TRẮC NGHIỆM
                        val contentScrollState = rememberScrollState()

                        // Tự động nhận diện độ sâu kéo cuộn từ trên xuống dưới trên tổng chiều dài nội dung
                        LaunchedEffect(contentScrollState.value, contentScrollState.maxValue) {
                            if (contentScrollState.maxValue > 0) {
                                val ratio = (contentScrollState.value.toFloat() / contentScrollState.maxValue).coerceIn(0f, 1f)
                                if (ratio > maxContentScrollRatio) {
                                    maxContentScrollRatio = ratio
                                }
                            } else if (lessonContents.isEmpty()) {
                                maxContentScrollRatio = 1f
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(contentScrollState),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 1. Nội dung bài học
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
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        elevation = CardDefaults.cardElevation(1.dp)
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
                                                        textSize = 15f
                                                        setTextColor(AndroidColor.parseColor("#222222"))
                                                        setLineSpacing(8f, 1.3f)
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
                                            .padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.Article, contentDescription = null, modifier = Modifier.size(40.dp), tint = RedPrimary)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = lesson.description.ifEmpty { "Nội dung chi tiết đang được cập nhật từ hệ thống Web Quản Trị Vùng 4." },
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            lineHeight = 20.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(4.dp))

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
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        elevation = CardDefaults.cardElevation(1.dp)
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
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                val ext = if (file.fileName.contains(".")) file.fileName.substringAfterLast(".").lowercase() else "pdf"
                                                val icon = when {
                                                    ext.contains("pdf") -> Icons.Default.PictureAsPdf
                                                    ext.contains("doc") -> Icons.Default.Description
                                                    ext.contains("xls") -> Icons.Default.TableChart
                                                    ext.contains("ppt") -> Icons.Default.Slideshow
                                                    else -> Icons.Default.InsertDriveFile
                                                }
                                                Icon(imageVector = icon, contentDescription = null, tint = RedPrimary, modifier = Modifier.size(28.dp))
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(
                                                        text = file.title.ifEmpty { file.fileName.ifEmpty { "Tài liệu học tập" } },
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp,
                                                        maxLines = 1
                                                    )
                                                    Text(
                                                        text = ext.uppercase(),
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            Button(
                                                onClick = { viewingFile = file },
                                                colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Xem", fontSize = 13.sp)
                                            }
                                        }
                                    }
                                }

                                viewingFile?.let { f ->
                                    val ext = if (f.fileName.contains(".")) f.fileName.substringAfterLast(".").lowercase() else "pdf"
                                    InAppDocumentViewerDialog(
                                        fileTitle = f.title.ifEmpty { f.fileName.ifEmpty { "Tài liệu" } },
                                        fileUrl = f.downloadUrl,
                                        fileFormat = ext,
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
                                        Text("Chưa có tài liệu đính kèm nào cho bài học này.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(4.dp))

                            // 3. CÂU HỎI TRẮC NGHIỆM & TÍNH ĐIỂM KIỂM TRA
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Quiz, contentDescription = null, tint = RedPrimary)
                                Text(
                                    text = "Câu hỏi kiểm tra trắc nghiệm",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = RedPrimary
                                )
                            }

                            sampleQuestions.forEachIndexed { qIndex, (question, options, correctIndex) ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(1.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = question,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            lineHeight = 20.sp
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

                            // Nút nộp bài trắc nghiệm & tự động cập nhật về Web Quản trị
                            Button(
                                onClick = {
                                    val correctCount = sampleQuestions.indices.count { qIndex ->
                                        val correctIndex = sampleQuestions[qIndex].third
                                        selectedQuizAnswers[qIndex] == correctIndex
                                    }
                                    val percent = (correctCount * 100) / sampleQuestions.size

                                    quizSubmitted = true
                                    currentScore = correctCount
                                    currentTotalQuestions = sampleQuestions.size
                                    currentScorePercent = percent

                                    triggerAutoSave(
                                        scoreVal = correctCount,
                                        totalVal = sampleQuestions.size,
                                        forceComplete = (hasViewedAllSlides && hasReadContent)
                                    )

                                    if (hasViewedAllSlides && hasReadContent) {
                                        completionCelebrationDialog = true
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (quizSubmitted) Color(0xFF2E7D32) else RedPrimary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (quizSubmitted) "NỘP LẠI BÀI KIỂM TRA" else "NỘP BÀI KIỂM TRA TRẮC NGHIỆM",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            // Thẻ kết quả kiểm tra
                            if (quizSubmitted) {
                                val score = currentScore ?: sampleQuestions.indices.count { qIndex ->
                                    selectedQuizAnswers[qIndex] == sampleQuestions[qIndex].third
                                }
                                val percent = (score * 100) / sampleQuestions.size
                                val isPassed = percent >= 50

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isPassed) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(
                                                imageVector = if (isPassed) Icons.Default.EmojiEvents else Icons.Default.Info,
                                                contentDescription = null,
                                                tint = if (isPassed) GoldPrimary else RedPrimary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Text(
                                                text = "KẾT QUẢ: $score / ${sampleQuestions.size} CÂU ĐÚNG ($percent%)",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = if (isPassed) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                                            )
                                        }

                                        Text(
                                            text = if (isPassed) {
                                                "Đánh giá: ĐẠT YÊU CẦU BÀI HỌC CHÍNH TRỊ."
                                            } else {
                                                "Đánh giá: Chưa đạt yêu cầu. Đồng chí nên xem lại bài học để làm lại."
                                            },
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        if (isLoggedIn) {
                                            Text(
                                                text = "✓ Đã tự động cập nhật kết quả vào hồ sơ Web Quản trị",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF2E7D32)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // TAB 2: VIDEO
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
                                if (videoErrorMessage != null) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = videoErrorMessage ?: "Lỗi phát video",
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                            }
                                        }
                                    }
                                }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(240.dp),
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
                                                Text(vid.title.ifEmpty { "Video" }, fontSize = 13.sp, maxLines = 1)
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
                                            Text("Chưa có video bài giảng nào cho bài học này.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        // TAB 3: AUDIO
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
                                if (audioErrorMessage != null) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = audioErrorMessage ?: "Lỗi phát audio",
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                            }
                                        }
                                    }
                                }

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(2.dp)
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
                                            Text(text = formatMillis(audioCurrentPosition), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(text = formatMillis(audioDuration), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                            Text("Chưa có audio ghi âm nào cho bài học này.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

    // DIALOG CHÚC MỪNG HOÀN THÀNH 100% TIẾN ĐỘ & ĐIỂM SỐ
    if (completionCelebrationDialog) {
        val score = currentScore ?: 3
        val totalQ = currentTotalQuestions ?: 3
        val percent = currentScorePercent ?: 100

        AlertDialog(
            onDismissRequest = { completionCelebrationDialog = false },
            icon = {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(48.dp))
            },
            title = {
                Text(
                    text = "HOÀN THÀNH 100% BÀI HỌC",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF1B5E20),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Đồng chí đã hoàn thành toàn bộ bài học và kiểm tra:",
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                    Text("• Điểm trắc nghiệm: $score / $totalQ câu đúng ($percent%)", fontSize = 13.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)

                    HorizontalDivider()

                    Text(
                        text = "Tiến độ 100% đã được tự động lưu và cập nhật lên hệ thống Web Quản trị.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { completionCelebrationDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("Đóng", color = Color.White, fontWeight = FontWeight.Bold)
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
                        triggerAutoSave()
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
