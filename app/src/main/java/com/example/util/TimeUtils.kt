package com.example.util

import android.util.Log
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Tiện ích xử lý và định dạng thời gian chuẩn Múi giờ Việt Nam (GMT+7, Asia/Ho_Chi_Minh)
 * Đảm bảo đồng bộ tuyệt đối giữa ứng dụng di động và Web Quản trị Vùng 4 Hải Quân.
 */
object TimeUtils {
    private const val TAG = "TimeUtils"

    val VIETNAM_TIME_ZONE: TimeZone = TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
    val VIETNAM_LOCALE: Locale = Locale.forLanguageTag("vi-VN")
    val UTC_TIME_ZONE: TimeZone = TimeZone.getTimeZone("UTC")

    /**
     * Định dạng thời gian hiển thị: dd/MM/yyyy HH:mm theo Múi giờ Việt Nam (GMT+7)
     */
    fun formatDateTime(timestamp: Long, pattern: String = "dd/MM/yyyy HH:mm"): String {
        if (timestamp <= 0L) return ""
        return try {
            val sdf = SimpleDateFormat(pattern, VIETNAM_LOCALE)
            sdf.timeZone = VIETNAM_TIME_ZONE
            sdf.format(Date(timestamp))
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Định dạng thời gian hiển thị: HH:mm - dd/MM/yyyy theo Múi giờ Việt Nam (GMT+7)
     */
    fun formatTimeAndDate(timestamp: Long, pattern: String = "HH:mm - dd/MM/yyyy"): String {
        if (timestamp <= 0L) return ""
        return try {
            val sdf = SimpleDateFormat(pattern, VIETNAM_LOCALE)
            sdf.timeZone = VIETNAM_TIME_ZONE
            sdf.format(Date(timestamp))
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Định dạng ngày: dd/MM/yyyy theo Múi giờ Việt Nam (GMT+7)
     */
    fun formatDate(timestamp: Long, pattern: String = "dd/MM/yyyy"): String {
        if (timestamp <= 0L) return ""
        return try {
            val sdf = SimpleDateFormat(pattern, VIETNAM_LOCALE)
            sdf.timeZone = VIETNAM_TIME_ZONE
            sdf.format(Date(timestamp))
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Phân tích thời gian từ bất kỳ kiểu dữ liệu nào (Firestore Timestamp, Number, String, Date)
     * Trả về milliseconds epoch UTC chính xác.
     */
    fun parseTime(value: Any?): Long {
        if (value == null) return 0L
        return when (value) {
            is Number -> {
                val num = value.toLong()
                // Nếu giá trị dạng giây Unix (10 chữ số, ví dụ ~1.79 tỷ ở năm 2026), nhân 1000 sang milliseconds
                if (num in 1_000_000_000L..99_999_999_999L) {
                    num * 1000L
                } else {
                    num
                }
            }
            is Timestamp -> {
                value.seconds * 1000L + (value.nanoseconds / 1_000_000L)
            }
            is Date -> value.time
            is String -> parseTimeString(value)
            else -> 0L
        }
    }

    private fun parseTimeString(raw: String): Long {
        val str = raw.trim()
        if (str.isEmpty()) return 0L

        // 1. Trường hợp chuỗi số nguyên (timestamp dạng chuỗi)
        str.toLongOrNull()?.let { num ->
            return if (num in 1_000_000_000L..99_999_999_999L) num * 1000L else num
        }

        // 2. Kiểm tra chuỗi định dạng ISO-8601 chứa UTC ('Z' hoặc offset '+00:00')
        val isUtc = str.endsWith("Z", ignoreCase = true) ||
                str.contains("+00:00") ||
                str.contains("+0000")

        if (isUtc || str.contains("T")) {
            // Thử parse ISO với UTC
            if (isUtc) {
                val isoCleaned = str
                    .replace(Regex("\\.\\d+"), "") // Bỏ phần milli-giây lẻ ví dụ .345
                    .replace("Z", "", ignoreCase = true)
                    .replace(Regex("[+-]\\d{2}:?\\d{2}$"), "")
                    .trim()

                try {
                    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                    sdf.timeZone = UTC_TIME_ZONE
                    val parsed = sdf.parse(isoCleaned)
                    if (parsed != null) return parsed.time
                } catch (_: Exception) {}

                try {
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                    sdf.timeZone = UTC_TIME_ZONE
                    val parsed = sdf.parse(isoCleaned)
                    if (parsed != null) return parsed.time
                } catch (_: Exception) {}
            }
        }

        // 3. Các mẫu ngày giờ phổ biến từ Web Quản trị và hệ thống Việt Nam (mặc định Múi giờ Việt Nam GMT+7)
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd",
            "HH:mm:ss dd/MM/yyyy",
            "HH:mm:ss dd/M/yyyy",
            "HH:mm dd/MM/yyyy",
            "HH:mm dd/M/yyyy",
            "dd/MM/yyyy HH:mm:ss",
            "dd/MM/yyyy HH:mm",
            "dd/MM/yyyy",
            "dd-MM-yyyy HH:mm:ss",
            "dd-MM-yyyy HH:mm",
            "dd-MM-yyyy"
        )

        val cleanStr = str.replace(Regex("\\.\\d+"), "").trim()

        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, VIETNAM_LOCALE)
                sdf.timeZone = VIETNAM_TIME_ZONE
                val date = sdf.parse(cleanStr)
                if (date != null) {
                    return date.time
                }
            } catch (_: Exception) {
                // Thử định dạng tiếp theo
            }
        }

        return 0L
    }
}
