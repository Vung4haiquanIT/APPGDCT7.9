package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.RedPrimary

enum class VideoSourceType(val displayName: String) {
    YOUTUBE("YouTube"),
    GOOGLE_DRIVE("Google Drive"),
    DIRECT_STREAM("Video Tải lên"),
    WEB_URL("Liên kết Web")
}

object VideoUrlHelper {
    fun detectSourceType(url: String): VideoSourceType {
        val clean = url.trim().lowercase()
        return when {
            clean.contains("youtube.com") || clean.contains("youtu.be") -> VideoSourceType.YOUTUBE
            clean.contains("drive.google.com") -> VideoSourceType.GOOGLE_DRIVE
            clean.endsWith(".mp4") || clean.endsWith(".m3u8") || clean.endsWith(".webm") ||
            clean.endsWith(".mov") || clean.endsWith(".mkv") || clean.endsWith(".mpd") ||
            clean.contains("firebasestorage.googleapis.com") -> VideoSourceType.DIRECT_STREAM
            clean.startsWith("http://") || clean.startsWith("https://") -> VideoSourceType.WEB_URL
            else -> VideoSourceType.DIRECT_STREAM
        }
    }

    fun extractYouTubeVideoId(url: String): String? {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return null

        // 1. youtu.be/VIDEO_ID
        val youtuBeRegex = Regex("youtu\\.be/([a-zA-Z0-9_-]{11})", RegexOption.IGNORE_CASE)
        youtuBeRegex.find(trimmed)?.let { return it.groupValues[1] }

        // 2. youtube.com/embed/VIDEO_ID
        val embedRegex = Regex("youtube\\.com/embed/([a-zA-Z0-9_-]{11})", RegexOption.IGNORE_CASE)
        embedRegex.find(trimmed)?.let { return it.groupValues[1] }

        // 3. youtube.com/shorts/VIDEO_ID
        val shortsRegex = Regex("youtube\\.com/shorts/([a-zA-Z0-9_-]{11})", RegexOption.IGNORE_CASE)
        shortsRegex.find(trimmed)?.let { return it.groupValues[1] }

        // 4. youtube.com/v/VIDEO_ID
        val vRegex = Regex("youtube\\.com/v/([a-zA-Z0-9_-]{11})", RegexOption.IGNORE_CASE)
        vRegex.find(trimmed)?.let { return it.groupValues[1] }

        // 5. Query param ?v= or &v=
        val queryRegex = Regex("[?&]v=([a-zA-Z0-9_-]{11})", RegexOption.IGNORE_CASE)
        queryRegex.find(trimmed)?.let { return it.groupValues[1] }

        // 6. Generic regex for 11 chars
        if (trimmed.contains("youtube", ignoreCase = true) || trimmed.contains("youtu.be", ignoreCase = true)) {
            val generalRegex = Regex("([a-zA-Z0-9_-]{11})")
            generalRegex.findAll(trimmed).lastOrNull()?.let { return it.groupValues[1] }
        }

        return null
    }

    fun extractGoogleDriveFileId(url: String): String? {
        val trimmed = url.trim()
        val regex = Regex("drive\\.google\\.com/(?:file/d/|open\\?id=|uc\\?id=)([a-zA-Z0-9_-]+)", RegexOption.IGNORE_CASE)
        regex.find(trimmed)?.let { return it.groupValues[1] }
        return null
    }

    fun getGoogleDrivePreviewUrl(url: String): String {
        val id = extractGoogleDriveFileId(url)
        return if (id != null) {
            "https://drive.google.com/file/d/$id/preview"
        } else {
            url
        }
    }
}

/**
 * Trình phát Video Đa năng: Hỗ trợ mượt mà cả Video tải lên trực tiếp (ExoPlayer),
 * video liên kết YouTube, video Google Drive, và các đường dẫn video từ web.
 */
