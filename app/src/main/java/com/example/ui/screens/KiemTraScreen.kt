package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.ExamResultDoc
import com.example.model.ExamSessionDoc
import com.example.model.QuestionItem
import com.example.ui.components.Vung4LogoBadge
import com.example.ui.theme.RedPrimary
import com.example.ui.theme.NavySecondary
import com.example.viewmodel.AppViewModel
import kotlinx.coroutines.delay
import java.util.Locale

enum class ExamMode {
    OVERVIEW,       // Màn hình chọn chế độ
    TAKING_EXAM,    // Đang làm bài thi 20 câu
    EXAM_RESULT,    // Xem kết quả bài thi vừa làm
    QUESTION_BANK   // Tổng hợp tất cả câu hỏi đã đăng tải
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KiemTraScreen(
    viewModel: AppViewModel,
    onBack: (() -> Unit)? = null,
    onExamTakingStateChange: ((Boolean) -> Unit)? = null
) {
    val allQuestions by viewModel.questions.collectAsState()
    val examSessions by viewModel.examSessions.collectAsState()
    val userExamResults by viewModel.userExamResults.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val userDoc by viewModel.userDoc.collectAsState()
    val authActionLoading by viewModel.authActionLoading.collectAsState()

    val isAuthenticated = currentUser != null || userDoc != null
    val userName = userDoc?.name?.ifEmpty { currentUser?.displayName } ?: currentUser?.email ?: "Cán bộ / Học viên"

    var currentMode by remember { mutableStateOf(ExamMode.OVERVIEW) }
    
    // Thông báo trạng thái làm bài thi cho MainScreen để ẩn BottomBar
    LaunchedEffect(currentMode) {
        onExamTakingStateChange?.invoke(currentMode == ExamMode.TAKING_EXAM)
    }
    
    // Auth Dialog State
    var showLoginDialog by remember { mutableStateOf(false) }
    var showLoginRequiredDialog by remember { mutableStateOf(false) }

    // Active Exam State
    var activeExamId by remember { mutableStateOf("") }
    var activeExamName by remember { mutableStateOf("Đề thi 20 câu ngẫu nhiên") }
    var isOfficialWebExam by remember { mutableStateOf(false) }
    var examQuestions by remember { mutableStateOf<List<QuestionItem>>(emptyList()) }
    var userAnswers by remember { mutableStateOf<MutableMap<Int, Int>>(mutableMapOf()) }
    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    var examTimerSeconds by remember { mutableIntStateOf(20 * 60) } // 20 phút
    var isTimerRunning by remember { mutableStateOf(false) }
    var showSubmitConfirmDialog by remember { mutableStateOf(false) }
    var examTimeSpentSeconds by remember { mutableIntStateOf(0) }

    // Feedback dialog state
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var feedbackText by remember { mutableStateOf("") }
    var feedbackSentMsg by remember { mutableStateOf<String?>(null) }
    
    // Timer effect
    LaunchedEffect(currentMode, isTimerRunning) {
        if (currentMode == ExamMode.TAKING_EXAM && isTimerRunning) {
            while (examTimerSeconds > 0 && currentMode == ExamMode.TAKING_EXAM) {
                delay(1000L)
                examTimerSeconds--
                examTimeSpentSeconds++
            }
            if (examTimerSeconds <= 0 && currentMode == ExamMode.TAKING_EXAM) {
                // Tự động nộp bài khi hết giờ
                currentMode = ExamMode.EXAM_RESULT
                isTimerRunning = false
                val score = examQuestions.indices.count { idx -> userAnswers[idx] == examQuestions[idx].correctIndex }
                viewModel.submitExamResult(
                    score = score,
                    totalQuestions = examQuestions.size,
                    timeSpentSeconds = examTimeSpentSeconds,
                    examId = activeExamId,
                    examName = "$activeExamName (Tự động nộp khi hết giờ)"
                )
            }
        }
    }

    // Khởi tạo bài thi từ đợt thi Web Quản trị hoặc đề thi ngẫu nhiên
    fun startExamForSession(session: ExamSessionDoc? = null) {
        if (!isAuthenticated) {
            showLoginRequiredDialog = true
            return
        }

        if (session != null) {
            isOfficialWebExam = true
            activeExamId = session.id
            activeExamName = session.title
            
            // 1. Ưu tiên lấy trực tiếp danh sách câu hỏi nhúng bên trong đợt thi từ Web Quản trị
            val sessionQuestions = if (session.questionsList.isNotEmpty()) {
                session.questionsList
            } else if (session.questionIds.isNotEmpty()) {
                val qSet = session.questionIds.toSet()
                allQuestions.filter { it.id in qSet }
            } else if (session.category.isNotBlank()) {
                // Chỉ lấy câu hỏi kiểm tra chung, tuyệt đối không lấy nhầm câu hỏi ôn tập cuối bài học GDCT (lessonId != "")
                allQuestions.filter { it.category.equals(session.category, ignoreCase = true) && it.lessonId.isBlank() }
            } else {
                // Chỉ lấy câu hỏi kiểm tra ngân hàng chung
                allQuestions.filter { it.lessonId.isBlank() }
            }

            val targetCount = if (session.totalQuestions > 0) session.totalQuestions else if (sessionQuestions.isNotEmpty()) sessionQuestions.size else 20

            examQuestions = if (sessionQuestions.isNotEmpty()) {
                if (sessionQuestions.size >= targetCount) {
                    sessionQuestions.take(targetCount)
                } else {
                    // Hiển thị chính xác toàn bộ danh sách câu hỏi của đợt thi mà không tự ý lấy nhầm câu hỏi GDCT bài học
                    sessionQuestions
                }
            } else {
                val nonLessonQuestions = allQuestions.filter { it.lessonId.isBlank() }
                if (nonLessonQuestions.isNotEmpty()) {
                    nonLessonQuestions.shuffled().take(minOf(targetCount, nonLessonQuestions.size))
                } else {
                    allQuestions.take(minOf(targetCount, allQuestions.size))
                }
            }

            examTimerSeconds = if (session.durationMinutes > 0) session.durationMinutes * 60 else 20 * 60
        } else {
            isOfficialWebExam = false
            activeExamId = "random_practice_${System.currentTimeMillis()}"
            activeExamName = "Đề thi 20 câu ngẫu nhiên"
            val nonLessonQuestions = allQuestions.filter { it.lessonId.isBlank() }
            val pool = if (nonLessonQuestions.isNotEmpty()) nonLessonQuestions else allQuestions
            val totalToPick = minOf(20, pool.size)
            examQuestions = pool.shuffled().take(totalToPick)
            examTimerSeconds = 20 * 60
        }

        userAnswers = mutableMapOf()
        currentQuestionIndex = 0
        examTimeSpentSeconds = 0
        isTimerRunning = true
        currentMode = ExamMode.TAKING_EXAM
    }

    // Xử lý nút Back trên thanh điều hướng khi đang xem Ngân hàng câu hỏi hoặc Kết quả thi -> quay về Tổng quan
    BackHandler(enabled = currentMode == ExamMode.QUESTION_BANK || currentMode == ExamMode.EXAM_RESULT) {
        currentMode = ExamMode.OVERVIEW
    }

    // GIAO DIỆN LÀM BÀI THI TOÀN MÀN HÌNH KHÔNG THỂ BẤM NHẦM TÙY CHỌN KHÁC
    if (currentMode == ExamMode.TAKING_EXAM) {
        Dialog(
            onDismissRequest = {
                showSubmitConfirmDialog = true
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                decorFitsSystemWindows = false
            )
        ) {
            BackHandler {
                showSubmitConfirmDialog = true
            }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                com.example.ui.components.TrongDongBackground {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                    ) {
                        TopAppBar(
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Vung4LogoBadge(size = 32.dp)
                                    Column {
                                        Text(
                                            text = activeExamName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "Vùng 4 Hải quân - Hệ thống kiểm tra trực tuyến",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                        )
                                    }
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = { showSubmitConfirmDialog = true }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Thoát / Nộp bài",
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            },
                            actions = {
                                Button(
                                    onClick = { showSubmitConfirmDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Text(
                                        text = "NỘP BÀI",
                                        fontWeight = FontWeight.Bold,
                                        color = RedPrimary,
                                        fontSize = 12.sp
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = RedPrimary,
                                titleContentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )

                        ExamTakingView(
                            examQuestions = examQuestions,
                            currentIndex = currentQuestionIndex,
                            userAnswers = userAnswers,
                            remainingSeconds = examTimerSeconds,
                            onSelectAnswer = { qIndex, answerIndex ->
                                val updated = HashMap(userAnswers)
                                updated[qIndex] = answerIndex
                                userAnswers = updated
                            },
                            onJumpToQuestion = { currentQuestionIndex = it },
                            onNext = {
                                if (currentQuestionIndex < examQuestions.size - 1) {
                                    currentQuestionIndex++
                                }
                            },
                            onPrev = {
                                if (currentQuestionIndex > 0) {
                                    currentQuestionIndex--
                                }
                            },
                            onSubmit = {
                                showSubmitConfirmDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

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
                        Column {
                            Text(
                                text = when (currentMode) {
                                    ExamMode.OVERVIEW -> "KIỂM TRA TRẮC NGHIỆM"
                                    ExamMode.TAKING_EXAM -> "BÀI THI TRẮC NGHIỆM"
                                    ExamMode.EXAM_RESULT -> "KẾT QUẢ KIỂM TRA"
                                    ExamMode.QUESTION_BANK -> "TỔNG HỢP CÂU HỎI ĐÃ ĐĂNG"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Vùng 4 Hải quân - Hệ thống kiểm tra trực tuyến",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (currentMode != ExamMode.OVERVIEW || onBack != null) {
                        IconButton(onClick = {
                            when (currentMode) {
                                ExamMode.TAKING_EXAM -> {
                                    showSubmitConfirmDialog = true
                                }
                                ExamMode.EXAM_RESULT, ExamMode.QUESTION_BANK -> {
                                    currentMode = ExamMode.OVERVIEW
                                }
                                ExamMode.OVERVIEW -> {
                                    onBack?.invoke()
                                }
                            }
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Quay lại",
                                tint = MaterialTheme.colorScheme.onPrimary
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (currentMode) {
                ExamMode.OVERVIEW -> {
                    ExamOverviewView(
                        totalQuestionsCount = allQuestions.size,
                        examSessions = examSessions,
                        isAuthenticated = isAuthenticated,
                        onOpenLogin = { showLoginRequiredDialog = true },
                        onStartSessionExam = { session -> startExamForSession(session) },
                        onStartExam = { startExamForSession(null) },
                        onOpenQuestionBank = { currentMode = ExamMode.QUESTION_BANK }
                    )
                }
                ExamMode.TAKING_EXAM -> {
                    // Trạng thái đang hiển thị qua FullScreen Dialog ở trên
                    Box(modifier = Modifier.fillMaxSize())
                }
                ExamMode.EXAM_RESULT -> {
                    ExamResultView(
                        examQuestions = examQuestions,
                        userAnswers = userAnswers,
                        timeSpentSeconds = examTimeSpentSeconds,
                        examName = activeExamName,
                        isOfficialWebExam = isOfficialWebExam,
                        onOpenFeedback = { showFeedbackDialog = true },
                        onRetakeNewExam = { startExamForSession(null) },
                        onBackToBank = { currentMode = ExamMode.QUESTION_BANK },
                        onBackToOverview = { currentMode = ExamMode.OVERVIEW }
                    )
                }
                ExamMode.QUESTION_BANK -> {
                    QuestionBankView(
                        allQuestions = allQuestions,
                        onStartExam = { startExamForSession(null) }
                    )
                }
            }
        }
    }

    // Submit confirmation dialog
    if (showSubmitConfirmDialog) {
        val answeredCount = userAnswers.size
        val totalCount = examQuestions.size
        val unAnsweredCount = totalCount - answeredCount

        AlertDialog(
            onDismissRequest = { showSubmitConfirmDialog = false },
            icon = {
                Icon(
                    Icons.Default.HelpOutline,
                    contentDescription = null,
                    tint = RedPrimary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Xác nhận nộp bài thi",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text("Tên bài thi: $activeExamName", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = RedPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Đã trả lời: $answeredCount / $totalCount câu")
                    if (unAnsweredCount > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "⚠️ Bạn còn $unAnsweredCount câu chưa chọn đáp án!",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Bài làm và điểm số sẽ được tự động đồng bộ và lưu trên trang Web Quản trị.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSubmitConfirmDialog = false
                        isTimerRunning = false
                        currentMode = ExamMode.EXAM_RESULT
                        val score = examQuestions.indices.count { idx -> userAnswers[idx] == examQuestions[idx].correctIndex }
                        viewModel.submitExamResult(
                            score = score,
                            totalQuestions = examQuestions.size,
                            timeSpentSeconds = examTimeSpentSeconds,
                            examId = activeExamId,
                            examName = activeExamName
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                ) {
                    Text("Nộp bài thi", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showSubmitConfirmDialog = false }) {
                    Text("Làm tiếp")
                }
            }
        )
    }

    // Feedback dialog
    if (showFeedbackDialog) {
        AlertDialog(
            onDismissRequest = { 
                showFeedbackDialog = false
                feedbackSentMsg = null 
            },
            icon = {
                Icon(
                    Icons.Default.Feedback,
                    contentDescription = null,
                    tint = RedPrimary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Phản ánh nội dung sai sót",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Bài thi: $activeExamName",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = RedPrimary
                    )
                    Text(
                        text = "Nếu phát hiện sai sót trong đề thi hoặc câu hỏi, vui lòng ghi rõ phản ánh bên dưới để gửi về Ban Quản trị Web:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = feedbackText,
                        onValueChange = { feedbackText = it },
                        placeholder = { Text("Nhập nội dung phản ánh...", fontSize = 13.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        maxLines = 4
                    )
                    if (feedbackSentMsg != null) {
                        Text(
                            text = feedbackSentMsg!!,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (feedbackText.isNotBlank()) {
                            viewModel.sendExamFeedback(
                                examId = activeExamId,
                                examName = activeExamName,
                                feedbackContent = feedbackText,
                                onSuccess = {
                                    feedbackSentMsg = "Đã gửi phản ánh thành công về Web Quản trị!"
                                    feedbackText = ""
                                },
                                onError = { err ->
                                    feedbackSentMsg = "Lỗi gửi: $err"
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                ) {
                    Text("Gửi phản ánh", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { 
                    showFeedbackDialog = false
                    feedbackSentMsg = null 
                }) {
                    Text("Đóng")
                }
            }
        )
    }

    // DIALOG THÔNG BÁO YÊU CẦU ĐĂNG NHẬP
    if (showLoginRequiredDialog) {
        AlertDialog(
            onDismissRequest = { showLoginRequiredDialog = false },
            icon = {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = RedPrimary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "YÊU CẦU ĐĂNG NHẬP",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = RedPrimary
                )
            },
            text = {
                Text(
                    text = "Chỉ cán bộ, chiến sĩ và học viên đã đăng nhập tài khoản vào ứng dụng mới được tham gia làm bài kiểm tra. Kết quả bài làm sẽ được tự động đồng bộ và báo cáo về máy chủ Web Quản trị Vùng 4.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLoginRequiredDialog = false
                        showLoginDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Đăng nhập ngay", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showLoginRequiredDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Hủy")
                }
            }
        )
    }

    // DIALOG ĐĂNG NHẬP TRỰC TIẾP
    if (showLoginDialog) {
        com.example.ui.components.LoginDialog(
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
}

// -------------------------------------------------------------
// 1. OVERVIEW SCREEN (CHỌN CHẾ ĐỘ THI / TỔNG HỢP CÂU HỎI)
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExamOverviewView(
    totalQuestionsCount: Int,
    examSessions: List<ExamSessionDoc>,
    isAuthenticated: Boolean,
    onOpenLogin: () -> Unit,
    onStartSessionExam: (ExamSessionDoc) -> Unit,
    onStartExam: () -> Unit,
    onOpenQuestionBank: () -> Unit
) {
    // Only display exam sessions published by Web Admin from Firestore
    val displaySessions = examSessions
    var showAllExamsDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = RedPrimary.copy(alpha = 0.08f)),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(RedPrimary.copy(alpha = 0.3f)))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Vung4LogoBadge(size = 64.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "HỆ THỐNG KIỂM TRA TRỰC TUYẾN",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = RedPrimary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "VÙNG 4 HẢI QUÂN NHÂN DÂN VIỆT NAM",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = NavySecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // List of Active Exam Sessions from Web Admin
        if (displaySessions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(Color.Gray.copy(alpha = 0.3f))
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassEmpty,
                            contentDescription = null,
                            tint = RedPrimary,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "CHƯA CÓ ĐỢT THI NÀO TỪ WEB QUẢN TRỊ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = RedPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Hiện chưa có đợt thi chính thức nào được đăng tải. Đợt thi sẽ tự động hiển thị tại đây ngay khi Ban Quản trị Web phát hành đợt thi mới.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        } else {
            // Giới hạn hiển thị tối đa 2 bài kiểm tra gần nhất
            items(displaySessions.take(2)) { session ->
                ExamSessionCard(
                    session = session,
                    isAuthenticated = isAuthenticated,
                    onStartSessionExam = onStartSessionExam
                )
            }

            // Nút xem thêm nếu có nhiều hơn 2 bài kiểm tra
            if (displaySessions.size > 2) {
                item {
                    OutlinedButton(
                        onClick = { showAllExamsDialog = true },
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
                                imageVector = Icons.Default.FormatListBulleted,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = RedPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Xem thêm bài kiểm tra (${displaySessions.size} đợt thi)",
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

        // Section Title: Chế độ ôn luyện tự do
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Quiz, contentDescription = null, tint = NavySecondary, modifier = Modifier.size(22.dp))
                Text(
                    text = "LUYỆN TẬP TỰ DO & ÔN TẬP",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = NavySecondary
                )
            }
        }

        // Chế độ 1: Làm đề thi 20 câu ngẫu nhiên
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onStartExam() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(RedPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Quiz,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "LÀM ĐỀ THI 20 CÂU NGẪU NHIÊN",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Lấy ngẫu nhiên 20 câu từ toàn bộ ngân hàng câu hỏi",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = RedPrimary, modifier = Modifier.size(16.dp))
                            Text("Thời gian: 20 phút", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = RedPrimary, modifier = Modifier.size(16.dp))
                            Text("Số lượng: 20 câu", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onStartExam,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isAuthenticated) RedPrimary.copy(alpha = 0.85f) else RedPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            if (!isAuthenticated) Icons.Default.Lock else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (!isAuthenticated) "ĐĂNG NHẬP ĐỂ LÀM BÀI" else "BẮT ĐẦU LÀM BÀI THI",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Chế độ 2: Tổng hợp tất cả câu hỏi đã đăng tải (chỉ hiển thị khi đã đăng nhập)
        if (isAuthenticated) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenQuestionBank() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(3.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(NavySecondary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "XEM & ÔN TẬP TOÀN BỘ CÂU HỎI",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Ngân hàng $totalQuestionsCount câu hỏi đã đăng tải",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = onOpenQuestionBank,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("MỞ NGÂN HÀNG CÂU HỎI", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // HỘP THOẠI XEM TẤT CẢ CÁC ĐỢT THI & BÀI KIỂM TRA
    if (showAllExamsDialog) {
        Dialog(
            onDismissRequest = { showAllExamsDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            BackHandler { showAllExamsDialog = false }

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
                                        text = "DANH SÁCH BÀI KIỂM TRA",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Toàn bộ ${displaySessions.size} đợt thi từ Web Quản trị",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { showAllExamsDialog = false }) {
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
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
                    ) {
                        items(displaySessions) { session ->
                            ExamSessionCard(
                                session = session,
                                isAuthenticated = isAuthenticated,
                                onStartSessionExam = { targetSession ->
                                    showAllExamsDialog = false
                                    onStartSessionExam(targetSession)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExamSessionCard(
    session: ExamSessionDoc,
    isAuthenticated: Boolean,
    onStartSessionExam: (ExamSessionDoc) -> Unit,
    modifier: Modifier = Modifier
) {
    val isOpen = session.status.equals("open", ignoreCase = true) || session.status.equals("active", ignoreCase = true)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (isOpen) Modifier.clickable { onStartSessionExam(session) } else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(4.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isOpen) RedPrimary.copy(alpha = 0.4f) else Color.Gray.copy(alpha = 0.2f)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Badge status
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isOpen) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = if (isOpen) "🟢 ĐANG MỞ" else "🔒 CHƯA MỞ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOpen) Color(0xFF2E7D32) else Color.Gray,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = RedPrimary, modifier = Modifier.size(16.dp))
                    Text("${session.durationMinutes} phút", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = RedPrimary, modifier = Modifier.size(16.dp))
                    Text("${session.totalQuestions} câu hỏi", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.MilitaryTech, contentDescription = null, tint = NavySecondary, modifier = Modifier.size(16.dp))
                    Text("Báo cáo Web", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NavySecondary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = { if (isOpen) onStartSessionExam(session) },
                enabled = isOpen,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!isAuthenticated) RedPrimary.copy(alpha = 0.85f) else RedPrimary
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    if (!isAuthenticated) Icons.Default.Lock else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (!isOpen) "ĐỢT THI CHƯA MỞ"
                           else if (!isAuthenticated) "ĐĂNG NHẬP ĐỂ VÀO THI"
                           else "VÀO LÀM BÀI THI NGAY",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// -------------------------------------------------------------
// 2. TAKING EXAM VIEW (GIAO DIỆN LÀM ĐỀ THI 20 CÂU)
// -------------------------------------------------------------
@Composable
private fun ExamTakingView(
    examQuestions: List<QuestionItem>,
    currentIndex: Int,
    userAnswers: Map<Int, Int>,
    remainingSeconds: Int,
    onSelectAnswer: (questionIndex: Int, answerIndex: Int) -> Unit,
    onJumpToQuestion: (Int) -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onSubmit: () -> Unit
) {
    val currentQuestion = examQuestions.getOrNull(currentIndex) ?: return
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    val isTimeLow = remainingSeconds < 180 // Dưới 3 phút

    val questionListState = rememberLazyListState()
    LaunchedEffect(currentIndex) {
        if (currentIndex in examQuestions.indices) {
            questionListState.animateScrollToItem(currentIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Status & Timer Bar
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Câu ${currentIndex + 1}/${examQuestions.size}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = RedPrimary
                        )
                        Text(
                            text = "(Đã làm: ${userAnswers.size}/${examQuestions.size})",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Countdown Timer
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isTimeLow) MaterialTheme.colorScheme.error else RedPrimary
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Timer,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = formattedTime,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Dải số thứ tự câu hỏi để nhảy nhanh
                LazyRow(
                    state = questionListState,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(examQuestions.size) { idx ->
                        val isSelected = idx == currentIndex
                        val isAnswered = userAnswers.containsKey(idx)
                        
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        isSelected -> RedPrimary
                                        isAnswered -> NavySecondary
                                        else -> MaterialTheme.colorScheme.surface
                                    }
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) RedPrimary else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { onJumpToQuestion(idx) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${idx + 1}",
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                color = when {
                                    isSelected || isAnswered -> Color.White
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }
        }

        // Question & Options Container
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                // Category Chip
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = RedPrimary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = currentQuestion.categoryName.ifEmpty { "Chuyên đề Vùng 4" },
                        color = RedPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Question Text
                Text(
                    text = "Câu ${currentIndex + 1}: ${currentQuestion.question}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Options A, B, C, D
            itemsIndexed(currentQuestion.options) { optIndex, optionText ->
                val isOptionSelected = userAnswers[currentIndex] == optIndex
                val optionLetter = when (optIndex) {
                    0 -> "A"
                    1 -> "B"
                    2 -> "C"
                    3 -> "D"
                    else -> "${optIndex + 1}"
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectAnswer(currentIndex, optIndex) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isOptionSelected) RedPrimary.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = if (isOptionSelected) CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(RedPrimary)
                    ) else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isOptionSelected) RedPrimary
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = optionLetter,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isOptionSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = optionText,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            fontWeight = if (isOptionSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Bottom Navigation & Submit Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onPrev,
                        enabled = currentIndex > 0,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Câu trước")
                    }

                    Button(
                        onClick = onSubmit,
                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Nộp bài", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onNext,
                        enabled = currentIndex < examQuestions.size - 1,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Câu sau")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))
                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }
    }
}

// -------------------------------------------------------------
// 3. EXAM RESULT VIEW (XEM KẾT QUẢ VÀ BÀI LÀM CHI TIẾT)
// -------------------------------------------------------------
@Composable
private fun ExamResultView(
    examQuestions: List<QuestionItem>,
    userAnswers: Map<Int, Int>,
    timeSpentSeconds: Int,
    examName: String = "Bài thi trắc nghiệm",
    isOfficialWebExam: Boolean = false,
    onOpenFeedback: () -> Unit = {},
    onRetakeNewExam: () -> Unit,
    onBackToBank: () -> Unit,
    onBackToOverview: () -> Unit
) {
    val total = examQuestions.size
    val correctCount = examQuestions.indices.count { idx -> userAnswers[idx] == examQuestions[idx].correctIndex }
    val percent = if (total > 0) (correctCount * 100 / total) else 0
    val passed = percent >= 50

    val ratingText = when {
        percent >= 90 -> "XUẤT SẮC"
        percent >= 80 -> "GIỎI"
        percent >= 65 -> "KHÁ"
        percent >= 50 -> "ĐẠT YÊU CẦU"
        else -> "CHƯA ĐẠT"
    }

    val ratingColor = when {
        percent >= 80 -> Color(0xFF2E7D32) // Green
        percent >= 50 -> NavySecondary
        else -> RedPrimary
    }

    val spentMinutes = timeSpentSeconds / 60
    val spentSeconds = timeSpentSeconds % 60

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Result Summary Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ratingColor.copy(alpha = 0.1f)),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(ratingColor.copy(alpha = 0.4f)))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        if (passed) Icons.Default.EmojiEvents else Icons.Default.Warning,
                        contentDescription = null,
                        tint = ratingColor,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = examName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = RedPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "KẾT QUẢ: $ratingText",
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = ratingColor,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$correctCount / $total CÂU ĐÚNG ($percent%)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Thời gian làm bài: ${spentMinutes} phút ${spentSeconds} giây",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFE8F5E9)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                            Text(
                                text = "Đã gửi thành tích về Web Quản trị & lưu ở Cá nhân",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onOpenFeedback,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Feedback, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Phản ánh sai sót về đề thi", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onRetakeNewExam,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Làm đề khác", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = onBackToOverview,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Về menu", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        if (isOfficialWebExam) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = NavySecondary, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "ĐỢT THI CHÍNH THỨC TỪ WEB QUẢN TRỊ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = NavySecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Đối với đợt thi được tạo từ Web Quản trị, chi tiết các câu hỏi và đáp án không hiển thị sau khi nộp bài để đảm bảo tính bảo mật. Kết quả xếp loại và số câu trả lời đúng đã được lưu về máy chủ.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        } else {
            // Section Title: Chi tiết bài làm
            item {
                Text(
                    text = "CHI TIẾT ĐÁP ÁN VÀ LỜI GIẢI (${examQuestions.size} câu)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

        // Danh sách câu hỏi chi tiết kèm giải thích
        itemsIndexed(examQuestions) { idx, question ->
            val userSelected = userAnswers[idx]
            val isCorrect = userSelected == question.correctIndex

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCorrect) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        if (isCorrect) Color(0xFF4CAF50) else Color(0xFFE57373)
                    )
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                contentDescription = null,
                                tint = if (isCorrect) Color(0xFF2E7D32) else Color(0xFFC62828),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Câu ${idx + 1}: ${if (isCorrect) "ĐÚNG" else "SAI"}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isCorrect) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.White.copy(alpha = 0.8f)
                        ) {
                            Text(
                                text = question.categoryName,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = question.question,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    question.options.forEachIndexed { optIdx, optText ->
                        val isThisUserChoice = userSelected == optIdx
                        val isThisCorrectAnswer = optIdx == question.correctIndex
                        val optLetter = when (optIdx) {
                            0 -> "A"
                            1 -> "B"
                            2 -> "C"
                            3 -> "D"
                            else -> "${optIdx + 1}"
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = when {
                                isThisCorrectAnswer -> Color(0xFFC8E6C9)
                                isThisUserChoice && !isCorrect -> Color(0xFFFFCDD2)
                                else -> Color.White.copy(alpha = 0.6f)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "$optLetter.",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = when {
                                        isThisCorrectAnswer -> Color(0xFF1B5E20)
                                        isThisUserChoice -> Color(0xFFB71C1C)
                                        else -> Color.DarkGray
                                    }
                                )
                                Text(
                                    text = optText,
                                    fontSize = 13.sp,
                                    color = Color.Black,
                                    fontWeight = if (isThisCorrectAnswer || isThisUserChoice) FontWeight.SemiBold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isThisCorrectAnswer) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Đáp án đúng",
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (question.explanation.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.9f)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = Color(0xFFF57F17),
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text("Giải thích:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFF57F17))
                                    Text(question.explanation, fontSize = 12.sp, color = Color.DarkGray, lineHeight = 16.sp)
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

// -------------------------------------------------------------
// 4. QUESTION BANK VIEW (TỔNG HỢP TẤT CẢ CÂU HỎI ĐÃ ĐĂNG TẢI)
// -------------------------------------------------------------
@Composable
private fun QuestionBankView(
    allQuestions: List<QuestionItem>,
    onStartExam: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }
    // Map of questionId -> Boolean (xem đáp án hay chưa)
    var expandedAnswerMap by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    // Map of questionId -> Int (chọn làm thử)
    var practiceAnswerMap by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    val categories = listOf(
        "ALL" to "Tất cả (${allQuestions.size})",
        "GDCT" to "GD Chính trị",
        "GDPL" to "GD Pháp luật",
        "LICHSU" to "Lịch sử Truyền thống",
        "BIENDAO" to "Biển đảo Việt Nam",
        "DIEULENH" to "Điều lệnh"
    )

    val filteredQuestions = remember(allQuestions, searchQuery, selectedCategoryFilter) {
        allQuestions.filter { q ->
            val matchCat = when (selectedCategoryFilter) {
                "ALL" -> true
                "GDCT" -> q.category.equals("GDCT", ignoreCase = true) || q.categoryName.contains("chính trị", ignoreCase = true)
                "GDPL" -> q.category.equals("GDPL", ignoreCase = true) || q.categoryName.contains("pháp luật", ignoreCase = true)
                "LICHSU" -> q.category.contains("LICH", ignoreCase = true) || q.category.contains("TRUYEN", ignoreCase = true) || q.categoryName.contains("lịch sử", ignoreCase = true)
                "BIENDAO" -> q.category.contains("BIEN", ignoreCase = true) || q.categoryName.contains("biển đảo", ignoreCase = true)
                "DIEULENH" -> q.category.contains("DIEU", ignoreCase = true) || q.categoryName.contains("điều lệnh", ignoreCase = true)
                else -> true
            }
            val matchSearch = searchQuery.isBlank() || 
                q.question.contains(searchQuery, ignoreCase = true) ||
                q.options.any { it.contains(searchQuery, ignoreCase = true) } ||
                q.explanation.contains(searchQuery, ignoreCase = true)
            matchCat && matchSearch
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Search & Filter Header
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Tìm kiếm câu hỏi theo từ khóa...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = RedPrimary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Xóa")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                // Category Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { (catKey, catLabel) ->
                        val isSelected = selectedCategoryFilter == catKey
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategoryFilter = catKey },
                            label = { Text(catLabel, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RedPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        // Questions List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Danh sách câu hỏi (${filteredQuestions.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(onClick = onStartExam) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = RedPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Làm đề thi 20 câu", color = RedPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            if (filteredQuestions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.SearchOff, contentDescription = null, modifier = Modifier.size(40.dp), tint = RedPrimary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Không tìm thấy câu hỏi nào phù hợp với bộ lọc.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(filteredQuestions) { index, question ->
                    val isExpanded = expandedAnswerMap[question.id] ?: false
                    val userPracticeChoice = practiceAnswerMap[question.id]

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = RedPrimary.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = question.categoryName,
                                        color = RedPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }

                                Text(
                                    text = "Câu ${index + 1}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = question.question,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                lineHeight = 20.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // 4 options
                            question.options.forEachIndexed { optIndex, optText ->
                                val isCorrect = optIndex == question.correctIndex
                                val isSelectedByPractice = userPracticeChoice == optIndex
                                val showAnswerHighlight = isExpanded || userPracticeChoice != null
                                val optLetter = when (optIndex) {
                                    0 -> "A"
                                    1 -> "B"
                                    2 -> "C"
                                    3 -> "D"
                                    else -> "${optIndex + 1}"
                                }

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            val updated = HashMap(practiceAnswerMap)
                                            updated[question.id] = optIndex
                                            practiceAnswerMap = updated
                                        },
                                    shape = RoundedCornerShape(8.dp),
                                    color = when {
                                        showAnswerHighlight && isCorrect -> Color(0xFFE8F5E9)
                                        showAnswerHighlight && isSelectedByPractice && !isCorrect -> Color(0xFFFFEBEE)
                                        else -> MaterialTheme.colorScheme.surface
                                    },
                                    border = if (showAnswerHighlight && isCorrect) CardDefaults.outlinedCardBorder().copy(
                                        brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF4CAF50))
                                    ) else null
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "$optLetter.",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = when {
                                                showAnswerHighlight && isCorrect -> Color(0xFF2E7D32)
                                                showAnswerHighlight && isSelectedByPractice && !isCorrect -> Color(0xFFC62828)
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                        Text(
                                            text = optText,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (showAnswerHighlight && isCorrect) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = "Đáp án đúng",
                                                tint = Color(0xFF2E7D32),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Action: Toggle Xem Đáp Án & Lời Giải
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (userPracticeChoice != null) {
                                    val isPracticeCorrect = userPracticeChoice == question.correctIndex
                                    Text(
                                        text = if (isPracticeCorrect) "✅ Bạn đã trả lời ĐÚNG!" else "❌ Bạn đã trả lời SAI!",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isPracticeCorrect) Color(0xFF2E7D32) else Color(0xFFC62828)
                                    )
                                } else {
                                    Text(
                                        text = "💡 Chạm để làm thử",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                TextButton(
                                    onClick = {
                                        val updated = HashMap(expandedAnswerMap)
                                        updated[question.id] = !isExpanded
                                        expandedAnswerMap = updated
                                    }
                                ) {
                                    Icon(
                                        if (isExpanded) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = RedPrimary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isExpanded) "Ẩn giải thích" else "Xem đáp án & giải thích",
                                        color = RedPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            if (isExpanded && question.explanation.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Info,
                                            contentDescription = null,
                                            tint = RedPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column {
                                            Text("Giải thích chi tiết:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = RedPrimary)
                                            Text(
                                                text = question.explanation,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                lineHeight = 17.sp
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
