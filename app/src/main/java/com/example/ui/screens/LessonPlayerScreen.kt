package com.example.ui.screens

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.util.Log
import android.widget.TextView
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import com.example.ui.components.TrongDongBackground
import com.example.ui.components.Vung4LogoBadge
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.RedPrimary
import com.example.viewmodel.AppViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("OPT_IN_IS_NOT_ENABLED", "UnstableApiUsage")
@OptIn(UnstableApi::class, ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LessonPlayerScreen(
    lesson: Lesson,
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    // Ẩn thanh điều hướng dưới đáy khi xem bài học để tránh bấm nhầm
    DisposableEffect(Unit) {
        viewModel.setViewingLesson(true)
        onDispose {
            viewModel.setViewingLesson(false)
        }
    }

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

    // Danh sách các slide đã xem đủ ít nhất 5 giây (ghi nhớ theo lesson.id, không bị reset khi lưu tiến độ)
    var viewedSlideIndices by remember(lesson.id) {
        mutableStateOf<Set<Int>>(
            if (existingProgress?.viewedSlides == true || existingProgress?.completed == true) {
                (0 until maxOf(1, lessonSlides.size)).toSet()
            } else {
                emptySet()
            }
        )
    }

    // Thời gian đã xem ở từng slide (map từ index slide -> số giây đã xem)
    var slideDwellMap by remember(lesson.id) {
        mutableStateOf<Map<Int, Int>>(emptyMap())
    }

    // Thời gian đã xem ở slide hiện tại đang hiển thị (đếm từ 0s -> 5s)
    var currentSlideDwellSeconds by remember { mutableIntStateOf(0) }

    // Tỷ lệ cuộn nội dung cao nhất học viên đã đạt được (0.0 -> 1.0, CHỈ TĂNG KHI LƯỚT XUỐNG TỪ TỪ, KHÔNG GIẢM KHI LƯỚT LÊN)
    var maxContentScrollRatio by remember(lesson.id) {
        mutableFloatStateOf(
            if (existingProgress?.readContent == true || existingProgress?.completed == true) 1f else 0f
        )
    }

    // Trạng thái cảnh báo khi lướt nội dung quá nhanh
    var isScrollingTooFast by remember { mutableStateOf(false) }
    var fastScrollWarningDismissTime by remember { mutableLongStateOf(0L) }
    var lastScrollPosition by remember { mutableIntStateOf(0) }
    var lastScrollTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // 3. Trả lời câu hỏi trắc nghiệm & nộp bài
    var quizSubmitted by remember(lesson.id) {
        mutableStateOf(
            existingProgress?.passedQuiz == true ||
            existingProgress?.completed == true ||
            (existingProgress?.score != null)
        )
    }

    var currentScore by remember(lesson.id) {
        mutableStateOf(existingProgress?.score)
    }
    var currentTotalQuestions by remember(lesson.id) {
        mutableStateOf(existingProgress?.totalQuestions ?: 3)
    }
    var currentScorePercent by remember(lesson.id) {
        mutableStateOf(existingProgress?.scorePercentage)
    }

    val allQuestions by viewModel.questions.collectAsState()

    // Lấy danh sách câu hỏi trắc nghiệm đã tạo từ Web Quản trị gắn với bài học / chuyên đề này
    val lessonQuestions = remember(allQuestions, lesson.id, lesson.courseId, lesson.category) {
        val matchedDirect = allQuestions.filter { q ->
            (q.lessonId.isNotBlank() && q.lessonId == lesson.id) ||
            (q.courseId.isNotBlank() && q.courseId == lesson.id)
        }
        if (matchedDirect.isNotEmpty()) {
            matchedDirect
        } else {
            // Lọc câu hỏi theo courseId của chuyên đề (nếu câu hỏi đó gán cho courseId)
            allQuestions.filter { q ->
                (q.courseId.isNotBlank() && q.courseId == lesson.courseId && q.lessonId.isBlank())
            }
        }
    }

    // Seed để chọn ngẫu nhiên 1 câu hỏi từ bộ đề thi và xáo trộn các đáp án ngẫu nhiên
    var randomQuestionSeed by remember(lesson.id) { mutableLongStateOf(System.currentTimeMillis() + (1..10000).random()) }

    // Câu hỏi ngẫu nhiên hiện tại được chọn từ bộ đề với các đáp án được đảo ngẫu nhiên
    val currentRandomQuestion = remember(lessonQuestions, randomQuestionSeed) {
        if (lessonQuestions.isNotEmpty()) {
            val randomIndex = (Math.abs(randomQuestionSeed) % lessonQuestions.size).toInt()
            val picked = lessonQuestions[randomIndex]
            picked.withShuffledOptions(randomQuestionSeed + 777L)
        } else null
    }

    // Đáp án người dùng chọn cho câu hỏi ngẫu nhiên hiện tại (null nếu chưa chọn)
    var selectedSingleOptionIndex by remember { mutableStateOf<Int?>(null) }

    // Đánh dấu đã nộp bài ở lần kiểm tra hiện tại (chỉ được trả lời 1 lần)
    var isCurrentAttemptSubmitted by remember { mutableStateOf(false) }

    // Kết quả lần trả lời: true = Đúng (100%), false = Sai (0%), null = Chưa trả lời
    var lastAttemptResult by remember { mutableStateOf<Boolean?>(null) }

    // Hiển thị Bảng thông báo kết quả kiểm tra
    var showQuizResultNoticeDialog by remember { mutableStateOf(false) }

    // Trạng thái mở Giao diện Trả lời câu hỏi (Overlay toàn màn hình)
    var isAnsweringQuizOverlayOpen by remember { mutableStateOf(false) }

    // 1. Tỷ lệ Slide & Nội dung (Dùng cho thông tin bổ trợ, không tính vào tiến độ hoàn thành)
    val hasViewedAllSlides = lessonSlides.isEmpty() || viewedSlideIndices.size >= lessonSlides.size
    val hasReadContent = maxContentScrollRatio >= 0.95f

    // 2. TIẾN ĐỘ HOÀN THÀNH BÀI HỌC:
    // Mỗi lần làm bài chọn 1 câu hỏi ngẫu nhiên. Trả lời ĐÚNG đạt 100% tiến độ bài học, trả lời SAI đạt 0%.
    val progressPercentage = remember(lastAttemptResult, existingProgress, lessonQuestions.size) {
        if (lessonQuestions.isEmpty()) {
            100
        } else if (lastAttemptResult == true) {
            100
        } else if (lastAttemptResult == false) {
            0
        } else if (existingProgress?.scorePercentage != null) {
            existingProgress.scorePercentage.coerceIn(0, 100)
        } else if (existingProgress?.completed == true) {
            100
        } else {
            0
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
    var downloadingFileIds by remember { mutableStateOf(setOf<String>()) }
    var cachedFileIds by remember { mutableStateOf(setOf<String>()) }
    var savedToDeviceFileIds by remember { mutableStateOf(setOf<String>()) }

    // Xử lý nút Back trên thanh điều hướng: đóng tài liệu nếu đang mở, nếu không thì quay lại danh sách bài học
    BackHandler {
        if (viewingFile != null) {
            viewingFile = null
        } else {
            onBack()
        }
    }

    LaunchedEffect(lesson.id) {
        viewModel.recordLessonViewed(lesson.id)
    }

    LaunchedEffect(lessonFiles) {
        withContext(Dispatchers.IO) {
            val downloadedKeys = com.example.ui.components.getDownloadedFileKeys(context)
            val saved = lessonFiles.filter { f ->
                downloadedKeys.contains(f.id) ||
                downloadedKeys.contains(f.downloadUrl) ||
                downloadedKeys.contains(f.fileName) ||
                downloadedKeys.contains(f.title) ||
                com.example.ui.components.isDocumentSavedToDevice(context, f.id, f.downloadUrl, f.fileName, f.title)
            }.map { it.id }.toSet()
            savedToDeviceFileIds = saved

            val cached = lessonFiles.filter { f ->
                saved.contains(f.id) ||
                com.example.ui.components.isDocumentCachedInApp(context, f.downloadUrl, f.fileName, "") ||
                com.example.ui.components.isDocumentCachedInApp(context, f.downloadUrl, f.title, "")
            }.map { it.id }.toSet()
            cachedFileIds = cached
        }
    }

    // Tự động lưu và đồng bộ tiến độ về Web Quản trị trong nền
    fun triggerAutoSave(
        scoreVal: Int? = currentScore,
        totalVal: Int? = currentTotalQuestions,
        forceComplete: Boolean = isFullyCompleted
    ) {
        val totalQ = totalVal ?: lessonQuestions.size
        val score = scoreVal ?: (if (quizSubmitted) (currentScore ?: totalQ) else null)

        viewModel.updateLessonProgress(
            lessonId = lesson.id,
            completed = forceComplete,
            score = score,
            totalQuestions = totalQ,
            viewedSlides = hasViewedAllSlides,
            readContent = hasReadContent,
            passedQuiz = if (lessonQuestions.isEmpty()) true else quizSubmitted,
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

    TrongDongBackground(
        watermarkAlpha = 0.08f,
        showCornerBorders = false,
        showTopBottomBorders = false
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    windowInsets = WindowInsets(0, 0, 0, 0),
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 8.dp)
                        ) {
                            Vung4LogoBadge(size = 32.dp)
                            Text(
                                text = lesson.title.ifEmpty { "Chi tiết bài học" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = Color.White
                            )
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
            ) {

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
                    .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 4.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        // TAB 0: SLIDES
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (lessonSlides.isNotEmpty()) {
                                val pagerState = rememberPagerState(pageCount = { lessonSlides.size })

                                // Bộ đếm thời gian từng slide: Học viên phải xem mỗi slide ít nhất 5 giây mới được tính là đã xem
                                LaunchedEffect(pagerState.currentPage, selectedTab, lessonSlides.size) {
                                    if (selectedTab == 0 && lessonSlides.isNotEmpty()) {
                                        val page = pagerState.currentPage
                                        if (viewedSlideIndices.contains(page)) {
                                            currentSlideDwellSeconds = 5
                                        } else {
                                            var dwell = slideDwellMap[page] ?: 0
                                            currentSlideDwellSeconds = dwell
                                            while (dwell < 5 && pagerState.currentPage == page && selectedTab == 0) {
                                                kotlinx.coroutines.delay(1000L)
                                                dwell++
                                                currentSlideDwellSeconds = dwell
                                                slideDwellMap = slideDwellMap + (page to dwell)
                                                if (dwell >= 5) {
                                                    viewedSlideIndices = viewedSlideIndices + page
                                                    break
                                                }
                                            }
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
                                            text = "Chưa có nội dung slide bài giảng",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
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

                        // Tự động tắt cảnh báo lướt nhanh sau thời gian chờ
                        LaunchedEffect(fastScrollWarningDismissTime) {
                            if (fastScrollWarningDismissTime > 0) {
                                val remaining = fastScrollWarningDismissTime - System.currentTimeMillis()
                                if (remaining > 0) {
                                    kotlinx.coroutines.delay(remaining)
                                }
                                isScrollingTooFast = false
                            }
                        }

                        // Xử lý bài học có nội dung ngắn vừa vặn 1 màn hình (không cuộn được)
                        LaunchedEffect(selectedTab, lessonContents.size) {
                            if (selectedTab == 1) {
                                kotlinx.coroutines.delay(5000L)
                                if (contentScrollState.maxValue <= 0) {
                                    maxContentScrollRatio = 1f
                                }
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
                                        val descText = lesson.description.ifBlank { "Chưa có nội dung" }
                                        Text(
                                            text = descText,
                                            fontSize = 14.sp,
                                            fontWeight = if (descText == "Chưa có nội dung") FontWeight.Medium else FontWeight.Normal,
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
                                    val isCached = cachedFileIds.contains(file.id) || com.example.ui.components.isDocumentCachedInApp(context, file.downloadUrl, file.fileName, "") || com.example.ui.components.isDocumentCachedInApp(context, file.downloadUrl, file.title, "")
                                    val isSavedToDevice = savedToDeviceFileIds.contains(file.id) || com.example.ui.components.isDocumentSavedToDevice(context, file.id, file.downloadUrl, file.fileName, file.title)
                                    val isDownloading = downloadingFileIds.contains(file.id)

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        elevation = CardDefaults.cardElevation(1.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
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
                                                    Icon(imageVector = icon, contentDescription = null, tint = RedPrimary, modifier = Modifier.size(32.dp))
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column {
                                                        Text(
                                                            text = file.title.ifEmpty { file.fileName.ifEmpty { "Tài liệu học tập" } },
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 14.sp,
                                                            maxLines = 2,
                                                            lineHeight = 19.sp
                                                        )
                                                        Spacer(modifier = Modifier.height(3.dp))
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Surface(
                                                                shape = RoundedCornerShape(4.dp),
                                                                color = MaterialTheme.colorScheme.surfaceVariant
                                                            ) {
                                                                Text(
                                                                    text = ext.uppercase(),
                                                                    fontSize = 10.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                )
                                                            }
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            if (isSavedToDevice) {
                                                                Surface(
                                                                    shape = RoundedCornerShape(4.dp),
                                                                    color = Color(0xFFE8F5E9)
                                                                ) {
                                                                    Row(
                                                                        verticalAlignment = Alignment.CenterVertically,
                                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                    ) {
                                                                        Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(12.dp))
                                                                        Spacer(modifier = Modifier.width(4.dp))
                                                                        Text(
                                                                            text = "Đã tải về máy (Thư mục Downloads)",
                                                                            fontSize = 10.sp,
                                                                            fontWeight = FontWeight.Bold,
                                                                            color = Color(0xFF2E7D32)
                                                                        )
                                                                    }
                                                                }
                                                            } else if (isCached) {
                                                                Surface(
                                                                    shape = RoundedCornerShape(4.dp),
                                                                    color = Color(0xFFE8F5E9)
                                                                ) {
                                                                    Row(
                                                                        verticalAlignment = Alignment.CenterVertically,
                                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                    ) {
                                                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(12.dp))
                                                                        Spacer(modifier = Modifier.width(4.dp))
                                                                        Text(
                                                                            text = "Đã lưu đệm (Xem ngoại tuyến)",
                                                                            fontSize = 10.sp,
                                                                            fontWeight = FontWeight.Bold,
                                                                            color = Color(0xFF2E7D32)
                                                                        )
                                                                    }
                                                                }
                                                            } else {
                                                                Text(
                                                                    text = "Mở lần đầu để tự động lưu đệm",
                                                                    fontSize = 11.sp,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))

                                            // Action Buttons Row (Xem ngay / Tải về)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Nút Xem ngay
                                                Button(
                                                    onClick = {
                                                        viewingFile = file
                                                        cachedFileIds = cachedFileIds + file.id
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Xem ngay", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                }

                                                // Nút Tải về máy / Đã tải (Hiển thị dạng thư mục mở vị trí tệp)
                                                if (isSavedToDevice) {
                                                    OutlinedButton(
                                                        onClick = {
                                                            com.example.ui.components.openDownloadsFolder(context, file.fileName)
                                                        },
                                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E7D32)),
                                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2E7D32)),
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF2E7D32))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Đã tải về máy", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                                    }
                                                } else {
                                                    OutlinedButton(
                                                        onClick = {
                                                            if (isDownloading) return@OutlinedButton
                                                            val stdName = com.example.ui.components.getStandardFileName(file.title.ifBlank { file.fileName }, "", file.downloadUrl)
                                                            downloadingFileIds = downloadingFileIds + file.id
                                                            coroutineScope.launch {
                                                                val (downloadedFile, err) = com.example.ui.components.downloadFileToAppStorage(context, file.downloadUrl, stdName)
                                                                downloadingFileIds = downloadingFileIds - file.id
                                                                if (downloadedFile != null && downloadedFile.exists()) {
                                                                    cachedFileIds = cachedFileIds + file.id
                                                                    com.example.ui.components.saveToDeviceDownloads(context, downloadedFile, stdName)
                                                                    com.example.ui.components.markFileAsDownloaded(
                                                                        context = context,
                                                                        fileId = file.id,
                                                                        fileUrl = file.downloadUrl,
                                                                        fileName = stdName,
                                                                        lessonId = lesson.id,
                                                                        lessonTitle = lesson.title,
                                                                        fileTitle = file.title.ifBlank { file.fileName },
                                                                        localPath = downloadedFile.absolutePath,
                                                                        fileSize = downloadedFile.length()
                                                                    )
                                                                    savedToDeviceFileIds = savedToDeviceFileIds + file.id
                                                                    android.widget.Toast.makeText(
                                                                        context,
                                                                        "Đã tải & lưu tài liệu vào thư mục Downloads trên máy! Bấm vào để mở vị trí tệp.",
                                                                        android.widget.Toast.LENGTH_LONG
                                                                    ).show()
                                                                } else {
                                                                    // Fallback nếu tải qua AppStorage thất bại
                                                                    val okSystem = com.example.ui.components.downloadFileViaSystemManager(context, file.downloadUrl, file.title, stdName)
                                                                    if (okSystem) {
                                                                        com.example.ui.components.markFileAsDownloaded(
                                                                            context = context,
                                                                            fileId = file.id,
                                                                            fileUrl = file.downloadUrl,
                                                                            fileName = stdName,
                                                                            lessonId = lesson.id,
                                                                            lessonTitle = lesson.title,
                                                                            fileTitle = file.title.ifBlank { file.fileName }
                                                                        )
                                                                        savedToDeviceFileIds = savedToDeviceFileIds + file.id
                                                                    } else {
                                                                        android.widget.Toast.makeText(
                                                                            context,
                                                                            "Lỗi tải tài liệu: ${err ?: "Không thể kết nối"}",
                                                                            android.widget.Toast.LENGTH_SHORT
                                                                        ).show()
                                                                    }
                                                                }
                                                            }
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                                        shape = RoundedCornerShape(8.dp),
                                                        enabled = !isDownloading
                                                    ) {
                                                        if (isDownloading) {
                                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = RedPrimary)
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text("Đang tải...", fontSize = 13.sp)
                                                        } else {
                                                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text("Tải về máy", fontSize = 13.sp)
                                                        }
                                                    }
                                                }
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
                                        onDismiss = {
                                            viewingFile = null
                                            if (com.example.ui.components.isDocumentSavedToDevice(context, f.id, f.downloadUrl, f.fileName, f.title)) {
                                                savedToDeviceFileIds = savedToDeviceFileIds + f.id
                                            }
                                            cachedFileIds = cachedFileIds + f.id
                                        }
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
                                        Text("Chưa có nội dung tài liệu đính kèm nào cho bài học này.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(4.dp))

                            // 3. MỤC KIỂM TRA ĐÁNH GIÁ CUỐI BÀI
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Quiz, contentDescription = null, tint = RedPrimary)
                                Text(
                                    text = "Mục kiểm tra đánh giá cuối bài",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = RedPrimary
                                )
                            }

                            if (lessonQuestions.isNotEmpty()) {
                                val isAnsweredCorrectly = lastAttemptResult == true || (lastAttemptResult == null && (existingProgress?.completed == true || (existingProgress?.scorePercentage ?: 0) >= 100))

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (isAnsweredCorrectly) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFFE8F5E9),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E7D32)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = Color(0xFF2E7D32)
                                                )
                                                Text(
                                                    text = "✓ ĐÃ TRẢ LỜI ĐÚNG (Tiến độ 100% - Đã hoàn thành)",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF1B5E20)
                                                )
                                            }
                                        }
                                    } else if (lastAttemptResult == false) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFFFFEBEE),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, RedPrimary),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Cancel,
                                                    contentDescription = null,
                                                    tint = RedPrimary
                                                )
                                                Text(
                                                    text = "❌ TRẢ LỜI CHƯA ĐÚNG - Vui lòng nhấn kiểm tra lại",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFB71C1C)
                                                )
                                            }
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            if (isAnsweredCorrectly) {
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "Đồng chí đã hoàn thành bài kiểm tra đánh giá này!",
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                selectedSingleOptionIndex = null
                                                randomQuestionSeed++
                                                isAnsweringQuizOverlayOpen = true
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isAnsweredCorrectly) Color(0xFF2E7D32) else RedPrimary
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isAnsweredCorrectly) Icons.Default.CheckCircle else Icons.Default.Quiz,
                                            contentDescription = null
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isAnsweredCorrectly) "ĐÃ HOÀN THÀNH" else "VÀO KIỂM TRA ĐÁNH GIÁ CUỐI BÀI",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            } else {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                Icons.Default.Quiz,
                                                contentDescription = null,
                                                modifier = Modifier.size(36.dp),
                                                tint = RedPrimary
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Chưa có nội dung kiểm tra đánh giá",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
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
                                            Text("Chưa có nội dung video bài giảng.", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                            Text("Chưa có nội dung audio ghi âm.", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

    // GIAO DIỆN MÀN HÌNH LÀM BÀI TRẮC NGHIỆM ĐỘC LẬP
    // Khi mở giao diện này, màn hình sẽ phủ toàn bộ, học viên không thể lướt lên trên để xem nội dung bài học phía trên
    if (isAnsweringQuizOverlayOpen && lessonQuestions.isNotEmpty()) {
        Dialog(
            onDismissRequest = { isAnsweringQuizOverlayOpen = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Thanh Tiêu đề của Giao diện Kiểm tra Trắc nghiệm
                    Surface(
                        color = RedPrimary,
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { isAnsweringQuizOverlayOpen = false },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Đóng",
                                    tint = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "BÀI KIỂM TRA ĐÁNH GIÁ CUỐI BÀI",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "01 câu hỏi ngẫu nhiên từ bộ đề (${lessonQuestions.size} câu)",
                                    fontSize = 11.sp,
                                    color = GoldPrimary,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Badge trạng thái dạng viên thuốc ngang, cố định không bao giờ bị co lại thành dải dọc trên màn hình hẹp
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = GoldPrimary
                            ) {
                                Text(
                                    text = "1 Lần trả lời",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    // Khung Cảnh báo Không xem được nội dung bài học
                    Surface(
                        color = Color(0xFFFFF3E0),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(18.dp))
                            Text(
                                text = "Màn hình đã được khóa ở chế độ làm bài kiểm tra: Đồng chí không thể xem lại nội dung bài học phía trên.",
                                fontSize = 11.sp,
                                color = Color(0xFFE65100),
                                lineHeight = 15.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Nội dung câu hỏi trắc nghiệm ngẫu nhiên
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (currentRandomQuestion != null) {
                            val q = currentRandomQuestion
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "CÂU HỎI KIỂM TRA NGẪU NHIÊN",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = RedPrimary
                                        )
                                        if (selectedSingleOptionIndex != null) {
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = Color(0xFFE8F5E9)
                                            ) {
                                                Text(
                                                    text = "✓ Đã chọn đáp án",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF2E7D32),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = q.question,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 22.sp
                                    )

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                    q.options.forEachIndexed { optIndex, option ->
                                        val isSelected = selectedSingleOptionIndex == optIndex
                                        val cardColor = if (isSelected) RedPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
                                        val textColor = if (isSelected) RedPrimary else MaterialTheme.colorScheme.onSurface

                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedSingleOptionIndex = optIndex
                                                },
                                            shape = RoundedCornerShape(10.dp),
                                            color = cardColor,
                                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, RedPrimary) else null
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(
                                                    selected = isSelected,
                                                    onClick = {
                                                        selectedSingleOptionIndex = optIndex
                                                    }
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = option,
                                                    fontSize = 14.sp,
                                                    color = textColor,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    lineHeight = 19.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Nút nộp bài kiểm tra: có Surface và navigationBarsPadding để hiển thị chuẩn xác trên tất cả các dòng máy, không bị phím ảo/thanh điều hướng che mất
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 8.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .navigationBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (currentRandomQuestion != null && selectedSingleOptionIndex != null) {
                                        val isCorrect = selectedSingleOptionIndex == currentRandomQuestion.correctIndex
                                        isCurrentAttemptSubmitted = true
                                        lastAttemptResult = isCorrect
                                        quizSubmitted = true
                                        currentScore = if (isCorrect) 1 else 0
                                        currentTotalQuestions = 1
                                        currentScorePercent = if (isCorrect) 100 else 0

                                        triggerAutoSave(
                                            scoreVal = if (isCorrect) 1 else 0,
                                            totalVal = 1,
                                            forceComplete = isCorrect
                                        )

                                        isAnsweringQuizOverlayOpen = false
                                        showQuizResultNoticeDialog = true
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedSingleOptionIndex != null) Color(0xFF2E7D32) else RedPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                enabled = selectedSingleOptionIndex != null
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "NỘP BÀI KIỂM TRA ĐÁNH GIÁ",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // BẢNG THÔNG BÁO KẾT QUẢ KIỂM TRA ĐÁNH GIÁ
    if (showQuizResultNoticeDialog) {
        AlertDialog(
            onDismissRequest = { showQuizResultNoticeDialog = false },
            icon = {
                Icon(
                    imageVector = if (lastAttemptResult == true) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = if (lastAttemptResult == true) Color(0xFF2E7D32) else RedPrimary,
                    modifier = Modifier.size(52.dp)
                )
            },
            title = {
                Text(
                    text = if (lastAttemptResult == true) "KẾT QUẢ: TRẢ LỜI ĐÚNG!" else "KẾT QUẢ: TRẢ LỜI SAI",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (lastAttemptResult == true) Color(0xFF2E7D32) else RedPrimary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (lastAttemptResult == true) {
                            "Chúc mừng đồng chí! Đáp án hoàn toàn chính xác.\n\nTiến độ bài học đã đạt 100% (ĐÃ HOÀN THÀNH)."
                        } else {
                            "Đáp án đồng chí lựa chọn chưa chính xác.\n\nĐồng chí có thể nhấn vào nút 'VÀO KIỂM TRA ĐÁNH GIÁ CUỐI BÀI' để thực hiện lại câu hỏi."
                        },
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showQuizResultNoticeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (lastAttemptResult == true) Color(0xFF2E7D32) else RedPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (lastAttemptResult == true) "ĐÃ HOÀN THÀNH" else "ĐÃ HIỂU",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
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
}

private fun formatMillis(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
