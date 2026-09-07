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
    val imageUrl: String = ""
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
                imageUrl = doc.getString("imageUrl") ?: doc.getString("image") ?: ""
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
    val updatedAt: Long = System.currentTimeMillis()
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
                updatedAt = parseTime(doc.get("updatedAt"))
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

private fun parseTime(value: Any?): Long {
    return when (value) {
        is Number -> value.toLong()
        is String -> {
            value.toLongOrNull() ?: System.currentTimeMillis()
        }
        is Timestamp -> value.seconds * 1000L
        else -> System.currentTimeMillis()
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

