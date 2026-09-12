package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

data class Course(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val status: String = "active",
    val version: Long = 1L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val imageUrl: String = "",
    val category: String = ""
) {
    companion object {
        fun fromDoc(doc: DocumentSnapshot): Course {
            return Course(
                id = doc.id,
                title = cleanHtml(doc.getString("title") ?: ""),
                description = cleanHtml(doc.getString("description") ?: ""),
                status = doc.getString("status") ?: "active",
                version = parseLong(doc.get("version")),
                createdAt = parseTime(doc.get("createdAt")),
                updatedAt = parseTime(doc.get("updatedAt")),
                imageUrl = doc.getString("imageUrl") ?: doc.getString("image") ?: "",
                category = doc.getString("category") ?: doc.getString("chuyenDe") ?: doc.getString("type") ?: ""
            )
        }
    }
}

data class Lesson(
    val id: String = "",
    val courseId: String = "",
    val title: String = "",
    val description: String = "",
    val status: String = "active",
    val version: Long = 1L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val category: String = ""
) {
    companion object {
        fun fromDoc(doc: DocumentSnapshot): Lesson {
            return Lesson(
                id = doc.id,
                courseId = doc.getString("courseId") ?: doc.getString("chuyenDeId") ?: "",
                title = cleanHtml(doc.getString("title") ?: ""),
                description = cleanHtml(doc.getString("description") ?: ""),
                status = doc.getString("status") ?: "active",
                version = parseLong(doc.get("version")),
                createdAt = parseTime(doc.get("createdAt")),
                updatedAt = parseTime(doc.get("updatedAt")),
                category = doc.getString("category") ?: doc.getString("chuyenDe") ?: doc.getString("type") ?: ""
            )
        }
    }
}

data class ContentItem(
    val id: String = "",
    val lessonId: String = "",
    val order: Int = 1,
    val title: String = "",
    val bodyHtml: String = "",
    val version: Long = 1L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun fromDoc(doc: DocumentSnapshot): ContentItem {
            return ContentItem(
                id = doc.id,
                lessonId = doc.getString("lessonId") ?: doc.getString("baiHocId") ?: "",
                order = parseInt(doc.get("order")),
                title = cleanHtml(doc.getString("title") ?: ""),
                bodyHtml = cleanBodyHtml(doc.getString("bodyHtml") ?: doc.getString("content") ?: doc.getString("body") ?: ""),
                version = parseLong(doc.get("version")),
                createdAt = parseTime(doc.get("createdAt")),
                updatedAt = parseTime(doc.get("updatedAt"))
            )
        }
    }
}

data class SlideItem(
    val id: String = "",
    val lessonId: String = "",
    val order: Int = 1,
    val imageUrl: String = "",
    val version: Long = 1L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun fromDoc(doc: DocumentSnapshot): SlideItem {
            return SlideItem(
                id = doc.id,
                lessonId = doc.getString("lessonId") ?: doc.getString("baiHocId") ?: "",
                order = parseInt(doc.get("order")),
                imageUrl = doc.getString("imageUrl") ?: doc.getString("url") ?: "",
                version = parseLong(doc.get("version")),
                createdAt = parseTime(doc.get("createdAt")),
                updatedAt = parseTime(doc.get("updatedAt"))
            )
        }
    }
}

data class VideoItem(
    val id: String = "",
    val lessonId: String = "",
    val title: String = "",
    val videoUrl: String = "",
    val version: Long = 1L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun fromDoc(doc: DocumentSnapshot): VideoItem {
            return VideoItem(
                id = doc.id,
                lessonId = doc.getString("lessonId") ?: doc.getString("baiHocId") ?: "",
                title = doc.getString("title") ?: "",
                videoUrl = doc.getString("videoUrl") ?: doc.getString("url") ?: "",
                version = parseLong(doc.get("version")),
                createdAt = parseTime(doc.get("createdAt")),
                updatedAt = parseTime(doc.get("updatedAt"))
            )
        }
    }
}

data class AudioItem(
    val id: String = "",
    val lessonId: String = "",
    val title: String = "",
    val audioUrl: String = "",
    val version: Long = 1L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun fromDoc(doc: DocumentSnapshot): AudioItem {
            return AudioItem(
                id = doc.id,
                lessonId = doc.getString("lessonId") ?: doc.getString("baiHocId") ?: "",
                title = doc.getString("title") ?: "",
                audioUrl = doc.getString("audioUrl") ?: doc.getString("url") ?: "",
                version = parseLong(doc.get("version")),
                createdAt = parseTime(doc.get("createdAt")),
                updatedAt = parseTime(doc.get("updatedAt"))
            )
        }
    }
}

data class StorageFileItem(
    val id: String = "",
    val category: String = "",
    val entityId: String = "",
    val lessonId: String = "",
    val title: String = "",
    val fileName: String = "",
    val storagePath: String = "",
    val downloadUrl: String = "",
    val version: Long = 1L,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun fromDoc(doc: DocumentSnapshot): StorageFileItem {
            val entityId = doc.getString("entityId") ?: doc.getString("lessonId") ?: doc.getString("baiHocId") ?: ""
            val lessonId = doc.getString("lessonId") ?: doc.getString("entityId") ?: doc.getString("baiHocId") ?: ""
            val downloadUrl = doc.getString("downloadUrl") ?: doc.getString("cloudinaryUrl") ?: doc.getString("url") ?: doc.getString("fileUrl") ?: ""
            val title = doc.getString("title") ?: doc.getString("fileName") ?: doc.getString("name") ?: "Tài liệu đính kèm"
            val fileName = doc.getString("fileName") ?: doc.getString("title") ?: doc.getString("name") ?: ""
            return StorageFileItem(
                id = doc.id,
                category = doc.getString("category") ?: "",
                entityId = entityId,
                lessonId = lessonId,
                title = title,
                fileName = fileName,
                storagePath = doc.getString("storagePath") ?: "",
                downloadUrl = downloadUrl,
                version = parseLong(doc.get("version")),
                createdAt = parseTime(doc.get("createdAt"))
            )
        }
    }
}

