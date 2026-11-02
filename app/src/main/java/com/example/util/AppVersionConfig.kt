package com.example.util

import com.example.BuildConfig

/**
 * CẤU HÌNH PHIÊN BẢN & TÙY CHỌN BẮT BUỘC CẬP NHẬT ỨNG DỤNG
 *
 * ============================================================================
 * HƯỚNG DẪN KHI PHÁT HÀNH PHIÊN BẢN MỚI (TĂNG PHIÊN BẢN):
 * ============================================================================
 * 1. Mở file `app/build.gradle.kts`, tăng `versionCode` (ví dụ từ 6 lên 7)
 *    và `versionName` (ví dụ từ "1.0.6" lên "1.0.7").
 *
 * 2. TÙY CHỌN BẮT BUỘC CẬP NHẬT TRONG CODE (`FORCE_UPDATE_REQUIRED`):
 *    - Đặt là `true`: Khi tăng phiên bản, TẤT CẢ các thiết bị đang chạy phiên bản cũ
 *      hơn BẮT BUỘC phải cập nhật lên phiên bản mới nhất mới được sử dụng ứng dụng.
 *      Màn hình khóa ứng dụng sẽ chặn toàn bộ truy cập, không thể đóng hay bỏ qua.
 *    - Đặt là `false`: Phiên bản cũ chỉ hiển thị thông báo gợi ý cập nhật, người dùng
 *      có thể chọn "Để sau" và vẫn tiếp tục sử dụng bình thường.
 *
 * 3. `AUTO_SYNC_TO_FIRESTORE = true`: Khi phiên bản mới khởi chạy, nó sẽ tự động
 *    đồng bộ cấu hình phiên bản này lên Firestore (collection `app_config`, doc `app_version`).
 *    Các máy đang cài bản cũ trên mạng sẽ lập tức nhận lệnh bắt buộc cập nhật!
 * ============================================================================
 */
object AppVersionConfig {

    /**
     * TÙY CHỌN TRONG CODE:
     * - `true`: Phiên bản cũ BẮT BUỘC phải cập nhật mới được sử dụng app.
     * - `false`: Phiên bản cũ không bắt buộc, chỉ nhắc nhở tùy chọn.
     */
    const val FORCE_UPDATE_REQUIRED: Boolean = true

    /**
     * Mã phiên bản hiện tại (versionCode trong build.gradle.kts).
     */
    val CURRENT_VERSION_CODE: Long = BuildConfig.VERSION_CODE.toLong()

    /**
     * Tên phiên bản hiện tại hiển thị cho người dùng (versionName trong build.gradle.kts).
     */
    val CURRENT_VERSION_NAME: String = BuildConfig.VERSION_NAME

    /**
     * Đường dẫn tải về hoặc trang ứng dụng trên Google Play Store để người dùng cập nhật.
     */
    const val UPDATE_DOWNLOAD_URL: String = "https://play.google.com/store/apps/details?id=com.aistudio.gdctv4.vungtu"

    /**
     * Tiêu đề thông báo cập nhật
     */
    const val UPDATE_TITLE: String = "Yêu cầu cập nhật phiên bản mới"

    /**
     * Thông điệp giải thích lý do cần cập nhật bắt buộc
     */
    const val UPDATE_MESSAGE: String = "Đã có phiên bản mới của ứng dụng với nhiều cải tiến quan trọng, cập nhật chuyên đề chính trị mới và nâng cao bảo mật hệ thống. Đồng chí bắt buộc phải nâng cấp lên phiên bản mới nhất để tiếp tục sử dụng ứng dụng."

    /**
     * Tự động đồng bộ cấu hình phiên bản mới lên Firebase Firestore để thông báo đến toàn bộ các máy cũ
     */
    const val AUTO_SYNC_TO_FIRESTORE: Boolean = true

    /**
     * CHẾ ĐỘ GIẢ LẬP KIỂM TRA (Dành cho lập trình viên thử nghiệm màn hình khóa cập nhật):
     * - Đặt `true`: App sẽ mô phỏng ngay lập tức trạng thái phiên bản cũ bị bắt buộc cập nhật
     *   để kiểm tra giao diện và nút bấm mà không cần phải giảm versionCode.
     * - Đặt `false` khi chạy thực tế.
     */
    const val DEBUG_SIMULATE_FORCE_UPDATE: Boolean = false
}
