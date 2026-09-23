package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

fun safeString(doc: DocumentSnapshot, vararg keys: String): String {
    for (k in keys) {
        val v = doc.get(k) ?: continue
        val s = when (v) {
            is String -> v
            is Number -> v.toString()
            is Boolean -> v.toString()
            else -> v.toString()
        }.trim()
        if (s.isNotBlank() && s != "null") return s
    }
    return ""
}

data class Course(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val status: String = "active",
    val version: Long = 1L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val imageUrl: String = "",
    val category: String = "",
    val year: String = "",
    val courseYear: String = ""
) {
    companion object {
        fun fromDoc(doc: DocumentSnapshot): Course {
            val yr = safeString(doc, "courseYear", "course_year", "year", "nam", "namHoc", "schoolYear")
            val title = cleanHtml(safeString(doc, "title", "name", "ten", "tenChuyenDe", "courseTitle"))
            val desc = cleanHtml(safeString(doc, "description", "desc", "moTa", "summary", "noiDung"))
            val status = safeString(doc, "status", "trangThai").ifBlank { "active" }
            val img = safeString(doc, "imageUrl", "image", "thumbnail", "photoUrl", "hinhAnh", "banner")
            val cat = safeString(doc, "category", "chuyenDe", "type", "loai")

            return Course(
                id = doc.id,
                title = title,
                description = desc,
                status = status,
                version = parseLong(doc.get("version")),
                createdAt = parseTime(doc.get("createdAt")),
                updatedAt = parseTime(doc.get("updatedAt")),
                imageUrl = img,
                category = cat,
                year = yr,
                courseYear = yr
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
    val category: String = "",
    val year: String = "",
    val courseYear: String = "",
    val questions: List<QuestionItem> = emptyList()
) {
    companion object {
        fun fromDoc(doc: DocumentSnapshot): Lesson {
            val yr = safeString(doc, "courseYear", "course_year", "year", "nam", "namHoc", "schoolYear")
            val parentCourseId = doc.reference.parent.parent?.id ?: ""
            val cId = safeString(doc, "courseId", "course_id", "chuyenDeId", "chuyen_de_id", "course", "chuyenDe").ifBlank { parentCourseId }
            val title = cleanHtml(safeString(doc, "title", "name", "ten", "tenBaiHoc", "lessonTitle"))
            val desc = cleanHtml(safeString(doc, "description", "desc", "moTa", "summary", "noiDung"))
            val status = safeString(doc, "status", "trangThai").ifBlank { "active" }
            val cat = safeString(doc, "category", "chuyenDe", "type", "loai")

            val rawQs = doc.get("questions") ?: doc.get("quiz") ?: doc.get("cauHoi") ?: doc.get("dsCauHoi") ?: doc.get("quizQuestions")
            val embeddedQs = mutableListOf<QuestionItem>()
            if (rawQs is List<*>) {
                rawQs.forEachIndexed { idx, item ->
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
                                is String -> when (rawCorr.trim().uppercase()) {
                                    "A", "0" -> 0
                                    "B", "1" -> 1
                                    "C", "2" -> 2
                                    "D", "3" -> 3
                                    else -> rawCorr.toIntOrNull() ?: 0
                                }
                                else -> 0
                            }
                            val qId = (item["id"] ?: item["_id"] ?: item["questionId"] ?: "${doc.id}_q_$idx").toString()
                            if (qText.isNotBlank()) {
                                embeddedQs.add(
                                    QuestionItem(
                                        id = qId,
                                        lessonId = doc.id,
                                        courseId = cId,
                                        category = cat.ifBlank { "GDCT" },
                                        categoryName = title.ifBlank { "Bài học" },
                                        question = cleanHtml(qText),
                                        options = opts.map { cleanHtml(it) },
                                        correctIndex = corrIdx,
                                        explanation = cleanHtml((item["explanation"] ?: item["giaiThich"] ?: "").toString())
                                    )
                                )
                            }
                        } catch (_: Exception) {}
                    }
                }
            }

            return Lesson(
                id = doc.id,
                courseId = cId,
                title = title,
                description = desc,
                status = status,
                version = parseLong(doc.get("version")),
                createdAt = parseTime(doc.get("createdAt")),
                updatedAt = parseTime(doc.get("updatedAt")),
                category = cat,
                year = yr,
                courseYear = yr,
                questions = embeddedQs
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
            val parentLessonId = doc.reference.parent.parent?.id ?: ""
            return ContentItem(
                id = doc.id,
                lessonId = safeString(doc, "lessonId", "baiHocId", "lesson_id").ifBlank { parentLessonId },
                order = parseInt(doc.get("order")),
                title = cleanHtml(safeString(doc, "title", "name", "ten")),
                bodyHtml = cleanBodyHtml(safeString(doc, "bodyHtml", "content", "body", "noiDung")),
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
            val parentLessonId = doc.reference.parent.parent?.id ?: ""
            return SlideItem(
                id = doc.id,
                lessonId = safeString(doc, "lessonId", "baiHocId", "lesson_id").ifBlank { parentLessonId },
                order = parseInt(doc.get("order")),
                imageUrl = safeString(doc, "imageUrl", "url", "image", "hinhAnh"),
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
            val parentLessonId = doc.reference.parent.parent?.id ?: ""
            return VideoItem(
                id = doc.id,
                lessonId = safeString(doc, "lessonId", "baiHocId", "lesson_id").ifBlank { parentLessonId },
                title = safeString(doc, "title", "name", "ten"),
                videoUrl = safeString(doc, "videoUrl", "url", "video"),
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
            val parentLessonId = doc.reference.parent.parent?.id ?: ""
            return AudioItem(
                id = doc.id,
                lessonId = safeString(doc, "lessonId", "baiHocId", "lesson_id").ifBlank { parentLessonId },
                title = safeString(doc, "title", "name", "ten"),
                audioUrl = safeString(doc, "audioUrl", "url", "audio"),
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
            val entityId = safeString(doc, "entityId", "lessonId", "baiHocId")
            val lessonId = safeString(doc, "lessonId", "entityId", "baiHocId")
            val downloadUrl = safeString(doc, "downloadUrl", "cloudinaryUrl", "url", "fileUrl")
            val title = safeString(doc, "title", "fileName", "name").ifBlank { "Tài liệu đính kèm" }
            val fileName = safeString(doc, "fileName", "title", "name")
            return StorageFileItem(
                id = doc.id,
                category = safeString(doc, "category"),
                entityId = entityId,
                lessonId = lessonId,
                title = title,
                fileName = fileName,
                storagePath = safeString(doc, "storagePath"),
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
    val role: String = "Đồng chí",
    val unit: String = "Vùng 4 Hải Quân",
    val rank: String = "",
    val phone: String = "",
    val avatarUrl: String = "",
    val targetGroup: String = "",
    val targetAudience: String = "",
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
            val rawTargetObj = doc.get("targetGroup")
                ?: doc.get("target_group")
                ?: doc.get("targetGroups")
                ?: doc.get("target_groups")
                ?: doc.get("TargetGroup")
                ?: doc.get("targetgroup")
                ?: doc.get("doiTuong")
                ?: doc.get("loaiDoiTuong")
                ?: doc.get("loai_doi_tuong")
                ?: doc.get("doiTuongNguoiDung")
                ?: doc.get("nhomDoiTuong")
                ?: doc.get("targetAudience")
                ?: doc.get("target_audience")
                ?: doc.get("audience")
                ?: doc.get("userGroup")
                ?: doc.get("user_group")

            val rawTarget = when (rawTargetObj) {
                is List<*> -> rawTargetObj.mapNotNull { item ->
                    when (item) {
                        is Map<*, *> -> (item["name"] ?: item["value"] ?: item["code"] ?: item["title"] ?: item["label"] ?: item["id"])?.toString()?.trim()
                        else -> item?.toString()?.trim()
                    }
                }.filter { !it.isNullOrBlank() }.joinToString(", ")
                is Map<*, *> -> (rawTargetObj["name"] ?: rawTargetObj["value"] ?: rawTargetObj["code"] ?: rawTargetObj["title"] ?: rawTargetObj["label"] ?: rawTargetObj["id"] ?: rawTargetObj.toString()).toString().trim()
                is String -> rawTargetObj.trim()
                else -> rawTargetObj?.toString()?.trim() ?: ""
            }

            val effectiveTarget = rawTarget.ifBlank {
                doc.getString("role") ?: doc.getString("userType") ?: doc.getString("vaiTro") ?: "Đồng chí"
            }
            return UserDoc(
                id = doc.id,
                name = doc.getString("name") ?: doc.getString("displayName") ?: doc.getString("fullName") ?: "",
                email = doc.getString("email") ?: doc.getString("username") ?: "",
                role = doc.getString("role") ?: doc.getString("userType") ?: "Đồng chí",
                unit = doc.getString("unit") ?: doc.getString("donVi") ?: "Vùng 4 Hải Quân",
                rank = doc.getString("rank") ?: doc.getString("capBac") ?: doc.getString("chucVu") ?: "",
                phone = doc.getString("phone") ?: doc.getString("soDienThoai") ?: "",
                targetGroup = effectiveTarget,
                targetAudience = effectiveTarget,
                avatarUrl = doc.getString("avatarUrl")
                    ?: doc.getString("avatar")
                    ?: doc.getString("avatarBase64")
                    ?: doc.getString("photoUrl")
                    ?: doc.getString("photoURL")
                    ?: doc.getString("avatar_url")
                    ?: doc.getString("imageUrl")
                    ?: doc.getString("image")
                    ?: doc.getString("anhDaiDien")
                    ?: doc.getString("anh_dai_dien")
                    ?: doc.getString("hinhDaiDien")
                    ?: doc.getString("picture")
                    ?: doc.getString("userAvatar")
                    ?: "",
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
    val type: String = "admin", // "admin", "reminder", "system", "feedback", "reply"
    val targetUserId: String? = null,
    val targetUserIds: List<String> = emptyList(),
    val targetEmail: String? = null,
    val targetUserName: String? = null,
    val targetGroup: String? = null,
    val targetLessonId: String? = null,
    val targetCourseId: String? = null,
    val imageUrl: String? = null,
    val images: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val priority: String = "normal" // "urgent", "high", "normal"
) {
    companion object {
        fun fromDoc(doc: DocumentSnapshot): NotificationItem {
            val rawTargetUserIds = mutableListOf<String>()
            val singleTargetUserId = (
                doc.getString("targetUserId")
                    ?: doc.getString("userId")
                    ?: doc.getString("receiverId")
                    ?: doc.getString("recipientId")
                    ?: doc.getString("toUserId")
                    ?: doc.getString("target_user_id")
                    ?: doc.getString("user_id")
                    ?: doc.getString("nguoiNhanId")
                    ?: doc.getString("idNguoiNhan")
                    ?: doc.getString("targetUser")
                    ?: doc.getString("receiver")
                    ?: doc.get("targetUserId")?.toString()
                    ?: doc.get("userId")?.toString()
            )?.trim()?.takeIf { it.isNotBlank() }

            if (!singleTargetUserId.isNullOrBlank()) {
                rawTargetUserIds.addAll(singleTargetUserId.split(",").map { it.trim() }.filter { it.isNotBlank() })
            }

            val arrayFields = listOf("targetUserIds", "targetUsers", "userIds", "recipients", "danhSachNguoiNhan", "danhSachId")
            for (field in arrayFields) {
                val rawObj = doc.get(field)
                if (rawObj is List<*>) {
                    rawTargetUserIds.addAll(rawObj.mapNotNull { it?.toString()?.trim() }.filter { it.isNotBlank() })
                }
            }

            val targetEmail = (
                doc.getString("targetEmail")
                    ?: doc.getString("userEmail")
                    ?: doc.getString("email")
                    ?: doc.getString("target_email")
                    ?: doc.getString("user_email")
                    ?: doc.getString("emailNguoiNhan")
            )?.trim()?.takeIf { it.isNotBlank() }

            val targetUserName = (
                doc.getString("targetUserName")
                    ?: doc.getString("userName")
                    ?: doc.getString("nguoiNhan")
                    ?: doc.getString("target_user_name")
                    ?: doc.getString("user_name")
                    ?: doc.getString("tenNguoiNhan")
                    ?: doc.getString("hoTen")
                    ?: doc.getString("ho_ten")
            )?.trim()?.takeIf { it.isNotBlank() }

            val targetGroup = (
                doc.getString("targetGroup")
                    ?: doc.getString("target_group")
                    ?: doc.getString("doiTuong")
                    ?: doc.getString("targetAudience")
            )?.trim()?.takeIf { it.isNotBlank() }

            val title = cleanHtml(
                doc.getString("title")
                    ?: doc.getString("tieuDe")
                    ?: doc.getString("subject")
                    ?: doc.getString("tieu_de")
                    ?: if (singleTargetUserId != null || !targetUserName.isNullOrBlank()) "Phản hồi từ Web Quản trị" else "Thông báo từ Web Quản trị"
            )

            val message = cleanHtml(
                doc.getString("message")
                    ?: doc.getString("content")
                    ?: doc.getString("noiDung")
                    ?: doc.getString("phanHoi")
                    ?: doc.getString("reply")
                    ?: doc.getString("feedbackReply")
                    ?: doc.getString("noi_dung")
                    ?: doc.getString("adminReply")
                    ?: ""
            )

            val rawType = doc.getString("type") ?: doc.getString("loai") ?: doc.getString("loaiThongBao") ?: doc.getString("loai_thong_bao")
            val type = when {
                !rawType.isNullOrBlank() -> rawType
                !singleTargetUserId.isNullOrBlank() || !targetUserName.isNullOrBlank() -> "feedback"
                else -> "admin"
            }

            val notifImages = mutableListOf<String>()
            val singleImageUrl = (
                doc.getString("imageUrl")
                    ?: doc.getString("image")
                    ?: doc.getString("hinhAnh")
                    ?: doc.getString("hinh_anh")
                    ?: doc.getString("photoUrl")
            )?.trim()?.takeIf { it.isNotBlank() }

            if (!singleImageUrl.isNullOrBlank()) {
                notifImages.add(singleImageUrl)
            }

            val imageListFields = listOf("images", "imageUrls", "attachedImages", "danhSachHinhAnh", "photos")
            for (field in imageListFields) {
                val rawObj = doc.get(field)
                if (rawObj is List<*>) {
                    notifImages.addAll(rawObj.mapNotNull { it?.toString()?.trim() }.filter { it.isNotBlank() })
                }
            }

            return NotificationItem(
                id = doc.id,
                title = title,
                message = message,
                type = type,
                targetUserId = singleTargetUserId,
                targetUserIds = rawTargetUserIds.distinct(),
                targetEmail = targetEmail,
                targetUserName = targetUserName,
                targetGroup = targetGroup,
                targetLessonId = doc.getString("targetLessonId") ?: doc.getString("lessonId"),
                targetCourseId = doc.getString("targetCourseId") ?: doc.getString("courseId"),
                imageUrl = singleImageUrl ?: notifImages.firstOrNull(),
                images = notifImages.distinct(),
                timestamp = parseTime(doc.get("timestamp") ?: doc.get("createdAt") ?: doc.get("date") ?: doc.get("thoiGian")),
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
    val examSessionId: String = "",
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
            val parentId = try { doc.reference.parent.parent?.id ?: "" } catch (_: Exception) { "" }
            val parentColl = try { doc.reference.parent.id } catch (_: Exception) { "" }
            val lId = (doc.getString("lessonId") ?: doc.getString("baiHocId") ?: doc.getString("lesson_id") ?: "").ifBlank {
                if (parentColl in listOf("questions", "quiz", "cauHoi", "cau_hoi")) parentId else ""
            }
            val eId = (doc.getString("examId") ?: doc.getString("examSessionId") ?: doc.getString("dotThiId") ?: doc.getString("dot_thi_id") ?: "").ifBlank {
                if (parentColl in listOf("exam_questions", "cauHoiKiemTra", "exam_sessions")) parentId else ""
            }

            return QuestionItem(
                id = doc.id,
                lessonId = lId,
                courseId = doc.getString("courseId") ?: doc.getString("chuyenDeId") ?: "",
                examSessionId = eId,
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

        // Không tự tạo danh sách câu hỏi mẫu, chỉ lấy dữ liệu thực từ bài học & đợt kiểm tra
        fun getDefaultQuestionBank(): List<QuestionItem> {
            return emptyList()
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
    val maxAttempts: Int = 1,
    val targetGroup: List<String> = emptyList(),
    val targetGroupText: String = "",
    val targetAudience: List<String> = emptyList(),
    val targetAudienceText: String = "",
    val targetUnits: List<String> = emptyList()
) {
    /**
     * Kiểm tra đợt thi có phù hợp với loại đối tượng của tài khoản người dùng đang đăng nhập hay không
     */
    fun isApplicableForUser(user: UserDoc?): Boolean {
        // Chưa đăng nhập -> hiển thị đầy đủ cho khách xem
        if (user == null) return true

        // Tài khoản Admin quản trị có thể xem tất cả bài thi để kiểm tra
        if (user.role.contains("Admin", ignoreCase = true) || user.role.equals("Quản trị viên", ignoreCase = true)) {
            return true
        }

        // 1. Kiểm tra đơn vị (nếu đợt thi có quy định danh sách đơn vị cụ thể)
        if (targetUnits.isNotEmpty()) {
            val userUnit = user.unit.trim().lowercase()
            val matchUnit = targetUnits.any { u ->
                val normU = u.trim().lowercase()
                normU == "tất cả" || normU == "toàn đơn vị" || normU == "toàn quân" || normU == "all" ||
                        userUnit.contains(normU) || normU.contains(userUnit)
            }
            if (!matchUnit && userUnit.isNotBlank()) {
                return false
            }
        }

        // 2. Xác định loại đối tượng chuẩn của tài khoản người dùng
        val userCategory = getUserAudienceCategory(user)

        // 3. Nếu tài khoản thuộc diện ALL / Tất cả / SQ và QNCN -> có thể xem tất cả
        if (userCategory == "ALL") return true

        // 4. KIỂM TRA ĐẶC BIỆT CHẶN QNCN KHÔNG THỂ THẤY BÀI THI CỦA SQ:
        val titleDescNorm = removeAccents("${title} ${description}".lowercase())
        val isSQOnlyInText = (titleDescNorm.contains("si quan") || titleDescNorm.contains("doi tuong sq") || titleDescNorm.contains("cho sq") || titleDescNorm.contains("danh cho sq")) &&
                !titleDescNorm.contains("qncn") && !titleDescNorm.contains("quan nhan chuyen nghiep")
        val isQNCNOnlyInText = (titleDescNorm.contains("qncn") || titleDescNorm.contains("quan nhan chuyen nghiep") || titleDescNorm.contains("doi tuong qncn") || titleDescNorm.contains("cho qncn") || titleDescNorm.contains("danh cho qncn")) &&
                !titleDescNorm.contains("si quan") && !titleDescNorm.contains("sq")

        if (userCategory == "QNCN") {
            // Nếu đợt thi mang nội dung hoặc mục tiêu là Sĩ quan -> Chặn tuyệt đối
            if (isSQOnlyInText) return false
            val hasExplicitSQ = targetAudience.any {
                val tNorm = removeAccents(it.trim().lowercase())
                (tNorm == "sq" || tNorm.contains("si quan") || tNorm.contains("can bo") || tNorm.contains("doi tuong 1") || tNorm == "dt1") &&
                        !tNorm.contains("qncn") && !tNorm.contains("quan nhan chuyen nghiep")
            }
            val hasExplicitQNCNOrAll = targetAudience.any {
                val tNorm = removeAccents(it.trim().lowercase())
                tNorm == "all" || tNorm == "tat ca" || tNorm == "toan quan" || tNorm == "qncn" || tNorm.contains("quan nhan chuyen nghiep") ||
                        (tNorm.contains("sq") && tNorm.contains("qncn"))
            }
            if (hasExplicitSQ && !hasExplicitQNCNOrAll) {
                return false
            }
        }

        if (userCategory == "SQ") {
            // Nếu đợt thi mang nội dung hoặc mục tiêu là QNCN -> Chặn tuyệt đối
            if (isQNCNOnlyInText) return false
            val hasExplicitQNCN = targetAudience.any {
                val tNorm = removeAccents(it.trim().lowercase())
                (tNorm == "qncn" || tNorm.contains("quan nhan chuyen nghiep") || tNorm.contains("chuyen nghiep") || tNorm.contains("doi tuong 2") || tNorm == "dt2") &&
                        !tNorm.contains("sq") && !tNorm.contains("si quan")
            }
            val hasExplicitSQOrAll = targetAudience.any {
                val tNorm = removeAccents(it.trim().lowercase())
                tNorm == "all" || tNorm == "tat ca" || tNorm == "toan quan" || tNorm == "sq" || tNorm.contains("si quan") ||
                        (tNorm.contains("sq") && tNorm.contains("qncn"))
            }
            if (hasExplicitQNCN && !hasExplicitSQOrAll) {
                return false
            }
        }

        // 5. Nếu đợt thi không đặt giới hạn đối tượng hoặc áp dụng cho tất cả
        if (targetAudience.isEmpty()) {
            return true
        }
        val isAllAudience = targetAudience.any {
            val a = removeAccents(it.trim().lowercase())
            a == "tất cả" || a == "tat ca" || a == "toàn quân" || a == "toan quan" ||
                    a == "toàn đơn vị" || a == "toan don vi" || a == "mọi đối tượng" ||
                    a == "all" || a == "chung" || a == "mặc định" || a == "*" ||
                    (a.contains("sq") && a.contains("qncn"))
        }
        if (isAllAudience) return true

        // 6. So khớp từng đối tượng mục tiêu của đợt thi với nhóm của người dùng
        return targetAudience.any { target ->
            val cleanTarget = target.trim()
            if (cleanTarget.isBlank()) return@any true
            matchAudienceCategory(cleanTarget, userCategory, user)
        }
    }

    companion object {
        /**
         * Định dạng chuỗi hiển thị đối tượng:
         * Xử lý các đối tượng chính: SQ, QNCN, HSQ-BS
         */
        fun formatAudienceDisplay(raw: String): String {
            val trimmed = raw.trim()
            if (trimmed.isBlank()) return "Tất cả"
            val norm = removeAccents(trimmed).lowercase()
            if (norm == "all" || norm == "tat ca" || norm == "toan quan" || norm == "toan don vi" || norm == "moi doi tuong" || norm == "*") {
                return "Tất cả"
            }

            val hasSQ = norm.contains("sq") || norm.contains("si quan")
            val hasQNCN = norm.contains("qncn") || norm.contains("quan nhan chuyen nghiep")
            val hasHSQBS = norm.contains("hsq") || norm.contains("bs") || norm.contains("cs") || norm.contains("binh si") || norm.contains("chien si") || norm.contains("ha si quan")

            if (hasSQ && hasQNCN && hasHSQBS) {
                return "Tất cả"
            }
            if (hasSQ && hasQNCN && !hasHSQBS) {
                return "SQ và QNCN"
            }
            if (hasSQ && hasHSQBS && !hasQNCN) {
                return "SQ và HSQ-BS"
            }
            if (hasQNCN && hasHSQBS && !hasSQ) {
                return "QNCN và HSQ-BS"
            }

            val parts = trimmed.split(",", ";").map { it.trim() }.filter { it.isNotBlank() }
            if (parts.isEmpty()) return "Tất cả"
            return parts.joinToString(", ") { part ->
                val pNorm = removeAccents(part).lowercase()
                if (pNorm == "all" || pNorm == "tat ca" || pNorm == "toan quan" || pNorm == "moi doi tuong") {
                    "Tất cả"
                } else if (pNorm.contains("hsq") || pNorm.contains("binh si") || pNorm.contains("chien si") || pNorm.contains("bs") || pNorm.contains("ha si quan")) {
                    "HSQ-BS"
                } else if (pNorm == "sq" || pNorm.contains("si quan")) {
                    "SQ"
                } else if (pNorm == "qncn" || pNorm.contains("quan nhan chuyen nghiep")) {
                    "QNCN"
                } else {
                    part
                }
            }
        }

        /**
         * Phân loại đối tượng chuẩn xác cho tài khoản người dùng:
         * - "QNCN": Quân nhân chuyên nghiệp
         * - "SQ": Sĩ quan / Cán bộ
         * - "HSQ_CS": Hạ sĩ quan - Binh sĩ / Chiến sĩ
         * - "HV": Học viên
         * - "CNVQP": Công nhân viên quốc phòng
         */
        fun getUserAudienceCategory(user: UserDoc): String {
            val targetStr = (user.targetGroup.ifBlank { user.targetAudience }).trim()
            val targetNorm = removeAccents(targetStr.lowercase()).replace("_", " ").replace("-", " ")
            val roleStr = user.role.trim().lowercase()
            val roleNorm = removeAccents(roleStr).replace("_", " ").replace("-", " ")
            val rankStr = user.rank.trim().lowercase()
            val rankNorm = removeAccents(rankStr).replace("_", " ").replace("-", " ")

            // 0. Nếu đối tượng là ALL hoặc cả SQ và QNCN
            if (targetNorm == "all" || targetNorm == "tat ca" || targetNorm == "toan quan" || (targetNorm.contains("sq") && targetNorm.contains("qncn"))) {
                return "ALL"
            }

            // 1. Ưu tiên kiểm tra QNCN trước (vì quân hàm có thể là "Thiếu úy QNCN", "Trung úy QNCN"...)
            if (targetNorm.contains("qncn") || targetNorm.contains("quan nhan chuyen nghiep") || targetNorm.contains("chuyen nghiep") || targetNorm == "doi tuong 2" || targetNorm == "dt2" ||
                roleNorm.contains("qncn") || roleNorm.contains("quan nhan chuyen nghiep") ||
                rankNorm.contains("qncn") || rankNorm.contains("chuyen nghiep")) {
                return "QNCN"
            }

            // 2. Kiểm tra SQ (Sĩ quan / Cán bộ / Đối tượng 1) - Tuyệt đối không chứa QNCN
            val isSQRank = listOf("thieu uy", "trung uy", "thuong uy", "dai uy", "thieu ta", "trung ta", "thuong ta", "dai ta", "chuan do doc", "pho do doc", "do doc", "tuong").any { rankNorm.contains(it) }
            val isSQTarget = targetNorm.contains("si quan") || targetNorm == "sq" || targetNorm.contains("can bo") || targetNorm == "doi tuong 1" || targetNorm == "dt1"
            val isSQRole = roleNorm.contains("si quan") || roleNorm == "sq" || roleNorm.contains("can bo")
            val hasQNCN = targetNorm.contains("qncn") || targetNorm.contains("quan nhan chuyen nghiep") || roleNorm.contains("qncn") || rankNorm.contains("qncn")

            if ((isSQTarget || isSQRole || isSQRank) && !hasQNCN) {
                return "SQ"
            }

            // 3. Kiểm tra Hạ sĩ quan - Binh sĩ / Chiến sĩ (Đối tượng 3)
            if (targetNorm.contains("ha si quan") || targetNorm.contains("binh si") || targetNorm.contains("chien si") || targetNorm.contains("hsq") || targetNorm == "doi tuong 3" || targetNorm == "dt3" ||
                roleNorm.contains("ha si quan") || roleNorm.contains("chien si") || roleNorm.contains("binh si") ||
                listOf("binh nhi", "binh nhat", "ha si", "trung si", "thuong si").any { rankNorm.contains(it) }) {
                return "HSQ_CS"
            }

            // 4. Kiểm tra Học viên (Đối tượng 4)
            if (targetNorm.contains("hoc vien") || targetNorm.contains("sinh vien") || targetNorm == "hv" || targetNorm == "doi tuong 4" || targetNorm == "dt4" ||
                roleNorm.contains("hoc vien") || roleNorm.contains("sinh vien") || roleNorm == "hv") {
                return "HV"
            }

            // 5. Kiểm tra Công nhân viên quốc phòng
            if (targetNorm.contains("cong nhan") || targetNorm.contains("cnvqp") || targetNorm.contains("cnqp") ||
                roleNorm.contains("cong nhan") || roleNorm.contains("cnvqp") || roleNorm.contains("cnqp")) {
                return "CNVQP"
            }

            // Mặc định dựa trên targetGroup / targetAudience nếu có giá trị riêng
            if (targetStr.isNotBlank()) return targetStr.uppercase()
            return "OTHER"
        }

        /**
         * So khớp loại đối tượng yêu cầu của đề thi với nhóm đối tượng của tài khoản
         */
        private fun matchAudienceCategory(target: String, userCategory: String, user: UserDoc): Boolean {
            val t = target.trim()
            val tNorm = removeAccents(t.lowercase()).replace("_", " ").replace("-", " ")

            // Áp dụng cho tất cả
            if (tNorm == "all" || tNorm == "tat ca" || tNorm == "toan quan" || tNorm == "toan don vi" || tNorm == "moi doi tuong" || tNorm == "*") {
                return true
            }

            // Nếu đợt thi áp dụng cho cả SQ và QNCN
            if ((tNorm.contains("sq") || tNorm.contains("si quan")) && (tNorm.contains("qncn") || tNorm.contains("quan nhan chuyen nghiep"))) {
                return userCategory == "SQ" || userCategory == "QNCN" || userCategory == "ALL"
            }

            // Đợt thi dành riêng cho SQ (Sĩ quan)
            val isTargetSQ = (tNorm == "sq" || tNorm.contains("si quan") || tNorm.contains("can bo") || tNorm.contains("doi tuong 1") || tNorm == "dt1") &&
                    !tNorm.contains("qncn") && !tNorm.contains("quan nhan chuyen nghiep")
            if (isTargetSQ) {
                return userCategory == "SQ" || userCategory == "ALL"
            }

            // Đợt thi dành riêng cho QNCN (Quân nhân chuyên nghiệp)
            val isTargetQNCN = tNorm == "qncn" || tNorm.contains("quan nhan chuyen nghiep") || tNorm.contains("chuyen nghiep") || tNorm.contains("doi tuong 2") || tNorm == "dt2"
            if (isTargetQNCN) {
                return userCategory == "QNCN" || userCategory == "ALL"
            }

            // Chặn nghiêm ngặt: QNCN không thể thấy SQ và SQ không thể thấy QNCN
            if (userCategory == "QNCN" && isTargetSQ) return false
            if (userCategory == "SQ" && isTargetQNCN) return false

            // Đợt thi dành cho Hạ sĩ quan - Chiến sĩ
            val isTargetHSQCS = tNorm.contains("ha si quan") || tNorm.contains("binh si") || tNorm.contains("chien si") || tNorm.contains("hsq") || tNorm.contains("doi tuong 3") || tNorm == "dt3"
            if (isTargetHSQCS) {
                return userCategory == "HSQ_CS" || userCategory == "ALL"
            }

            // Đợt thi dành cho Học viên
            val isTargetHV = tNorm.contains("hoc vien") || tNorm.contains("sinh vien") || tNorm == "hv" || tNorm.contains("doi tuong 4") || tNorm == "dt4"
            if (isTargetHV) {
                return userCategory == "HV" || userCategory == "ALL"
            }

            // Đợt thi dành cho CNVQP
            val isTargetCNQP = tNorm.contains("cong nhan") || tNorm.contains("cnvqp") || tNorm.contains("cnqp")
            if (isTargetCNQP) {
                return userCategory == "CNVQP" || userCategory == "ALL"
            }

            // So khớp trực tiếp chuỗi nếu là đối tượng tùy biến
            val userRawAudience = removeAccents((user.targetGroup.ifBlank { user.targetAudience }).trim().lowercase()).replace("_", " ").replace("-", " ")
            if (userRawAudience.isNotBlank()) {
                if (tNorm == userRawAudience) return true
                if (!isTargetSQ && !isTargetQNCN && (tNorm.contains(userRawAudience) || userRawAudience.contains(tNorm))) {
                    return true
                }
            }

            return false
        }

        fun removeAccents(input: String): String {
            val normalized = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD)
            return normalized.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "").replace("đ", "d").replace("Đ", "D")
        }

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

            // 1. Phân giải danh sách Loại Đối Tượng dự thi (targetGroup / targetGroups / target_group / ...)
            val rawAudience = doc.get("targetGroup")
                ?: doc.get("target_group")
                ?: doc.get("targetGroups")
                ?: doc.get("target_groups")
                ?: doc.get("TargetGroup")
                ?: doc.get("targetgroup")
                ?: doc.get("doiTuong")
                ?: doc.get("doiTuongThi")
                ?: doc.get("loaiDoiTuong")
                ?: doc.get("loai_doi_tuong")
                ?: doc.get("targetAudience")
                ?: doc.get("target_audience")
                ?: doc.get("targetAudiences")
                ?: doc.get("doiTuongs")
                ?: doc.get("nhomDoiTuong")
                ?: doc.get("audience")
                ?: doc.get("audiences")
                ?: doc.get("participants")
                ?: doc.get("roles")
                ?: doc.get("applicableRoles")
                ?: doc.get("targetUsers")
                ?: doc.get("userTypes")

            val audienceList: List<String> = when (rawAudience) {
                is List<*> -> rawAudience.mapNotNull { item ->
                    when (item) {
                        is Map<*, *> -> (item["name"] ?: item["value"] ?: item["code"] ?: item["title"] ?: item["label"] ?: item["id"])?.toString()?.trim()
                        else -> item?.toString()?.trim()
                    }
                }.filter { !it.isNullOrBlank() }
                is Map<*, *> -> {
                    val v = (rawAudience["name"] ?: rawAudience["value"] ?: rawAudience["code"] ?: rawAudience["title"] ?: rawAudience["label"] ?: rawAudience["id"])?.toString()?.trim()
                    if (!v.isNullOrBlank()) listOf(v) else emptyList()
                }
                is String -> {
                    if (rawAudience.isBlank()) emptyList()
                    else rawAudience.split(",", ";", "/").map { it.trim() }.filter { it.isNotBlank() }
                }
                else -> {
                    val s = rawAudience?.toString()?.trim() ?: ""
                    if (s.isNotBlank()) listOf(s) else emptyList()
                }
            }

            // Nếu trường đối tượng trong DB bị trống, kiểm tra tiêu đề và mô tả của đợt thi để tránh bài thi SQ bị hiển thị cho QNCN
            val examTitle = cleanHtml(doc.getString("title") ?: doc.getString("tenDotThi") ?: doc.getString("tieuDe") ?: "Đợt kiểm tra trực tuyến")
            val examDesc = cleanHtml(doc.getString("description") ?: doc.getString("moTa") ?: doc.getString("noiDung") ?: "")
            val combinedTextNorm = removeAccents("$examTitle $examDesc".lowercase())

            val hasSQInText = combinedTextNorm.contains("si quan") || combinedTextNorm.contains("doi tuong sq") || combinedTextNorm.contains("cho sq") || combinedTextNorm.contains("danh cho sq") || combinedTextNorm.contains("can bo")
            val hasQNCNInText = combinedTextNorm.contains("qncn") || combinedTextNorm.contains("quan nhan chuyen nghiep") || combinedTextNorm.contains("chuyen nghiep") || combinedTextNorm.contains("doi tuong qncn") || combinedTextNorm.contains("cho qncn") || combinedTextNorm.contains("danh cho qncn")

            val effectiveAudienceRaw = if (audienceList.isNotEmpty()) {
                audienceList
            } else {
                if (hasSQInText && !hasQNCNInText) {
                    listOf("SQ")
                } else if (hasQNCNInText && !hasSQInText) {
                    listOf("QNCN")
                } else {
                    emptyList()
                }
            }

            // 2. Phân giải danh sách Đơn Vị được phép dự thi (targetUnits / donVi)
            val rawUnits = doc.get("targetUnits")
                ?: doc.get("allowedUnits")
                ?: doc.get("units")
                ?: doc.get("donVi")
                ?: doc.get("donViThi")

            val unitList: List<String> = when (rawUnits) {
                is List<*> -> rawUnits.mapNotNull { it?.toString()?.trim() }.filter { it.isNotBlank() }
                is String -> {
                    if (rawUnits.isBlank()) emptyList()
                    else rawUnits.split(",", ";").map { it.trim() }.filter { it.isNotBlank() }
                }
                else -> emptyList()
            }

            val isAllAudience = effectiveAudienceRaw.isEmpty() || effectiveAudienceRaw.any {
                val a = removeAccents(it.trim().lowercase())
                a == "all" || a == "tat ca" || a == "toan quan" || a == "toan don vi" || a == "moi doi tuong" || a == "*"
            }

            val audienceDisplay = if (isAllAudience) {
                "Tất cả"
            } else {
                formatAudienceDisplay(effectiveAudienceRaw.joinToString(", "))
            }

            val effectiveAudienceList = if (isAllAudience) {
                listOf("ALL", "SQ", "QNCN", "HSQ-BS", "Tất cả")
            } else {
                effectiveAudienceRaw
            }

            return ExamSessionDoc(
                id = doc.id,
                title = examTitle,
                description = examDesc,
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
                maxAttempts = if (maxAtt > 0) maxAtt else 1,
                targetGroup = effectiveAudienceList,
                targetGroupText = audienceDisplay,
                targetAudience = effectiveAudienceList,
                targetAudienceText = audienceDisplay,
                targetUnits = unitList
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
    val timestamp: Long = System.currentTimeMillis(),
    val isOfficial: Boolean = false,
    val examType: String = ""
) {
    /**
     * Kiểm tra xem kết quả thi này có thuộc về Đợt thi / Bài kiểm tra chính thức (từ Web Quản trị) hay không.
     * Loại trừ các lượt "Luyện tập tự do", "Đề thi ngẫu nhiên", "Ôn tập".
     */
    fun isOfficialExam(examSessions: List<ExamSessionDoc> = emptyList()): Boolean {
        // 1. Nếu có đánh dấu rõ ràng loại bài thi
        val typeLower = examType.lowercase().trim()
        if (typeLower == "practice" || typeLower == "review" || typeLower == "luyen_tap" || typeLower == "on_tap" || typeLower == "free") {
            return false
        }
        if (isOfficial || typeLower == "official" || typeLower == "chinh_thuc") {
            return true
        }

        // 2. Kiểm tra từ khóa trong tên bài thi (các bài luyện tập, làm thử, đề thi 20 câu ngẫu nhiên)
        val nameLower = examName.lowercase().trim()
        if (nameLower.contains("ngẫu nhiên") ||
            nameLower.contains("luyện tập") ||
            nameLower.contains("ôn tập") ||
            nameLower.contains("tự do") ||
            nameLower.contains("làm thử") ||
            nameLower.contains("test thử") ||
            nameLower.startsWith("đề thi 20 câu ngẫu nhiên")
        ) {
            return false
        }

        // 3. Kiểm tra mã bài thi (examId)
        val idLower = examId.lowercase().trim()
        if (idLower.startsWith("random_practice") ||
            idLower.startsWith("practice") ||
            idLower.contains("practice") ||
            idLower.contains("luyen_tap") ||
            idLower.contains("on_tap") ||
            idLower.isBlank()
        ) {
            return false
        }

        // 4. Đối chiếu trực tiếp với danh sách các đợt thi chính thức từ Web Quản trị
        if (examSessions.any { it.id == examId || it.title.equals(examName, ignoreCase = true) }) {
            return true
        }

        // 5. Nếu có examId hợp lệ từ Web Quản trị (không phải random/practice)
        return examId.isNotBlank() && !idLower.startsWith("random")
    }

    companion object {
        fun fromDoc(doc: DocumentSnapshot): ExamResultDoc {
            val scoreVal = (doc.getLong("score") ?: doc.getLong("diem") ?: doc.getLong("soCauDung") ?: 0L).toInt()
            val totalVal = (doc.getLong("totalQuestions") ?: doc.getLong("tongSoCau") ?: doc.getLong("soCauHoi") ?: 20L).toInt()
            val percentVal = (doc.getLong("scorePercentage") ?: doc.getLong("phanTramDiem") ?: (if (totalVal > 0) scoreVal * 100 / totalVal else 0).toLong()).toInt()

            val isOff = doc.getBoolean("isOfficial") 
                ?: doc.getBoolean("chinhThuc") 
                ?: (safeString(doc, "examType", "type").equals("official", ignoreCase = true))
                ?: (safeString(doc, "loaiBaiThi").equals("chinh_thuc", ignoreCase = true))
            val exType = safeString(doc, "examType", "loaiBaiThi", "type")

            return ExamResultDoc(
                id = doc.id,
                userId = safeString(doc, "userId", "user_id", "nguoiDungId"),
                userName = safeString(doc, "userName", "hoTen", "tenHocVien").ifBlank { "Đồng chí" },
                userEmail = safeString(doc, "userEmail", "email"),
                userUnit = safeString(doc, "userUnit", "unit", "donVi").ifBlank { "Vùng 4 Hải Quân" },
                userRank = safeString(doc, "userRank", "rank", "capBac"),
                examId = safeString(doc, "examId", "dotThiId", "examSessionId"),
                examName = safeString(doc, "examName", "tenDotThi", "tenBaiThi").ifBlank { "Bài kiểm tra trắc nghiệm" },
                score = scoreVal,
                totalQuestions = totalVal,
                scorePercentage = percentVal,
                passed = doc.getBoolean("passed") ?: doc.getBoolean("dat") ?: (percentVal >= 50),
                timeSpentSeconds = (doc.getLong("timeSpentSeconds") ?: doc.getLong("thoiGianLamBai") ?: 0L).toInt(),
                timestamp = parseTime(doc.get("timestamp") ?: doc.get("thoiGianNop") ?: doc.get("createdAt")),
                isOfficial = isOff,
                examType = exType
            )
        }
    }
}

data class InternalBroadcastItem(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val audioUrl: String = "",
    val category: String = "Bản tin phát thanh",
    val durationText: String = "",
    val dateText: String = "",
    val broadcaster: String = "",
    val status: String = "PUBLISHED",
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun fromDoc(doc: DocumentSnapshot): InternalBroadcastItem {
            val title = doc.getString("title") 
                ?: doc.getString("tieuDe") 
                ?: doc.getString("name") 
                ?: doc.getString("tenBanTin") 
                ?: "Bản tin truyền thanh nội bộ"

            val broadcaster = doc.getString("broadcaster") 
                ?: doc.getString("coQuanPhat") 
                ?: doc.getString("nguoiDang") 
                ?: doc.getString("createdBy") 
                ?: ""

            var desc = doc.getString("description") 
                ?: doc.getString("moTa") 
                ?: doc.getString("noiDung") 
                ?: doc.getString("content") 
                ?: ""
            if (desc.isBlank() && broadcaster.isNotBlank()) {
                desc = broadcaster
            }

            val audioUrl = doc.getString("audioUrl") 
                ?: doc.getString("url") 
                ?: doc.getString("link") 
                ?: doc.getString("fileUrl") 
                ?: doc.getString("downloadUrl") 
                ?: doc.getString("audio") 
                ?: ""

            val catLabel = doc.getString("categoryLabel")
            val catRaw = doc.getString("category") ?: doc.getString("chuyenMuc") ?: doc.getString("theLoai") ?: ""
            val cat = when {
                !catLabel.isNullOrBlank() -> catLabel
                catRaw.equals("BAN_TIN_THOI_SU", ignoreCase = true) -> "Bản tin Thời sự Vùng"
                catRaw.equals("CHUYEN_MUC", ignoreCase = true) -> "Chuyên mục phát thanh"
                catRaw.equals("LOI_BAC_DAY", ignoreCase = true) -> "Lời Bác dạy ngày này năm xưa"
                catRaw.equals("KE_CHUYEN", ignoreCase = true) -> "Kể chuyện Truyền thống"
                catRaw.isNotBlank() -> catRaw
                else -> "Bản tin phát thanh"
            }

            val durationFormatted = doc.getString("durationFormatted")
            val durationSecs = doc.getLong("durationSeconds")
            val duration = when {
                !durationFormatted.isNullOrBlank() -> durationFormatted
                durationSecs != null && durationSecs > 0 -> {
                    val m = durationSecs / 60
                    val s = durationSecs % 60
                    String.format(java.util.Locale.US, "%02d:%02d", m, s)
                }
                else -> doc.getString("durationText") 
                    ?: doc.getString("thoiLuong") 
                    ?: doc.getString("duration") 
                    ?: "Phát thanh"
            }

            val broadcastDate = doc.getString("broadcastDate")
            val date = when {
                !broadcastDate.isNullOrBlank() -> {
                    try {
                        val parts = broadcastDate.split("-")
                        if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else broadcastDate
                    } catch (_: Exception) { broadcastDate }
                }
                else -> doc.getString("dateText") 
                    ?: doc.getString("ngayPhat") 
                    ?: doc.getString("date") 
                    ?: "Chính thức"
            }

            val time = parseTime(doc.get("createdAt") ?: doc.get("updatedAt") ?: doc.get("date") ?: doc.get("timestamp"))

            return InternalBroadcastItem(
                id = doc.id,
                title = title,
                description = desc,
                audioUrl = audioUrl,
                category = cat,
                durationText = duration,
                dateText = date,
                broadcaster = broadcaster,
                status = doc.getString("status") ?: "PUBLISHED",
                createdAt = time
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
    return com.example.util.TimeUtils.parseTime(value)
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