data class UserDoc(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "Học viên",
    val unit: String = "Vùng 4 Hải Quân",
    val rank: String = "",
    val phone: String = "",
    val avatarUrl: String = "",
    val permissions: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun fromDoc(doc: DocumentSnapshot): UserDoc {
            val rawPerms = doc.get("permissions")
            val permsList = when (rawPerms) {
                is List<*> -> rawPerms.mapNotNull { it?.toString() }
                else -> emptyList()
            }
            return UserDoc(
                id = doc.id,
                name = doc.getString("name") ?: doc.getString("displayName") ?: doc.getString("fullName") ?: "",
                email = doc.getString("email") ?: doc.getString("username") ?: "",
                role = doc.getString("role") ?: doc.getString("userType") ?: "Học viên",
                unit = doc.getString("unit") ?: doc.getString("donVi") ?: "Vùng 4 Hải Quân",
                rank = doc.getString("rank") ?: doc.getString("capBac") ?: doc.getString("chucVu") ?: "",
                phone = doc.getString("phone") ?: doc.getString("soDienThoai") ?: "",
                avatarUrl = doc.getString("avatarUrl") ?: doc.getString("avatar") ?: doc.getString("photoUrl") ?: doc.getString("hinhDaiDien") ?: "",
                permissions = permsList,
                createdAt = parseTime(doc.get("createdAt")),
                updatedAt = parseTime(doc.get("updatedAt"))
            )
        }
    }
}

data class ProgressDoc(
    val id: String = "",
    val userId: String = "",
    val lessonId: String = "",
    val completed: Boolean = false,
    val score: Int? = null,
    val totalQuestions: Int? = null,
    val scorePercentage: Int? = null,
    val viewedSlides: Boolean = false,
    val readContent: Boolean = false,
    val passedQuiz: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun fromDoc(doc: DocumentSnapshot): ProgressDoc {
            val rawScore = doc.get("score") ?: doc.get("diem") ?: doc.get("correctAnswers")
            val scoreVal = when (rawScore) {
                is Number -> rawScore.toInt()
                is String -> rawScore.toIntOrNull()
                else -> null
            }
            val rawTotal = doc.get("totalQuestions") ?: doc.get("tongSoCau")
            val totalVal = when (rawTotal) {
                is Number -> rawTotal.toInt()
                is String -> rawTotal.toIntOrNull()
                else -> null
            }
            val rawPercent = doc.get("scorePercentage") ?: doc.get("phanTramDiem")
            val percentVal = when (rawPercent) {
                is Number -> rawPercent.toInt()
                is String -> rawPercent.toIntOrNull()
                else -> if (scoreVal != null && totalVal != null && totalVal > 0) (scoreVal * 100 / totalVal) else null
            }

            return ProgressDoc(
                id = doc.id,
                userId = doc.getString("userId") ?: doc.getString("user_id") ?: doc.getString("nguoiDungId") ?: "",
                lessonId = doc.getString("lessonId") ?: doc.getString("baiHocId") ?: doc.getString("lesson_id") ?: "",
                completed = parseBoolean(doc.get("completed") ?: doc.get("hoanThanh") ?: doc.get("isCompleted")),
                score = scoreVal,
                totalQuestions = totalVal,
                scorePercentage = percentVal,
                viewedSlides = parseBoolean(doc.get("viewedSlides") ?: doc.get("daXemSlide")),
                readContent = parseBoolean(doc.get("readContent") ?: doc.get("daDocNoiDung")),
                passedQuiz = parseBoolean(doc.get("passedQuiz") ?: doc.get("passed") ?: doc.get("daDat")),
                updatedAt = parseTime(doc.get("updatedAt") ?: doc.get("thoiGianHoanThanh"))
            )
        }
    }
}

data class NotificationItem(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "admin", // "admin", "reminder", "system"
    val targetLessonId: String? = null,
    val targetCourseId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val priority: String = "normal" // "urgent", "high", "normal"
) {
    companion object {
        fun fromDoc(doc: DocumentSnapshot): NotificationItem {
            return NotificationItem(
                id = doc.id,
                title = cleanHtml(doc.getString("title") ?: doc.getString("tieuDe") ?: "Thông báo từ Web Quản trị"),
                message = cleanHtml(doc.getString("message") ?: doc.getString("content") ?: doc.getString("noiDung") ?: ""),
                type = doc.getString("type") ?: doc.getString("loai") ?: "admin",
                targetLessonId = doc.getString("targetLessonId") ?: doc.getString("lessonId"),
                targetCourseId = doc.getString("targetCourseId") ?: doc.getString("courseId"),
                timestamp = parseTime(doc.get("timestamp") ?: doc.get("createdAt") ?: doc.get("date")),
                isRead = doc.getBoolean("isRead") ?: false,
                priority = doc.getString("priority") ?: doc.getString("mucDo") ?: "normal"
            )
        }
    }
}

data class BannerItem(
    val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val imageUrl: String = "",
    val linkUrl: String = "",
    val targetLessonId: String? = null,
    val order: Int = 0,
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun fromDoc(doc: DocumentSnapshot): BannerItem {
            val imgUrl = doc.getString("imageUrl") 
                ?: doc.getString("image") 
                ?: doc.getString("hinhAnh") 
                ?: doc.getString("url") 
                ?: doc.getString("posterUrl")
                ?: doc.getString("bannerUrl")
                ?: doc.getString("photoUrl")
                ?: doc.getString("fileUrl")
                ?: doc.getString("src")
                ?: ""
            return BannerItem(
                id = doc.id,
                title = cleanHtml(doc.getString("title") ?: doc.getString("tieuDe") ?: doc.getString("name") ?: ""),
                subtitle = cleanHtml(doc.getString("subtitle") ?: doc.getString("subTitle") ?: doc.getString("moTa") ?: doc.getString("content") ?: ""),
                imageUrl = imgUrl,
                linkUrl = doc.getString("linkUrl") ?: doc.getString("link") ?: "",
                targetLessonId = doc.getString("targetLessonId") ?: doc.getString("lessonId") ?: doc.getString("baiHocId"),
                order = (doc.getLong("order") ?: doc.getLong("thuTu") ?: doc.getLong("viTri") ?: 0L).toInt(),
                active = doc.getBoolean("active") ?: doc.getBoolean("isActive") ?: doc.getBoolean("hienThi") ?: true,
                createdAt = parseTime(doc.get("createdAt") ?: doc.get("timestamp") ?: doc.get("thoiGianTao"))
            )
        }

        fun getDefaultMilitaryBanners(): List<BannerItem> {
            return listOf(
                BannerItem(
                    id = "default_banner_1",
                    title = "HỌC TẬP, RÈN LUYỆN\nVÌ LÝ TƯỞNG CỘNG SẢN",
                    subtitle = "Kiên định mục tiêu độc lập dân tộc\nvà chủ nghĩa xã hội",
                    order = 1
                ),
                BannerItem(
                    id = "default_banner_2",
                    title = "PHÁT HUY TRUYỀN THỐNG\nĐOÀN KẾT, KỶ CƯƠNG",
                    subtitle = "Chiến sĩ Hải quân Vùng 4 tinh nhuệ,\nchính quy, hiện đại, sẵn sàng chiến đấu",
                    order = 2
                ),
                BannerItem(
                    id = "default_banner_3",
                    title = "QUYẾT TÂM BẢO VỆ\nVỮNG CHẮC BIỂN ĐẢO",
                    subtitle = "Mỗi con tàu, hòn đảo là một cột mốc\nchủ quyền thiêng liêng của Tổ quốc",
                    order = 3
                ),
                BannerItem(
                    id = "default_banner_4",
                    title = "HỌC TẬP VÀ LÀM THEO\nTƯ TƯỞNG, ĐẠO ĐỨC BÁC HỒ",
                    subtitle = "Cần, kiệm, liêm, chính, chí công vô tư\ntrong mọi nhiệm vụ công tác huấn luyện",
                    order = 4
                ),
                BannerItem(
                    id = "default_banner_5",
                    title = "CHỦ ĐỘNG, SÁNG TẠO\nHUẤN LUYỆN CHIẾN ĐẤU GIỎI",
                    subtitle = "Kỷ luật nghiêm minh, sẵn sàng nhận\nvà hoàn thành xuất sắc mọi nhiệm vụ",
                    order = 5
                )
            )
        }
    }
}


