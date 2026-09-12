package com.example.ui.components

import android.net.Uri
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.model.AudioItem
import com.example.model.Lesson
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.NavySecondary
import com.example.ui.theme.RedPrimary
import com.example.viewmodel.AppViewModel
import kotlinx.coroutines.delay

/**
 * Mục âm thanh phát thanh trong Truyền thanh nội bộ
 */
data class BroadcastAudio(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val audioUrl: String,
    val durationText: String = "05:20",
    val dateText: String = "Hôm nay"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TruyenThanhNoiBoDialog(
    viewModel: AppViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val audiosFromDb by viewModel.audios.collectAsState()
    val lessons by viewModel.lessons.collectAsState()

    // Danh sách bài học để map tên bài học với audio nếu cần
    val lessonMap = remember(lessons) { lessons.associateBy { it.id } }

    // Danh sách phát thanh mặc định đặc sắc kết hợp cùng dữ liệu Firebase
    val allBroadcasts = remember(audiosFromDb, lessons) {
        val list = mutableListOf<BroadcastAudio>()

        // 1. Thêm các file audio thực tế từ cơ sở dữ liệu Firebase Firestore
        audiosFromDb.forEach { dbAudio ->
            val linkedLesson = lessonMap[dbAudio.lessonId]
            val cat = if (linkedLesson != null) "Ghi âm bài giảng" else "Phát thanh nội bộ"
            list.add(
                BroadcastAudio(
                    id = dbAudio.id,
                    title = dbAudio.title.ifEmpty { linkedLesson?.title ?: "Chương trình phát thanh nội bộ" },
                    category = cat,
                    description = linkedLesson?.description ?: "Nội dung truyền thanh chính trị - tuyên truyền giáo dục",
                    audioUrl = dbAudio.audioUrl,
                    durationText = "Audio",
                    dateText = "Chính thức"
                )
            )
        }

        // 2. Thêm các chuyên mục phát thanh tiêu biểu của Vùng 4 Hải quân
        list.add(
            BroadcastAudio(
                id = "bt_sang",
                title = "Bản tin Phát thanh Nội bộ: Thời sự & Huấn luyện SSCĐ số 01",
                category = "Bản tin phát thanh",
                description = "Bản tin tổng hợp tình hình biển đảo, nhiệm vụ sẵn sàng chiến đấu và gương cán bộ, chiến sĩ tiêu biểu.",
                audioUrl = "",
                durationText = "12:35",
                dateText = "Phát thanh 06h00"
            )
        )
        list.add(
            BroadcastAudio(
                id = "bt_chieu",
                title = "Bản tin Chiều: Nhịp sống doanh trại & Điểm sáng văn hóa",
                category = "Bản tin phát thanh",
                description = "Chuyên mục xây dựng nền nếp chính quy, rèn luyện kỷ luật và phong trào thể thao văn nghệ tại các đơn vị.",
                audioUrl = "",
                durationText = "15:10",
                dateText = "Phát thanh 17h00"
            )
        )
        list.add(
            BroadcastAudio(
                id = "hk_hq",
                title = "Hành khúc Hải quân Việt Nam (Trọng Loan)",
                category = "Hành khúc & Bài ca Biển",
                description = "Giai điệu hào hùng, niềm tự hào của những người lính giữ biển trời thiêng liêng của Tổ quốc.",
                audioUrl = "",
                durationText = "04:15",
                dateText = "Ca khúc truyền thống"
            )
        )
        list.add(
            BroadcastAudio(
                id = "hk_bien",
                title = "Lướt sóng ra khơi (Thế Dương)",
                category = "Hành khúc & Bài ca Biển",
                description = "Bài ca truyền thống khắc họa ý chí kiên cường và tình cảm của chiến sĩ Hải quân nhân dân Việt Nam.",
                audioUrl = "",
                durationText = "03:52",
                dateText = "Ca khúc truyền thống"
            )
        )
        list.add(
            BroadcastAudio(
                id = "lbh_1",
                title = "Chuyên mục: Lời Bác dạy ngày này năm xưa",
                category = "Lời Bác dạy",
                description = "Mỗi ngày một lời Bác dạy cán bộ, chiến sĩ Quân đội nhân dân Việt Nam về phẩm chất Bộ đội Cụ Hồ.",
                audioUrl = "",
                durationText = "06:45",
                dateText = "Phát sóng hàng ngày"
            )
        )
        list.add(
            BroadcastAudio(
                id = "ls_tau_khong_so",
                title = "Kể chuyện Truyền thống: Huyền thoại Đường Hồ Chí Minh trên biển",
                category = "Kể chuyện truyền thống",
                description = "Ký ức hào hùng về những con tàu Không số cảm tử vượt sóng gió chi viện cho chiến trường miền Nam.",
                audioUrl = "",
                durationText = "18:20",
                dateText = "Chuyên đề lịch sử"
            )
        )

        list
    }

    // State phát thanh
    var selectedBroadcast by remember { mutableStateOf(allBroadcasts.firstOrNull()) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableStateOf(0L) }
    var totalDurationMs by remember { mutableStateOf(1L) }
    var isDraggingSlider by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("Tất cả") }

    val categories = listOf("Tất cả", "Bản tin phát thanh", "Ghi âm bài giảng", "Hành khúc & Bài ca Biển", "Lời Bác dạy")

    val filteredList = remember(allBroadcasts, searchQuery, selectedCategoryFilter) {
        allBroadcasts.filter { item ->
            val matchCat = if (selectedCategoryFilter == "Tất cả") true else item.category.equals(selectedCategoryFilter, ignoreCase = true)
            val matchSearch = if (searchQuery.isBlank()) true else {
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.description.contains(searchQuery, ignoreCase = true) ||
                item.category.contains(searchQuery, ignoreCase = true)
            }
            matchCat && matchSearch
        }
    }

    // Khởi tạo ExoPlayer cho phát thanh
    val radioPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = false
        }
    }

    DisposableEffect(radioPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    isPlaying = false
                }
            }
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        radioPlayer.addListener(listener)
        onDispose {
            radioPlayer.removeListener(listener)
            try {
                radioPlayer.stop()
                radioPlayer.release()
            } catch (_: Exception) {}
        }
    }

    // Progress ticker
    LaunchedEffect(radioPlayer, isPlaying) {
        while (true) {
            if (radioPlayer.isPlaying && !isDraggingSlider) {
                currentPositionMs = radioPlayer.currentPosition
                totalDurationMs = radioPlayer.duration.coerceAtLeast(1L)
            }
            delay(400)
        }
    }

    fun playTrack(track: BroadcastAudio) {
        selectedBroadcast = track
        if (track.audioUrl.isNotBlank()) {
            try {
                val mediaItem = MediaItem.fromUri(Uri.parse(track.audioUrl))
                radioPlayer.setMediaItem(mediaItem)
                radioPlayer.setPlaybackSpeed(playbackSpeed)
                radioPlayer.prepare()
                radioPlayer.play()
                isPlaying = true
            } catch (e: Exception) {
                Log.e("TruyenThanh", "Cannot play audio: ${e.message}")
            }
        } else {
            // Trường hợp phát thanh mô phỏng nội bộ
            isPlaying = true
            totalDurationMs = 180000L // 3 phút
            currentPositionMs = 0L
        }
    }

    fun togglePlayPause() {
        if (selectedBroadcast == null) {
            selectedBroadcast = allBroadcasts.firstOrNull()
        }
        val current = selectedBroadcast ?: return

        if (current.audioUrl.isNotBlank()) {
            if (radioPlayer.isPlaying) {
                radioPlayer.pause()
                isPlaying = false
            } else {
                if (radioPlayer.playbackState == Player.STATE_IDLE || radioPlayer.playbackState == Player.STATE_ENDED) {
                    playTrack(current)
                } else {
                    radioPlayer.play()
                    isPlaying = true
                }
            }
        } else {
            isPlaying = !isPlaying
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
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
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Quay lại",
                                tint = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        Vung4LogoBadge(size = 36.dp)

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "TRUYỀN THANH NỘI BỘ",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Kênh thông tin & phát thanh chính trị Vùng 4 Hải quân",
                                fontSize = 11.sp,
                                color = GoldPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Badge Trực tuyến
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF2E7D32)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                                Text(
                                    text = "ĐANG PHÁT",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color(0xFFF7F9FC))
            ) {
                // KHUNG PHÁT THANH HIỆN TẠI (HERO PLAYER CARD)
                selectedBroadcast?.let { currentTrack ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        shape = RoundedCornerShape(18.dp),
                        elevation = CardDefaults.cardElevation(4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            NavySecondary.copy(alpha = 0.08f),
                                            Color.White
                                        )
                                    )
                                )
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Radio Icon Avatar với hiệu ứng phát sóng
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (isPlaying) Brush.linearGradient(listOf(Color(0xFF00838F), NavySecondary))
                                            else Brush.linearGradient(listOf(Color.Gray, Color.DarkGray))
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Podcasts else Icons.Default.Radio,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF00838F).copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = currentTrack.category,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF00838F),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = currentTrack.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Thanh kéo tiến trình âm thanh
                            val sliderPos = if (totalDurationMs > 0) (currentPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f) else 0f
                            Slider(
                                value = sliderPos,
                                onValueChange = { frac ->
                                    isDraggingSlider = true
                                    currentPositionMs = (frac * totalDurationMs).toLong()
                                },
                                onValueChangeFinished = {
                                    isDraggingSlider = false
                                    if (currentTrack.audioUrl.isNotBlank()) {
                                        radioPlayer.seekTo(currentPositionMs)
                                    }
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = RedPrimary,
                                    activeTrackColor = RedPrimary,
                                    inactiveTrackColor = RedPrimary.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.fillMaxWidth().height(24.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = formatAudioTime(currentPositionMs),
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (currentTrack.audioUrl.isNotBlank()) formatAudioTime(totalDurationMs) else currentTrack.durationText,
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Bảng điều khiển phát thanh
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Tốc độ phát
                                TextButton(
                                    onClick = {
                                        playbackSpeed = when (playbackSpeed) {
                                            1.0f -> 1.25f
                                            1.25f -> 1.5f
                                            else -> 1.0f
                                        }
                                        if (currentTrack.audioUrl.isNotBlank()) {
                                            radioPlayer.setPlaybackSpeed(playbackSpeed)
                                        }
                                    }
                                ) {
                                    Text("${playbackSpeed}x", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NavySecondary)
                                }

                                // Tua lùi 15 giây
                                IconButton(
                                    onClick = {
                                        val newPos = (currentPositionMs - 15000L).coerceAtLeast(0L)
                                        currentPositionMs = newPos
                                        if (currentTrack.audioUrl.isNotBlank()) {
                                            radioPlayer.seekTo(newPos)
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Replay10, contentDescription = "Lùi 15s", tint = NavySecondary)
                                }

                                // Nút Play / Pause tròn nổi bật
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(RedPrimary, Color(0xFFC62828))))
                                        .clickable { togglePlayPause() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "Tạm dừng" else "Phát",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                // Tua tới 15 giây
                                IconButton(
                                    onClick = {
                                        val newPos = (currentPositionMs + 15000L).coerceAtMost(totalDurationMs)
                                        currentPositionMs = newPos
                                        if (currentTrack.audioUrl.isNotBlank()) {
                                            radioPlayer.seekTo(newPos)
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Forward10, contentDescription = "Tới 15s", tint = NavySecondary)
                                }

                                // Làm mới/phát lại từ đầu
                                IconButton(
                                    onClick = {
                                        currentPositionMs = 0L
                                        if (currentTrack.audioUrl.isNotBlank()) {
                                            radioPlayer.seekTo(0L)
                                            radioPlayer.play()
                                            isPlaying = true
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Phát lại", tint = NavySecondary)
                                }
                            }
                        }
                    }
                }

                // KHUNG TÌM KIẾM & PHÂN LOẠI
                Column(modifier = Modifier.padding(horizontal = 14.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        placeholder = { Text("Tìm kiếm chương trình phát thanh...", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = RedPrimary, modifier = Modifier.size(20.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Xóa", modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RedPrimary,
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Category Filter Scroll
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { cat ->
                            val isSelected = selectedCategoryFilter == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategoryFilter = cat },
                                label = { Text(cat, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RedPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = Color.White,
                                    labelColor = MaterialTheme.colorScheme.onSurface
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isSelected) RedPrimary else Color(0xFFDDDDDD)
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // DANH SÁCH BẢN TIN PHÁT THANH
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(filteredList) { index, item ->
                        val isCurrent = selectedBroadcast?.id == item.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isCurrent) {
                                        togglePlayPause()
                                    } else {
                                        playTrack(item)
                                    }
                                },
                            shape = RoundedCornerShape(14.dp),
                            elevation = CardDefaults.cardElevation(if (isCurrent) 3.dp else 1.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) Color(0xFFF0F7F9) else Color.White
                            ),
                            border = if (isCurrent) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00838F)) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Số thứ tự / Trạng thái đang phát
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isCurrent && isPlaying) Color(0xFF00838F)
                                            else if (isCurrent) RedPrimary
                                            else Color(0xFFECEFF1)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isCurrent && isPlaying) {
                                        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    } else if (isCurrent) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    } else {
                                        Text(
                                            text = "${index + 1}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = item.category,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF00838F)
                                        )
                                        Text(
                                            text = "• ${item.dateText}",
                                            fontSize = 10.5.sp,
                                            color = Color.Gray
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = item.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isCurrent) Color(0xFF00838F) else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (item.description.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = item.description,
                                            fontSize = 11.sp,
                                            color = Color.Gray,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = item.durationText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.Gray
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    IconButton(
                                        onClick = {
                                            if (isCurrent) togglePlayPause() else playTrack(item)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isCurrent && isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                            contentDescription = "Phát",
                                            tint = if (isCurrent) Color(0xFF00838F) else RedPrimary,
                                            modifier = Modifier.size(28.dp)
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

private fun formatAudioTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
