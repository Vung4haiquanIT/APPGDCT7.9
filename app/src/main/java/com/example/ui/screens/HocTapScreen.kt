package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Course
import com.example.model.Lesson
import com.example.model.ProgressDoc
import com.example.ui.components.Vung4LogoBadge
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.RedPrimary
import com.example.ui.theme.NavySecondary
import com.example.viewmodel.AppViewModel
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun HocTapScreen(
    viewModel: AppViewModel,
    initialCategory: String? = null,
    onBack: (() -> Unit)? = null
) {
    val courses by viewModel.courses.collectAsState()
    val lessons by viewModel.lessons.collectAsState()
    val progressList by viewModel.progressList.collectAsState()
    
    var activeLessonForPlayer by remember { mutableStateOf<Lesson?>(null) }

    if (activeLessonForPlayer != null) {
        LessonPlayerScreen(
            lesson = activeLessonForPlayer!!,
            viewModel = viewModel,
            onBack = { activeLessonForPlayer = null }
        )
    } else {
        HocTapContent(
            courses = courses,
            lessons = lessons,
            progressList = progressList,
            initialCategory = initialCategory,
            onBack = onBack,
            onLessonSelect = { activeLessonForPlayer = it }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HocTapContent(
    courses: List<Course>,
    lessons: List<Lesson>,
    progressList: List<ProgressDoc>,
    initialCategory: String? = null,
    onBack: (() -> Unit)? = null,
    onLessonSelect: (Lesson) -> Unit
) {
    // Category or Course filter state
    var selectedFilterKey by remember(initialCategory) {
        mutableStateOf(
            when (initialCategory?.lowercase()?.trim()) {
                "gdct", "cat_gdct" -> "CAT_GDCT"
                "gdpl", "cat_gdpl" -> "CAT_GDPL"
                "tu_sach_phap_luat", "tusachphapluat", "tu_sach", "tu_sach_pl", "tusach", "cat_tusachpl" -> "CAT_TUSACHPL"
                "lich_su", "lichsu", "truyenthong", "cat_lichsu" -> "CAT_LICHSU"
                "bien_dao", "biendao", "cat_biendao" -> "CAT_BIENDAO"
                null, "" -> "ALL"
                else -> {
                    if (initialCategory.startsWith("COURSE_")) initialCategory
                    else if (courses.any { it.id == initialCategory }) "COURSE_$initialCategory"
                    else "ALL"
                }
            }
        )
    }

    LaunchedEffect(initialCategory) {
        if (!initialCategory.isNullOrBlank()) {
            val targetKey = when (initialCategory.lowercase().trim()) {
                "gdct", "cat_gdct" -> "CAT_GDCT"
                "gdpl", "cat_gdpl" -> "CAT_GDPL"
                "tu_sach_phap_luat", "tusachphapluat", "tu_sach", "tu_sach_pl", "tusach", "cat_tusachpl" -> "CAT_TUSACHPL"
                "lich_su", "lichsu", "truyenthong", "cat_lichsu" -> "CAT_LICHSU"
                "bien_dao", "biendao", "cat_biendao" -> "CAT_BIENDAO"
                else -> {
                    if (initialCategory.startsWith("COURSE_")) initialCategory
                    else if (courses.any { it.id == initialCategory }) "COURSE_$initialCategory"
                    else "ALL"
                }
            }
            selectedFilterKey = targetKey
        }
    }

    var searchQuery by remember { mutableStateOf("") }

    // Nếu người dùng đang tìm kiếm, phím Back trên thanh điều hướng sẽ xoá tìm kiếm trước
    BackHandler(enabled = searchQuery.isNotEmpty()) {
        searchQuery = ""
    }

    // Map course id to course title for easy filtering
    val courseMap = remember(courses) { courses.associateBy { it.id } }

    // Filter lessons based on selected filter and search query
    val filteredLessons = remember(lessons, selectedFilterKey, searchQuery, courses) {
        lessons.filter { lesson ->
            val course = courseMap[lesson.courseId]
            val courseTitle = course?.title ?: ""
            val lessonTitle = lesson.title
            val lessonDesc = lesson.description

            val matchesFilter = when {
                selectedFilterKey == "ALL" -> true
                selectedFilterKey.startsWith("COURSE_") -> {
                    val targetCourseId = selectedFilterKey.removePrefix("COURSE_")
                    lesson.courseId == targetCourseId
                }
                selectedFilterKey == "CAT_GDCT" -> {
                    lesson.category.contains("gdct", ignoreCase = true) ||
                    lesson.category.contains("chinh_tri", ignoreCase = true) ||
                    lesson.category.contains("chính trị", ignoreCase = true) ||
                    course?.category?.contains("gdct", ignoreCase = true) == true ||
                    course?.category?.contains("chinh_tri", ignoreCase = true) == true ||
                    course?.category?.contains("chính trị", ignoreCase = true) == true ||
                    courseTitle.contains("chính trị", ignoreCase = true) ||
                    courseTitle.contains("gdct", ignoreCase = true) ||
                    courseTitle.contains("chính luận", ignoreCase = true) ||
                    lessonTitle.contains("chính trị", ignoreCase = true) ||
                    lessonTitle.contains("gdct", ignoreCase = true) ||
                    lessonTitle.contains("tư tưởng", ignoreCase = true) ||
                    lessonTitle.contains("nghị quyết", ignoreCase = true) ||
                    lessonTitle.contains("đảng", ignoreCase = true) ||
                    lessonTitle.contains("lý tưởng", ignoreCase = true) ||
                    lessonDesc.contains("chính trị", ignoreCase = true) ||
                    lessonDesc.contains("tư tưởng", ignoreCase = true) ||
                    (course == null && !lessonTitle.contains("pháp luật", ignoreCase = true) && !lessonTitle.contains("luật", ignoreCase = true) && !lessonTitle.contains("lịch sử", ignoreCase = true) && !lessonTitle.contains("biển đảo", ignoreCase = true))
                }
                selectedFilterKey == "CAT_GDPL" -> {
                    lesson.category.contains("gdpl", ignoreCase = true) ||
                    lesson.category.contains("phap_luat", ignoreCase = true) ||
                    lesson.category.contains("pháp luật", ignoreCase = true) ||
                    course?.category?.contains("gdpl", ignoreCase = true) == true ||
                    course?.category?.contains("phap_luat", ignoreCase = true) == true ||
                    course?.category?.contains("pháp luật", ignoreCase = true) == true ||
                    courseTitle.contains("pháp luật", ignoreCase = true) ||
                    courseTitle.contains("gdpl", ignoreCase = true) ||
                    courseTitle.contains("luật", ignoreCase = true) ||
                    lessonTitle.contains("pháp luật", ignoreCase = true) ||
                    lessonTitle.contains("gdpl", ignoreCase = true) ||
                    lessonTitle.contains("luật", ignoreCase = true) ||
                    lessonTitle.contains("kỷ luật", ignoreCase = true) ||
                    lessonTitle.contains("quy định", ignoreCase = true) ||
                    lessonTitle.contains("nghị định", ignoreCase = true) ||
                    lessonTitle.contains("thông tư", ignoreCase = true) ||
                    lessonTitle.contains("an toàn", ignoreCase = true) ||
                    lessonDesc.contains("pháp luật", ignoreCase = true) ||
                    lessonDesc.contains("kỷ luật", ignoreCase = true)
                }
                selectedFilterKey == "CAT_TUSACHPL" -> {
                    lesson.category.contains("tu_sach", ignoreCase = true) ||
                    lesson.category.contains("tusach", ignoreCase = true) ||
                    lesson.category.contains("tủ sách", ignoreCase = true) ||
                    course?.category?.contains("tu_sach", ignoreCase = true) == true ||
                    course?.category?.contains("tủ sách", ignoreCase = true) == true ||
                    courseTitle.contains("tủ sách", ignoreCase = true) ||
                    courseTitle.contains("sách pháp luật", ignoreCase = true) ||
                    courseTitle.contains("văn bản pháp luật", ignoreCase = true) ||
                    lessonTitle.contains("tủ sách", ignoreCase = true) ||
                    lessonTitle.contains("sách pháp luật", ignoreCase = true) ||
                    lessonTitle.contains("sổ tay", ignoreCase = true) ||
                    lessonTitle.contains("bộ luật", ignoreCase = true) ||
                    lessonTitle.contains("văn bản", ignoreCase = true) ||
                    lessonTitle.contains("hướng dẫn", ignoreCase = true) ||
                    lessonDesc.contains("tủ sách", ignoreCase = true) ||
                    lessonDesc.contains("sách pháp luật", ignoreCase = true) ||
                    lessonDesc.contains("văn bản pháp luật", ignoreCase = true) ||
                    // If no explicit category match yet, match law-related lessons
                    (course == null && (lessonTitle.contains("luật", ignoreCase = true) || lessonTitle.contains("pháp luật", ignoreCase = true) || lessonTitle.contains("nghị định", ignoreCase = true) || lessonTitle.contains("thông tư", ignoreCase = true)))
                }
                selectedFilterKey == "CAT_LICHSU" -> {
                    lesson.category.contains("lich_su", ignoreCase = true) ||
                    lesson.category.contains("truyen_thong", ignoreCase = true) ||
                    lesson.category.contains("lịch sử", ignoreCase = true) ||
                    lesson.category.contains("truyền thống", ignoreCase = true) ||
                    course?.category?.contains("lich_su", ignoreCase = true) == true ||
                    course?.category?.contains("truyền thống", ignoreCase = true) == true ||
                    courseTitle.contains("lịch sử", ignoreCase = true) ||
                    courseTitle.contains("truyền thống", ignoreCase = true) ||
                    courseTitle.contains("tiền thân", ignoreCase = true) ||
                    lessonTitle.contains("lịch sử", ignoreCase = true) ||
                    lessonTitle.contains("truyền thống", ignoreCase = true) ||
                    lessonTitle.contains("chiến công", ignoreCase = true) ||
                    lessonTitle.contains("đánh thắng", ignoreCase = true) ||
                    lessonTitle.contains("đoàn tàu không số", ignoreCase = true) ||
                    lessonTitle.contains("đường hồ chí minh", ignoreCase = true) ||
                    lessonTitle.contains("anh hùng", ignoreCase = true) ||
                    lessonDesc.contains("lịch sử", ignoreCase = true) ||
                    lessonDesc.contains("truyền thống", ignoreCase = true)
                }
                selectedFilterKey == "CAT_BIENDAO" -> {
                    lesson.category.contains("bien_dao", ignoreCase = true) ||
                    lesson.category.contains("biển đảo", ignoreCase = true) ||
                    lesson.category.contains("hai_dao", ignoreCase = true) ||
                    course?.category?.contains("bien_dao", ignoreCase = true) == true ||
                    course?.category?.contains("biển đảo", ignoreCase = true) == true ||
                    courseTitle.contains("biển đảo", ignoreCase = true) ||
                    courseTitle.contains("biển đông", ignoreCase = true) ||
                    courseTitle.contains("trường sa", ignoreCase = true) ||
                    courseTitle.contains("hoàng sa", ignoreCase = true) ||
                    courseTitle.contains("chủ quyền", ignoreCase = true) ||
                    courseTitle.contains("nhà giàn", ignoreCase = true) ||
                    lessonTitle.contains("biển đảo", ignoreCase = true) ||
                    lessonTitle.contains("biển đông", ignoreCase = true) ||
                    lessonTitle.contains("trường sa", ignoreCase = true) ||
                    lessonTitle.contains("hoàng sa", ignoreCase = true) ||
                    lessonTitle.contains("chủ quyền", ignoreCase = true) ||
                    lessonTitle.contains("cam ranh", ignoreCase = true) ||
                    lessonTitle.contains("nhà giàn", ignoreCase = true) ||
                    lessonTitle.contains("hải đảo", ignoreCase = true) ||
                    lessonDesc.contains("biển đảo", ignoreCase = true) ||
                    lessonDesc.contains("chủ quyền biển", ignoreCase = true)
                }
                else -> true
            }

            val matchesSearch = searchQuery.isBlank() ||
                lessonTitle.contains(searchQuery, ignoreCase = true) ||
                lessonDesc.contains(searchQuery, ignoreCase = true) ||
                courseTitle.contains(searchQuery, ignoreCase = true)

            matchesFilter && matchesSearch
        }.sortedWith(
            compareByDescending<Lesson> { it.createdAt.coerceAtLeast(it.updatedAt) }
                .thenByDescending { it.id }
        )
    }

    // Helper function to check if a course belongs to fixed categories
    fun isFixedCategoryCourse(title: String, category: String): Boolean {
        val t = title.lowercase()
        val c = category.lowercase()
        return t.contains("chính trị") || t.contains("gdct") || t.contains("chính luận") ||
               c.contains("gdct") || c.contains("chinh_tri") || c.contains("chính trị") ||
               t.contains("pháp luật") || t.contains("gdpl") || t.contains("kỷ luật") ||
               c.contains("gdpl") || c.contains("phap_luat") || c.contains("pháp luật") ||
               t.contains("tủ sách") || t.contains("sách pháp luật") ||
               c.contains("tu_sach") || c.contains("tủ sách") ||
               t.contains("lịch sử") || t.contains("truyền thống") ||
               c.contains("lich_su") || c.contains("truyền thống") ||
               t.contains("biển đảo") || t.contains("trường sa") || t.contains("hoàng sa") || t.contains("chủ quyền") ||
               c.contains("bien_dao") || c.contains("biển đảo")
    }

    // List of filter options following Web Quản trị fixed order + dynamic extra courses
    val filterTabs = remember(courses, lessons) {
        val list = mutableListOf(
            "ALL" to "Tất cả (${lessons.size})",
            "CAT_GDCT" to "GD Chính trị (GDCT)",
            "CAT_GDPL" to "GD Pháp luật (GDPL)",
            "CAT_TUSACHPL" to "Tủ sách Pháp luật",
            "CAT_LICHSU" to "Lịch sử Truyền thống",
            "CAT_BIENDAO" to "Biển đảo Việt Nam"
        )
        // Add extra courses if any exist from Web Quản trị that don't duplicate fixed categories
        courses.filter { !isFixedCategoryCourse(it.title, it.category) }.forEach { course ->
            val key = "COURSE_${course.id}"
            if (list.none { it.first == key }) {
                list.add(key to (course.title.ifEmpty { "Chuyên đề bổ sung" }))
            }
        }
        list
    }

    // State for scrolling tab row
    val tabRowState = rememberLazyListState()

    // Smooth scroll to selected category tab whenever key changes
    LaunchedEffect(selectedFilterKey, filterTabs) {
        val index = filterTabs.indexOfFirst { it.first == selectedFilterKey }
        if (index >= 0) {
            tabRowState.animateScrollToItem(index)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Vung4LogoBadge(size = 32.dp)
                        Text(
                            text = "HỌC TẬP CHUYÊN ĐỀ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = MaterialTheme.colorScheme.onPrimary)
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
        ) {
            // Search Bar & Filter Tabs
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Tìm kiếm bài học, chuyên đề...", fontSize = 13.sp) },
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

                    // Scrollable Category Tabs
                    LazyRow(
                        state = tabRowState,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(filterTabs) { _, (key, label) ->
                            val isSelected = selectedFilterKey == key
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedFilterKey = key },
                                label = {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RedPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // Lessons List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Danh sách bài học (${filteredLessons.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                if (filteredLessons.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.MenuBook,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = RedPrimary
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Không có bài học nào trong chuyên đề này.",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Vui lòng chọn chuyên đề khác hoặc đăng tải bài học từ Web Quản Trị.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(filteredLessons) { lesson ->
                        val progress = progressList.find { it.lessonId == lesson.id }
                        val isCompleted = progress?.completed == true
                        val hasScore = progress?.score != null && progress.totalQuestions != null

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onLessonSelect(lesson) },
                            shape = RoundedCornerShape(14.dp),
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
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isCompleted) Color(0xFFE8F5E9) else RedPrimary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.PlayCircle,
                                        contentDescription = null,
                                        tint = if (isCompleted) Color(0xFF2E7D32) else RedPrimary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    val parentCourse = courseMap[lesson.courseId]
                                    if (parentCourse != null && parentCourse.title.isNotEmpty()) {
                                        Text(
                                            text = parentCourse.title,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = RedPrimary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                    }

                                    Text(
                                        text = lesson.title.ifEmpty { "Bài học chính trị" },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        lineHeight = 19.sp,
                                        maxLines = 2,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    if (hasScore || isCompleted) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (isCompleted) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0xFFC8E6C9)
                                                ) {
                                                    Text(
                                                        text = "Đã hoàn thành",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF1B5E20),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
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
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HocTapScreenPreview() {
    val sampleCourses = listOf(
        Course(id = "c1", title = "Giáo dục chính trị cơ bản", category = "gdct"),
        Course(id = "c2", title = "Luật biển Việt Nam 2012", category = "gdpl"),
        Course(id = "c3", title = "Lịch sử Hải quân Vùng 4", category = "lichsu")
    )
    val sampleLessons = listOf(
        Lesson(id = "l1", courseId = "c1", title = "Bài 1: Tư tưởng Hồ Chí Minh về bảo vệ Tổ quốc", category = "gdct", description = "Tìm hiểu về tư tưởng Bác Hồ..."),
        Lesson(id = "l2", courseId = "c2", title = "Bài 2: Quy định về đường cơ sở và lãnh hải", category = "gdpl", description = "Các quy định pháp lý về biển..."),
        Lesson(id = "l3", courseId = "c3", title = "Bài 3: Truyền thống Đoàn tàu Không số", category = "lichsu", description = "Lịch sử đường Hồ Chí Minh trên biển...")
    )
    val sampleProgress = listOf(
        ProgressDoc(lessonId = "l1", completed = true, score = 10, totalQuestions = 10, scorePercentage = 100),
        ProgressDoc(lessonId = "l2", completed = false, score = 7, totalQuestions = 10, scorePercentage = 70)
    )

    MyApplicationTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            HocTapContent(
                courses = sampleCourses,
                lessons = sampleLessons,
                progressList = sampleProgress,
                onLessonSelect = {}
            )
        }
    }
}
