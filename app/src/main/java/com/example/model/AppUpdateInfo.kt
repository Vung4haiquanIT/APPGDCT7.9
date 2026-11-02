package com.example.model

/**
 * Dữ liệu trạng thái phiên bản và chính sách cập nhật của ứng dụng
 */
data class AppUpdateInfo(
    val isUpdateAvailable: Boolean = false,
    val isForceUpdate: Boolean = false,
    val latestVersionCode: Long = 0L,
    val latestVersionName: String = "",
    val minRequiredVersionCode: Long = 0L,
    val currentVersionCode: Long = 0L,
    val currentVersionName: String = "",
    val updateUrl: String = "",
    val updateTitle: String = "Yêu cầu cập nhật phiên bản mới",
    val updateMessage: String = "Đã có phiên bản mới của ứng dụng. Đồng chí vui lòng cập nhật để tiếp tục sử dụng.",
    val releaseNotes: String = ""
)