data class QuestionItem(
    val id: String = "",
    val lessonId: String = "",
    val courseId: String = "",
    val category: String = "GDCT", // "GDCT", "GDPL", "LICHSU", "BIENDAO", "DIEULENH"
    val categoryName: String = "Giáo dục chính trị",
    val question: String = "",
    val options: List<String> = emptyList(),
    val correctIndex: Int = 0,
    val explanation: String = ""
) {
    /**
     * Đảo ngẫu nhiên thứ tự các đáp án trong câu hỏi, đồng thời tự động cập nhật
     * lại correctIndex tương ứng với đáp án đúng để chấm điểm hoàn toàn chính xác.
     */
    fun withShuffledOptions(seed: Long = System.currentTimeMillis()): QuestionItem {
        if (options.size <= 1) return this
        // Chuẩn hóa loại bỏ tiền tố A. B. C. D. nếu người dùng lỡ nhập vào
        val cleanedOptions = options.map { opt ->
            opt.replace(Regex("^[A-Da-d][\\.\\)]\\s*"), "").trim()
        }
        val correctOptionText = cleanedOptions.getOrNull(correctIndex) ?: ""
        val rnd = java.util.Random(seed)
        val indexed = cleanedOptions.mapIndexed { idx, txt -> idx to txt }
        val shuffledIndexed = indexed.shuffled(rnd)
        val newOptions = shuffledIndexed.map { it.second }
        val newCorrectIndex = if (correctOptionText.isNotBlank()) {
            val found = newOptions.indexOf(correctOptionText)
            if (found >= 0) found else correctIndex
        } else {
            val found = shuffledIndexed.indexOfFirst { it.first == correctIndex }
            if (found >= 0) found else 0
        }
        return copy(options = newOptions, correctIndex = newCorrectIndex)
    }

    companion object {
        fun fromDoc(doc: DocumentSnapshot): QuestionItem {
            val q = doc.getString("question") ?: doc.getString("cauHoi") ?: doc.getString("content") ?: doc.getString("title") ?: ""
            val rawOptions = doc.get("options") ?: doc.get("dapAn") ?: doc.get("choices") ?: doc.get("answers")
            val optList: List<String> = when (rawOptions) {
                is List<*> -> rawOptions.mapNotNull { it?.toString() }
                else -> {
                    val a = doc.getString("optionA") ?: doc.getString("dapAnA") ?: ""
                    val b = doc.getString("optionB") ?: doc.getString("dapAnB") ?: ""
                    val c = doc.getString("optionC") ?: doc.getString("dapAnC") ?: ""
                    val d = doc.getString("optionD") ?: doc.getString("dapAnD") ?: ""
                    if (a.isNotEmpty() || b.isNotEmpty()) listOf(a, b, c, d).filter { it.isNotEmpty() }
                    else emptyList()
                }
            }
            val rawCorrect = doc.get("correctIndex") ?: doc.get("correctAnswer") ?: doc.get("dapAnDung") ?: doc.get("correct")
            val cIndex = when (rawCorrect) {
                is Number -> rawCorrect.toInt()
                is String -> {
                    when (rawCorrect.trim().uppercase()) {
                        "A", "0" -> 0
                        "B", "1" -> 1
                        "C", "2" -> 2
                        "D", "3" -> 3
                        else -> rawCorrect.toIntOrNull() ?: 0
                    }
                }
                else -> 0
            }
            val cat = doc.getString("category") ?: doc.getString("chuyenDe") ?: doc.getString("loai") ?: "GDCT"
            return QuestionItem(
                id = doc.id,
                lessonId = doc.getString("lessonId") ?: doc.getString("baiHocId") ?: "",
                courseId = doc.getString("courseId") ?: doc.getString("chuyenDeId") ?: "",
                category = cat,
                categoryName = doc.getString("categoryName") ?: doc.getString("tenChuyenDe") ?: getCategoryDisplayName(cat),
                question = cleanHtml(q),
                options = optList.map { cleanHtml(it) },
                correctIndex = cIndex,
                explanation = cleanHtml(doc.getString("explanation") ?: doc.getString("giaiThich") ?: "")
            )
        }

        fun getCategoryDisplayName(cat: String): String {
            return when (cat.uppercase()) {
                "GDCT", "CHINHTRI" -> "Giáo dục chính trị"
                "GDPL", "PHAPLUAT" -> "Giáo dục pháp luật"
                "LICHSU", "LICH_SU", "TRUYENTHONG" -> "Lịch sử & Truyền thống"
                "BIENDAO", "BIEN_DAO" -> "Biển đảo Việt Nam"
                "DIEULENH" -> "Điều lệnh & Kỷ luật"
                else -> "Kiến thức chung"
            }
        }

        fun getDefaultQuestionBank(): List<QuestionItem> {
            return listOf(
                // ================= GDCT =================
                QuestionItem(
                    id = "bank_q01",
                    category = "GDCT",
                    categoryName = "Giáo dục chính trị",
                    question = "Mục tiêu lý tưởng chiến đấu cao cả của Quân đội nhân dân Việt Nam là gì?",
                    options = listOf(
                        "Độc lập dân tộc gắn liền với Chủ nghĩa xã hội, vì hạnh phúc của Nhân dân",
                        "Phát triển kinh tế và mở rộng quan hệ quốc tế",
                        "Xây dựng lực lượng quân đội vũ trang hiện đại thuần túy",
                        "Bảo đảm quốc phòng trong phạm vi đất liền"
                    ),
                    correctIndex = 0,
                    explanation = "Quân đội nhân dân Việt Nam mang bản chất giai cấp công nhân, chiến đấu vì độc lập dân tộc và chủ nghĩa xã hội, vì tự do, hạnh phúc của Nhân dân."
                ),
                QuestionItem(
                    id = "bank_q02",
                    category = "GDCT",
                    categoryName = "Giáo dục chính trị",
                    question = "Nguyên tắc cơ bản nhất trong sự lãnh đạo của Đảng đối với Quân đội nhân dân Việt Nam là gì?",
                    options = listOf(
                        "Tuyệt đối, trực tiếp về mọi mặt",
                        "Chỉ đạo gián tiếp qua cơ quan tham mưu",
                        "Phân cấp lãnh đạo độc lập theo từng quân khu",
                        "Tự quản lý theo chế độ chỉ huy đơn vị"
                    ),
                    correctIndex = 0,
                    explanation = "Đảng Cộng sản Việt Nam lãnh đạo Quân đội nhân dân Việt Nam tuyệt đối, trực tiếp về mọi mặt."
                ),
                QuestionItem(
                    id = "bank_q03",
                    category = "GDCT",
                    categoryName = "Giáo dục chính trị",
                    question = "Yếu tố nào giữ vai trò quyết định tạo nên sức mạnh chiến đấu của Quân đội ta theo tư tưởng Hồ Chí Minh?",
                    options = listOf(
                        "Chính trị - tinh thần",
                        "Vũ khí trang bị hiện đại",
                        "Số lượng quân số đông đảo",
                        "Kinh phí bảo đảm quốc phòng"
                    ),
                    correctIndex = 0,
                    explanation = "Chủ tịch Hồ Chí Minh khẳng định người trước súng sau, nhân tố chính trị - tinh thần là cội nguồn sức mạnh quyết định chiến thắng."
                ),
                QuestionItem(
                    id = "bank_q04",
                    category = "GDCT",
                    categoryName = "Giáo dục chính trị",
                    question = "Nội dung cuộc vận động 'Phát huy truyền thống, cống hiến tài năng, xứng danh Bộ đội Cụ Hồ - Người chiến sĩ Hải quân' nhấn mạnh phẩm chất gì?",
                    options = listOf(
                        "Kiên định bản lĩnh, tuyệt đối trung thành, đoàn kết kỷ cương, sẵn sàng chiến đấu hy sinh vì chủ quyền biển đảo",
                        "Phát triển kỹ năng tin học văn phòng",
                        "Đầu tư công nghệ tự động hóa",
                        "Thực hiện giao lưu văn hóa quốc tế"
                    ),
                    correctIndex = 0,
                    explanation = "Xứng danh 'Bộ đội Cụ Hồ - Người chiến sĩ Hải quân' thể hiện bản lĩnh chính trị vững vàng, ý chí quyết tâm bảo vệ vững chắc chủ quyền biển đảo Tổ quốc."
                ),
                QuestionItem(
                    id = "bank_q05",
                    category = "GDCT",
                    categoryName = "Giáo dục chính trị",
                    question = "Phương châm giáo dục chính trị tại đơn vị cơ sở gồm các yếu tố nào?",
                    options = listOf(
                        "Cơ bản, hệ thống, thống nhất, thực tiễn và hiệu quả",
                        "Lý thuyết chuyên sâu, hàn lâm",
                        "Thực hành nhanh, rút gọn nội dung",
                        "Tự học tự nghiên cứu không cần kiểm tra"
                    ),
                    correctIndex = 0,
                    explanation = "Công tác GDCT phải quán triệt phương châm 'Cơ bản, hệ thống, thống nhất, thực tiễn và hiệu quả'."
                ),
                QuestionItem(
                    id = "bank_q06",
                    category = "GDCT",
                    categoryName = "Giáo dục chính trị",
                    question = "Chiến lược bảo vệ Tổ quốc trong tình hình mới xác định mục tiêu trọng yếu nào?",
                    options = listOf(
                        "Bảo vệ vững chắc độc lập, chủ quyền, thống nhất, toàn vẹn lãnh thổ; bảo vệ Đảng, Nhà nước, Nhân dân và chế độ XHCN",
                        "Xây dựng liên minh quân sự với các cường quốc",
                        "Chuyển toàn bộ nền kinh tế sang thời chiến",
                        "Tăng cường trang bị vũ khí tấn công tầm xa"
                    ),
                    correctIndex = 0,
                    explanation = "Chiến lược bảo vệ Tổ quốc kiên định bảo vệ độc lập, chủ quyền, thống nhất, toàn vẹn lãnh thổ và giữ vững môi trường hòa bình, ổn định."
                ),

                // ================= GDPL =================
                QuestionItem(
                    id = "bank_q07",
                    category = "GDPL",
                    categoryName = "Giáo dục pháp luật",
                    question = "Theo Luật Biển Việt Nam năm 2012, chiều rộng lãnh hải của nước Cộng hòa XHCN Việt Nam là bao nhiêu hải lý?",
                    options = listOf(
                        "12 hải lý tính từ đường cơ sở",
                        "24 hải lý tính từ đường cơ sở",
                        "200 hải lý tính từ đất liền",
                        "6 hải lý tính từ bờ biển"
                    ),
                    correctIndex = 0,
                    explanation = "Điều 11 Luật Biển Việt Nam 2012 quy định: Lãnh hải của Việt Nam rộng 12 hải lý tính từ đường cơ sở ra phía ngoài."
                ),
                QuestionItem(
                    id = "bank_q08",
                    category = "GDPL",
                    categoryName = "Giáo dục pháp luật",
                    question = "Vùng đặc quyền kinh tế của Việt Nam theo Luật Biển Việt Nam năm 2012 có phạm vi như thế nào?",
                    options = listOf(
                        "200 hải lý tính từ đường cơ sở ra",
                        "12 hải lý tính từ đường cơ sở",
                        "50 hải lý tính từ bờ biển",
                        "Không giới hạn khoảng cách"
                    ),
                    correctIndex = 0,
                    explanation = "Điều 15 Luật Biển Việt Nam quy định: Vùng đặc quyền kinh tế của Việt Nam có chiều rộng 200 hải lý tính từ đường cơ sở."
                ),
                QuestionItem(
                    id = "bank_q09",
                    category = "GDPL",
                    categoryName = "Giáo dục pháp luật",
                    question = "Hành vi nào sau đây bị nghiêm cấm tuyệt đối theo Luật Phòng, chống ma túy và kỷ luật Quân đội?",
                    options = listOf(
                        "Sản xuất, tàng trữ, vận chuyển, mua bán, sử dụng trái phép chất ma túy",
                        "Tham gia tuyên truyền phòng chống ma túy",
                        "Kiểm tra sức khỏe định kỳ",
                        "Khai báo y tế tại đơn vị"
                    ),
                    correctIndex = 0,
                    explanation = "Mọi hành vi liên quan đến tàng trữ, vận chuyển, mua bán, sử dụng trái phép chất ma túy đều bị nghiêm cấm và xử lý nghiêm theo pháp luật và kỷ luật Quân đội."
                ),
                QuestionItem(
                    id = "bank_q10",
                    category = "GDPL",
                    categoryName = "Giáo dục pháp luật",
                    question = "Theo Luật Sĩ quan Quân đội nhân dân Việt Nam, nghĩa vụ cao nhất của sĩ quan là gì?",
                    options = listOf(
                        "Tuyệt đối trung thành với Tổ quốc, Nhân dân, Đảng và Nhà nước; sẵn sàng chiến đấu, hy sinh bảo vệ Tổ quốc",
                        "Tham gia hoạt động kinh tế đơn thuần",
                        "Tự do lựa chọn vị trí công tác",
                        "Đi công tác nước ngoài tự túc"
                    ),
                    correctIndex = 0,
                    explanation = "Sĩ quan có nghĩa vụ tuyệt đối trung thành với Tổ quốc, Nhân dân, Đảng và Nhà nước; chấp hành nghiêm pháp luật, điều lệnh, kỷ luật."
                ),
                QuestionItem(
                    id = "bank_q11",
                    category = "GDPL",
                    categoryName = "Giáo dục pháp luật",
                    question = "Hành vi điều khiển phương tiện tham gia giao thông mà trong máu hoặc hơi thở có nồng độ cồn bị xử lý ra sao?",
                    options = listOf(
                        "Bị xử phạt vi phạm hành chính nghiêm khắc hoặc truy cứu trách nhiệm hình sự và xử lý kỷ luật quân đội",
                        "Chỉ bị nhắc nhở nội bộ",
                        "Không bị xử lý nếu đi lại gần",
                        "Được miễn phạt nếu là quân nhân"
                    ),
                    correctIndex = 0,
                    explanation = "Luật Trật tự an toàn giao thông đường bộ và quy định Bộ Quốc phòng nghiêm cấm điều khiển phương tiện khi có nồng độ cồn, xử lý nghiêm minh mọi vi phạm."
                ),
                QuestionItem(
                    id = "bank_q12",
                    category = "GDPL",
                    categoryName = "Giáo dục pháp luật",
                    question = "Công ước Liên Hợp Quốc về Luật Biển năm 1982 (UNCLOS 1982) được Quốc hội Việt Nam phê chuẩn vào năm nào?",
                    options = listOf(
                        "Năm 1994",
                        "Năm 1982",
                        "Năm 2000",
                        "Năm 2012"
                    ),
                    correctIndex = 0,
                    explanation = "Quốc hội nước Cộng hòa XHCN Việt Nam đã phê chuẩn Công ước UNCLOS 1982 vào ngày 23/6/1994."
                ),

                // ================= LICHSU / TRUYENTHONG =================
                QuestionItem(
                    id = "bank_q13",
                    category = "LICHSU",
                    categoryName = "Lịch sử & Truyền thống",
                    question = "Ngày truyền thống của Vùng 4 Hải quân là ngày nào?",
                    options = listOf(
                        "Ngày 26 tháng 10 năm 1975",
                        "Ngày 07 tháng 5 năm 1955",
                        "Ngày 05 tháng 8 năm 1964",
                        "Ngày 22 tháng 12 năm 1944"
                    ),
                    correctIndex = 0,
                    explanation = "Vùng 4 Hải quân (tiền thân là Vùng 4 Duyên hải) được thành lập ngày 26/10/1975 theo Quyết định của Bộ Quốc phòng."
                ),
                QuestionItem(
                    id = "bank_q14",
                    category = "LICHSU",
                    categoryName = "Lịch sử & Truyền thống",
                    question = "Ngày truyền thống đánh thắng trận đầu của Hải quân nhân dân Việt Nam và quân dân miền Bắc là ngày nào?",
                    options = listOf(
                        "Ngày 02 và 05 tháng 8 năm 1964",
                        "Ngày 26 tháng 10 năm 1975",
                        "Ngày 30 tháng 4 năm 1975",
                        "Ngày 14 tháng 3 năm 1988"
                    ),
                    correctIndex = 0,
                    explanation = "Ngày 2 và 5/8/1964, Bộ đội Hải quân đã anh dũng đánh đuổi tàu khu trục Ma-đốc của đế quốc Mỹ và bắn rơi nhiều máy bay, lập nên truyền thống 'Đánh thắng trận đầu'."
                ),
                QuestionItem(
                    id = "bank_q15",
                    category = "LICHSU",
                    categoryName = "Lịch sử & Truyền thống",
                    question = "Truyền thống vẻ vang 16 chữ vàng của Quân chủng Hải quân Nhân dân Việt Nam là gì?",
                    options = listOf(
                        "'Chiến đấu anh dũng, mưu trí sáng tạo, làm chủ vùng biển, quyết chiến quyết thắng'",
                        "'Đoàn kết hiệp đồng, lập công tập thể, kỷ luật nghiêm minh'",
                        "'Đoàn kết, kiên cường, vượt qua sóng gió, hoàn thành xuất sắc nhiệm vụ'",
                        "'Bảo vệ biển đảo, trung thành tận tụy, sẵn sàng chiến đấu'"
                    ),
                    correctIndex = 0,
                    explanation = "16 chữ vàng truyền thống của Hải quân nhân dân Việt Nam: 'Chiến đấu anh dũng, mưu trí sáng tạo, làm chủ vùng biển, quyết chiến quyết thắng'."
                ),
                QuestionItem(
                    id = "bank_q16",
                    category = "LICHSU",
                    categoryName = "Lịch sử & Truyền thống",
                    question = "Đoàn tàu Không số huyền thoại gắn liền với con đường vận tải chiến lược nào trên biển?",
                    options = listOf(
                        "Đường Hồ Chí Minh trên biển (Đoàn 125 Hải quân)",
                        "Đường Trường Sơn trên bộ (Đoàn 559)",
                        "Tuyến vận tải đường sắt Bắc Nam",
                        "Tuyến phà sông Gianh"
                    ),
                    correctIndex = 0,
                    explanation = "Đoàn tàu Không số (Đoàn 125) đã lập nên kỳ tích huyền thoại Đường Hồ Chí Minh trên biển, chi viện vũ khí cho chiến trường miền Nam đánh giặc."
                ),
                QuestionItem(
                    id = "bank_q17",
                    category = "LICHSU",
                    categoryName = "Lịch sử & Truyền thống",
                    question = "Ngày truyền thống thành lập Quân chủng Hải quân nhân dân Việt Nam là ngày nào?",
                    options = listOf(
                        "Ngày 07 tháng 5 năm 1955",
                        "Ngày 22 tháng 12 năm 1944",
                        "Ngày 19 tháng 8 năm 1945",
                        "Ngày 02 tháng 9 năm 1945"
                    ),
                    correctIndex = 0,
                    explanation = "Ngày 07/5/1955, Bộ Quốc phòng ra Nghị định thành lập Cục Phòng thủ bờ bể - tiền thân của Quân chủng Hải quân ngày nay."
                ),
                QuestionItem(
                    id = "bank_q18",
                    category = "LICHSU",
                    categoryName = "Lịch sử & Truyền thống",
                    question = "Vùng 4 Hải quân hai lần vinh dự được Đảng và Nhà nước phong tặng danh hiệu cao quý nào?",
                    options = listOf(
                        "Anh hùng Lực lượng Vũ trang Nhân dân",
                        "Huân chương Sao vàng",
                        "Huân chương Độc lập hạng Nhất",
                        "Cờ thi đua của Chính phủ"
                    ),
                    correctIndex = 0,
                    explanation = "Vùng 4 Hải quân vinh dự hai lần được Đảng, Nhà nước phong tặng danh hiệu Anh hùng Lực lượng Vũ trang Nhân dân."
                ),

                // ================= BIENDAO =================
                QuestionItem(
                    id = "bank_q19",
                    category = "BIENDAO",
                    categoryName = "Biển đảo Việt Nam",
                    question = "Huyện đảo Trường Sa trực thuộc tỉnh/thành phố nào của nước ta?",
                    options = listOf(
                        "Tỉnh Khánh Hòa",
                        "Tỉnh Bà Rịa - Vũng Tàu",
                        "Thành phố Đà Nẵng",
                        "Tỉnh Bình Thuận"
                    ),
                    correctIndex = 0,
                    explanation = "Huyện đảo Trường Sa trực thuộc tỉnh Khánh Hòa, là địa bàn chiến lược do Vùng 4 Hải quân trực tiếp quản lý và bảo vệ."
                ),
                QuestionItem(
                    id = "bank_q20",
                    category = "BIENDAO",
                    categoryName = "Biển đảo Việt Nam",
                    question = "Vịnh Cam Ranh có vị trí và tầm quan trọng chiến lược quân sự như thế nào?",
                    options = listOf(
                        "Là một trong những vịnh nước sâu kín gió tự nhiên tốt nhất thế giới, là căn cứ quân sự chiến lược án ngữ Biển Đông",
                        "Chỉ là cảng du lịch dân sự",
                        "Là cảng nông phục vụ đánh bắt cá gần bờ",
                        "Là vùng đầm lầy bảo tồn sinh thái"
                    ),
                    correctIndex = 0,
                    explanation = "Vịnh Cam Ranh là cảng nước sâu chiến lược tự nhiên bậc nhất, là căn cứ quân sự trọng yếu bảo vệ sườn phía Đông của Tổ quốc."
                ),
                QuestionItem(
                    id = "bank_q21",
                    category = "BIENDAO",
                    categoryName = "Biển đảo Việt Nam",
                    question = "Đường cơ sở dùng để tính chiều rộng lãnh hải của Việt Nam được áp dụng theo phương pháp nào?",
                    options = listOf(
                        "Phương pháp đường cơ sở thẳng nối các điểm mốc thích hợp",
                        "Phương pháp đường cơ sở thông thường ngấn nước triều thấp nhất",
                        "Phương pháp tự do không theo mốc",
                        "Phương pháp đường kinh tuyến địa lý"
                    ),
                    correctIndex = 0,
                    explanation = "Việt Nam công bố đường cơ sở thẳng năm 1982 nối các điểm mốc từ đảo Hòn Nhạn đến đảo Cồn Cỏ theo đúng quy định UNCLOS 1982."
                ),
                QuestionItem(
                    id = "bank_q22",
                    category = "BIENDAO",
                    categoryName = "Biển đảo Việt Nam",
                    question = "Chủ trương giải quyết các tranh chấp ở Biển Đông của Đảng và Nhà nước ta là gì?",
                    options = listOf(
                        "Bằng biện pháp hòa bình, trên cơ sở luật pháp quốc tế, đặc biệt là UNCLOS 1982 và DOC, hướng tới COC",
                        "Sử dụng vũ lực đơn phương",
                        "Từ bỏ đàm phán ngoại giao",
                        "Chỉ giải quyết song phương không theo luật pháp quốc tế"
                    ),
                    correctIndex = 0,
                    explanation = "Việt Nam kiên quyết, kiên trì giải quyết mọi bất đồng, tranh chấp bằng biện pháp hòa bình trên cơ sở luật pháp quốc tế, nhất là UNCLOS 1982."
                ),

                // ================= DIEULENH =================
                QuestionItem(
                    id = "bank_q23",
                    category = "DIEULENH",
                    categoryName = "Điều lệnh & Kỷ luật",
                    question = "Quân nhân Quân đội nhân dân Việt Nam có bao nhiêu lời thề danh dự?",
                    options = listOf(
                        "10 lời thề danh dự",
                        "12 lời thề danh dự",
                        "8 lời thề danh dự",
                        "5 lời thề danh dự"
                    ),
                    correctIndex = 0,
                    explanation = "Quân nhân trong Quân đội nhân dân Việt Nam tuyên thệ 10 lời thề danh dự thiêng liêng dưới Quân kỳ Quyết thắng."
                ),
                QuestionItem(
                    id = "bank_q24",
                    category = "DIEULENH",
                    categoryName = "Điều lệnh & Kỷ luật",
                    question = "Quân nhân có bao nhiêu điều kỷ luật khi tiếp xúc và quan hệ với Nhân dân?",
                    options = listOf(
                        "12 điều kỷ luật",
                        "10 điều kỷ luật",
                        "15 điều kỷ luật",
                        "6 điều kỷ luật"
                    ),
                    correctIndex = 0,
                    explanation = "Quân đội nhân dân Việt Nam có 12 điều kỷ luật khi quan hệ với nhân dân để luôn giữ vững tình đoàn kết quân dân cá nước."
                ),
                QuestionItem(
                    id = "bank_q25",
                    category = "DIEULENH",
                    categoryName = "Điều lệnh & Kỷ luật",
                    question = "Trong chế độ sinh hoạt, học tập và công tác, một ngày trong quân đội có bao nhiêu chế độ nề nếp chính?",
                    options = listOf(
                        "11 chế độ trong ngày",
                        "8 chế độ trong ngày",
                        "15 chế độ trong ngày",
                        "5 chế độ trong ngày"
                    ),
                    correctIndex = 0,
                    explanation = "Theo Điều lệnh Quản lý bộ đội, quân nhân thực hiện nghiêm 11 chế độ sinh hoạt, học tập và công tác trong ngày."
                )
            )
        }
    }
}

