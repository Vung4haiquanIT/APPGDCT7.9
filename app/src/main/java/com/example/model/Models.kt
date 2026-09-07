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
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun fromDoc(doc: DocumentSnapshot): ProgressDoc {
            return ProgressDoc(
                id = doc.id,
                userId = doc.getString("userId") ?: "",
                lessonId = doc.getString("lessonId") ?: doc.getString("baiHocId") ?: "",
                completed = parseBoolean(doc.get("completed")),
                updatedAt = parseTime(doc.get("updatedAt"))
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

