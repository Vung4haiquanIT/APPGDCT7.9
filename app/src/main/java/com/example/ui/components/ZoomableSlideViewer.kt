package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.model.SlideItem
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.RedPrimary
import kotlinx.coroutines.launch

/**
 * Tìm kiếm Activity từ Context một cách an toàn
 */
fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/**
 * Thành phần hiển thị 1 hình ảnh slide với khả năng thu phóng bằng 2 ngón tay (Pinch to Zoom),
 * kéo xem toàn cảnh (Pan) và chạm 2 lần để phóng to / thu nhỏ (Double-tap to Zoom).
 */
@Composable
fun ZoomableSlideImage(
    imageUrl: String,
    pageIndex: Int,
    isFullScreen: Boolean,
    currentScale: Float,
    onScaleChanged: (Float) -> Unit,
    onTap: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var scale by remember(imageUrl, pageIndex) { mutableFloatStateOf(currentScale) }
    var offsetX by remember(imageUrl, pageIndex) { mutableFloatStateOf(0f) }
    var offsetY by remember(imageUrl, pageIndex) { mutableFloatStateOf(0f) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    // Đồng bộ scale khi được gọi từ bên ngoài (ví dụ qua nút + / -)
    LaunchedEffect(currentScale) {
        if (kotlin.math.abs(scale - currentScale) > 0.05f) {
            scale = currentScale
            if (scale <= 1.05f) {
                offsetX = 0f
                offsetY = 0f
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { containerSize = it }
            .pointerInput(imageUrl, pageIndex) {
                detectTapGestures(
                    onTap = {
                        onTap?.invoke()
                    },
                    onDoubleTap = {
                        if (scale > 1.2f) {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        } else {
                            scale = 2.5f
                        }
                        onScaleChanged(scale)
                    }
                )
            }
            .pointerInput(imageUrl, pageIndex, scale) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var zoom = 1f
                    var pan = Offset.Zero
                    var pastTouchSlop = false
                    val touchSlop = viewConfiguration.touchSlop

                    do {
                        val event = awaitPointerEvent()
                        val canceled = event.changes.any { it.isConsumed }
                        if (canceled) break

                        val pressedPointers = event.changes.filter { it.pressed }
                        val pointerCount = pressedPointers.size

                        if (pointerCount >= 2) {
                            // Khi có từ 2 ngón tay trở lên: luôn xử lý pinch-to-zoom và pan 2 ngón
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()

                            if (!pastTouchSlop) {
                                zoom *= zoomChange
                                pan += panChange
                                val centroidSize = event.calculateCentroidSize(useCurrent = false)
                                val zoomMotion = kotlin.math.abs(1 - zoom) * centroidSize
                                val panMotion = pan.getDistance()

                                if (zoomMotion > touchSlop || panMotion > touchSlop) {
                                    pastTouchSlop = true
                                }
                            }

                            if (pastTouchSlop) {
                                val newScale = (scale * zoomChange).coerceIn(1f, 5f)
                                scale = newScale
                                onScaleChanged(newScale)

                                if (newScale > 1.02f && containerSize.width > 0 && containerSize.height > 0) {
                                    val maxPanX = ((containerSize.width * (newScale - 1f)) / 2f).coerceAtLeast(0f)
                                    val maxPanY = ((containerSize.height * (newScale - 1f)) / 2f).coerceAtLeast(0f)
                                    offsetX = (offsetX + panChange.x).coerceIn(-maxPanX, maxPanX)
                                    offsetY = (offsetY + panChange.y).coerceIn(-maxPanY, maxPanY)
                                } else {
                                    offsetX = 0f
                                    offsetY = 0f
                                }

                                event.changes.forEach { it.consume() }
                            }
                        } else if (pointerCount == 1 && scale > 1.05f) {
                            // Khi đang thu phóng lớn (>1.05x), cho phép 1 ngón tay pan kéo rê slide xem các góc
                            val panChange = event.calculatePan()
                            if (!pastTouchSlop) {
                                pan += panChange
                                if (pan.getDistance() > touchSlop) {
                                    pastTouchSlop = true
                                }
                            }

                            if (pastTouchSlop) {
                                if (containerSize.width > 0 && containerSize.height > 0) {
                                    val maxPanX = ((containerSize.width * (scale - 1f)) / 2f).coerceAtLeast(0f)
                                    val maxPanY = ((containerSize.height * (scale - 1f)) / 2f).coerceAtLeast(0f)
                                    offsetX = (offsetX + panChange.x).coerceIn(-maxPanX, maxPanX)
                                    offsetY = (offsetY + panChange.y).coerceIn(-maxPanY, maxPanY)
                                }
                                event.changes.forEach { it.consume() }
                            }
                        } else {
                            // pointerCount == 1 && scale <= 1.05f: CHẾ ĐỘ BÌNH THƯỜNG
                            // KHÔNG consume touch events để HorizontalPager tự do lướt sang 2 bên chuyển slide mượt mà!
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl.isNotBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Slide ${pageIndex + 1}",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offsetX
                        translationY = offsetY
                    }
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isFullScreen) Color(0xFF1E1E24) else RedPrimary.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = if (isFullScreen) GoldPrimary else RedPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Slide số ${pageIndex + 1}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (isFullScreen) Color.White else Color.Unspecified
                    )
                }
            }
        }

        // Huy hiệu hỗ trợ khi đang thu phóng lớn
        if (scale > 1.15f) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
                    .clickable {
                        scale = 1f
                        offsetX = 0f
                        offsetY = 0f
                        onScaleChanged(1f)
                    },
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.75f),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.RestartAlt,
                        contentDescription = "Đặt lại",
                        tint = GoldPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Thu phóng ${(scale * 100).toInt()}% • Chạm để đặt lại 100%",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Trình xem Slide đầy đủ tính năng:
 * - Hỗ trợ xem trực tiếp dạng thẻ hoặc xem toàn màn hình (Full Screen Dialog)
 * - Thu phóng bằng 2 ngón tay (Pinch to Zoom từ 1x đến 5x)
 * - Kéo xem các góc khi đang phóng to (Pan)
 * - Chạm 2 lần (Double Tap) để phóng to / thu nhỏ tức thì
 * - Các nút điều khiển zoom (+, -, 100%)
 * - Nút xoay ngang màn hình (Landscape) khi xem toàn màn hình
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EnhancedSlideViewer(
    lessonSlides: List<SlideItem>,
    pagerState: PagerState,
    currentSlideDwellSeconds: Int,
    isSlideCompleted: Boolean,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    var isFullScreenOpen by remember { mutableStateOf(false) }
    var currentZoomScale by remember { mutableFloatStateOf(1f) }

    // Khi chuyển trang slide, tự động đặt lại độ thu phóng về 100%
    LaunchedEffect(pagerState.currentPage) {
        currentZoomScale = 1f
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // KHUNG XEM SLIDE CHÍNH (DẠNG THẺ)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(3.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Pager hiển thị các slide. Khi đang thu phóng lớn (>1.05x), tắt vuốt chuyển trang để người dùng kéo rê ảnh tự do
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = currentZoomScale <= 1.05f
                ) { page ->
                    val slide = lessonSlides[page]
                    ZoomableSlideImage(
                        imageUrl = slide.imageUrl,
                        pageIndex = page,
                        isFullScreen = false,
                        currentScale = if (page == pagerState.currentPage) currentZoomScale else 1f,
                        onScaleChanged = { newScale ->
                            if (page == pagerState.currentPage) {
                                currentZoomScale = newScale
                            }
                        }
                    )
                }

                // THANH ĐIỀU KHIỂN NHANH TRÊN ĐẦU SLIDE
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Huy hiệu hiển thị số trang
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.65f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Slide ${pagerState.currentPage + 1}/${lessonSlides.size}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (isSlideCompleted) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Đã xem",
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    // Nút mở chế độ Toàn Màn Hình
                    Surface(
                        modifier = Modifier.clickable {
                            isFullScreenOpen = true
                        },
                        shape = RoundedCornerShape(20.dp),
                        color = RedPrimary.copy(alpha = 0.9f),
                        shadowElevation = 3.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Fullscreen,
                                contentDescription = "Xem full màn hình",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Toàn màn hình",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // BỘ NÚT ĐIỀU HƯỚNG VÀ THU PHÓNG BÊN DƯỚI
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Nút "Slide trước"
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
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.ChevronLeft, contentDescription = null)
                Spacer(modifier = Modifier.width(2.dp))
                Text("Slide trước", fontSize = 13.sp)
            }

            // Thanh công cụ thu phóng nhanh (+ / - / 1x)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            currentZoomScale = (currentZoomScale - 0.5f).coerceAtLeast(1f)
                        },
                        enabled = currentZoomScale > 1.05f,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            Icons.Default.ZoomOut,
                            contentDescription = "Thu nhỏ",
                            tint = if (currentZoomScale > 1.05f) RedPrimary else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        "${(currentZoomScale * 100).toInt()}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clickable { currentZoomScale = 1f }
                            .padding(horizontal = 4.dp)
                    )

                    IconButton(
                        onClick = {
                            currentZoomScale = (currentZoomScale + 0.5f).coerceAtMost(5f)
                        },
                        enabled = currentZoomScale < 5f,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            Icons.Default.ZoomIn,
                            contentDescription = "Phóng to",
                            tint = if (currentZoomScale < 5f) RedPrimary else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Nút "Slide sau"
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
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text("Slide sau", fontSize = 13.sp)
                Spacer(modifier = Modifier.width(2.dp))
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        }
    }

    // =========================================================================================
    // GIAO DIỆN XEM TOÀN MÀN HÌNH (FULL SCREEN VIEWER DIALOG)
    // =========================================================================================
    if (isFullScreenOpen) {
        val fullScreenPagerState = rememberPagerState(
            initialPage = pagerState.currentPage,
            pageCount = { lessonSlides.size }
        )
        var fullScreenZoomScale by remember { mutableFloatStateOf(1f) }
        var isLandscape by remember { mutableStateOf(false) }
        var showFullScreenControls by remember { mutableStateOf(true) }

        // Đồng bộ trang giữa full screen và màn hình chính
        LaunchedEffect(fullScreenPagerState.currentPage) {
            fullScreenZoomScale = 1f
            if (pagerState.currentPage != fullScreenPagerState.currentPage) {
                pagerState.scrollToPage(fullScreenPagerState.currentPage)
            }
        }

        // Tự động hoàn trả hướng xoay màn hình khi đóng Dialog
        DisposableEffect(Unit) {
            onDispose {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }

        Dialog(
            onDismissRequest = {
                isFullScreenOpen = false
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            BackHandler {
                isFullScreenOpen = false
            }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Pager ảnh toàn màn hình
                    HorizontalPager(
                        state = fullScreenPagerState,
                        modifier = Modifier.fillMaxSize(),
                        userScrollEnabled = fullScreenZoomScale <= 1.05f
                    ) { page ->
                        val slide = lessonSlides[page]
                        ZoomableSlideImage(
                            imageUrl = slide.imageUrl,
                            pageIndex = page,
                            isFullScreen = true,
                            currentScale = if (page == fullScreenPagerState.currentPage) fullScreenZoomScale else 1f,
                            onScaleChanged = { newScale ->
                                if (page == fullScreenPagerState.currentPage) {
                                    fullScreenZoomScale = newScale
                                }
                            },
                            onTap = {
                                showFullScreenControls = !showFullScreenControls
                            }
                        )
                    }

                    // Nút đóng nhanh nổi khi thanh công cụ ẩn
                    AnimatedVisibility(
                        visible = !showFullScreenControls,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(14.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.65f),
                            shadowElevation = 4.dp,
                            modifier = Modifier.clickable { isFullScreenOpen = false }
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Đóng toàn màn hình",
                                tint = Color.White,
                                modifier = Modifier
                                    .padding(10.dp)
                                    .size(24.dp)
                            )
                        }
                    }

                    // THANH TIÊU ĐỀ PHÍA TRÊN (TOP BAR OVERLAY)
                    AnimatedVisibility(
                        visible = showFullScreenControls,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
                        modifier = Modifier.align(Alignment.TopCenter)
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.Black.copy(alpha = 0.75f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Nút đóng Fullscreen
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color.White.copy(alpha = 0.18f),
                                    modifier = Modifier.clickable { isFullScreenOpen = false }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Đóng",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "Đóng",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                // Tiêu đề số Slide
                                Text(
                                    "Slide ${fullScreenPagerState.currentPage + 1} / ${lessonSlides.size}",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Nút ẩn thanh điều khiển
                                    IconButton(onClick = { showFullScreenControls = false }) {
                                        Icon(
                                            Icons.Default.VisibilityOff,
                                            contentDescription = "Ẩn thanh điều khiển",
                                            tint = Color.White
                                        )
                                    }

                                    // Nút xoay ngang/dọc màn hình
                                    IconButton(onClick = {
                                        isLandscape = !isLandscape
                                        activity?.requestedOrientation = if (isLandscape) {
                                            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                        } else {
                                            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                        }
                                    }) {
                                        Icon(
                                            Icons.Default.ScreenRotation,
                                            contentDescription = "Xoay màn hình",
                                            tint = if (isLandscape) GoldPrimary else Color.White
                                        )
                                    }

                                    // Nút thu nhỏ lại màn hình thường
                                    IconButton(onClick = { isFullScreenOpen = false }) {
                                        Icon(
                                            Icons.Default.FullscreenExit,
                                            contentDescription = "Thoát toàn màn hình",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // THANH ĐIỀU KHIỂN PHÍA DƯỚI (BOTTOM BAR OVERLAY)
                    AnimatedVisibility(
                        visible = showFullScreenControls,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                        exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.Black.copy(alpha = 0.78f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Nút Slide trước
                                IconButton(
                                    onClick = {
                                        if (fullScreenPagerState.currentPage > 0) {
                                            coroutineScope.launch {
                                                fullScreenPagerState.animateScrollToPage(fullScreenPagerState.currentPage - 1)
                                            }
                                        }
                                    },
                                    enabled = fullScreenPagerState.currentPage > 0
                                ) {
                                    Icon(
                                        Icons.Default.ChevronLeft,
                                        contentDescription = "Slide trước",
                                        tint = if (fullScreenPagerState.currentPage > 0) Color.White else Color.Gray,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                // Bộ nút thu phóng giữa màn hình
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            fullScreenZoomScale = (fullScreenZoomScale - 0.5f).coerceAtLeast(1f)
                                        },
                                        enabled = fullScreenZoomScale > 1.05f
                                    ) {
                                        Icon(
                                            Icons.Default.ZoomOut,
                                            contentDescription = "Thu nhỏ",
                                            tint = if (fullScreenZoomScale > 1.05f) Color.White else Color.Gray
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.White.copy(alpha = 0.2f),
                                        modifier = Modifier.clickable { fullScreenZoomScale = 1f }
                                    ) {
                                        Text(
                                            "${(fullScreenZoomScale * 100).toInt()}%",
                                            color = GoldPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            fullScreenZoomScale = (fullScreenZoomScale + 0.5f).coerceAtMost(5f)
                                        },
                                        enabled = fullScreenZoomScale < 5f
                                    ) {
                                        Icon(
                                            Icons.Default.ZoomIn,
                                            contentDescription = "Phóng to",
                                            tint = if (fullScreenZoomScale < 5f) Color.White else Color.Gray
                                        )
                                    }
                                }

                                // Nút Slide sau
                                IconButton(
                                    onClick = {
                                        if (fullScreenPagerState.currentPage < lessonSlides.size - 1) {
                                            coroutineScope.launch {
                                                fullScreenPagerState.animateScrollToPage(fullScreenPagerState.currentPage + 1)
                                            }
                                        }
                                    },
                                    enabled = fullScreenPagerState.currentPage < lessonSlides.size - 1
                                ) {
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = "Slide sau",
                                        tint = if (fullScreenPagerState.currentPage < lessonSlides.size - 1) Color.White else Color.Gray,
                                        modifier = Modifier.size(32.dp)
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