@Composable
fun UniversalVideoPlayer(
    videoUrl: String,
    exoPlayer: ExoPlayer,
    isFullScreen: Boolean,
    onToggleFullScreen: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    playerErrorMessage: String? = null,
    onOpenExternal: () -> Unit = {}
) {
    val context = LocalContext.current
    val detectedType = remember(videoUrl) { VideoUrlHelper.detectSourceType(videoUrl) }
    val isExternalUrl = detectedType != VideoSourceType.DIRECT_STREAM

    // Với link Web / YouTube / Drive: mặc định hiển thị thẻ Video Preview tương tác cao cấp
    // Người dùng có thể nhấn phát ngay (mở YouTube/trình duyệt ổn định không crash) hoặc tùy chọn nhúng WebView
    var useInAppWebView by remember(videoUrl) { mutableStateOf(false) }
    var customFullScreenView by remember { mutableStateOf<View?>(null) }

    Box(
        modifier = modifier
            .background(Color.Black)
    ) {
        when {
            customFullScreenView != null -> {
                AndroidView(
                    factory = { customFullScreenView!! },
                    modifier = Modifier.fillMaxSize()
                )
            }
            !isExternalUrl -> {
                // 1. Video tải lên trực tiếp (mp4, m3u8, Firebase Storage): dùng ExoPlayer
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = true
                            setFullscreenButtonClickListener { fs ->
                                onToggleFullScreen(fs)
                            }
                        }
                    },
                    update = { pv ->
                        if (pv.player != exoPlayer) pv.player = exoPlayer
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            useInAppWebView -> {
                // 2. Tùy chọn nhúng WebView trong ứng dụng (nếu người dùng chủ động chọn)
                key(videoUrl) {
                    WebVideoPlayerView(
                        videoUrl = videoUrl,
                        sourceType = detectedType,
                        onCustomViewChanged = { view ->
                            customFullScreenView = view
                            if (view != null) onToggleFullScreen(true)
                        },
                        onFallbackToExternal = {
                            useInAppWebView = false
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            else -> {
                // 3. Trình phát tương tác chuyên nghiệp cho YouTube / Google Drive / URL
                // Tránh lỗi crash Chromium trên máy ảo và mang lại trải nghiệm xem chất lượng cao 1080p/4K
                ExternalVideoInteractiveCard(
                    videoUrl = videoUrl,
                    sourceType = detectedType,
                    isFullScreen = isFullScreen,
                    onPlayClicked = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            onOpenExternal()
                        }
                    },
                    onToggleInAppWeb = {
                        useInAppWebView = true
                    },
                    onToggleFullScreen = onToggleFullScreen,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Thanh công cụ nhỏ góc trên bên phải khi không ở chế độ full-screen (cho ExoPlayer hoặc WebView)
        if (!isFullScreen && (!isExternalUrl || useInAppWebView)) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isExternalUrl) {
                    IconButton(
                        onClick = { useInAppWebView = false },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartDisplay,
                            contentDescription = "Thẻ phát YouTube",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                IconButton(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            onOpenExternal()
                        }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "Mở ứng dụng ngoài",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = { onToggleFullScreen(true) },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "Xem toàn màn hình",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

/**
 * Giao diện Thẻ Video Tương tác: Hiển thị hình thu nhỏ chính thức từ YouTube,
 * nút Phát lớn màu đỏ, nhãn chất lượng cao và hành động mở xem video tức thì không gián đoạn.
 */
@Composable
fun ExternalVideoInteractiveCard(
    videoUrl: String,
    sourceType: VideoSourceType,
    isFullScreen: Boolean,
    onPlayClicked: () -> Unit,
    onToggleInAppWeb: () -> Unit,
    onToggleFullScreen: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val ytId = remember(videoUrl) { VideoUrlHelper.extractYouTubeVideoId(videoUrl) }
    val thumbnailUrl = remember(ytId) {
        if (ytId != null) "https://img.youtube.com/vi/$ytId/hqdefault.jpg" else null
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .clickable { onPlayClicked() }
    ) {
        // Ảnh thumbnail video nếu có
        if (thumbnailUrl != null) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = "Hình thu nhỏ video",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Lớp phủ gradient tạo độ tương phản cao cho chữ và nút phát
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.45f),
                            Color.Black.copy(alpha = 0.25f),
                            Color.Black.copy(alpha = 0.75f)
                        )
                    )
                )
        )

        // Nút Phát trung tâm lớn
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                onClick = onPlayClicked,
                shape = CircleShape,
                color = RedPrimary,
                shadowElevation = 8.dp,
                modifier = Modifier.size(if (isFullScreen) 72.dp else 56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Phát video",
                        tint = Color.White,
                        modifier = Modifier.size(if (isFullScreen) 44.dp else 34.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.65f)
            ) {
                Text(
                    text = when (sourceType) {
                        VideoSourceType.YOUTUBE -> "Nhấn để phát video trên YouTube"
                        VideoSourceType.GOOGLE_DRIVE -> "Nhấn để phát video Google Drive"
                        else -> "Nhấn để phát video trực tuyến"
                    },
                    color = Color.White,
                    fontSize = if (isFullScreen) 14.sp else 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }
        }

        // Huy hiệu góc trên bên trái
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            shape = RoundedCornerShape(8.dp),
            color = when (sourceType) {
                VideoSourceType.YOUTUBE -> RedPrimary
                VideoSourceType.GOOGLE_DRIVE -> Color(0xFF1976D2)
                else -> Color(0xFFE65100)
            }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = when (sourceType) {
                        VideoSourceType.YOUTUBE -> Icons.Default.SmartDisplay
                        VideoSourceType.GOOGLE_DRIVE -> Icons.Default.CloudQueue
                        else -> Icons.Default.Language
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = sourceType.displayName,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Thanh công cụ góc dưới
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Nút mở nhúng WebView trong ứng dụng
            Surface(
                onClick = onToggleInAppWeb,
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.65f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Nhúng web",
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }
            }

            // Nút Toàn màn hình
            if (!isFullScreen) {
                IconButton(
                    onClick = { onToggleFullScreen(true) },
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "Toàn màn hình",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebVideoPlayerView(
    videoUrl: String,
    sourceType: VideoSourceType,
    onCustomViewChanged: (View?) -> Unit = {},
    onFallbackToExternal: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                webViewInstance?.apply {
                    stopLoading()
                    loadUrl("about:blank")
                    destroy()
                }
            } catch (_: Exception) {}
        }
    }

    Box(modifier = modifier.background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        // mediaPlaybackRequiresUserGesture = true ngăn Chromium tự động nạp GPU decoders gây crash tiến trình renderer
                        mediaPlaybackRequiresUserGesture = true
                        builtInZoomControls = false
                        displayZoomControls = false
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                    }
                    webChromeClient = object : WebChromeClient() {
                        private var customCallback: CustomViewCallback? = null

                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            if (newProgress >= 70) {
                                isLoading = false
                            }
                        }

                        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                            customCallback = callback
                            onCustomViewChanged(view)
                        }

                        override fun onHideCustomView() {
                            customCallback?.onCustomViewHidden()
                            onCustomViewChanged(null)
                        }
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            isLoading = true
                        }
                        override fun onPageFinished(view: WebView?, url: String?) {
                            isLoading = false
                        }
                        override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                            Log.w("UniversalVideoPlayer", "Renderer process gone, handling gracefully")
                            try {
                                (view?.parent as? ViewGroup)?.removeView(view)
                                view?.destroy()
                            } catch (_: Exception) {}
                            onFallbackToExternal()
                            return true // Trả về true để ngăn Android đóng ứng dụng
                        }
                        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                            return false
                        }
                    }

                    loadAppropriateContent(this, videoUrl, sourceType)
                    webViewInstance = this
                }
            },
            update = { wv ->
                webViewInstance = wv
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = RedPrimary,
                    modifier = Modifier.size(36.dp),
                    strokeWidth = 3.dp
                )
            }
        }
    }
}