data class ExamSessionDoc(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val status: String = "open", // "open", "active", "closed"
    val durationMinutes: Int = 20,
    val totalQuestions: Int = 20,
    val questionIds: List<String> = emptyList(),
    val questionsList: List<QuestionItem> = emptyList(),
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long = System.currentTimeMillis() + 86400000L * 30,
    val createdAt: Long = System.currentTimeMillis(),
    val maxAttempts: Int = 1
) {
    companion object {
        fun fromDoc(doc: DocumentSnapshot): ExamSessionDoc {
            val rawStatus = doc.getString("status") ?: doc.getString("trangThai") ?: "open"
            val isOpenBool = doc.getBoolean("isOpen") ?: doc.getBoolean("dangMo") ?: true
            val effectiveStatus = if (!isOpenBool) "closed" else rawStatus.lowercase()

            val rawQIds = doc.get("questionIds") ?: doc.get("dsCauHoiId") ?: doc.get("listQuestionIds")
            val qIdList = when (rawQIds) {
                is List<*> -> rawQIds.mapNotNull { it?.toString() }
                else -> emptyList()
            }

            val rawQuestions = doc.get("questions") ?: doc.get("dsCauHoi") ?: doc.get("cauHoiList") ?: doc.get("questionsList")
            val embeddedQList = mutableListOf<QuestionItem>()
            val parsedQIds = mutableListOf<String>()

            if (rawQuestions is List<*>) {
                rawQuestions.forEach { item ->
                    if (item is Map<*, *>) {
                        try {
                            val qText = (item["question"] ?: item["cauHoi"] ?: item["content"] ?: item["title"] ?: "").toString()
                            val rawOpts = item["options"] ?: item["dapAn"] ?: item["choices"] ?: item["answers"]
                            val opts = when (rawOpts) {
                                is List<*> -> rawOpts.mapNotNull { it?.toString() }
                                else -> {
                                    val a = (item["optionA"] ?: item["dapAnA"] ?: "").toString()
                                    val b = (item["optionB"] ?: item["dapAnB"] ?: "").toString()
                                    val c = (item["optionC"] ?: item["dapAnC"] ?: "").toString()
                                    val d = (item["optionD"] ?: item["dapAnD"] ?: "").toString()
                                    listOf(a, b, c, d).filter { it.isNotEmpty() }
                                }
                            }
                            val rawCorr = item["correctIndex"] ?: item["correctAnswer"] ?: item["dapAnDung"] ?: item["correct"] ?: 0
                            val corrIdx = when (rawCorr) {
                                is Number -> rawCorr.toInt()
                                is String -> {
                                    when (rawCorr.trim().uppercase()) {
                                        "A", "0" -> 0
                                        "B", "1" -> 1
                                        "C", "2" -> 2
                                        "D", "3" -> 3
                                        else -> rawCorr.toIntOrNull() ?: 0
                                    }
                                }
                                else -> 0
                            }
                            val qId = (item["id"] ?: item["_id"] ?: item["questionId"] ?: System.currentTimeMillis().toString()).toString()
                            if (qText.isNotBlank()) {
                                embeddedQList.add(
                                    QuestionItem(
                                        id = qId,
                                        question = cleanHtml(qText),
                                        options = opts.map { cleanHtml(it) },
                                        correctIndex = corrIdx,
                                        category = doc.getString("category") ?: "GDCT",
                                        explanation = cleanHtml((item["explanation"] ?: item["giaiThich"] ?: "").toString())
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            // Skip item on error
                        }
                    } else if (item is String) {
                        parsedQIds.add(item)
                    }
                }
            }

            val finalQIds = if (qIdList.isNotEmpty()) qIdList else parsedQIds

            val dur = parseNumber(doc.get("durationMinutes") ?: doc.get("thoiGianLamBai") ?: doc.get("thoiGian") ?: doc.get("thoiGianThi"), 20L).toInt()
            val totalQ = parseNumber(doc.get("totalQuestions") ?: doc.get("soCauHoi") ?: doc.get("tongSoCau") ?: doc.get("soLuongCauHoi"), if (embeddedQList.isNotEmpty()) embeddedQList.size.toLong() else 20L).toInt()
            val maxAtt = parseNumber(doc.get("maxAttempts") ?: doc.get("soLuotThi") ?: doc.get("soLanThi") ?: doc.get("limitAttempts") ?: doc.get("soLuotKiemTra") ?: doc.get("soLan") ?: doc.get("luotThi"), 1L).toInt()

            return ExamSessionDoc(
                id = doc.id,
                title = cleanHtml(doc.getString("title") ?: doc.getString("tenDotThi") ?: doc.getString("tieuDe") ?: "Đợt kiểm tra trực tuyến"),
                description = cleanHtml(doc.getString("description") ?: doc.getString("moTa") ?: doc.getString("noiDung") ?: ""),
                category = doc.getString("category") ?: doc.getString("chuyenDe") ?: "",
                status = effectiveStatus,
                durationMinutes = if (dur > 0) dur else 20,
                totalQuestions = if (totalQ > 0) totalQ else (if (embeddedQList.isNotEmpty()) embeddedQList.size else 20),
                questionIds = finalQIds,
                questionsList = embeddedQList,
                startTime = parseTime(doc.get("startTime") ?: doc.get("thoiGianBatDau") ?: doc.get("batDau") ?: doc.get("createdAt")),
                endTime = parseTime(doc.get("endTime") ?: doc.get("thoiGianKetThuc") ?: doc.get("ketThuc") ?: (System.currentTimeMillis() + 86400000L * 30)),
                createdAt = parseTime(
                    doc.get("createdAt")
                        ?: doc.get("created_at")
                        ?: doc.get("thoiGianTao")
                        ?: doc.get("ngayTao")
                        ?: doc.get("timestamp")
                        ?: doc.get("date")
                        ?: doc.get("startTime")
                        ?: doc.get("thoiGianBatDau")
                ),
                maxAttempts = if (maxAtt > 0) maxAtt else 1
            )
        }
    }
}

data class ExamResultDoc(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val userUnit: String = "",
    val userRank: String = "",
    val examId: String = "",
    val examName: String = "",
    val score: Int = 0,
    val totalQuestions: Int = 0,
    val scorePercentage: Int = 0,
    val passed: Boolean = false,
    val timeSpentSeconds: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        fun fromDoc(doc: DocumentSnapshot): ExamResultDoc {
            val scoreVal = (doc.getLong("score") ?: doc.getLong("diem") ?: doc.getLong("soCauDung") ?: 0L).toInt()
            val totalVal = (doc.getLong("totalQuestions") ?: doc.getLong("tongSoCau") ?: doc.getLong("soCauHoi") ?: 20L).toInt()
            val percentVal = (doc.getLong("scorePercentage") ?: doc.getLong("phanTramDiem") ?: (if (totalVal > 0) scoreVal * 100 / totalVal else 0).toLong()).toInt()

            return ExamResultDoc(
                id = doc.id,
                userId = doc.getString("userId") ?: doc.getString("user_id") ?: doc.getString("nguoiDungId") ?: "",
                userName = doc.getString("userName") ?: doc.getString("hoTen") ?: doc.getString("tenHocVien") ?: "Học viên",
                userEmail = doc.getString("userEmail") ?: doc.getString("email") ?: "",
                userUnit = doc.getString("userUnit") ?: doc.getString("unit") ?: doc.getString("donVi") ?: "Vùng 4 Hải Quân",
                userRank = doc.getString("userRank") ?: doc.getString("rank") ?: doc.getString("capBac") ?: "",
                examId = doc.getString("examId") ?: doc.getString("dotThiId") ?: doc.getString("examSessionId") ?: "",
                examName = doc.getString("examName") ?: doc.getString("tenDotThi") ?: doc.getString("tenBaiThi") ?: "Bài kiểm tra trắc nghiệm",
                score = scoreVal,
                totalQuestions = totalVal,
                scorePercentage = percentVal,
                passed = doc.getBoolean("passed") ?: doc.getBoolean("dat") ?: (percentVal >= 50),
                timeSpentSeconds = (doc.getLong("timeSpentSeconds") ?: doc.getLong("thoiGianLamBai") ?: 0L).toInt(),
                timestamp = parseTime(doc.get("timestamp") ?: doc.get("thoiGianNop") ?: doc.get("createdAt"))
            )
        }
    }
}

private fun parseLong(value: Any?): Long {
    return when (value) {
        is Number -> value.toLong()
        is String -> value.toLongOrNull() ?: 1L
        else -> 1L
    }
}

private fun parseInt(value: Any?): Int {
    return when (value) {
        is Number -> value.toInt()
        is String -> value.toIntOrNull() ?: 1
        else -> 1
    }
}

private fun parseBoolean(value: Any?): Boolean {
    return when (value) {
        is Boolean -> value
        is String -> value.toBoolean()
        is Number -> value.toInt() != 0
        else -> false
    }
}

private fun parseNumber(value: Any?, default: Long = 0L): Long {
    return when (value) {
        is Number -> value.toLong()
        is String -> value.trim().toLongOrNull() ?: default
        else -> default
    }
}

private fun parseTime(value: Any?): Long {
    return when (value) {
        is Number -> value.toLong()
        is Timestamp -> value.seconds * 1000L + (value.nanoseconds / 1_000_000L)
        is java.util.Date -> value.time
        is String -> {
            val str = value.trim()
            str.toLongOrNull() ?: try {
                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).parse(str)?.time
                    ?: java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).parse(str)?.time
                    ?: java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).parse(str)?.time
                    ?: java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).parse(str)?.time
                    ?: java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).parse(str)?.time
                    ?: 0L
            } catch (e: Exception) {
                0L
            }
        }
        else -> 0L
    }
}

fun cleanHtml(html: String): String {
    if (html.isBlank()) return ""
    var cleaned = html
        .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("</p>", RegexOption.IGNORE_CASE), "\n\n")
        .replace(Regex("</div>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("</li>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("<li>", RegexOption.IGNORE_CASE), "• ")
        .replace(Regex("<p[^>]*>", RegexOption.IGNORE_CASE), "")
        .replace(Regex("<div[^>]*>", RegexOption.IGNORE_CASE), "")
        .replace(Regex("</?[a-z][^>]*>", RegexOption.IGNORE_CASE), "")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&ndash;", "-")
        .replace("&mdash;", "-")
    
    // Remove markdown code blocks if any
    cleaned = cleaned.replace(Regex("```[a-zA-Z]*\\n?"), "").replace("```", "")
    
    // Clean up excessive newlines
    cleaned = cleaned.replace(Regex("\\n{3,}"), "\n\n")
    return cleaned.trim()
}

fun cleanBodyHtml(html: String): String {
    if (html.isBlank()) return ""
    var cleaned = html
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&ndash;", "-")
        .replace("&mdash;", "-")
    cleaned = cleaned.replace(Regex("```[a-zA-Z]*\\n?"), "").replace("```", "")
    return cleaned.trim()
}