private fun loadAppropriateContent(webView: WebView, videoUrl: String, sourceType: VideoSourceType) {
    val ytId = VideoUrlHelper.extractYouTubeVideoId(videoUrl)
    if (ytId != null || sourceType == VideoSourceType.YOUTUBE) {
        val targetId = ytId ?: run {
            if (videoUrl.contains("/embed/")) {
                videoUrl.substringAfter("/embed/").substringBefore("?").substringBefore("/")
            } else ""
        }
        if (targetId.isNotBlank()) {
            val html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                    <style>
                        * { margin: 0; padding: 0; box-sizing: border-box; }
                        html, body {
                            width: 100%;
                            height: 100%;
                            background-color: #000000;
                            overflow: hidden;
                            display: flex;
                            align-items: center;
                            justify-content: center;
                        }
                        iframe {
                            position: absolute;
                            top: 0;
                            left: 0;
                            width: 100%;
                            height: 100%;
                            border: 0;
                        }
                    </style>
                </head>
                <body>
                    <iframe
                        src="https://www.youtube-nocookie.com/embed/$targetId?playsinline=1&rel=0&modestbranding=1"
                        allow="accelerometer; encrypted-media; gyroscope; picture-in-picture"
                        allowfullscreen>
                    </iframe>
                </body>
                </html>
            """.trimIndent()
            webView.loadDataWithBaseURL("https://www.youtube-nocookie.com", html, "text/html", "UTF-8", null)
        } else {
            webView.loadUrl(videoUrl)
        }
    } else if (sourceType == VideoSourceType.GOOGLE_DRIVE) {
        val previewUrl = VideoUrlHelper.getGoogleDrivePreviewUrl(videoUrl)
        webView.loadUrl(previewUrl)
    } else {
        webView.loadUrl(videoUrl)
    }
}
